#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

for required_file in \
  dist/InterpretaAI-mvp-debug.apk \
  dist/InterpretaAI-Proposta-MVP.pdf \
  dist/SHA256.txt; do
  test -f "$required_file" || {
    echo "ERRO: falta $required_file. Em clone do GitHub, baixe o APK no Release mais recente." >&2
    exit 1
  }
done

shasum -a 256 -c dist/SHA256.txt

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
checked_media = 0
link_pattern = re.compile(r"\[[^]]*\]\(([^)]+)\)")
media_pattern = re.compile(r'<img\s+[^>]*src="([^"]+)"', re.IGNORECASE)
for source in files:
    text = source.read_text(encoding="utf-8")
    targets = link_pattern.findall(text)
    media = media_pattern.findall(text)
    for target in targets + media:
        clean = target.split("#", 1)[0]
        if not clean or "://" in clean or clean.startswith("mailto:"):
            continue
        if target in media:
            checked_media += 1
        destination = (source.parent / clean).resolve()
        if not destination.exists():
            broken.append(f"{source.relative_to(root)} -> {target}")
if broken:
    raise SystemExit("Links locais quebrados:\n" + "\n".join(broken))
print(f"Links locais verificados em {len(files)} arquivos Markdown; {checked_media} imagens conferidas.")
PY

git diff --check HEAD

./tools/check-hacktudo-compliance.sh

if [[ "${1:-}" == "--full" ]]; then
  ./gradlew :app:testDebugUnitTest :server:test :app:lintDebug :app:assembleDebug :server:bootJar
fi

echo "Entrega e documentação verificadas."
