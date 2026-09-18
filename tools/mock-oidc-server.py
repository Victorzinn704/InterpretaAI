#!/usr/bin/env python3
"""Loopback-only OIDC metadata and synthetic-token server for staging smoke tests."""

import argparse
import base64
import json
import subprocess
import time
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def base64url(value: bytes) -> str:
    return base64.urlsafe_b64encode(value).rstrip(b"=").decode()


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, required=True)
    parser.add_argument("--private-key")
    parser.add_argument("--audience", default="interpretaai-api")
    args = parser.parse_args()
    issuer = f"http://127.0.0.1:{args.port}"

    if args.private_key:
        modulus_output = subprocess.run(
            ["openssl", "rsa", "-in", args.private_key, "-modulus", "-noout"],
            check=True,
            capture_output=True,
            text=True,
        ).stdout.strip()
        modulus = bytes.fromhex(modulus_output.removeprefix("Modulus="))
    else:
        # Enough for decoder construction in startup-only callers.
        modulus = bytes([0xD3]) + bytes([0x01]) * 254 + bytes([0x03])
    encoded_modulus = base64url(modulus)

    def synthetic_token() -> str:
        if not args.private_key:
            raise RuntimeError("synthetic token endpoint is disabled")
        now = int(time.time())
        header = base64url(json.dumps(
            {"alg": "RS256", "kid": "smoke-only", "typ": "JWT"},
            separators=(",", ":"),
        ).encode())
        payload = base64url(json.dumps({
            "iss": issuer,
            "sub": "smoke|teacher",
            "aud": [args.audience],
            "iat": now,
            "nbf": now - 1,
            "exp": now + 300,
        }, separators=(",", ":")).encode())
        signing_input = f"{header}.{payload}".encode()
        signature = subprocess.run(
            ["openssl", "dgst", "-sha256", "-sign", args.private_key],
            input=signing_input,
            check=True,
            capture_output=True,
        ).stdout
        return f"{header}.{payload}.{base64url(signature)}"

    class Handler(BaseHTTPRequestHandler):
        def do_GET(self) -> None:  # noqa: N802 - HTTP handler API
            if self.path == "/.well-known/openid-configuration":
                body = {
                    "issuer": issuer,
                    "authorization_endpoint": issuer + "/authorize",
                    "token_endpoint": issuer + "/token",
                    "jwks_uri": issuer + "/jwks",
                    "userinfo_endpoint": issuer + "/userinfo",
                    "subject_types_supported": ["public"],
                    "id_token_signing_alg_values_supported": ["RS256"],
                    "response_types_supported": ["code"],
                }
            elif self.path == "/jwks":
                body = {"keys": [{
                    "kty": "RSA", "use": "sig", "alg": "RS256", "kid": "smoke-only",
                    "n": encoded_modulus, "e": "AQAB",
                }]}
            elif self.path == "/smoke-token" and args.private_key:
                body = {
                    "access_token": synthetic_token(),
                    "token_type": "Bearer",
                    "expires_in": 300,
                }
            else:
                self.send_error(404)
                return
            encoded = json.dumps(body).encode()
            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.send_header("Content-Length", str(len(encoded)))
            self.end_headers()
            self.wfile.write(encoded)

        def log_message(self, _format: str, *_args: object) -> None:
            return

    ThreadingHTTPServer(("127.0.0.1", args.port), Handler).serve_forever()


if __name__ == "__main__":
    main()
