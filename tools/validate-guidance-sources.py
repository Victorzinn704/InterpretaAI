#!/usr/bin/env python3
"""Valida integridade e estado das fontes candidatas/aprovadas do RAG."""

from __future__ import annotations

import hashlib
import json
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
GUIDANCE_DIR = ROOT / "docs" / "v2" / "guidance"
MANIFEST_PATH = GUIDANCE_DIR / "manifest.json"


def main() -> None:
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    sources = manifest["sources"]
    source_ids = [source["sourceId"] for source in sources]
    if len(source_ids) != len(set(source_ids)):
        raise SystemExit("sourceId duplicado no manifesto")

    declared_paths: set[str] = set()
    approved = 0
    for source in sources:
        relative = source["contentPath"]
        declared_paths.add(relative)
        target = (GUIDANCE_DIR / relative).resolve()
        if GUIDANCE_DIR.resolve() not in target.parents:
            raise SystemExit(f"caminho fora de guidance/: {relative}")
        if not target.is_file():
            raise SystemExit(f"fonte ausente: {relative}")
        actual = hashlib.sha256(target.read_bytes()).hexdigest()
        if actual != source["contentSha256"]:
            raise SystemExit(f"hash divergente: {relative}")
        if source["reviewStatus"] == "APPROVED":
            approved += 1
            for field in ("approvedBy", "approvedAt", "validFrom"):
                if not source.get(field):
                    raise SystemExit(f"fonte aprovada sem {field}: {relative}")

    undeclared = {
        item.name
        for item in GUIDANCE_DIR.glob("*.md")
        if item.name not in declared_paths
    }
    if undeclared:
        raise SystemExit(f"fontes sem manifesto: {sorted(undeclared)}")

    print(
        f"Fontes do RAG: {len(sources)} íntegras, "
        f"{approved} aprovadas e {len(sources) - approved} não aprovadas."
    )


if __name__ == "__main__":
    main()
