#!/bin/sh
# Rebuilds every icon that comes from artwork in art/. Add a line per new icon.
# --size: artwork radius in icon units (26 is typical; bigger = larger in the icon)
set -e
cd "$(dirname "$0")/.."
python3 tools/import_art.py art/spotify.jpg  spotify  --size 25
python3 tools/import_art.py art/outlook.jpg  outlook  --size 30 --mono shade
python3 tools/import_art.py art/whatsapp.jpg whatsapp --size 31
