#!/usr/bin/env python3
"""VTLOffmp3g: serves this folder on http://localhost:8765/ and opens the browser."""
import functools, http.server, os, socketserver, sys, webbrowser

ROOT = os.path.dirname(os.path.abspath(__file__))
TYPES = {".js": "text/javascript", ".mjs": "text/javascript", ".wasm": "application/wasm",
         ".json": "application/json", ".css": "text/css", ".ttf": "font/ttf", ".ffpreset": "text/plain"}


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map, **TYPES}

    def log_message(self, *args):
        pass


def main():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8765
    handler = functools.partial(Handler, directory=ROOT)
    for p in range(port, port + 30):
        try:
            server = socketserver.ThreadingTCPServer(("127.0.0.1", p), handler)
            break
        except OSError:
            continue
    else:
        sys.exit("No free port")
    url = f"http://localhost:{server.server_address[1]}/"
    print(f"VTLOffmp3g est prêt : {url}  (Ctrl+C pour arrêter)")
    if "--no-browser" not in sys.argv:
        webbrowser.open(url)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass


if __name__ == "__main__":
    main()
