#!/usr/bin/env python3
"""Genera los capitulos de FTB Quests (SNBT) y sus imagenes (un fondo por capitulo y carteles de seccion con Cinzel) desde tools/quest_data/capN.py.

Idioma: ingles por defecto en modpack-config/ftbquests; la version en espanol va en modpack-config/ftbquests_es (los ids son los mismos, el progreso
se conserva al cambiar de idioma). Los textos en ingles estan en tools/quest_data/capN_en.py.
Un capitulo con SAVED (ruta de un .snbt guardado por el juego, p. ej. acomodado a mano en el editor) no se redistribuye: se conservan sus posiciones y
solo se cambia el idioma del texto (tools/quest_localize.py).

Uso:  python tools/gen_quests.py [--ids build/quest_ids.json] [--instance <carpeta de la instancia>] [--lang en]
  --ids       indice de ids reales (item/entity/structure/biome/dimension) para validar las tareas; los ids de minecraft: no se validan.
  --instance  ademas copia capitulos (idioma elegido con --lang, ingles por defecto) e imagenes a esa instancia (el juego debe estar cerrado).
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
OUT = {"en": os.path.join(ROOT, "modpack-config", "ftbquests", "quests"), "es": os.path.join(ROOT, "modpack-config", "ftbquests_es", "quests")}
TEX = os.path.join(ROOT, "modpack-config", "legacy-kubejs", "assets", "kubejs", "textures", "gui", "quest")
SRC_ART = os.path.join(ROOT, "tools", "quest_art")
FONT = os.path.join(ROOT, "src", "main", "resources", "assets", "tcorigenes", "font", "cinzel.ttf")
TEXREF = "kubejs:textures/gui/quest/"
CHAPTERS = ["cap1", "cap2", "cap3"]

# forma y tamano segun el tipo de mision
KINDS = {
    "base": ("circle", 1.0),
    "craft": ("rsquare", 1.0),
    "lore": ("diamond", 1.0),
    "place": ("octagon", 1.2),
    "boss": ("hexagon", 1.5),
    "final": ("hexagon", 2.0),
}
COLS_IN = 4          # misiones por fila dentro de una seccion
DX = DY = 1.75       # separacion entre misiones
BANNER_W, BANNER_H = 8.0, 1.2
HEAD = 1.5           # alto del cartel de seccion antes de la primera fila


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


# ---- idioma ----
def localized(mod, lang):
    """Devuelve (titulo, kicker, nombres de seccion, {key: quest con el texto en el idioma pedido})."""
    if lang == "es":
        return mod.TITLE, mod.KICKER, [sec["name"] for sec in mod.SECTIONS], {qq["key"]: qq for sec in mod.SECTIONS for qq in sec["quests"]}
    en = importlib.import_module("quest_data." + mod.__name__.split(".")[-1] + "_en").EN
    quests = {}
    for sec in mod.SECTIONS:
        for qq in sec["quests"]:
            title, lore, tip, check_title = en["quests"][qq["key"]]
            tasks = [dict(t, title=check_title) if t["type"] == "checkmark" else t for t in qq["tasks"]]
            quests[qq["key"]] = dict(qq, title=title, lore=lore, tip=tip, tasks=tasks)
    return en["title"], en["kicker"], en["sections"], quests


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


def image_snbt(name, x, y, w, h, order):
    return "\t\t{\n\t\t\timage: %s\n\t\t\tx: %.2fd\n\t\t\ty: %.2fd\n\t\t\twidth: %.2fd\n\t\t\theight: %.2fd\n\t\t\trotation: 0.0d\n\t\t\torder: %d\n\t\t}" % (
        s(TEXREF + name), x, y, w, h, order)


# ---- distribucion: secciones en cuadricula (serpentina), cada una con su cartel; todo cabe en una sola imagen de fondo ----
def section_block(n):
    pts = []
    for i in range(n):
        row, col = divmod(i, COLS_IN)
        if row % 2 == 1:
            col = COLS_IN - 1 - col
        pts.append((col * DX, row * DY + 0.25 * math.sin(i * 1.1)))
    ncols = min(n, COLS_IN)
    rows = (n - 1) // COLS_IN + 1
    return pts, (ncols - 1) * DX, (rows - 1) * DY + 1.0


def layout(sections, grid):
    """Devuelve [(quest, x, y, deps_keys)] y deja en cada seccion _banner=(x_centro, y_arriba)."""
    cols, rows = grid
    blocks = [section_block(len(sec["quests"])) for sec in sections]
    sh = max(b[2] for b in blocks) + HEAD + 2.6
    sw = max(BANNER_W + 2.5, sh * rows * (16 / 9) * 0.88 / cols)
    order = []
    for r in range(rows):
        for c in (range(cols) if r % 2 == 0 else range(cols - 1, -1, -1)):
            order.append((c, r))
    placed, prev, prev_pos = [], None, None
    for sec, (pts, w, h), (c, r) in zip(sections, blocks, order):
        cx = (c - (cols - 1) / 2) * sw
        cy = (r - (rows - 1) / 2) * sh + (0.9 if c % 2 else -0.9)
        top = cy - (HEAD + h) / 2
        left = cx - w / 2
        abs_pts = [(left + px, top + HEAD + py) for px, py in pts]
        if prev_pos is not None:  # empezar del lado mas cercano al final de la seccion anterior
            mirrored = [(2 * cx - x, y) for x, y in abs_pts]
            if math.hypot(prev_pos[0] - mirrored[0][0], prev_pos[1] - mirrored[0][1]) < math.hypot(prev_pos[0] - abs_pts[0][0], prev_pos[1] - abs_pts[0][1]):
                abs_pts = mirrored
        qs = sec["quests"]
        for i, qq in enumerate(qs):
            deps = [qs[i - 1]["key"]] if i > 0 else ([prev] if prev else [])
            placed.append((qq, round(abs_pts[i][0], 2), round(abs_pts[i][1], 2), deps))
        sec["_banner"] = (round(cx, 2), round(top, 2))
        prev = qs[-1]["key"]
        prev_pos = abs_pts[-1]
    return placed


def background_rect(placed, sections):
    xs = [p[1] for p in placed] + [sec["_banner"][0] - BANNER_W / 2 for sec in sections] + [sec["_banner"][0] + BANNER_W / 2 for sec in sections]
    ys = [p[2] for p in placed] + [sec["_banner"][1] for sec in sections]
    cx, cy = (min(xs) + max(xs)) / 2, (min(ys) + max(ys)) / 2
    w = max(max(xs) - min(xs) + 16, (max(ys) - min(ys) + 10) * 16 / 9)
    h = w * 9 / 16
    return cx - w / 2, cy - h / 2, w, h


# ---- validacion ----
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


def chapter_snbt(mod, placed, order, lang, title):
    name = mod.FILENAME
    out = ["{", "\tdefault_hide_dependency_lines: false", "\tdefault_quest_shape: \"circle\"", "\tfilename: " + s(name), "\tid: " + s(hid(name, "chapter"))]
    out.append("\ticon: " + s(mod.ICON))
    bx0, by0, bw, bh = background_rect([(qq, x, y, d) for qq, x, y, d in placed], mod.SECTIONS)
    out.append("\timages: [")
    out.append(image_snbt(mod.BG_OUT, bx0, by0, bw, bh, 0))
    for n, sec in enumerate(mod.SECTIONS):
        cx, top = sec["_banner"]
        out.append(image_snbt("%s_s%d_%s.png" % (mod.PREFIX, n + 1, lang), cx - BANNER_W / 2, top, BANNER_W, BANNER_H, 1))
    out.append("\t]")
    out.append("\torder_index: %d" % order)
    out.append("\tquest_links: [ ]")
    out.append("\ttitle: " + s(title))
    out.append("\tquests: [")
    for qq, x, y, deps in placed:
        shape, size = KINDS[qq["kind"]]
        desc = [qq["lore"]] + (["&7" + qq["tip"]] if qq["tip"] else [])
        out.append("\t\t{")
        out.append("\t\t\tid: " + s(hid(name, qq["key"])))
        out.append("\t\t\ttitle: " + s(qq["title"]))
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
    if im.width < 1600:
        im = im.resize((1600, int(im.height * 1600 / im.width)), Image.LANCZOS)  # la de 800 px: suavizada, no a pixeles
    w, h = im.size
    lit = im.point(lambda v: min(255, int(v * bright)))
    # vineta suave hacia un velo carmesi oscuro en los bordes (se lee mejor el texto de las misiones)
    vig = Image.new("L", im.size, 0)
    ImageDraw.Draw(vig).ellipse((-w * 0.30, -h * 0.40, w * 1.30, h * 1.40), fill=255)
    vig = vig.filter(ImageFilter.GaussianBlur(min(w, h) * 0.20))
    Image.composite(lit, Image.new("RGB", im.size, (28, 6, 10)), vig.point(lambda v: int(80 + v * 175 / 255))).save(dst, optimize=True)


def make_banner(dst, kicker, title):
    W, H = 1000, 150
    im = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    gold, crim = (226, 190, 110, 255), (176, 30, 44, 255)
    f_small = ImageFont.truetype(FONT, 26)
    size = 58
    while size > 28:
        f_big = ImageFont.truetype(FONT, size)
        if d.textlength(title.upper(), font=f_big) < W - 160:
            break
        size -= 2
    f_big = ImageFont.truetype(FONT, size)
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


def make_generated_background(dst, base, accent, seed):
    """Fondo sin imagen: degradado oscuro, brumas de color difusas y luciernagas chicas."""
    import random
    from PIL import ImageChops
    rnd = random.Random(seed)
    w, h = 1600, 900
    im = Image.new("RGB", (w, h))
    d = ImageDraw.Draw(im)
    for y in range(h):
        t = y / h
        d.line((0, y, w, y), fill=tuple(int(base[0][i] * (1 - t) + base[1][i] * t) for i in range(3)))
    mist = Image.new("RGB", (w, h), (0, 0, 0))
    md = ImageDraw.Draw(mist)
    for _ in range(14):
        x, y, r = rnd.randrange(w), rnd.randrange(h), rnd.randrange(140, 300)
        md.ellipse((x - r, y - r // 2, x + r, y + r // 2), fill=tuple(int(a * rnd.uniform(0.10, 0.22)) for a in accent))
    im = ImageChops.add(im, mist.filter(ImageFilter.GaussianBlur(70)))
    glow = Image.new("RGB", (w, h), (0, 0, 0))
    gd = ImageDraw.Draw(glow)
    for _ in range(130):
        x, y, r = rnd.randrange(w), rnd.randrange(h), rnd.choice((1, 1, 2, 2, 3))
        gd.ellipse((x - r, y - r, x + r, y + r), fill=tuple(int(a * rnd.uniform(0.35, 0.9)) for a in accent))
    ImageChops.add(im, glow.filter(ImageFilter.GaussianBlur(1.2))).save(dst, optimize=True)


def main():
    import quest_localize
    ap = argparse.ArgumentParser()
    ap.add_argument("--ids", default=os.path.join(ROOT, "build", "quest_ids.json"))
    ap.add_argument("--instance", action="append", default=[])
    ap.add_argument("--lang", default="en", choices=["en", "es"], help="idioma que se copia a --instance")
    args = ap.parse_args()
    ids = json.load(open(args.ids)) if os.path.exists(args.ids) else None
    if ids is None:
        print("aviso: sin indice de ids, no se validan los ids de mods")
    os.makedirs(TEX, exist_ok=True)
    problems, written = [], []
    for lang in ("en", "es"):
        os.makedirs(os.path.join(OUT[lang], "chapters"), exist_ok=True)
        for old in os.listdir(os.path.join(OUT[lang], "chapters")):
            os.remove(os.path.join(OUT[lang], "chapters", old))
    for order, name in enumerate(CHAPTERS):
        mod = importlib.import_module("quest_data." + name)
        placed = layout(mod.SECTIONS, mod.GRID)
        validate(mod.FILENAME, placed, ids, problems)
        src = os.path.join(SRC_ART, mod.BG_SRC) if mod.BG_SRC else None
        if src and os.path.exists(src):
            make_background(src, os.path.join(TEX, mod.BG_OUT), getattr(mod, "BG_BRIGHT", 0.7))
        else:
            make_generated_background(os.path.join(TEX, mod.BG_OUT), mod.BG_GRADIENT, mod.BG_ACCENT, order)
        saved = getattr(mod, "SAVED", None)
        saved_text = open(saved, encoding="utf-8").read() if saved and os.path.exists(saved) else None
        for lang in ("en", "es"):
            title, kicker, sec_names, quests = localized(mod, lang)
            if saved_text is not None:  # capitulo acomodado a mano: se conservan sus posiciones, solo cambia el texto
                texts = {k: (qq["title"], qq["lore"], qq["tip"], next((t["title"] for t in qq["tasks"] if t["type"] == "checkmark"), "")) for k, qq in quests.items()}
                aliases = {}
                for other in ("en", "es"):
                    for k, qq in localized(mod, other)[3].items():
                        aliases[s(qq["title"])[1:-1]] = k
                body = quest_localize.localize_saved(saved_text, mod.FILENAME, texts, title, mod.PREFIX, lang, aliases)
            else:
                body = chapter_snbt(mod, [(quests[qq["key"]], x, y, deps) for qq, x, y, deps in placed], order, lang, title)
            with open(os.path.join(OUT[lang], "chapters", mod.FILENAME + ".snbt"), "w", encoding="utf-8", newline="\n") as f:
                f.write(body)
            for n, nm in enumerate(sec_names):
                make_banner(os.path.join(TEX, "%s_s%d_%s.png" % (mod.PREFIX, n + 1, lang)), kicker, nm)
        written.append((mod.FILENAME, len(placed), "posiciones guardadas a mano" if saved_text is not None else "distribucion generada"))
    for lang in ("en", "es"):
        with open(os.path.join(OUT[lang], "chapter_groups.snbt"), "w", encoding="utf-8", newline="\n") as f:
            f.write("{\n\tchapter_groups: [\n\t]\n}\n")
    for row in written:
        print("capitulo:", row)
    if problems:
        print("PROBLEMAS (%d):" % len(problems))
        for p in problems:
            print(" -", p)
    for inst in args.instance:
        dst_q = os.path.join(inst, "config", "ftbquests", "quests", "chapters")
        os.makedirs(dst_q, exist_ok=True)
        for fn in os.listdir(dst_q):
            if fn.startswith("capitulo_"):
                os.remove(os.path.join(dst_q, fn))
        for fn in os.listdir(os.path.join(OUT[args.lang], "chapters")):
            shutil.copy(os.path.join(OUT[args.lang], "chapters", fn), os.path.join(dst_q, fn))
        shutil.copy(os.path.join(OUT[args.lang], "chapter_groups.snbt"), os.path.join(inst, "config", "ftbquests", "quests", "chapter_groups.snbt"))
        dst_t = os.path.join(inst, "kubejs", "assets", "kubejs", "textures", "gui", "quest")
        os.makedirs(dst_t, exist_ok=True)
        for fn in os.listdir(dst_t):
            if fn.startswith(("c1_", "c2_", "c3_", "cap1_", "cap2_", "cap3_")):
                os.remove(os.path.join(dst_t, fn))
        for fn in os.listdir(TEX):
            shutil.copy(os.path.join(TEX, fn), os.path.join(dst_t, fn))
        print("copiado a", inst, "(%s)" % args.lang)
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
