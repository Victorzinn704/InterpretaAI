#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

test -f dist/InterpretaAI-mvp-debug.apk
test -f dist/InterpretaAI-Proposta-MVP.pdf
test -f dist/SHA256.txt

(cd dist && shasum -a 256 -c SHA256.txt)

summary_lines="$(awk 'NF { count++ } END { print count + 0 }' docs/RESUMO_10_LINHAS.md)"
test "$summary_lines" -eq 10 || {
  echo "ERRO: RESUMO_10_LINHAS.md possui $summary_lines linhas não vazias." >&2
  exit 1
}

if command -v pdfinfo >/dev/null 2>&1; then
  pdf_pages="$(pdfinfo dist/InterpretaAI-Proposta-MVP.pdf | awk '/^Pages:/ { print $2 }')"
  test "$pdf_pages" -eq 10 || {
    echo "ERRO: o PDF possui $pdf_pages páginas; esperado: 10." >&2
    exit 1
  }
else
  echo "AVISO: pdfinfo não encontrado; contagem de páginas não verificada."
fi

python3 - <<'PY'
import re
from pathlib import Path

root = Path.cwd()
files = [root / "README.md", root / "server/README.md", root / "dist/README.md", root / "output/README.md"]
files.extend(sorted((root / "docs").glob("*.md")))
broken = []
pattern = re.compile(r"\[[^]]*\]\(([^)]+)\)")
for source in files:
    for target in pattern.findall(source.read_text(encoding="utf-8")):
        clean = target.split("#", 1)[0]
        if not clean or "://" in clean or clean.startswith("mailto:"):
            continue
        destination = (source.parent / clean).resolve()
        if not destination.exists():
            broken.append(f"{source.relative_to(root)} -> {target}")
if broken:
    raise SystemExit("Links locais quebrados:\n" + "\n".join(broken))
print(f"Links locais verificados em {len(files)} arquivos Markdown.")
PY

git diff --check HEAD

if [[ "${1:-}" == "--full" ]]; then
  ./gradlew :app:testDebugUnitTest :server:test :app:lintDebug :app:assembleDebug :server:bootJar
fi

echo "Entrega e documentação verificadas."
