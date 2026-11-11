"""HTTP front for the intent interpreter.

Run from python-service:

    PYTHONPATH=src python3 -m python_service.server
"""

import json
import os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer

from python_service.health import status
from python_service.intent import interpret


class IntentHandler(BaseHTTPRequestHandler):
    protocol_version = "HTTP/1.1"

    def do_GET(self):
        if self.path.split("?", 1)[0].rstrip("/") == "/health":
            self._send(200, status())
            return
        self._send(404, {"message": "Not found"})

    def do_POST(self):
        if self.path.split("?", 1)[0].rstrip("/") != "/interpret":
            self._send(404, {"message": "Not found"})
            return
        raw = read_body(self.headers, self.rfile)
        try:
            body = json.loads(raw.decode("utf-8") or "{}")
        except json.JSONDecodeError:
            self._send(400, {"message": "Request body must be JSON."})
            return
        text = body.get("text", "") if isinstance(body, dict) else ""
        if not isinstance(text, str):
            text = ""
        self._send(200, interpret(text))

    def _send(self, code, payload):
        data = json.dumps(payload).encode("utf-8")
        self.send_response(code)
        self.send_header("Content-Type", "application/json; charset=utf-8")
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Connection", "close")
        self.end_headers()
        self.wfile.write(data)


def read_body(headers, reader):
    transfer = headers.get("Transfer-Encoding", "")
    if "chunked" in transfer.lower():
        return _read_chunked(reader)
    length = headers.get("Content-Length")
    if length:
        return reader.read(int(length))
    return b""


def _read_chunked(reader):
    chunks = []
    while True:
        line = reader.readline()
        if not line:
            break
        size = int(line.split(b";", 1)[0].strip() or b"0", 16)
        if size == 0:
            reader.readline()
            break
        chunks.append(reader.read(size))
        reader.read(2)
    return b"".join(chunks)


def main():
    port = int(os.environ.get("PYTHON_SERVICE_PORT", "8081"))
    server = ThreadingHTTPServer(("127.0.0.1", port), IntentHandler)
    print(f"python-service listening on http://127.0.0.1:{port}", flush=True)
    server.serve_forever()


if __name__ == "__main__":
    main()
