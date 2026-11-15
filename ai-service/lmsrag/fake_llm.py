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


def _vn(x: float) -> str:
    """Số kiểu Việt Nam: dấu phẩy thập phân, bỏ số 0 thừa."""
    text = f"{x:.4f}".rstrip("0").rstrip(".")
    return text.replace(".", ",")


def _level(difficulty: float) -> int:
    return 0 if difficulty < 0.4 else (1 if difficulty < 0.65 else 2)


def _skill_kind(skill: str) -> str:
    key = skill.casefold()
    if any(k in key for k in ("frac", "phan_so", "phanso", "phân số")):
        return "fraction"
    if any(k in key for k in ("dec", "thap_phan", "thapphan", "thập phân")):
        return "decimal"
    if any(k in key for k in ("percent", "phan_tram", "phantram", "phần trăm")):
        return "percent"
    return "generic"


_HINTS = {
    "fraction": ["Xem hai phân số đã cùng mẫu chưa; nếu chưa, hãy quy đồng trước.",
                 "Rút gọn kết quả bằng cách chia cả tử và mẫu cho ước chung lớn nhất."],
    "decimal": ["Đặt các chữ số thẳng cột theo dấu phẩy, hoặc làm như số tự nhiên rồi đặt lại dấu phẩy.",
                "Đếm số chữ số ở phần thập phân để đặt dấu phẩy cho đúng."],
    "percent": ["Đổi phần trăm thành phân số có mẫu là một trăm.",
                "Nhân số đã cho với phân số đó, rồi kiểm tra lại kết quả có hợp lý không."],
    "generic": ["Xác định phép tính cần dùng, rồi làm từng bước.", "Kiểm tra lại kết quả sau khi tính."],
}


def _new_task(skill: str, difficulty: float, seed: int) -> tuple[str, str, Any, list[str]]:
    """(prompt, response_type, answer_key, hints): đề tất định, đúng kỹ năng và mức độ khó."""
    from fractions import Fraction

    kind, level = _skill_kind(skill), _level(difficulty)
    r = [(seed >> (8 * i)) & 0xFF for i in range(8)]
    if kind == "fraction":
        den = (7, 11, 13)[r[0] % 3]
        if level == 0:
            a = 1 + r[1] % (den // 2)
            b = 1 + r[2] % (den - a - 1)
            return (f"Tính {a}/{den} + {b}/{den}. Viết kết quả dưới dạng phân số tối giản a/b.",
                    "exact", f"{a + b}/{den}", _HINTS[kind])
        if level == 1:
            a = den // 2 + 1 + r[1] % (den // 2 - 1)
            b = 1 + r[2] % (a - 1)
            return (f"Tính {a}/{den} - {b}/{den}. Viết kết quả dưới dạng phân số tối giản a/b.",
                    "exact", f"{a - b}/{den}", _HINTS[kind])
        x = Fraction(2 + r[1] % 5, 3 + r[2] % 6)
        y = Fraction(2 + r[3] % 7, 3 + r[4] % 8)
        prod = x * y
        prompt = (f"Tính {x.numerator}/{x.denominator} × {y.numerator}/{y.denominator}. "
                  "Viết kết quả dưới dạng phân số tối giản a/b.")
        if prod.denominator == 1:
            return prompt, "numeric", prod.numerator, _HINTS[kind]
        return prompt, "exact", f"{prod.numerator}/{prod.denominator}", _HINTS[kind]
    if kind == "decimal":
        a = (10 * (1 + r[1] % 9) + 1 + r[5] % 9) / 10      # luôn có phần thập phân khác 0
        b = (10 * (1 + r[2] % 4) + 1 + r[6] % 9) / 10
        if level == 0:
            return f"Tính {_vn(a)} + {_vn(b)}.", "numeric", round(a + b, 4), _HINTS[kind]
        if level == 1:
            k = 2 + r[3] % 8
            return f"Tính {_vn(a)} × {k}.", "numeric", round(a * k, 4), _HINTS[kind]
        q = 2 + r[3] % 8
        return f"Tính {_vn(round(b * q, 4))} : {_vn(b)}.", "numeric", q, _HINTS[kind]
    if kind == "percent":
        if level == 0:
            p, n = (10, 20, 50)[r[1] % 3], 100 * (1 + r[2] % 9)
            return f"Tìm {p}% của {n}.", "numeric", n * p // 100, _HINTS[kind]
        if level == 1:
            p, n = (15, 25, 35, 45)[r[1] % 4], 200 * (1 + r[2] % 9)
            return f"Tìm {p}% của {n}.", "numeric", n * p // 100, _HINTS[kind]
        p, price = (10, 15, 20, 25, 30)[r[1] % 5], 20000 * (5 + r[2] % 20)
        return (f"Một món hàng giá {price:,} đồng được giảm {p}%. Hỏi giá sau khi giảm là bao nhiêu đồng?"
                .replace(",", "."), "numeric", price * (100 - p) // 100, _HINTS[kind])
    a, b = 10 + r[1] % 40, 10 + r[2] % 40
    return f"Tính giá trị của biểu thức {a} + {b}.", "numeric", a + b, _HINTS[kind]


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
        prompt_text, response_type, answer, hints = _new_task(str(skill), difficulty, seed)
        task: dict[str, Any] = {
            "item_id": "new_task", "skill_tag": skill, "difficulty": difficulty,
            "prompt": prompt_text, "response_type": response_type, "answer_key": answer,
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
    if not allow_new:
        hints = _HINTS[_skill_kind(str(skill))]
    task.update({
        "hints": list(hints),
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
