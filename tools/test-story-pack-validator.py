#!/usr/bin/env python3
"""Testes adversariais do validador semântico de LearningStoryPack."""

from __future__ import annotations

import copy
import importlib.util
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
module_spec = importlib.util.spec_from_file_location(
    "story_pack_validator", ROOT / "tools" / "validate-story-pack.py"
)
validator = importlib.util.module_from_spec(module_spec)
assert module_spec and module_spec.loader
module_spec.loader.exec_module(validator)

example = json.loads(
    (ROOT / "docs/v2/contracts/example-apple-story-pack.json").read_text(encoding="utf-8")
)


def codes(pack):
    return {issue["code"] for issue in validator.validate(pack)}


assert validator.validate(example) == [], "o exemplo oficial precisa ser válido"

broken = copy.deepcopy(example)
broken["nodes"][0]["nextNodeId"] = "nao_existe"
assert {"next_not_found", "dead_end", "end_not_reached", "unreachable_nodes"} <= codes(broken)

broken = copy.deepcopy(example)
word_node = next(node for node in broken["nodes"] if node["type"] == "WORD_BUILDER")
word_node["letterTiles"] = ["M", "A"]
assert "word_cannot_be_built" in codes(broken)

broken = copy.deepcopy(example)
broken["accessibility"]["minTouchTargetDp"] = 32
assert "touch_target_too_small" in codes(broken)

broken = copy.deepcopy(example)
broken["nodes"][0]["dialogue"][0]["text"] = "Você errou. Preste atenção."
assert {"punitive_error", "guilt"} <= codes(broken)

broken = copy.deepcopy(example)
broken["provenance"]["assetOrigins"][0]["reviewedByTeacher"] = False
assert "asset_not_reviewed" in codes(broken)

broken = copy.deepcopy(example)
broken["nodes"][0]["supports"] = list(reversed(broken["nodes"][0]["supports"]))
assert "support_order" in codes(broken)

print("Validador semântico: exemplo válido e 6 mutações adversariais bloqueadas.")
