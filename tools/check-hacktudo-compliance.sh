#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$repo_dir"

required_files=(
  "docs/references/HACKTUDO-2026-Regulamento-oficial.pdf"
  "docs/REGULAMENTO_HACKTUDO_2026.md"
  "docs/AUTORIA_ORIGINALIDADE_E_IA.md"
  "docs/ASSET_PROVENANCE.md"
  "THIRD_PARTY_NOTICES.md"
  "LICENSE.md"
)

for required_file in "${required_files[@]}"; do
  test -f "$required_file" || {
    echo "ERRO: falta $required_file" >&2
    exit 1
  }
done

expected_regulation_hash="eb3a0078daa9eb30a17b173c0516b2dc6fd575f929401b317c6f572e36d05813"
actual_regulation_hash="$(shasum -a 256 docs/references/HACKTUDO-2026-Regulamento-oficial.pdf | awk '{print $1}')"
test "$actual_regulation_hash" = "$expected_regulation_hash" || {
  echo "ERRO: a cópia auditada do regulamento mudou; revise fonte, data e auditoria." >&2
  exit 1
}

if command -v pdfinfo >/dev/null 2>&1; then
  regulation_pages="$(pdfinfo docs/references/HACKTUDO-2026-Regulamento-oficial.pdf | awk '/^Pages:/ {print $2}')"
  test "$regulation_pages" -eq 23 || {
    echo "ERRO: regulamento preservado possui $regulation_pages páginas; esperado: 23." >&2
    exit 1
  }
fi

# `head -1` closes the pipe early and makes `git log` exit with SIGPIPE (141)
# under `set -o pipefail`; `sed` consumes the complete, small commit stream.
first_commit="$(git log --reverse --format='%H|%aI' | sed -n '1p')"
expected_first_commit="24406e712c5e0161edf18ad78d5f5f996f363a36|2026-09-12T20:36:47-03:00"
test "$first_commit" = "$expected_first_commit" || {
  echo "ERRO: a evidência temporal do primeiro commit mudou: $first_commit" >&2
  exit 1
}

if rg -n "qwen2\.5:3b|Qwen 2\.5 3B" \
  README.md server docs tools app \
  --glob '!docs/AUTORIA_ORIGINALIDADE_E_IA.md' \
  --glob '!docs/REGULAMENTO_HACKTUDO_2026.md' \
  --glob '!docs/v2/AUTHORING_MODEL_SMOKE.md' \
  --glob '!output/**' --glob '!tmp/**' --glob '!*.pdf'; then
  echo "ERRO: ainda existe referência operacional ao Qwen 3B de licença restritiva." >&2
  exit 1
fi

if ! rg -q 'Qwen 2\.5 3B.*licença restritiva' docs/v2/AUTHORING_MODEL_SMOKE.md; then
  echo "ERRO: o ensaio local precisa declarar o bloqueio de licença do Qwen 3B." >&2
  exit 1
fi

if rg -n "estilo (da )?Turma da Mônica|modelo Turma da Mônica" README.md app server \
  --ignore-case; then
  echo "ERRO: referência pública de imitação de franquia encontrada." >&2
  exit 1
fi

while IFS='|' read -r path expected_hash; do
  actual_hash="$(shasum -a 256 "$path" | awk '{print $1}')"
  test "$actual_hash" = "$expected_hash" || {
    echo "ERRO: ativo mudou sem atualizar a proveniência: $path" >&2
    exit 1
  }
