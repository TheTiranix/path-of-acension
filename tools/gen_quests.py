#!/usr/bin/env python3
"""Genera los capitulos de FTB Quests (SNBT) y sus imagenes (fondos oscurecidos y carteles de seccion con Cinzel) desde tools/quest_data/capN.py.

Uso:  python tools/gen_quests.py [--ids build/quest_ids.json] [--instance <carpeta de la instancia>]...
  --ids       indice de ids reales (item/entity/structure/biome/dimension) para validar las tareas; los ids de minecraft: no se validan.
  --instance  ademas copia capitulos e imagenes a esa instancia (config/ftbquests y kubejs).
Escribe en modpack-config/ftbquests/quests/chapters y modpack-config/legacy-kubejs/assets/kubejs/textures/gui/quest.
"""
import argparse
import hashlib
import importlib
import json
import math
import os
import shutil
import sys

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "tools"))
QUESTS = os.path.join(ROOT, "modpack-config", "ftbquests", "quests")
TEX = os.path.join(ROOT, "modpack-config", "legacy-kubejs", "assets", "kubejs", "textures", "gui", "quest")
SRC_ART = os.path.join(ROOT, "tools", "quest_art")
FONT = os.path.join(ROOT, "src", "main", "resources", "assets", "tcorigenes", "font", "cinzel.ttf")
TEXREF = "kubejs:textures/gui/quest/"

# forma y tamano segun el tipo de mision
KINDS = {
    "base": ("circle", 1.0),
    "craft": ("rsquare", 1.0),
    "lore": ("diamond", 1.0),
    "place": ("octagon", 1.2),
    "boss": ("hexagon", 1.5),
    "final": ("hexagon", 2.0),
}
COLS = 7
DX, DY = 1.8, 1.9


def hid(*parts):
    return hashlib.md5("|".join(parts).encode("utf-8")).hexdigest()[:16].upper()


def s(text):
    return '"' + str(text).replace("\\", "\\\\").replace('"', '\\"').replace("\n", "\\n") + '"'


# ---- constructores de datos (los usan los capNN.py) ----
def item(i, n=1):
    return {"type": "item", "item": i, "count": n}


def kill(e, n=1):
    return {"type": "kill", "entity": e, "value": n}


def dim(d):
    return {"type": "dimension", "dimension": d}


def struct(x):
    return {"type": "structure", "structure": x}


def biome(b):
    return {"type": "biome", "biome": b}


def adv(a):
    return {"type": "advancement", "advancement": a, "criterion": ""}


def check(title=""):
    return {"type": "checkmark", "title": title}


def xp(n):
    return {"type": "xp", "xp": n}


def levels(n):
    return {"type": "xp_levels", "xp_levels": n}


def give(i, n=1):
    return {"type": "item", "item": i, "count": n}


def q(key, title, lore, tasks, rewards, icon=None, tip="", kind="base", sub=""):
    return {"key": key, "title": title, "lore": lore, "tip": tip, "tasks": tasks, "rewards": rewards, "icon": icon, "kind": kind, "sub": sub}


# ---- escritura SNBT ----
def task_snbt(chapter, key, i, t):
    out = ["\t\t\t\t{"]
    out.append("\t\t\t\t\tid: " + s(hid(chapter, key, "t%d" % i)))
    out.append("\t\t\t\t\ttype: " + s(t["type"]))
    if t["type"] == "item":
        out.append("\t\t\t\t\titem: " + s(t["item"]))
        if t["count"] > 1:
            out.append("\t\t\t\t\tcount: %dL" % t["count"])
    elif t["type"] == "kill":
        out.append("\t\t\t\t\tentity: " + s(t["entity"]))
        out.append("\t\t\t\t\tvalue: %dL" % t["value"])
    elif t["type"] in ("dimension", "structure", "biome"):
        out.append("\t\t\t\t\t%s: %s" % (t["type"], s(t[t["type"]])))
    elif t["type"] == "advancement":
        out.append("\t\t\t\t\tadvancement: " + s(t["advancement"]))
        out.append("\t\t\t\t\tcriterion: \"\"")
    elif t["type"] == "checkmark" and t["title"]:
        out.append("\t\t\t\t\ttitle: " + s(t["title"]))
    out.append("\t\t\t\t}")
    return "\n".join(out)


