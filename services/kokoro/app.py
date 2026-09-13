import io
import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

import numpy as np
import soundfile as sf
from kokoro import KPipeline


PIPELINE = KPipeline(lang_code="p")
ALLOWED_VOICES = {"pf_dora", "pm_alex"}


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path != "/health":
            self.send_error(404)
            return
        self._send_json(200, {"status": "UP", "engine": "kokoro", "language": "pt-BR"})

    def do_POST(self):
        if self.path != "/synthesize":
            self.send_error(404)
            return
        try:
            size = min(int(self.headers.get("Content-Length", "0")), 4096)
            payload = json.loads(self.rfile.read(size))
            text = " ".join(str(payload.get("text", "")).split())[:360]
            voice = str(payload.get("voice", "pf_dora"))
            if not text or voice not in ALLOWED_VOICES:
                self._send_json(400, {"error": "invalid_request"})
                return

            chunks = []
            for _, _, audio in PIPELINE(text, voice=voice, speed=0.96):
                chunks.append(audio.detach().cpu().numpy() if hasattr(audio, "detach") else np.asarray(audio))
            if not chunks:
                raise RuntimeError("empty_audio")

            output = io.BytesIO()
            sf.write(output, np.concatenate(chunks), 24000, format="WAV", subtype="PCM_16")
            data = output.getvalue()
            self.send_response(200)
            self.send_header("Content-Type", "audio/wav")
            self.send_header("Content-Length", str(len(data)))
            self.send_header("Cache-Control", "no-store")
            self.end_headers()
            self.wfile.write(data)
        except Exception:
            self._send_json(503, {"error": "synthesis_unavailable"})

    def _send_json(self, status, payload):
        data = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def log_message(self, message, *args):
        print("kokoro", self.address_string(), message % args, flush=True)


if __name__ == "__main__":
    host = os.getenv("KOKORO_HOST", "127.0.0.1")
    port = int(os.getenv("KOKORO_PORT", "8091"))
    print(f"Kokoro pt-BR em http://{host}:{port}", flush=True)
    ThreadingHTTPServer((host, port), Handler).serve_forever()
