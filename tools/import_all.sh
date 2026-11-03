#!/bin/sh
# Rebuilds every icon that comes from artwork in art/. Add a line per new icon.
#   --size    artwork radius in icon units (26 is typical; bigger = larger in the icon)
#   --height  stretch vertically, e.g. 1.1 if the art looks too wide
#   --center circle  centre stars and other pointy shapes on their points
#   --mono shade     better themed-icon version for solid filled shapes
set -e
cd "$(dirname "$0")/.."
python3 tools/import_art.py art/spotify.jpg  spotify  --size 25
python3 tools/import_art.py art/outlook.jpg  outlook  --size 30 --height 1.1 --mono shade
python3 tools/import_art.py art/whatsapp.jpg whatsapp --size 31
python3 tools/import_art.py art/nab.jpg      nab      --size 27 --center circle
python3 tools/import_art.py art/claude.jpg   claude   --size 27 --center circle
