#!/usr/bin/env python3
"""Minimal loopback-only OIDC metadata server for staging startup smoke tests."""

import argparse
import base64
import json
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--port", type=int, required=True)
    args = parser.parse_args()
    issuer = f"http://127.0.0.1:{args.port}"
    # Sufficient for decoder construction; this server never issues or signs a token.
    modulus = bytes([0xD3]) + bytes([0x01]) * 254 + bytes([0x03])
    encoded_modulus = base64.urlsafe_b64encode(modulus).rstrip(b"=").decode()

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
