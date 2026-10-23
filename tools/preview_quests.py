#!/usr/bin/env python3
"""Dibuja una vista previa aproximada de un capitulo generado (fondo, carteles, lineas y formas) en build/quest_preview_<cap>.png.
No es el render real de FTB Quests: sirve para revisar la distribucion antes de abrir el juego."""
import importlib
import math
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
import gen_quests as g  # noqa: E402

S = 42  # pixeles por unidad


def main(name):
    mod = importlib.import_module("quest_data." + name)
    placed = g.layout(mod.SECTIONS)
    xs = [p[1] for p in placed]
    ys = [p[2] for p in placed]
    x0, x1, y0, y1 = min(xs) - 3, max(xs) + 3, min(ys) - 3.5, max(ys) + 3
    W, H = int((x1 - x0) * S), int((y1 - y0) * S)
    im = Image.new("RGB", (W, H), (10, 10, 14))
    tile = Image.open(os.path.join(g.TEX, mod.BG_OUT)).convert("RGB")
    flip = Image.open(os.path.join(g.TEX, mod.BG_OUT.replace(".png", "_f.png"))).convert("RGB")
    tw = max(32.0, (max(xs) - min(xs)) + 20)
    th = tw * 9 / 16
    cx = (min(xs) + max(xs)) / 2
    for i in range(int(math.ceil((max(ys) + 3.0 - (min(ys) - 3.5)) / th))):
        t = (tile if i % 2 == 0 else flip).resize((int(tw * S), int(th * S)))
        im.paste(t, (int((cx - tw / 2 - x0) * S), int((min(ys) - 3.5 + i * th - y0) * S)))
    d = ImageDraw.Draw(im)
    pos = {p[0]["key"]: (p[1], p[2]) for p in placed}
    for qq, x, y, deps in placed:
        for dep in deps:
            ax, ay = pos[dep]
            d.line(((ax - x0) * S + S / 2, (ay - y0) * S + S / 2, (x - x0) * S + S / 2, (y - y0) * S + S / 2), fill=(160, 150, 130), width=2)
    for n, sec in enumerate(mod.SECTIONS):
        bx, by = sec["_banner"]
        ban = Image.open(os.path.join(g.TEX, "%s_s%d.png" % (mod.PREFIX, n + 1))).convert("RGBA")
        ban = ban.resize((int(10 * S), int(1.5 * S)))
        im.paste(ban, (int((bx - 5 - x0) * S), int((by - 0.1 - y0) * S)), ban)
    cols = {"base": (120, 120, 130), "craft": (150, 120, 80), "lore": (110, 130, 170), "place": (80, 150, 120), "boss": (190, 50, 60), "final": (220, 170, 60)}
    for qq, x, y, deps in placed:
        shape, size = g.KINDS[qq["kind"]]
        r = size * S / 2
        cx0, cy0 = (x - x0) * S + S / 2, (y - y0) * S + S / 2
        c = cols[qq["kind"]]
        if shape == "circle":
            d.ellipse((cx0 - r, cy0 - r, cx0 + r, cy0 + r), fill=(25, 25, 30), outline=c, width=3)
        elif shape == "diamond":
            d.polygon([(cx0, cy0 - r), (cx0 + r, cy0), (cx0, cy0 + r), (cx0 - r, cy0)], fill=(25, 25, 30), outline=c)
        elif shape == "rsquare":
            d.rounded_rectangle((cx0 - r, cy0 - r, cx0 + r, cy0 + r), 8, fill=(25, 25, 30), outline=c, width=3)
        else:
            k = 6 if shape == "hexagon" else 8
            d.polygon([(cx0 + r * math.cos(2 * math.pi * i / k), cy0 + r * math.sin(2 * math.pi * i / k)) for i in range(k)], fill=(25, 25, 30), outline=c)
    out = os.path.join(ROOT, "build", "quest_preview_%s.png" % name)
    im.save(out)
    print(out, im.size)


if __name__ == "__main__":
    for n in sys.argv[1:] or ["cap1"]:
        main(n)
