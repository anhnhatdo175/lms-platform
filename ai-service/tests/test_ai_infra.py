"""Gói 1: fake LLM, embedders, Qdrant permission filter, constants, ZPD, API."""
import json

import numpy as np
import pytest
from fastapi.testclient import TestClient

from lmsrag import config
from lmsrag.api import Services, create_app
from lmsrag.embedder import HashEmbedder, make_embedder
from lmsrag.fake_llm import fake_generate
from lmsrag.layer1_data.learner_state import LearnerState
from lmsrag.layer4_recommender import zpd_score
from lmsrag.layer5_generation.generator import GenerationContext, LLMGenerator
from lmsrag.qdrant_store import ChunkRecord, QdrantChunkStore, make_client


# ---------- constants (Spec) ----------
def test_spec_constants():
    assert config.DIFFICULTY_LEVELS == (0.25, 0.5, 0.75)
    assert config.EMA_ALPHA == 0.3
    assert config.ZPD_DELTA == 0.10
    assert config.COLD_START_MIN_SIGNALS == 3
    assert config.COLD_START_MASTERY == 0.5


# ---------- Score_ZPD (cô's piecewise function) ----------
@pytest.mark.parametrize("delta,expected", [
    (0.05, 1.0), (0.30, 1.0), (0.10, 1.0),
    (0.0499, 0.7), (-0.10, 0.7), (0.0, 0.7),
    (0.3001, 0.5), (0.50, 0.5),
    (-0.1001, 0.3), (-0.9, 0.3),
    (0.5001, 0.2), (0.9, 0.2),
])
def test_zpd_boundaries(delta, expected):
    assert zpd_score(delta) == expected


# ---------- embedders ----------
def test_hash_embedder_deterministic_normalised():
    e = HashEmbedder()
    a, b = e.encode(["cộng phân số"]), e.encode(["cộng phân số"])
    assert np.array_equal(a, b)
    assert abs(float(np.linalg.norm(a[0])) - 1.0) < 1e-5
    assert e.encode_queries(["x"]).shape == (1, e.dim)


def test_make_embedder_selects_and_rejects():
    assert make_embedder("hash").name == "hash"
    with pytest.raises(ValueError):
        make_embedder("nope")


def test_e5_uses_prefixes(monkeypatch):
    from lmsrag import embedder as emb

    seen = []

    class Dummy:
        def encode(self, texts, **kw):
            seen.extend(texts)
            return np.ones((len(texts), 4), dtype="float32")

    e = emb.E5Embedder.__new__(emb.E5Embedder)
    e.model = Dummy()
    e.encode(["doc"]); e.encode_queries(["q"])
    assert seen == ["passage: doc", "query: q"]


# ---------- Qdrant store: permission filter ----------
@pytest.fixture()
def store():
    e = HashEmbedder()
    s = QdrantChunkStore(make_client(":memory:"), collection="t")
    chunks = [ChunkRecord(1, 10, 0, "cộng phân số cùng mẫu", skill_code="frac"),
              ChunkRecord(2, 20, 0, "cộng phân số khác mẫu", skill_code="frac"),
              ChunkRecord(3, 10, 0, "hình học tam giác", skill_code="geo"),
              ChunkRecord(4, 10, 0, "tài liệu chung về phân số và hình học")]
    s.upsert_chunks(chunks, e.encode([c.text for c in chunks]))
    return s, e.encode_queries(["cộng phân số"])[0]


def test_search_never_returns_other_courses(store):
    s, q = store
    assert {h["course_id"] for h in s.search(q, [10])} == {10}
    assert {h["course_id"] for h in s.search(q, [20])} == {20}


def test_empty_allow_list_returns_nothing(store):
    s, q = store
    assert s.search(q, []) == []
    assert s.search(q, [10], allowed_document_ids=[]) == []


def test_document_and_skill_filters_and_delete(store):
    s, q = store
    assert {h["document_id"] for h in s.search(q, [10, 20], allowed_document_ids=[2])} == {2}
    assert {h["skill_code"] for h in s.search(q, [10, 20], skill_code="geo")} == {"geo", None}
    s.delete_document(1)
    assert 1 not in {h["document_id"] for h in s.search(q, [10, 20])}


def test_skill_filter_keeps_untagged_course_material(store):
    # Spring ingest tài liệu tri thức KHÔNG kèm skill_code; lọc kỹ năng không được loại các chunk đó.
    s, q = store
    hits = s.search(q, [10], skill_code="frac", top_k=10)
    assert {h["document_id"] for h in hits} == {1, 4}
    assert all(h["skill_code"] in ("frac", None) for h in hits)