def reward_snbt(chapter, key, i, r):
    out = ["\t\t\t\t{", "\t\t\t\t\tid: " + s(hid(chapter, key, "r%d" % i)), "\t\t\t\t\ttype: " + s(r["type"])]
    if r["type"] == "item":
        out.append("\t\t\t\t\titem: " + s(r["item"]))
        if r["count"] > 1:
            out.append("\t\t\t\t\tcount: %d" % r["count"])
    elif r["type"] == "xp":
        out.append("\t\t\t\t\txp: %d" % r["xp"])
    elif r["type"] == "xp_levels":
        out.append("\t\t\t\t\txp_levels: %d" % r["xp_levels"])
    out.append("\t\t\t\t}")
    return "\n".join(out)


def layout(sections):
    """Devuelve [(quest, x, y, deps_keys)] en serpentina por seccion; cada seccion arranca del lado donde termino la anterior."""
    placed = []
    y = 0.0
    prev = None
    prev_x = None
    width = (COLS - 1) * DX
    for sec in sections:
        qs = sec["quests"]
        banner_y = y
        y += 1.6
        pts = []
        for i in range(len(qs)):
            row, col = divmod(i, COLS)
            if row % 2 == 1:
                col = COLS - 1 - col
            pts.append((col * DX, y + row * DY + 0.35 * math.sin(i * 0.9)))
        if prev_x is not None and abs(prev_x - (width - pts[0][0])) < abs(prev_x - pts[0][0]):
            pts = [(width - px, py) for px, py in pts]
        for i, qq in enumerate(qs):
            deps = [qs[i - 1]["key"]] if i > 0 else ([prev] if prev else [])
            placed.append((qq, round(pts[i][0], 2), round(pts[i][1], 2), deps))
        rows = (len(qs) - 1) // COLS + 1
        sec["_banner"] = (round(width / 2, 2), round(banner_y, 2))
        y += rows * DY + 0.8
        prev = qs[-1]["key"]
        prev_x = pts[-1][0]
    return placed


def check_ids(ids, kind, value, where, problems):
    if ids is None or value.startswith("minecraft:"):
        return
    if value not in ids.get(kind, []):
        problems.append("%s: %s '%s' no existe" % (where, kind, value))


def validate(chapter_name, placed, ids, problems):
    keys = [p[0]["key"] for p in placed]
    for k in set(keys):
        if keys.count(k) > 1:
            problems.append("%s: clave repetida %s" % (chapter_name, k))
    for qq, _, _, deps in placed:
        where = "%s/%s" % (chapter_name, qq["key"])
        if not qq["tasks"]:
            problems.append(where + ": sin tareas")
        for t in qq["tasks"]:
            if t["type"] == "item":
                check_ids(ids, "item", t["item"], where, problems)
            elif t["type"] == "kill":
                check_ids(ids, "entity", t["entity"], where, problems)
            elif t["type"] in ("dimension", "structure", "biome"):
                check_ids(ids, t["type"], t[t["type"]], where, problems)
        for r in qq["rewards"]:
            if r["type"] == "item":
                check_ids(ids, "item", r["item"], where, problems)


def image_snbt(name, x, y, w, h, order):
    return "\t\t{\n\t\t\timage: %s\n\t\t\tx: %.2fd\n\t\t\ty: %.2fd\n\t\t\twidth: %.2fd\n\t\t\theight: %.2fd\n\t\t\trotation: 0.0d\n\t\t\torder: %d\n\t\t}" % (
        s(TEXREF + name), x, y, w, h, order)


