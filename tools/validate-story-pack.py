#!/usr/bin/env python3
"""Validação semântica determinística de um LearningStoryPack."""

from __future__ import annotations

import argparse
import json
import re
import unicodedata
from collections import Counter
from pathlib import Path
from typing import Any


SUPPORT_ORDER = {"VOICE_REPEAT": 0, "VISUAL_CUE": 1, "CHOICE_REVEALED": 2}
BANNED_PATTERNS = {
    "punitive_error": re.compile(r"\b(voc[eê]\s+errou|resposta\s+errada|est[aá]\s+errado)\b", re.I),
    "diagnosis": re.compile(r"\b(diagn[oó]stic|transtorno|d[eé]ficit|dislexia)\w*\b", re.I),
    "guilt": re.compile(r"\b(n[aã]o\s+me\s+abandone|voc[eê]\s+est[aá]\s+demorando|preste\s+aten[cç][aã]o)\b", re.I),
    "grading": re.compile(r"\b(nota|ranking|reprovad[oa])\b", re.I),
}


def normalize_letters(value: str) -> list[str]:
    return [char for char in unicodedata.normalize("NFC", value.upper()) if not char.isspace()]


def text_values(value: Any, path: str = "$"):
    if isinstance(value, str):
        yield path, value
    elif isinstance(value, list):
        for index, item in enumerate(value):
            yield from text_values(item, f"{path}[{index}]")
    elif isinstance(value, dict):
        for key, item in value.items():
            yield from text_values(item, f"{path}.{key}")


