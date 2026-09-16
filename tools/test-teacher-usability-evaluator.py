#!/usr/bin/env python3
"""Testes determinísticos dos estados do estudo de usabilidade docente."""

from __future__ import annotations

import copy
import importlib.util
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
module_spec = importlib.util.spec_from_file_location(
    "teacher_usability_evaluator", ROOT / "tools" / "evaluate-teacher-usability.py"
)
evaluator = importlib.util.module_from_spec(module_spec)
assert module_spec and module_spec.loader
module_spec.loader.exec_module(evaluator)


def task(task_id: str, outcome: str = "WITHOUT_HELP") -> dict:
    return {
        "taskId": task_id,
        "outcome": outcome,
        "durationSeconds": 60,
        "deviations": [],
        "stateConfusions": [],
        "moderatorHelp": None,
        "findings": [],
    }


def session(index: int) -> dict:
    return {
        "participantId": f"P0{index}",
        "deviceClass": "DESKTOP",
        "digitalFamiliarity": "MEDIUM",
        "openedAdministration": False,
        "confusedPublishWithReady": False,
        "tasks": [task(task_id) for task_id in sorted(evaluator.TASK_IDS)],
        "finalComments": [],
    }


blank = {"schemaVersion": "1.0", "prototypeCommit": "fdafbb1", "sessions": []}
assert evaluator.evaluate(blank)["status"] == "NOT_RUN"

complete = copy.deepcopy(blank)
complete["sessions"] = [session(index) for index in range(1, 6)]
passed = evaluator.evaluate(complete)
assert passed["status"] == "PASSED"
assert all(passed["criteria"].values())

incomplete = copy.deepcopy(complete)
incomplete["sessions"].pop()
assert evaluator.evaluate(incomplete)["status"] == "INCOMPLETE"

needs_revision = copy.deepcopy(complete)
needs_revision["sessions"][0]["confusedPublishWithReady"] = True
needs_revision["sessions"][1]["confusedPublishWithReady"] = True
assert evaluator.evaluate(needs_revision)["status"] == "NEEDS_REVISION"

needs_revision = copy.deepcopy(complete)
needs_revision["sessions"][0]["tasks"][0]["findings"] = [
    {"severity": "BLOCKER", "observation": "Não encontrou como interromper a publicação."}
]
assert evaluator.evaluate(needs_revision)["status"] == "NEEDS_REVISION"

needs_revision = copy.deepcopy(complete)
for participant in needs_revision["sessions"][:2]:
    participant["tasks"][0]["outcome"] = "WITH_HELP"
assert evaluator.evaluate(needs_revision)["status"] == "NEEDS_REVISION"

invalid = copy.deepcopy(complete)
invalid["sessions"][1]["participantId"] = "P01"
try:
    evaluator.evaluate(invalid)
except SystemExit as error:
    assert str(error) == "participantId duplicado"
else:
    raise AssertionError("participante duplicado deveria ser bloqueado")

invalid = copy.deepcopy(complete)
invalid["sessions"][0]["tasks"][4]["taskId"] = "T4"
try:
    evaluator.evaluate(invalid)
except SystemExit as error:
    assert "não possui exatamente T1–T5" in str(error)
else:
    raise AssertionError("tarefas duplicadas deveriam ser bloqueadas")

print("Avaliador docente: 7 cenários de aceite e rejeição verificados.")
