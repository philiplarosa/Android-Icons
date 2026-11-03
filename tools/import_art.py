#!/usr/bin/env python3
"""Turn a square-ish artwork image into an adaptive icon for the pack.

    python3 tools/import_art.py art/spotify.jpg spotify
    python3 tools/import_art.py art/outlook.jpg outlook --size 30 --mono shade

Writes:
    app/src/main/res/drawable-nodpi/<name>_fg.png    artwork, centred and scaled
    app/src/main/res/drawable-nodpi/<name>_mono.png  single-colour version for themed icons
    app/src/main/res/drawable/<name>.xml             adaptive icon tying them together

The artwork's own background colour (sampled from the corners) becomes the
icon background, so art drawn on white gives a white icon. --size is the
radius of the artwork in adaptive-icon units (the visible icon is 72 units
across, the circle mask has radius 36); 26 suits most art.

Needs Pillow: pip install pillow
"""
import argparse
import itertools
import math
import os
import sys

from PIL import Image, ImageChops, ImageFilter

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(ROOT, "app", "src", "main", "res")
CANVAS = 648            # px for the 108-unit adaptive canvas (6 px per unit)
PX_PER_UNIT = CANVAS / 108


def ink(img, bg):
    """0-255 'how far from the background colour' per pixel."""
    diff = ImageChops.difference(img, Image.new("RGB", img.size, bg))
    r, g, b = diff.split()
    return ImageChops.lighter(ImageChops.lighter(r, g), b)


def convex_hull(points):
    pts = sorted(set(points))

    def cross(o, a, b):
        return (a[0] - o[0]) * (b[1] - o[1]) - (a[1] - o[1]) * (b[0] - o[0])

    lower, upper = [], []
    for p in pts:
        while len(lower) >= 2 and cross(lower[-2], lower[-1], p) <= 0:
            lower.pop()
        lower.append(p)
    for p in reversed(pts):
        while len(upper) >= 2 and cross(upper[-2], upper[-1], p) <= 0:
            upper.pop()
        upper.append(p)
    return lower[:-1] + upper[:-1]


