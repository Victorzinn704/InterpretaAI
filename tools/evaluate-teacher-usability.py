#!/usr/bin/env python3
"""Resume o estudo docente sem inventar sessões ou completar dados ausentes."""

from __future__ import annotations

import argparse
import json
import statistics
from pathlib import Path


TASK_IDS = {"T1", "T2", "T3", "T4", "T5"}


def evaluate(study: dict) -> dict:
    """Calcula o aceite somente a partir de sessões explicitamente registradas."""
    sessions = study.get("sessions", [])

    participant_ids = [session["participantId"] for session in sessions]
    if len(participant_ids) != len(set(participant_ids)):
        raise SystemExit("participantId duplicado")
    for session in sessions:
        task_ids = [task["taskId"] for task in session.get("tasks", [])]
        if set(task_ids) != TASK_IDS or len(task_ids) != len(TASK_IDS):
            raise SystemExit(f"{session['participantId']} não possui exatamente T1–T5")

    task_summary = {}
    for task_id in sorted(TASK_IDS):
        tasks = [
            task
            for session in sessions
            for task in session["tasks"]
            if task["taskId"] == task_id
        ]
        task_summary[task_id] = {
            "withoutHelp": sum(task["outcome"] == "WITHOUT_HELP" for task in tasks),
            "withHelp": sum(task["outcome"] == "WITH_HELP" for task in tasks),
            "notCompleted": sum(task["outcome"] == "NOT_COMPLETED" for task in tasks),
            "medianDurationSeconds": statistics.median(
                [task["durationSeconds"] for task in tasks]
            ) if tasks else None,
        }

    blocker_count = sum(
        finding["severity"] == "BLOCKER"
        for session in sessions
        for task in session["tasks"]
        for finding in task["findings"]
    )
    complete_sample = len(sessions) == 5
    criteria = {
        "fiveSessions": complete_sample,
        "fourOfFiveWithoutHelpPerTask": complete_sample and all(
            summary["withoutHelp"] >= 4 for summary in task_summary.values()
        ),
        "noAdministrationOpened": complete_sample and not any(
            session["openedAdministration"] for session in sessions
        ),
        "publishReadyConfusionAtMostOne": complete_sample and sum(
            session["confusedPublishWithReady"] for session in sessions
        ) <= 1,
        "noOpenBlocker": complete_sample and blocker_count == 0,
    }
    if not sessions:
        status = "NOT_RUN"
    elif not complete_sample:
        status = "INCOMPLETE"
    elif all(criteria.values()):
        status = "PASSED"
    else:
        status = "NEEDS_REVISION"

    return {
        "studyVersion": study["schemaVersion"],
        "prototypeCommit": study["prototypeCommit"],
        "status": status,
        "sessionCount": len(sessions),
        "taskSummary": task_summary,
        "blockerCount": blocker_count,
        "criteria": criteria,
    }


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("study", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    study = json.loads(args.study.read_text(encoding="utf-8"))
    report = evaluate(study)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps(report, ensure_ascii=False))


if __name__ == "__main__":
    main()
