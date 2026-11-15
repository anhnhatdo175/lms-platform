"""Internal HTTP API of the AI service: ``uvicorn lmsrag.api:app``.

Stateless: no MySQL, no user store, no authorization decisions. Spring Boot authenticates the
learner, computes which courses/documents they may read (BR-AI-01) and sends that list with every
retrieve/generate call. This service only retrieves inside that list and generates + validates.
Set ``AI_SERVICE_TOKEN`` to require the ``X-Internal-Token`` header on every /v1 call.
"""
from __future__ import annotations

import hmac
import logging
import os
from typing import Any, Literal

import numpy as np
from fastapi import Depends, FastAPI, Header, HTTPException, Query, Request
from pydantic import BaseModel, Field

from lmsrag import config
from lmsrag.embedder import make_embedder
from lmsrag.extract import ExtractError, extract_text
from lmsrag.layer2_embedding.chunk import chunk_text
from lmsrag.layer5_generation.generator import GenerationContext, LLMGenerator
from lmsrag.qdrant_store import ChunkRecord, QdrantChunkStore, make_client

log = logging.getLogger("lmsrag.api")


class IngestRequest(BaseModel):
    document_id: int
    course_id: int
    lesson_id: int | None = None
    skill_code: str | None = Field(default=None, max_length=100)
    text: str = Field(min_length=1, max_length=2_000_000)
    title: str = ""


class IngestResponse(BaseModel):
    document_id: int
    chunk_count: int
    embedder: str


class ExtractResponse(BaseModel):
    text: str
    char_count: int


class RetrieveRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    allowed_course_ids: list[int]
    allowed_document_ids: list[int] | None = None
    skill_code: str | None = None
    top_k: int = Field(default=5, ge=1, le=20)


class RetrievedChunk(BaseModel):
    doc_id: str
    document_id: int
    course_id: int
    chunk_index: int
    skill_code: str | None = None
    text: str
    score: float


class RetrieveResponse(BaseModel):
    chunks: list[RetrievedChunk]


class CandidateTask(BaseModel):
    """A question-bank item Spring already authorised. Never include the answer key."""
    item_id: str
    skill_tag: str
    difficulty: float | Literal["EASY", "MEDIUM", "HARD"]
    statement: str
    response_type: Literal["exact", "numeric", "text", "multiple_choice"] = "exact"


class GenerateRequest(BaseModel):
    query: str = Field(min_length=1, max_length=2000)
    allowed_course_ids: list[int]
    allowed_document_ids: list[int] | None = None
    target_skill: str
    target_difficulty: float = Field(default=0.5, ge=0, le=1)
    mode: Literal["practice", "explanation", "socratic"] = "practice"
    language: str = "vi"
    allow_new_task_generation: bool = False
    learner_profile: dict[str, Any]
    candidate_tasks: list[CandidateTask] = []
    top_k: int = Field(default=5, ge=1, le=10)


class GenerateResponse(BaseModel):
    result: dict[str, Any]
    retrieved_doc_ids: list[str]
    llm_backend: str


class Services:
    """Lazy holders so importing the app never loads a model or contacts Qdrant."""

    def __init__(self, embedder: Any = None, store: QdrantChunkStore | None = None):
        self._embedder, self._store = embedder, store

    @property
    def embedder(self):
        if self._embedder is None:
            self._embedder = make_embedder(os.environ.get("EMBEDDER", "hash"))
        return self._embedder

    @property
    def store(self) -> QdrantChunkStore:
        if self._store is None:
            self._store = QdrantChunkStore(make_client())
        return self._store


SKILL_MATCH_BONUS = 0.05
OVERFETCH = 3


def rerank_for_skill(hits: list[dict[str, Any]], target_skill: str | None, top_k: int) -> list[dict[str, Any]]:
    """Ưu tiên chunk gắn đúng kỹ năng mục tiêu hơn tài liệu chung (lọc kỹ năng đã là lọc mềm).

    Điểm = độ tương đồng + SKILL_MATCH_BONUS nếu khớp kỹ năng; giữ thứ tự ổn định khi bằng điểm.
    """
    def key(item: tuple[int, dict[str, Any]]) -> tuple[float, int]:
        idx, h = item
        bonus = SKILL_MATCH_BONUS if target_skill and h.get("skill_code") == target_skill else 0.0
        return (-(float(h["score"]) + bonus), idx)
    return [h for _, h in sorted(enumerate(hits), key=key)][:top_k]


def _difficulty_value(value: float | str) -> float:
    if isinstance(value, str):
        return dict(zip(("EASY", "MEDIUM", "HARD"), config.DIFFICULTY_LEVELS))[value]
    return float(value)