def chapter_snbt(mod, placed, order):
    name = mod.FILENAME
    cid = hid(name, "chapter")
    out = ["{", "\tdefault_hide_dependency_lines: false", "\tdefault_quest_shape: \"circle\"", "\tfilename: " + s(name), "\tid: " + s(cid)]
    out.append("\ticon: " + s(mod.ICON))
    # imagenes: fondo + carteles (el orden de la lista es el de dibujo)
    xs = [p[1] for p in placed]
    ys = [p[2] for p in placed]
    cx = (min(xs) + max(xs)) / 2
    y0, y1 = min(ys) - 3.5, max(ys) + 3.0
    tw = max(32.0, (max(xs) - min(xs)) + 20)
    th = tw * 9 / 16
    out.append("\timages: [")
    for i in range(int(math.ceil((y1 - y0) / th))):
        img = mod.BG_OUT if i % 2 == 0 else mod.BG_OUT.replace(".png", "_f.png")  # tiles alternados espejados: sin costura
        out.append(image_snbt(img, cx - tw / 2, y0 + i * th, tw, th, 0))
    for n, sec in enumerate(mod.SECTIONS):
        bx, by = sec["_banner"]
        w = 10.0
        h = w * 150 / 1000
        out.append(image_snbt("%s_s%d.png" % (mod.PREFIX, n + 1), bx - w / 2, by - 0.1, w, h, 1))
    out.append("\t]")
    out.append("\torder_index: %d" % order)
    out.append("\tquest_links: [ ]")
    out.append("\ttitle: " + s(mod.TITLE))
    out.append("\tquests: [")
    for qq, x, y, deps in placed:
        shape, size = KINDS[qq["kind"]]
        desc = [qq["lore"]]
        if qq["tip"]:
            desc.append("&7" + qq["tip"])
        out.append("\t\t{")
        out.append("\t\t\tid: " + s(hid(name, qq["key"])))
        out.append("\t\t\ttitle: " + s(qq["title"]))
        sub = qq["sub"] or ""
        if sub:
            out.append("\t\t\tsubtitle: " + s(sub))
        out.append("\t\t\tdescription: [" + " ".join(s(d) for d in desc) + "]")
        icon = qq["icon"]
        if icon is None and qq["tasks"] and qq["tasks"][0]["type"] == "item":
            icon = qq["tasks"][0]["item"]
        if icon:
            out.append("\t\t\ticon: " + s(icon))
        out.append("\t\t\tx: %.2fd" % x)
        out.append("\t\t\ty: %.2fd" % y)
        out.append("\t\t\tshape: " + s(shape))
        if size != 1.0:
            out.append("\t\t\tsize: %.1fd" % size)
        if deps:
            out.append("\t\t\tdependencies: [" + " ".join(s(hid(name, d)) for d in deps) + "]")
        out.append("\t\t\ttasks: [")
        out.extend(task_snbt(name, qq["key"], i, t) for i, t in enumerate(qq["tasks"]))
        out.append("\t\t\t]")
        out.append("\t\t\trewards: [")
        out.extend(reward_snbt(name, qq["key"], i, r) for i, r in enumerate(qq["rewards"]))
        out.append("\t\t\t]")
        out.append("\t\t}")
    out.append("\t]")
    out.append("}")
    return "\n".join(out) + "\n"


# ---- imagenes ----
def make_background(src, dst, bright):
    im = Image.open(src).convert("RGB")
    im.thumbnail((1280, 720), Image.LANCZOS)
    w, h = im.size
    lit = im.point(lambda v: min(255, int(v * bright)))
    # vineta suave hacia un velo carmesi oscuro en los bordes (se lee mejor el texto de las misiones)
    vig = Image.new("L", im.size, 0)
    ImageDraw.Draw(vig).ellipse((-w * 0.30, -h * 0.40, w * 1.30, h * 1.40), fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(min(w, h) * 0.20))
    out = Image.composite(lit, Image.new("RGB", im.size, (28, 6, 10)), vig.point(lambda v: int(80 + v * 175 / 255)))
    out.save(dst, optimize=True)
    out.transpose(Image.FLIP_TOP_BOTTOM).save(dst.replace(".png", "_f.png"), optimize=True)