def validate(pack: dict[str, Any]) -> list[dict[str, str]]:
    issues: list[dict[str, str]] = []

    def add(code: str, path: str, message: str) -> None:
        issues.append({"code": code, "path": path, "message": message})

    nodes = pack.get("nodes", [])
    assets = pack.get("assets", [])
    node_ids = [node.get("id") for node in nodes]
    asset_ids = [asset.get("id") for asset in assets]
    if len(node_ids) != len(set(node_ids)):
        add("duplicate_node_id", "$.nodes", "Identificadores de nó devem ser únicos.")
    if len(asset_ids) != len(set(asset_ids)):
        add("duplicate_asset_id", "$.assets", "Identificadores de mídia devem ser únicos.")

    nodes_by_id = {node.get("id"): node for node in nodes if node.get("id")}
    assets_by_id = {asset.get("id"): asset for asset in assets if asset.get("id")}
    start = pack.get("startNodeId")
    if start not in nodes_by_id:
        add("start_not_found", "$.startNodeId", "A cena inicial não existe.")

    root_objectives = set(pack.get("objectiveIds", []))
    for index, node in enumerate(nodes):
        node_path = f"$.nodes[{index}]"
        unknown_objectives = set(node.get("objectiveIds", [])) - root_objectives
        if unknown_objectives:
            add(
                "objective_not_declared",
                f"{node_path}.objectiveIds",
                f"Objetivos não declarados no pacote: {sorted(unknown_objectives)}",
            )
        next_id = node.get("nextNodeId")
        if next_id is not None and next_id not in nodes_by_id:
            add("next_not_found", f"{node_path}.nextNodeId", "A próxima cena não existe.")
        for field in ("visualAssetId", "imageAssetId"):
            asset_id = node.get(field)
            if asset_id is not None and asset_id not in assets_by_id:
                add("asset_not_found", f"{node_path}.{field}", "A mídia referenciada não existe.")

        supports = node.get("supports", [])
        levels = [support.get("level") for support in supports]
        if len(levels) != len(set(levels)):
            add("duplicate_support", f"{node_path}.supports", "Um nível de apoio foi repetido.")
        known_levels = [level for level in levels if level in SUPPORT_ORDER]
        if known_levels != sorted(known_levels, key=SUPPORT_ORDER.get):
            add("support_order", f"{node_path}.supports", "Apoios devem avançar progressivamente.")
        for support_index, support in enumerate(supports):
            target = support.get("visualTargetId")
            if target and target not in nodes_by_id and target not in assets_by_id:
                add(
                    "visual_target_not_found",
                    f"{node_path}.supports[{support_index}].visualTargetId",
                    "O alvo da pista visual não existe.",
                )

        if node.get("type") == "WORD_BUILDER":
            required = Counter(normalize_letters(node.get("targetWord", "")))
            available = Counter(
                char
                for tile in node.get("letterTiles", [])
                for char in normalize_letters(tile)
            )
            missing = required - available
            if missing:
                add(
                    "word_cannot_be_built",
                    f"{node_path}.letterTiles",
                    f"Faltam letras para formar a palavra: {dict(missing)}",
                )

    if start in nodes_by_id:
        visited: set[str] = set()
        active: set[str] = set()
        reaches_end = False

        def walk(node_id: str) -> None:
            nonlocal reaches_end
            if node_id in active:
                add("cycle_detected", "$.nodes", f"Ciclo detectado a partir de {node_id}.")
                return
            if node_id in visited:
                return
            visited.add(node_id)
            active.add(node_id)
            node = nodes_by_id[node_id]
            if node.get("type") == "END":
                reaches_end = True
            elif node.get("nextNodeId") in nodes_by_id:
                walk(node["nextNodeId"])
            else:
                add("dead_end", f"$.nodes[{node_id}]", "O caminho termina sem cena END.")
            active.remove(node_id)

        walk(start)
        unreachable = set(nodes_by_id) - visited
        if unreachable:
            add("unreachable_nodes", "$.nodes", f"Cenas inalcançáveis: {sorted(unreachable)}")
        if not reaches_end:
            add("end_not_reached", "$.nodes", "Não existe caminho válido até END.")

    accessibility = pack.get("accessibility", {})
    expected_accessibility = {
        "reducedStimuliSupported": True,
        "spokenInstructions": True,
        "noRequiredScroll": True,
    }
    if accessibility.get("minTouchTargetDp", 0) < 48:
        add("touch_target_too_small", "$.accessibility.minTouchTargetDp", "Mínimo: 48dp.")
    for field, expected in expected_accessibility.items():
        if accessibility.get(field) is not expected:
            add("accessibility_required", f"$.accessibility.{field}", "Recurso obrigatório ausente.")

    variants_seen: set[tuple[str, str]] = set()
    for asset_index, asset in enumerate(assets):
        for variant_index, variant in enumerate(asset.get("variants", [])):
            key = (asset.get("id", ""), variant.get("role", ""))
            if key in variants_seen:
                add(
                    "duplicate_asset_variant",
                    f"$.assets[{asset_index}].variants[{variant_index}]",
                    "A mídia repete a mesma variante.",
                )
            variants_seen.add(key)

    origins = pack.get("provenance", {}).get("assetOrigins", [])
    origin_ids = [origin.get("assetId") for origin in origins]
    if Counter(origin_ids) != Counter(asset_ids):
        add(
            "asset_provenance_mismatch",
            "$.provenance.assetOrigins",
            "Cada mídia precisa de exatamente uma origem.",
        )
    for index, origin in enumerate(origins):
        if origin.get("reviewedByTeacher") is not True:
            add(
                "asset_not_reviewed",
                f"$.provenance.assetOrigins[{index}].reviewedByTeacher",
                "Mídia não revisada pela professora.",
            )

    for path, value in text_values(pack):
        for code, pattern in BANNED_PATTERNS.items():
            if pattern.search(value):
                add(code, path, "Linguagem bloqueada para a jornada infantil.")

    return sorted(issues, key=lambda issue: (issue["path"], issue["code"]))


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("pack", type=Path)
    parser.add_argument("--report", type=Path)
    args = parser.parse_args()
    pack = json.loads(args.pack.read_text(encoding="utf-8"))
    issues = validate(pack)
    report = {
        "validatorVersion": "1.0",
        "pack": str(args.pack),
        "status": "VALID" if not issues else "BLOCKED",
        "issueCount": len(issues),
        "issues": issues,
    }
    if args.report:
        args.report.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n",
            encoding="utf-8",
        )
    print(json.dumps(report, ensure_ascii=False))
    if issues:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