def create_app(services: Services | None = None, generate_fn: Any = None) -> FastAPI:
    svc = services or Services()
    app = FastAPI(title="LMS AI service", version="0.1.0")

    def require_token(x_internal_token: str | None = Header(default=None)) -> None:
        expected = os.environ.get("AI_SERVICE_TOKEN", "")
        if expected and not hmac.compare_digest(x_internal_token or "", expected):
            raise HTTPException(status_code=401, detail={"code": "UNAUTHORIZED"})

    @app.get("/health")
    def health() -> dict[str, str]:
        return {"status": "ok", "llm_backend": config.LLM_BACKEND,
                "embedder": os.environ.get("EMBEDDER", "hash")}

    @app.post("/v1/ingest", response_model=IngestResponse, dependencies=[Depends(require_token)])
    def ingest(req: IngestRequest) -> IngestResponse:
        pieces = [c for c in chunk_text(req.text) if c.strip()]
        vectors = np.asarray(svc.embedder.encode(pieces), dtype="float32")
        records = [ChunkRecord(req.document_id, req.course_id, i, text, req.lesson_id, req.skill_code)
                   for i, text in enumerate(pieces)]
        svc.store.ensure_collection(int(vectors.shape[1]))
        svc.store.delete_document(req.document_id)        # idempotent: re-ingest replaces old chunks
        svc.store.upsert_chunks(records, vectors)
        return IngestResponse(document_id=req.document_id, chunk_count=len(records),
                              embedder=getattr(svc.embedder, "name", "unknown"))

    @app.post("/v1/extract", response_model=ExtractResponse, dependencies=[Depends(require_token)])
    async def extract(request: Request, filename: str = Query(min_length=1, max_length=255)) -> ExtractResponse:
        body = await request.body()
        try:
            text = extract_text(filename, body)
        except ExtractError as exc:
            raise HTTPException(status_code=422, detail={"code": "EXTRACT_FAILED", "message": str(exc)})
        return ExtractResponse(text=text, char_count=len(text))

    @app.delete("/v1/documents/{document_id}", status_code=204, dependencies=[Depends(require_token)])
    def delete_document(document_id: int) -> None:
        svc.store.delete_document(document_id)

    def _retrieve(query: str, courses, documents, skill, top_k) -> list[dict[str, Any]]:
        vec = np.asarray(svc.embedder.encode_queries([query]), dtype="float32")[0]
        return svc.store.search(vec, courses, documents, top_k=top_k, skill_code=skill)

    @app.post("/v1/retrieve", response_model=RetrieveResponse, dependencies=[Depends(require_token)])
    def retrieve(req: RetrieveRequest) -> RetrieveResponse:
        hits = _retrieve(req.query, req.allowed_course_ids, req.allowed_document_ids,
                         req.skill_code, req.top_k)
        return RetrieveResponse(chunks=[RetrievedChunk(**{k: h[k] for k in RetrievedChunk.model_fields}) for h in hits])

    @app.post("/v1/generate", response_model=GenerateResponse, dependencies=[Depends(require_token)])
    def generate(req: GenerateRequest) -> GenerateResponse:
        hits = rerank_for_skill(_retrieve(req.query, req.allowed_course_ids, req.allowed_document_ids,
                                          req.target_skill, req.top_k * OVERFETCH), req.target_skill, req.top_k)
        docs = [{"doc_id": h["doc_id"], "text": h["text"], "skill_tag": h.get("skill_code"),
                 "course_id": h["course_id"]} for h in hits]
        tasks = [{"item_id": t.item_id, "skill_tag": t.skill_tag, "statement": t.statement,
                  "difficulty": _difficulty_value(t.difficulty), "response_type": t.response_type,
                  "mapping_status": "mapped"} for t in req.candidate_tasks if t.skill_tag == req.target_skill]
        constraints = {"mode": req.mode, "language": req.language, "target_skill": req.target_skill,
                       "target_difficulty": req.target_difficulty,
                       "allow_new_task_generation": req.allow_new_task_generation}
        context = GenerationContext(retrieved_docs=docs, candidate_tasks=tasks[:5],
                                    learner_profile=req.learner_profile, constraints=constraints)
        try:
            result = LLMGenerator(generate_fn).generate(context)
        except ValueError as exc:                          # model output failed validation
            log.warning("generation output rejected: %s", exc)
            raise HTTPException(status_code=422, detail={"code": "AI_OUTPUT_INVALID"}) from exc
        except Exception as exc:                           # model/network failure: LMS must keep working (BR-AI-11)
            log.error("llm backend failure: %s", type(exc).__name__)
            raise HTTPException(status_code=502, detail={"code": "AI_LLM_UNAVAILABLE"}) from exc
        return GenerateResponse(result=result, retrieved_doc_ids=[d["doc_id"] for d in docs],
                                llm_backend=config.LLM_BACKEND)

    return app


app = create_app()