def make_generated_background(dst, base, accent, seed):
    """Fondo sin imagen: degradado oscuro, brumas de color difusas y luciernagas chicas."""
    import random
    from PIL import ImageChops
    rnd = random.Random(seed)
    w, h = 1280, 720
    im = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(im)
    for y in range(h):
        t = y / h
        d.line((0, y, w, y), fill=tuple(int(base[0][i] * (1 - t) + base[1][i] * t) for i in range(3)))
    mist = Image.new("RGB", (w, h), (0, 0, 0))
    md = ImageDraw.Draw(mist)
    for _ in range(14):
        x, y, r = rnd.randrange(w), rnd.randrange(h), rnd.randrange(140, 300)
        c = tuple(int(a * rnd.uniform(0.10, 0.22)) for a in accent)
        md.ellipse((x - r, y - r // 2, x + r, y + r // 2), fill=c)
    im = ImageChops.add(im, mist.filter(ImageFilter.GaussianBlur(70)))
    glow = Image.new("RGB", (w, h), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for _ in range(130):
        x, y, r = rnd.randrange(w), rnd.randrange(h), rnd.choice((1, 1, 2, 2, 3))
        c = tuple(int(a * rnd.uniform(0.35, 0.9)) for a in accent)
        gd.ellipse((x - r, y - r, x + r, y + r), fill=c)
    im = ImageChops.add(im, glow.filter(ImageFilter.GaussianBlur(1.2)))
    vig = Image.new("L", (w, h), 0)
    ImageDraw.Draw(vig).ellipse((-w * 0.30, -h * 0.40, w * 1.30, h * 1.40), fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(h * 0.20)).point(lambda v: int(80 + v * 175 / 255))
    im = Image.composite(im, Image.new("RGB", (w, h), (20, 4, 8)), vig)
    im.save(dst, optimize=True)
    im.transpose(Image.FLIP_TOP_BOTTOM).save(dst.replace(".png", "_f.png"), optimize=True)


def make_banner(dst, kicker, title):
    W, H = 1000, 150
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    gold, crim = (226, 190, 110, 255), (176, 30, 44, 255)
    f_small = ImageFont.truetype(FONT, 26)
    f_big = ImageFont.truetype(FONT, 58)
    size = 58
    while size > 28:
        f_big = ImageFont.truetype(FONT, size)
        if d.textlength(title.upper(), font=f_big) < W - 160:
            break
        size -= 2
    kw = d.textlength(kicker.upper(), font=f_small)
    d.text(((W - kw) / 2, 14), kicker.upper(), font=f_small, fill=crim)
    tw = d.textlength(title.upper(), font=f_big)
    shadow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).text(((W - tw) / 2 + 2, 52 + 3), title.upper(), font=f_big, fill=(0, 0, 0, 230))
    im = Image.alpha_composite(shadow.filter(ImageFilter.GaussianBlur(3)), im)
    d = ImageDraw.Draw(im)
    d.text(((W - tw) / 2, 52), title.upper(), font=f_big, fill=gold)
    # filetes carmesi a los costados
    d.line((40, 98, (W - tw) / 2 - 24, 98), fill=crim, width=3)
    d.line(((W + tw) / 2 + 24, 98, W - 40, 98), fill=crim, width=3)
    d.line((40, 98, 40, 88), fill=crim, width=3)
    d.line((W - 40, 98, W - 40, 88), fill=crim, width=3)
    im.save(dst, optimize=True)


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--ids", default=os.path.join(ROOT, "build", "quest_ids.json"))
    ap.add_argument("--instance", action="append", default=[])
    ap.add_argument("--chapters", default="cap1,cap2,cap3")
    args = ap.parse_args()
    ids = json.load(open(args.ids)) if os.path.exists(args.ids) else None
    if ids is None:
        print("aviso: sin indice de ids, no se validan los ids de mods")
    os.makedirs(os.path.join(QUESTS, "chapters"), exist_ok=True)
    os.makedirs(TEX, exist_ok=True)
    problems, written = [], []
    for order, name in enumerate(args.chapters.split(",")):
        mod = importlib.import_module("quest_data." + name)
        placed = layout(mod.SECTIONS)
        validate(mod.FILENAME, placed, ids, problems)
        with open(os.path.join(QUESTS, "chapters", mod.FILENAME + ".snbt"), "w", encoding="utf-8", newline="\n") as f:
            f.write(chapter_snbt(mod, placed, order))
        src = os.path.join(SRC_ART, mod.BG_SRC) if mod.BG_SRC else None
        if src and os.path.exists(src):
            make_background(src, os.path.join(TEX, mod.BG_OUT), getattr(mod, "BG_BRIGHT", 0.7))
        else:
            make_generated_background(os.path.join(TEX, mod.BG_OUT), mod.BG_GRADIENT, mod.BG_ACCENT, order)
        for n, sec in enumerate(mod.SECTIONS):
            make_banner(os.path.join(TEX, "%s_s%d.png" % (mod.PREFIX, n + 1)), mod.KICKER, sec["name"])
        written.append((mod.FILENAME, len(placed)))
    with open(os.path.join(QUESTS, "chapter_groups.snbt"), "w", encoding="utf-8", newline="\n") as f:
        f.write("{\n\tchapter_groups: [\n\t]\n}\n")
    print("capitulos:", written)
    if problems:
        print("PROBLEMAS (%d):" % len(problems))
        for p in problems:
            print(" -", p)
    for inst in args.instance:
        dst_q = os.path.join(inst, "config", "ftbquests", "quests", "chapters")
        os.makedirs(dst_q, exist_ok=True)
        for fn in os.listdir(os.path.join(QUESTS, "chapters")):
            shutil.copy(os.path.join(QUESTS, "chapters", fn), os.path.join(dst_q, fn))
        shutil.copy(os.path.join(QUESTS, "chapter_groups.snbt"), os.path.join(inst, "config", "ftbquests", "quests", "chapter_groups.snbt"))
        dst_t = os.path.join(inst, "kubejs", "assets", "kubejs", "textures", "gui", "quest")
        os.makedirs(dst_t, exist_ok=True)
        for fn in os.listdir(TEX):
            shutil.copy(os.path.join(TEX, fn), os.path.join(dst_t, fn))
        print("copiado a", inst)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
