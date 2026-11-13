#!/bin/sh
# VTLOffmp3g (macOS / Linux) : lance l'application dans le navigateur.
cd "$(dirname "$0")" && exec python3 serve.py "$@"