done <<'ASSETS'
app/src/main/res/drawable-nodpi/comic_scene_1.jpg|4f18f600d6d1c0d23388d6452a6d661888a86120dce677eda796d8bcabaef1f1
app/src/main/res/drawable-nodpi/comic_scene_1_v2.jpg|2862b8b18fe66d51222ffbbb173b0cf68816f920bff510dc1be03fe56011220f
app/src/main/res/drawable-nodpi/comic_scene_2.jpg|30c15e0e2bdb7b398072104d6273af1bfa7a0fdd034dbec93e13137769b81cdf
app/src/main/res/drawable-nodpi/comic_scene_3.jpg|3534378741e868ad3837502f1b34b10480b25256e88066c401c63bc83679ba1e
app/src/main/res/drawable-nodpi/comic_scene_4.jpg|0cf64522d1853ea24cfbf4a96ac2cc75ea4e901561130a23db854edabb4d8a2f
app/src/main/res/drawable-nodpi/comic_scene_5.jpg|fdbd20a11835befe82418ec96ec0cfc86b618d743ab31fb5bec9092c550ad398
app/src/main/res/drawable-nodpi/puzzle_apple.jpg|0c96c4cce814bebe6f627a42207858747aebe5a0ee7444c6c1daae1793d555bf
app/src/main/res/drawable-nodpi/puzzle_ball.jpg|044ebe26f781485b3c6cabd7f204d8c3a810070330ef7342ba57b2baa98bf1e9
app/src/main/res/drawable-nodpi/puzzle_banana.jpg|264324b3eec58f9a0ddf75637249aa7f3a81b45221670a6b1788c5132603df9d
app/src/main/res/drawable-nodpi/leia_and_alfa_v1.webp|2ce07a3f75c0c6b33835791b7ae2451bc04e690033f1996dd35144acf3edbb23
app/src/main/res/drawable-nodpi/leia_and_alfa_celebrate_v2.webp|a3d1884f52c32f4783188f5ae49f2142bb1b6e304dcd0b91bddd8e06fed9495a
app/src/main/res/drawable-nodpi/leia_and_alfa_support_v2.webp|8439374ab6456d7f76d73e9d981d355c1f86f1766090dc033044c23d2d89c9bb
app/src/main/res/drawable-nodpi/comic_ball_opening_v1.webp|0dfc537df3e39b5cc010cfe9544aa56ec4ca4edffcc02ca38b39586937174a5d
app/src/main/res/drawable-nodpi/comic_ball_clue_v1.webp|1e3f700beb9863688858882327f58018f613bd5085b8facae8c0134b74d21761
app/src/main/res/drawable-nodpi/comic_ball_celebration_v1.webp|2832f6ba9cf6e9b6ba00c98b9e0eab596ac1efef44a00f1108209517898e8b61
app/src/main/res/drawable-nodpi/comic_ball_story_01_missing_v2.webp|0fbf81f3f97311f1d041bb62c746c1954a948801288b04663830335449823b68
app/src/main/res/drawable-nodpi/comic_ball_story_02_trail_v2.webp|291e0d69ec0caa64dc73a52f971167349ddf7885ee0d3175e266985a1f011883
app/src/main/res/drawable-nodpi/comic_rain_path_01_shelter_v1.webp|df4ecd4482c0b97050497771791ca205424afee3f376d354299df743daeb1933
app/src/main/res/drawable-nodpi/comic_rain_path_02_stream_v1.webp|613c4c8a2322be801817a60a3103352e8073664bd6c556bfa43fa821e0e4ec8b
app/src/main/res/drawable-nodpi/comic_rain_path_03_routes_v1.webp|5da14716e072d630f260fd8583c1ff4f117ee18a0bc72ca45e561ae6bb0ba9d1
app/src/main/res/drawable-nodpi/comic_rain_path_04_garden_v1.webp|cedb39d3292f85285de96d3d396f3f39c6332516ebc55de5147f2608adb2d736
app/src/main/res/drawable-nodpi/ball_photo_v1.jpg|dc0ebbd736ab6fba70ea6da8b5a84d03eb252698b8a824a318fefc178422fda6
app/src/main/res/raw/cue_celebrate.wav|61de17fe1385ace06caad3ed3c0471d58483ace1c76bc524088eb1e6c1129616
app/src/main/res/raw/cue_discovery.wav|969cdb182aecf714834e85e39c45f32a6efa12e023838d125135f40a4f4ca8b5
app/src/main/res/raw/cue_swap.wav|3783404a80b9cdc9a5778e5bb7175dcf4330bad2c7c23545d96bf3a3fe74c465
app/src/main/res/raw/cue_tap.wav|2baf570ba81b0d089c793a3ccab082d1c9a85f63d80375656f03a4dcf6e9e143
ASSETS

pending_assets="$(rg -c '\| \*\*CONFIRMAR\*\* \|' docs/ASSET_PROVENANCE.md || true)"
echo "Regulamento, linha do tempo, créditos, modelo e hashes dos ativos: verificados."
if [[ "$pending_assets" -gt 0 ]]; then
  echo "ATENÇÃO: $pending_assets linhas de ativos ainda exigem confirmação humana antes do pitching."
fi
echo "Consulte o checklist bloqueante em docs/REGULAMENTO_HACKTUDO_2026.md."