def enclosing_circle(pts):
    """Smallest circle containing every point (exact; pts is a small convex hull)."""
    def from2(a, b):
        cx, cy = (a[0] + b[0]) / 2, (a[1] + b[1]) / 2
        return cx, cy, math.hypot(a[0] - cx, a[1] - cy)

    def from3(a, b, c):
        d = 2 * (a[0] * (b[1] - c[1]) + b[0] * (c[1] - a[1]) + c[0] * (a[1] - b[1]))
        if abs(d) < 1e-9:
            return None
        sa, sb, sc = (a[0] ** 2 + a[1] ** 2), (b[0] ** 2 + b[1] ** 2), (c[0] ** 2 + c[1] ** 2)
        ux = (sa * (b[1] - c[1]) + sb * (c[1] - a[1]) + sc * (a[1] - b[1])) / d
        uy = (sa * (c[0] - b[0]) + sb * (a[0] - c[0]) + sc * (b[0] - a[0])) / d
        return ux, uy, math.hypot(a[0] - ux, a[1] - uy)

    candidates = [from2(a, b) for a, b in itertools.combinations(pts, 2)]
    candidates += [c for t in itertools.combinations(pts, 3) if (c := from3(*t))]
    candidates.sort(key=lambda c: c[2])
    for cx, cy, r in candidates:
        if all(math.hypot(x - cx, y - cy) <= r + 1e-6 for x, y in pts):
            return cx, cy, r
    raise ValueError("no enclosing circle")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("image")
    ap.add_argument("name", help="lowercase resource name, e.g. spotify")
    ap.add_argument("--size", type=float, default=26, help="artwork radius in units (default 26)")
    ap.add_argument("--mono", choices=["ink", "shade"], default="ink",
                    help="how to build the themed-icon layer (default ink)")
    ap.add_argument("--height", type=float, default=1.0,
                    help="stretch the art vertically by this factor, e.g. 1.1 if it looks too wide")
    ap.add_argument("--center", choices=["box", "circle"], default="box",
                    help="box: centre of the bounding box. circle: centre of the smallest circle "
                         "around the art, which suits stars and other pointy shapes")
    ap.add_argument("--shift", type=float, nargs=2, default=(0.0, 0.0), metavar=("X", "Y"),
                    help="nudge the art by X,Y units (right/down positive) to centre it by eye")
    args = ap.parse_args()

    src = Image.open(args.image).convert("RGB")
    w, h = src.size

    # Background = median of the four corner patches.
    patch = max(4, min(w, h) // 40)
    corners = [src.crop(box) for box in ((0, 0, patch, patch), (w - patch, 0, w, patch),
                                         (0, h - patch, patch, h), (w - patch, h - patch, w, h))]
    px = sorted(p for c in corners for p in getattr(c, "get_flattened_data", c.getdata)())
    bg = px[len(px) // 2]

    # Clean faint background noise (JPEG artefacts, smudges) back to the flat colour.
    d = ink(src, bg)
    noise = d.point(lambda v: 255 if v < 14 else 0)
    src = Image.composite(Image.new("RGB", src.size, bg), src, noise)

    if args.height != 1.0:
        src = src.resize((w, round(h * args.height)), Image.LANCZOS)
        w, h = src.size

    # Find the artwork (ignores soft drop shadows), its centre and its true radius.
    mask = ink(src, bg).point(lambda v: 255 if v > 60 else 0).filter(ImageFilter.MinFilter(3))
    bbox = mask.getbbox()
    if not bbox:
        sys.exit("Couldn't find any artwork in the image.")
    small = mask.resize((w // 8, h // 8))
    points = [((x + 0.5) * 8, (y + 0.5) * 8)
              for y in range(small.height) for x in range(small.width) if small.getpixel((x, y))]
    if args.center == "circle":
        cx, cy, _ = enclosing_circle(convex_hull(points))
    else:
        cx, cy = (bbox[0] + bbox[2]) / 2, (bbox[1] + bbox[3]) / 2
    radius = max(math.hypot(x - cx, y - cy) for x, y in points)

    scale = args.size * PX_PER_UNIT / radius
    art = src.resize((round(w * scale), round(h * scale)), Image.LANCZOS)
    fg = Image.new("RGB", (CANVAS, CANVAS), bg)
    fg.paste(art, (round(CANVAS / 2 - cx * scale + args.shift[0] * PX_PER_UNIT),
                   round(CANVAS / 2 - cy * scale + args.shift[1] * PX_PER_UNIT)))

    # Monochrome (Android themed icons).
    #   ink:   anything clearly not background is solid. Good for outline/line art.
    #   shade: darker parts are solid, lighter parts fade out. Good for filled
    #          shapes, where "ink" would give a featureless blob.
    solid = ink(fg, bg).point(lambda v: max(0, min(255, (v - 40) * 255 // 60)))
    if args.mono == "shade":
        lum = fg.convert("L")
        dark = lum.point(lambda v: max(0, min(255, (215 - v) * 255 // 95)))
        a = ImageChops.multiply(solid, dark)
    else:
        a = solid
    mono = Image.new("RGBA", (CANVAS, CANVAS), (255, 255, 255, 0))
    mono.putalpha(a)

    nodpi = os.path.join(RES, "drawable-nodpi")
    os.makedirs(nodpi, exist_ok=True)
    fg.save(os.path.join(nodpi, f"{args.name}_fg.png"), optimize=True)
    mono.save(os.path.join(nodpi, f"{args.name}_mono.png"), optimize=True)

    color = "#{:02X}{:02X}{:02X}".format(*bg)
    with open(os.path.join(RES, "drawable", f"{args.name}.xml"), "w") as fh:
        fh.write(f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by tools/import_art.py from {os.path.relpath(args.image, ROOT)} -->
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background>
        <color android:color="{color}" />
    </background>
    <foreground android:drawable="@drawable/{args.name}_fg" />
    <monochrome android:drawable="@drawable/{args.name}_mono" />
</adaptive-icon>
""")
    print(f"{args.name}: background {color}, artwork radius {radius:.0f}px -> {args.size} units")


if __name__ == "__main__":
    main()