def test_dimension_mismatch_rejected(store):
    s, _ = store
    with pytest.raises(ValueError):
        s.ensure_collection(999)


# ---------- fake LLM ----------
def _context(allow_new=True, docs=True, candidates=False, mode="practice"):
    profile = {"skills": {"frac": {"mastery": 0.4}}}
    return GenerationContext(
        retrieved_docs=[{"doc_id": "1:0", "text": "Ví dụ", "skill_tag": "frac"}] if docs else [],
        candidate_tasks=[{"item_id": "q1", "skill_tag": "frac", "statement": "Tính 1/5 + 2/5",
                          "difficulty": 0.5, "response_type": "exact", "mapping_status": "mapped"}] if candidates else [],
        learner_profile=profile,
        constraints={"mode": mode, "target_skill": "frac", "target_difficulty": 0.5, "language": "vi",
                     "allow_new_task_generation": allow_new})


@pytest.fixture()
def fake(monkeypatch):
    monkeypatch.setattr(config, "LLM_BACKEND", "fake")


@pytest.mark.parametrize("kwargs", [
    dict(allow_new=True), dict(allow_new=False, candidates=True),
    dict(allow_new=False, candidates=True, mode="socratic"), dict(allow_new=True, mode="explanation"),
])
def test_fake_llm_passes_generator_validation(fake, kwargs):
    result = LLMGenerator().generate(_context(**kwargs))
    assert result["status"] == "ok" and result["safety"]["grounded"] is True


def test_fake_llm_is_deterministic(fake):
    a, b = LLMGenerator().generate(_context()), LLMGenerator().generate(_context())
    assert a == b


def test_fake_llm_insufficient_without_docs(fake):
    result = LLMGenerator().generate(_context(docs=False))
    assert result["status"] == "insufficient_context" and result["task"] is None


def test_fake_llm_never_leaks_answer_in_hints(fake):
    task = LLMGenerator().generate(_context())["task"]
    text = " ".join(task["hints"]) + task["personalization_reason"]
    assert str(task["answer_key"]) not in text


@pytest.mark.parametrize("skill,marker", [("fractions", "/"), ("decimals", ","), ("percentages", "%"),
                                          ("geo", "+")])
@pytest.mark.parametrize("difficulty", [0.25, 0.5, 0.75])
def test_fake_llm_task_matches_skill_and_passes_validation(fake, skill, marker, difficulty):
    ctx = GenerationContext(
        retrieved_docs=[{"doc_id": "1:0", "text": "Tài liệu chung", "skill_tag": None}],
        candidate_tasks=[], learner_profile={"skills": {skill: {"mastery": 0.5}}},
        constraints={"mode": "practice", "target_skill": skill, "target_difficulty": difficulty,
                     "language": "vi", "allow_new_task_generation": True})
    task = LLMGenerator().generate(ctx)["task"]
    assert task["skill_tag"] == skill and marker in task["prompt"]
    answer = str(task["answer_key"])
    assert all(answer not in h for h in task["hints"])
    if task["response_type"] == "exact":
        assert "/" in answer                       # phân số tối giản, chấm khớp chuỗi


def test_fake_generate_rejects_prompt_without_context():
    with pytest.raises(ValueError):
        fake_generate("no context here")


def test_openai_backend_requires_key(monkeypatch):
    from lmsrag import llm_client
    monkeypatch.setattr(config, "LLM_BACKEND", "openai")
    monkeypatch.setattr(config, "LLM_API_KEY", "")
    with pytest.raises(ValueError):
        llm_client.generate_answer("x")


# ---------- API ----------
@pytest.fixture()
def client(fake):
    svc = Services(HashEmbedder(), QdrantChunkStore(make_client(":memory:"), collection="api"))
    return TestClient(create_app(svc))


def _ingest(client, doc_id, course_id):
    return client.post("/v1/ingest", json={"document_id": doc_id, "course_id": course_id, "skill_code": "frac",
                                           "text": "Cộng phân số cùng mẫu: giữ mẫu, cộng tử số."})


GEN = {"query": "cộng phân số", "target_skill": "frac", "allow_new_task_generation": True,
       "learner_profile": {"skills": {"frac": {"mastery": 0.4}}}}


def test_api_health(client):
    assert client.get("/health").json()["status"] == "ok"


