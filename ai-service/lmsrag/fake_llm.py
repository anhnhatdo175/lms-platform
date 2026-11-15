"""Deterministic "fake LLM" for tests and demos (LLM_BACKEND=fake).

It never touches the network. It reads the CONTEXT_JSON block that
``LLMGenerator._build_prompt`` embeds in the prompt and returns a response that satisfies the
Layer 5 schema and validator, so the full pipeline can run without an external model. The output
is a pure function of the context: the same context always yields the same JSON.
"""
from __future__ import annotations

import hashlib
import json
from typing import Any

_MARKER = "CONTEXT_JSON:\n"


def _extract_context(prompt: str) -> dict[str, Any]:
    start = prompt.find(_MARKER)
    if start < 0:
        raise ValueError("fake LLM: prompt has no CONTEXT_JSON block")
    line = prompt[start + len(_MARKER):].split("\n", 1)[0]
    try:
        context = json.loads(line)
    except json.JSONDecodeError as exc:
        raise ValueError("fake LLM: CONTEXT_JSON is not valid JSON") from exc
    if not isinstance(context, dict):
        raise ValueError("fake LLM: CONTEXT_JSON must be an object")
    return context


def _profile_paths(profile: Any, prefix: str = "") -> list[str]:
    paths: list[str] = []
    if isinstance(profile, dict):
        for key in sorted(profile, key=str):
            child = f"{prefix}.{key}" if prefix else str(key)
            paths.extend(_profile_paths(profile[key], child))
    elif profile is not None and prefix:
        paths.append(prefix)
    return paths


def _seed(*parts: Any) -> int:
    digest = hashlib.sha256("|".join(str(p) for p in parts).encode("utf-8")).digest()
    return int.from_bytes(digest[:8], "big")


def _insufficient(mode: str) -> dict[str, Any]:
    return {
        "schema_version": "1.0", "status": "insufficient_context", "mode": mode,
        "task": None, "tutor_turn": None,
        "safety": {"contains_final_answer": False, "grounded": False},
    }


def fake_generate(prompt: str) -> str:
    """Return a schema-valid JSON string for the context embedded in ``prompt``."""
    context = _extract_context(prompt)
    constraints = context.get("constraints") or {}
    mode = constraints.get("mode", "practice")
    docs = context.get("retrieved_docs") or []
    candidates = context.get("candidate_tasks") or []
    paths = _profile_paths(context.get("learner_profile") or {})
    allow_new = bool(constraints.get("allow_new_task_generation", False))

    if not docs or not paths or (not allow_new and not candidates):
        return json.dumps(_insufficient(mode), ensure_ascii=False)

    if allow_new:
        skill = constraints.get("target_skill") or docs[0].get("skill_tag")
        if not skill:
            return json.dumps(_insufficient(mode), ensure_ascii=False)
        difficulty = float(constraints.get("target_difficulty", 0.5))
        seed = _seed(skill, difficulty, *sorted(str(d.get("doc_id")) for d in docs))
        a, b = 10 + seed % 40, 10 + (seed // 40) % 40
        task: dict[str, Any] = {
            "item_id": "new_task", "skill_tag": skill, "difficulty": difficulty,
            "prompt": f"Tính giá trị của biểu thức {a} + {b}.", "response_type": "numeric",
            "answer_key": a + b,
        }
    else:
        candidate = candidates[0]
        skill = candidate.get("skill_tag")
        task = {
            "item_id": str(candidate["item_id"]), "skill_tag": skill,
            "difficulty": candidate.get("difficulty", 0.5),
            "prompt": candidate.get("statement", candidate.get("prompt", "")),
            "response_type": candidate.get("response_type", "exact"),
        }
    supporting = [str(d["doc_id"]) for d in docs if d.get("skill_tag") in (None, skill)]
    if not supporting:
        return json.dumps(_insufficient(mode), ensure_ascii=False)
    task.update({
        "hints": ["Xác định phép tính cần dùng, rồi làm từng bước.", "Kiểm tra lại kết quả sau khi tính."],
        "personalization_reason": "Bài được chọn theo mức thành thạo hiện tại của bạn ở kỹ năng này.",
        "personalization_basis": [paths[0]],
        "supporting_doc_ids": supporting[:1],
    })
    tutor_turn = None
    if mode == "socratic":
        tutor_turn = {"turn_index": 1, "question": "Bước đầu tiên bạn sẽ làm là gì?",
                      "follow_up_questions": [], "wait_for_learner": True}
    result = {
        "schema_version": "1.0", "status": "ok", "mode": mode, "task": task,
        "tutor_turn": tutor_turn, "safety": {"contains_final_answer": False, "grounded": True},
    }
    return json.dumps(result, ensure_ascii=False)
