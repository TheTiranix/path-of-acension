#!/usr/bin/env python3
"""Dibuja una vista previa aproximada de un capitulo generado (fondo, carteles, lineas y formas) en build/quest_preview_<cap>.png.
No es el render real de FTB Quests: sirve para revisar la distribucion antes de abrir el juego.
Uso: python tools/preview_quests.py cap2 cap3 [--lang en|es]"""
import importlib
import math
import os
import sys

from PIL import Image, ImageDraw

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
import gen_quests as g  # noqa: E402

S = 34  # pixeles por unidad


def main(name, lang):
    mod = importlib.import_module("quest_data." + name)
    placed = g.layout(mod.SECTIONS, mod.GRID)
    bx, by, bw, bh = g.background_rect(placed, mod.SECTIONS)
    W, H = int(bw * S), int(bh * S)
    im = Image.open(os.path.join(g.TEX, mod.BG_OUT)).convert("RGB").resize((W, H))
    d = ImageDraw.Draw(im)
    pos = {p[0]["key"]: (p[1], p[2]) for p in placed}
    c = lambda x, y: ((x - bx) * S + S / 2, (y - by) * S + S / 2)  # noqa: E731
    for qq, x, y, deps in placed:
        for dep in deps:
            d.line(c(*pos[dep]) + c(x, y), fill=(170, 160, 140), width=2)
    for n, sec in enumerate(mod.SECTIONS):
        cx, top = sec["_banner"]
        ban = Image.open(os.path.join(g.TEX, "%s_s%d_%s.png" % (mod.PREFIX, n + 1, lang))).convert("RGBA").resize((int(g.BANNER_W * S), int(g.BANNER_H * S)))
        im.paste(ban, (int((cx - g.BANNER_W / 2 - bx) * S), int((top - by) * S)), ban)
    cols = {"base": (130, 130, 140), "craft": (170, 130, 80), "lore": (120, 140, 190), "place": (80, 170, 130), "boss": (210, 60, 70), "final": (230, 180, 70)}
    for qq, x, y, deps in placed:
        shape, size = g.KINDS[qq["kind"]]
        r = size * S / 2
        cx0, cy0 = c(x, y)
        col = cols[qq["kind"]]
        if shape == "circle":
            d.ellipse((cx0 - r, cy0 - r, cx0 + r, cy0 + r), fill=(25, 25, 30), outline=col, width=3)
        elif shape == "diamond":
            d.polygon([(cx0, cy0 - r), (cx0 + r, cy0), (cx0, cy0 + r), (cx0 - r, cy0)], fill=(25, 25, 30), outline=col)
        elif shape == "rsquare":
            d.rounded_rectangle((cx0 - r, cy0 - r, cx0 + r, cy0 + r), 8, fill=(25, 25, 30), outline=col, width=3)
        else:
            k = 6 if shape == "hexagon" else 8
            d.polygon([(cx0 + r * math.cos(2 * math.pi * i / k), cy0 + r * math.sin(2 * math.pi * i / k)) for i in range(k)], fill=(25, 25, 30), outline=col)
    out = os.path.join(ROOT, "build", "quest_preview_%s_%s.png" % (name, lang))
    im.save(out)
    print(out, im.size)


if __name__ == "__main__":
    args = [a for a in sys.argv[1:] if not a.startswith("--")]
    lang = "es" if "--lang=es" in sys.argv or ("--lang" in sys.argv and sys.argv[sys.argv.index("--lang") + 1] == "es") else "en"
    for n in args or ["cap2"]:
        main(n, lang)