def test_api_ingest_retrieve_generate_respects_permissions(client):
    assert _ingest(client, 1, 10).json()["chunk_count"] >= 1
    assert _ingest(client, 2, 20).status_code == 200
    ok = client.post("/v1/retrieve", json={"query": "cộng phân số", "allowed_course_ids": [10]}).json()
    assert {c["course_id"] for c in ok["chunks"]} == {10}
    gen = client.post("/v1/generate", json={**GEN, "allowed_course_ids": [10]}).json()
    assert gen["result"]["status"] == "ok" and all(d.startswith("1:") for d in gen["retrieved_doc_ids"])
    denied = client.post("/v1/generate", json={**GEN, "allowed_course_ids": [99]}).json()
    assert denied["result"]["status"] == "insufficient_context" and denied["retrieved_doc_ids"] == []


def test_api_generate_works_with_documents_ingested_without_skill(client):
    # Đúng như Spring gửi: không có skill_code. Trước đây mọi lần generate trả insufficient_context.
    r = client.post("/v1/ingest", json={"document_id": 7, "course_id": 10,
                                         "text": "Cộng phân số cùng mẫu: giữ mẫu, cộng tử số."})
    assert r.status_code == 200
    gen = client.post("/v1/generate", json={**GEN, "allowed_course_ids": [10], "allowed_document_ids": [7]}).json()
    assert gen["result"]["status"] == "ok" and gen["retrieved_doc_ids"]


def test_api_reingest_replaces_and_delete_removes(client):
    _ingest(client, 1, 10); _ingest(client, 1, 10)
    hits = client.post("/v1/retrieve", json={"query": "cộng", "allowed_course_ids": [10], "top_k": 20}).json()["chunks"]
    assert len({(c["document_id"], c["chunk_index"]) for c in hits}) == len(hits)
    assert client.delete("/v1/documents/1").status_code == 204
    assert client.post("/v1/retrieve", json={"query": "cộng", "allowed_course_ids": [10]}).json()["chunks"] == []


def test_api_llm_failure_is_502_without_detail_leak(client, monkeypatch):
    _ingest(client, 1, 10)
    def boom(*a, **k):
        raise RuntimeError("secret upstream detail")
    monkeypatch.setattr("lmsrag.llm_client.generate_answer", boom)
    r = client.post("/v1/generate", json={**GEN, "allowed_course_ids": [10]})
    assert r.status_code == 502 and "secret" not in r.text


def test_api_invalid_llm_output_is_422(client):
    _ingest(client, 1, 10)
    svc = Services(HashEmbedder(), QdrantChunkStore(make_client(":memory:"), collection="bad"))
    bad = TestClient(create_app(svc, generate_fn=lambda prompt, **k: json.dumps({"oops": 1})))
    bad.post("/v1/ingest", json={"document_id": 1, "course_id": 10, "skill_code": "frac", "text": "Cộng phân số."})
    assert bad.post("/v1/generate", json={**GEN, "allowed_course_ids": [10]}).status_code == 422


def test_api_internal_token(client, monkeypatch):
    monkeypatch.setenv("AI_SERVICE_TOKEN", "s3cret")
    body = {"query": "x", "allowed_course_ids": [1]}
    assert client.post("/v1/retrieve", json=body).status_code == 401
    assert client.post("/v1/retrieve", json=body, headers={"X-Internal-Token": "s3cret"}).status_code == 200
    assert client.get("/health").status_code == 200


# ---------- answer-leak regex (answer followed by sentence punctuation) ----------
def test_hint_revealing_answer_before_a_period_is_rejected(fake):
    import json as _json
    context = _context()
    good = LLMGenerator().generate(context)
    leaky = _json.loads(_json.dumps(good))
    leaky["task"]["hints"] = [f"Đáp án là {leaky['task']['answer_key']}."]
    leaky["safety"]["contains_final_answer"] = False
    calls = {"n": 0}

    def llm(prompt, **kwargs):
        calls["n"] += 1
        return _json.dumps(leaky if calls["n"] <= 2 else good)

    with pytest.raises(ValueError, match="final answer"):
        LLMGenerator(llm).generate(context)


def test_rerank_prefers_target_skill_but_keeps_strong_general_chunks():
    from lmsrag.api import rerank_for_skill
    hits = [{"doc_id": "a", "score": 0.80, "skill_code": None},
            {"doc_id": "b", "score": 0.78, "skill_code": "frac"},
            {"doc_id": "c", "score": 0.60, "skill_code": "frac"},
            {"doc_id": "d", "score": 0.90, "skill_code": None}]
    assert [h["doc_id"] for h in rerank_for_skill(hits, "frac", 3)] == ["d", "b", "a"]
    assert [h["doc_id"] for h in rerank_for_skill(hits, None, 2)] == ["d", "a"]
