"""Kiểm tra nhanh, không cần dataset ASSISTments: đường đánh giá baseline và script dữ liệu demo."""
import importlib.util
import sys
from pathlib import Path

import pytest

REPO = Path(__file__).resolve().parents[2]
sys.path.insert(0, str(REPO / "research" / "benchmark"))


def _load_demo():
    spec = importlib.util.spec_from_file_location("gen_demo", REPO / "research" / "demo" / "generate_demo_sql.py")
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    return module


def test_required_baselines_are_scored_on_the_same_queries():
    pytest.importorskip("sklearn")
    pytest.importorskip("scipy")
    from experiments.assistments import evaluate_ranking_runs

    relevant = {f"q{i}": {"a", "b"} for i in range(20)}
    perfect = {k: ["a", "b", "c", "d", "e"] for k in relevant}
    worst = {k: ["c", "d", "e", "f", "g"] for k in relevant}
    report = evaluate_ranking_runs(
        {"PureCB-ZPD": perfect, "Random": worst, "TraditionalRAG": worst}, relevant, k=5)
    assert set(report) == {"PureCB-ZPD", "Random", "TraditionalRAG"}
    first = next(iter(report["PureCB-ZPD"]))
    assert report["PureCB-ZPD"][first] >= report["Random"][first]


def test_demo_ema_matches_backend_formula():
    gen = _load_demo()
    # Bằng chứng đầu: 0.7*0.5 + 0.3*1 = 0.65; không cần trạng thái DB mặc định.
    states = gen.replay([("fractions", "EASY", 1, 0, 3)])
    assert states["fractions"] == [pytest.approx(0.65), 1]
    # Đúng có gợi ý bị phạt: outcome = 1 - 0.3*2 = 0.4
    assert gen.outcome(1, 2) == pytest.approx(0.4)
    assert gen.outcome(0, 2) == 0.0


def test_demo_profiles_cover_strong_weak_and_cold_start():
    gen = _load_demo()
    strong = gen.replay(gen.HISTORY["strong"])
    weak = gen.replay(gen.HISTORY["weak"])
    assert all(m >= 0.7 for s, (m, n) in strong.items() if s == "fractions")
    assert weak["fractions"][0] < 0.4 and weak["fractions"][1] >= 3
    assert weak["percentages"][1] < 3        # cold start theo kỹ năng
    assert gen.HISTORY["cold"] == []


def test_demo_sql_is_valid_mysql_and_in_sync_with_generator(capsys):
    sqlglot = pytest.importorskip("sqlglot")
    gen = _load_demo()
    gen.main()
    generated = capsys.readouterr().out
    assert generated == (REPO / "research" / "demo" / "seed_demo_ai.sql").read_text(encoding="utf-8")
    assert [s for s in sqlglot.parse(generated, read="mysql") if s]
    assert "password" not in generated.lower()
