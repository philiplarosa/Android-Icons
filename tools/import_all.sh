#!/bin/sh
# Rebuilds every icon that comes from artwork in art/. Add a line per new icon.
#   --size    artwork radius in icon units (26 is typical; bigger = larger in the icon)
#   --height  scale vertically: below 1 squashes art that looks too tall, above 1 stretches
#   --center circle  centre stars and other pointy shapes on their points
#   --mono shade     better themed-icon version for solid filled shapes
#   --shift X Y      nudge right/down (negative = left/up) when art looks off-centre by eye
set -e
cd "$(dirname "$0")/.."
python3 tools/import_art.py art/spotify.jpg  spotify  --size 25
python3 tools/import_art.py art/outlook.jpg  outlook  --size 28 --height 0.85 --mono shade
python3 tools/import_art.py art/whatsapp.jpg whatsapp --size 31 --shift 2 -1.5
python3 tools/import_art.py art/nab.jpg      nab      --size 27 --center circle
python3 tools/import_art.py art/claude.jpg   claude   --size 24 --center circle
python3 tools/import_art.py art/renoho.jpg   renoho   --size 29
python3 tools/import_art.py art/unifi_access.jpg unifi_access --size 27 --shift 0 -1
python3 tools/import_art.py art/unifi_protect.jpg unifi_protect --size 30 --shift 1.7 1
python3 tools/import_art.py art/samply.jpg     samply   --size 24
python3 tools/import_art.py art/canary.jpg     canary   --size 23
python3 tools/import_art.py art/tbfc.jpg       tbfc     --size 25
