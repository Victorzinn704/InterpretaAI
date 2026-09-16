#!/usr/bin/env bash
set -euo pipefail

repo_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
package="$repo_dir/build/interpretaai-oracle-arm64.tar.gz"
ssh_target=""
domain=""
identity=""
port="22"
apply=false

usage() {
  cat <<'EOF'
Uso: ./tools/deploy-oracle.sh --host usuario@IP --domain api.exemplo.com [opcoes]

Opcoes:
  --package ARQUIVO   Bundle gerado por package-oracle-deploy.sh
  --identity CHAVE    Chave SSH privada (nao e copiada nem impressa)
  --port PORTA        Porta SSH; padrao 22
  --apply             Transfere e executa o instalador remoto

Sem --apply, executa somente preflight de DNS, SSH, arquitetura, sudo e dependencias.
EOF
}

while (( $# > 0 )); do
  case "$1" in
    --host) ssh_target="${2:-}"; shift 2 ;;
    --domain) domain="${2:-}"; shift 2 ;;
    --package) package="${2:-}"; shift 2 ;;
    --identity) identity="${2:-}"; shift 2 ;;
    --port) port="${2:-}"; shift 2 ;;
    --apply) apply=true; shift ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Opcao desconhecida: $1" >&2; usage >&2; exit 2 ;;
  esac
done

if ! [[ "$ssh_target" =~ ^[A-Za-z0-9._-]+@[A-Za-z0-9.:_-]+$ ]]; then
  echo "--host deve ter o formato usuario@host." >&2
  exit 2
fi
if ! [[ "$domain" =~ ^[A-Za-z0-9]([A-Za-z0-9.-]*[A-Za-z0-9])?$ ]] || [[ "$domain" != *.* ]]; then
  echo "--domain deve conter somente o dominio publico, sem protocolo ou caminho." >&2
  exit 2
fi
if ! [[ "$port" =~ ^[1-9][0-9]{0,4}$ ]] || (( port > 65535 )); then
  echo "--port invalida." >&2
  exit 2
fi
[[ -f "$package" ]] || { echo "Pacote ausente: $package" >&2; exit 2; }
if [[ -n "$identity" && ! -f "$identity" ]]; then
  echo "Chave SSH ausente: $identity" >&2
  exit 2
fi
for dependency in python3 ssh scp shasum; do
  command -v "$dependency" >/dev/null 2>&1 || { echo "Dependencia local ausente: $dependency" >&2; exit 2; }
done

ssh_host="${ssh_target#*@}"
python3 - "$domain" "$ssh_host" <<'PY'
import socket
import sys

domain = sys.argv[1]
ssh_host = sys.argv[2]
def resolve(name, port):
    return sorted({item[4][0] for item in socket.getaddrinfo(name, port, type=socket.SOCK_STREAM)})

try:
    domain_addresses = resolve(domain, 443)
except socket.gaierror as error:
    raise SystemExit(f"DNS ainda nao resolve {domain}: {error}")
try:
    host_addresses = resolve(ssh_host, 22)
except socket.gaierror as error:
    raise SystemExit(f"Host SSH nao resolve {ssh_host}: {error}")
print("DNS:", domain, "->", ", ".join(domain_addresses))
print("SSH:", ssh_host, "->", ", ".join(host_addresses))
if not set(domain_addresses).intersection(host_addresses):
    raise SystemExit("O dominio ainda nao aponta para o host informado; deploy recusado")
PY

ssh_options=(-o BatchMode=yes -o ConnectTimeout=8 -p "$port")
scp_options=(-o BatchMode=yes -o ConnectTimeout=8 -P "$port")
if [[ -n "$identity" ]]; then
  ssh_options+=(-i "$identity" -o IdentitiesOnly=yes)
  scp_options+=(-i "$identity" -o IdentitiesOnly=yes)
fi

echo "Validando host remoto sem alterar estado..."
remote_report="$(ssh "${ssh_options[@]}" "$ssh_target" \
  'set -eu; printf "ARCH="; uname -m; printf "SUDO="; sudo -n true && echo OK; for cmd in caddy curl install java journalctl ollama openssl python3 runuser sed sha256sum systemctl systemd-analyze useradd; do command -v "$cmd" >/dev/null || echo "MISSING=$cmd"; done')"
printf '%s\n' "$remote_report"
if [[ "$remote_report" != *"ARCH=aarch64"* && "$remote_report" != *"ARCH=arm64"* ]]; then
  echo "Host nao e ARM64; bundle recusado." >&2
  exit 3
fi
if [[ "$remote_report" != *"SUDO=OK"* ]]; then
  echo "sudo nao interativo indisponivel para esse usuario." >&2
  exit 3
fi
if [[ "$remote_report" == *"MISSING="* ]]; then
  echo "Instale as dependencias listadas antes do deploy." >&2
  exit 3
fi

package_hash="$(shasum -a 256 "$package" | awk '{print $1}')"
echo "Preflight aprovado: pacote SHA-256 $package_hash"
if [[ "$apply" != true ]]; then
  echo "Nenhuma alteracao remota feita. Repita com --apply quando DNS e acesso estiverem definitivos."
  exit 0
fi

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
remote_dir="/tmp/interpretaai-deploy-$stamp"
remote_package="$remote_dir/interpretaai-oracle-arm64.tar.gz"
ssh "${ssh_options[@]}" "$ssh_target" "install -d -m 0700 '$remote_dir'"
scp "${scp_options[@]}" "$package" "$ssh_target:$remote_package"
ssh "${ssh_options[@]}" "$ssh_target" \
  "set -eu; printf '%s  %s\n' '$package_hash' '$remote_package' | sha256sum --check -; tar -xzf '$remote_package' -C '$remote_dir'; sudo '$remote_dir/interpretaai-oracle/install.sh' '$domain'"

echo "Deploy executado. Valide TLS e carga sintética antes de gerar o APK online:"
echo "  INTERPRETAAI_DEVICE_TOKEN=... deploy/oracle/verify-public.sh https://$domain"
echo "  INTERPRETAAI_DEVICE_TOKEN=... LOAD_CONCURRENCY=2 LOAD_TURNS=12 deploy/oracle/verify-classroom-load.sh https://$domain"
echo "Diretorio remoto temporario preservado para auditoria: $remote_dir"
