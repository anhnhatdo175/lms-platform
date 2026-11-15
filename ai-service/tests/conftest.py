import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
os.environ.setdefault("USE_TF", "0")

# test_layer1 reads the ASSISTments CSV (research/benchmark/data, not committed).
if not (Path(__file__).resolve().parents[2] / "research" / "benchmark" / "data" / "skill_builder_data.csv").exists():
    collect_ignore = ["test_layer1.py"]
