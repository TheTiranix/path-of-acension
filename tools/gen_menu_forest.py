"""Texturas del fondo animado del menu principal: bosque oscuro, lluvia, farol y rayos (ver client/MenuScene.java).

Cada capa existe en version oscura (lo normal) y version "iluminada" (la que se ve durante un rayo: todo rojo, con cosas turbias).
Todo se dibuja por codigo: no usa arte de ningun juego ni modpack.

Uso:  python tools/gen_menu_forest.py
Salida: src/main/resources/assets/tcorigenes/textures/gui/menu/
IMPORTANTE: LANTERN_X / LANTERN_Y (en pixeles de la textura de 1920x1080) tienen que coincidir con los de MenuScene.java.
"""
import math
import os
import random

import numpy as np
from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tcorigenes", "textures", "gui", "menu")
W, H = 1920, 1080
LANTERN_X, LANTERN_Y = 1230, 470
random.seed(31102026)
rng = np.random.default_rng(31102026)


def fbm(w, h, octaves=((4, 3, 1.0), (9, 5, 0.6), (20, 11, 0.3), (44, 24, 0.15))):
    total = np.zeros((h, w), dtype=np.float32)
    for gw, gh, amp in octaves:
        small = Image.fromarray((rng.random((gh, gw)) * 255).astype(np.uint8))
        total += np.asarray(small.resize((w, h), Image.BICUBIC), dtype=np.float32) / 255.0 * amp
    total -= total.min()
    return total / total.max()


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    img.save(os.path.join(OUT, name), optimize=True)


# ------------------------------------------------------------------ arboles
def make_tree(x, base, length, width, depth, spread=0.55):
    """Lista de segmentos (x1, y1, x2, y2, ancho) de un arbol muerto."""
    segs = []

    def grow(px, py, ang, ln, wd, d):
        if d == 0 or ln < 5:
            return
        nx, ny = px + math.cos(ang) * ln, py - math.sin(ang) * ln
        segs.append((px, py, nx, ny, max(1.0, wd)))
        for da in (random.uniform(0.25, spread), -random.uniform(0.25, spread)):
            if random.random() < 0.88:
                grow(nx, ny, ang + da + random.uniform(-0.2, 0.2), ln * random.uniform(0.6, 0.78), wd * 0.66, d - 1)

    grow(x, base, math.pi / 2 + random.uniform(-0.08, 0.08), length, width, depth)
    return segs


def draw_segs(d, segs, color):
    for x1, y1, x2, y2, w in segs:
        d.line((x1, y1, x2, y2), fill=color, width=int(w))
        r = w / 2.0
        d.ellipse((x2 - r, y2 - r, x2 + r, y2 + r), fill=color)


def layer_trees(kind):
    """Devuelve los arboles de cada capa (se usan para las dos versiones)."""
    trees = []
    if kind == "far":
        for x in range(-30, W + 60, 70):
            trees.append(make_tree(x + random.randint(-25, 25), random.randint(760, 840), random.randint(330, 560), random.randint(9, 15), 6, 0.45))
    elif kind == "mid":
        for x in (80, 330, 560, 820, 1010, 1330, 1560, 1790):
            trees.append(make_tree(x + random.randint(-30, 30), 1010 + random.randint(-20, 30), random.randint(330, 470), random.randint(30, 46), 7))
    else:  # near
        for x, ln, wd in ((30, 640, 105), (1880, 700, 120), (1500, 520, 70)):
            trees.append(make_tree(x, 1120, ln, wd, 7, 0.6))
    return trees


def render_layer(trees, color, lit_extra=None, haze=None):
    img = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for t in trees:
        draw_segs(d, t, color)
    if lit_extra:
        lit_extra(img, d, trees)
    if haze is not None:  # la niebla se come la base de los arboles lejanos
        a = np.asarray(img, dtype=np.float32)
        ramp = np.clip((np.arange(H)[:, None] - haze) / 220.0, 0, 1)
        a[..., 3] *= 1.0 - 0.8 * ramp
        img = Image.fromarray(a.astype(np.uint8), "RGBA")
    return img.filter(ImageFilter.GaussianBlur(1.1))


# ------------------------------------------------- cosas turbias (solo con rayo)
def hanging_body(d, x, y, ropelen, scale=1.0):
    """Silueta de un cuerpo colgado de una rama."""
    s = scale
    top = y + ropelen
    d.line((x, y, x, top), fill=(60, 14, 14, 255), width=3)
    d.ellipse((x - 11 * s, top, x + 11 * s, top + 24 * s), fill=(54, 12, 14, 255))  # cabeza
    d.polygon([(x - 17 * s, top + 24 * s), (x + 17 * s, top + 24 * s), (x + 11 * s, top + 100 * s), (x - 11 * s, top + 100 * s)], fill=(54, 12, 14, 255))
    d.line((x - 15 * s, top + 28 * s, x - 22 * s, top + 92 * s), fill=(54, 12, 14, 255), width=int(8 * s))
    d.line((x + 15 * s, top + 28 * s, x + 20 * s, top + 94 * s), fill=(54, 12, 14, 255), width=int(8 * s))
    d.line((x - 6 * s, top + 98 * s, x - 8 * s, top + 160 * s), fill=(54, 12, 14, 255), width=int(9 * s))
    d.line((x + 6 * s, top + 98 * s, x + 9 * s, top + 158 * s), fill=(54, 12, 14, 255), width=int(9 * s))
    d.ellipse((x - 6 * s, top + 8 * s, x - 2 * s, top + 12 * s), fill=(255, 220, 200, 255))  # ojos
    d.ellipse((x + 2 * s, top + 8 * s, x + 6 * s, top + 12 * s), fill=(255, 220, 200, 255))


def blood_drips(d, trees, count, col):
    for t in trees:
        for x1, y1, x2, y2, w in random.sample(t, min(len(t), count)):
            if w < 3:
                continue
            ln = random.randint(30, 120)
            d.line((x2, y2, x2 + random.randint(-3, 3), y2 + ln), fill=col, width=random.randint(2, 5))
            d.ellipse((x2 - 3, y2 + ln - 3, x2 + 4, y2 + ln + 5), fill=col)


def extra_far(img, d, trees):
    for _ in range(26):  # ojos en la oscuridad del bosque
        x, y = random.randint(40, W - 40), random.randint(420, 760)
        gap = random.randint(10, 17)
        col = random.choice([(255, 90, 70, 255), (255, 230, 200, 255)])
        for dx in (-gap // 2, gap // 2):
            d.ellipse((x + dx - 3, y - 2, x + dx + 3, y + 3), fill=col)
    gx, gy = 1480, 790  # una figura alta y flaca entre los arboles
    d.line((gx, gy, gx, gy - 330), fill=(40, 10, 12, 255), width=9)
    d.ellipse((gx - 12, gy - 372, gx + 12, gy - 330), fill=(40, 10, 12, 255))
    d.line((gx, gy - 300, gx - 70, gy - 90), fill=(40, 10, 12, 255), width=5)
    d.line((gx, gy - 300, gx + 74, gy - 80), fill=(40, 10, 12, 255), width=5)
    d.ellipse((gx - 6, gy - 362, gx - 1, gy - 356), fill=(255, 230, 200, 255))
    d.ellipse((gx + 3, gy - 362, gx + 8, gy - 356), fill=(255, 230, 200, 255))


def extra_mid(img, d, trees):
    blood_drips(d, trees, 6, (120, 10, 14, 255))
    for t, ln in ((trees[2], 120), (trees[4], 170), (trees[6], 90)):  # cuerpos colgados de las ramas mas altas
        seg = min(t, key=lambda s: s[3])
        hanging_body(d, seg[2], seg[3], ln, 1.15)
    for _ in range(14):  # manchas de sangre en los troncos
        t = random.choice(trees)
        x1, y1, x2, y2, w = t[0]
        px = x1 + random.randint(-int(w / 2), int(w / 2))
        py = random.randint(700, 1000)
        d.line((px, py, px + random.randint(-6, 6), py + random.randint(60, 230)), fill=(110, 8, 12, 230), width=random.randint(5, 14))


def extra_near(img, d, trees):
    for tx, ty in ((120, 760), (1790, 600)):  # manos marcadas en los troncos
        for k in range(4):
            d.line((tx + k * 13, ty, tx + k * 13 - 3, ty - 46), fill=(150, 10, 14, 255), width=7)
        d.ellipse((tx - 4, ty - 4, tx + 56, ty + 52), fill=(150, 10, 14, 255))
    for _ in range(5):  # aranazos
        sx, sy = random.randint(0, 220), random.randint(300, 900)
        for k in range(4):
            d.line((sx + k * 11, sy, sx + k * 11 + 18, sy + 150), fill=(40, 6, 8, 255), width=3)
    d.ellipse((260, 1010, 1100, 1160), fill=(110, 6, 10, 235))  # charco de sangre al frente


def lantern(img):
    """Farol colgado de una rama (parte de la capa del medio, siempre visible)."""
    d = ImageDraw.Draw(img)
    x, y = LANTERN_X, LANTERN_Y
    d.line((x, 0, x, y - 34), fill=(8, 8, 9, 255), width=4)
    d.rectangle((x - 10, y - 38, x + 10, y - 28), fill=(10, 10, 12, 255))
    d.rectangle((x - 16, y - 28, x + 16, y + 30), fill=(14, 12, 12, 255))
    d.rectangle((x - 11, y - 22, x + 11, y + 24), fill=(255, 176, 70, 255))
    d.rectangle((x - 6, y - 14, x + 6, y + 14), fill=(255, 228, 140, 255))
    d.rectangle((x - 18, y + 28, x + 18, y + 36), fill=(10, 10, 12, 255))


# ------------------------------------------------------------------ capas
def make_sky():
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    cl = fbm(W, H, ((3, 2, 1.0), (7, 4, 0.55), (16, 9, 0.3))) ** 1.2
    t = yy / H
    dark = np.zeros((H, W, 3), dtype=np.float32)
    dark[..., 0] = 5 + 8 * t + 10 * cl
    dark[..., 1] = 10 + 14 * t + 12 * cl
    dark[..., 2] = 13 + 16 * t + 12 * cl
    lit = np.zeros((H, W, 3), dtype=np.float32)
    lit[..., 0] = 120 + 90 * t + 40 * cl
    lit[..., 1] = 130 + 10 * t + 40 * cl - 80 * t
    lit[..., 2] = 160 - 60 * t + 30 * cl
    glow = np.exp(-(np.sqrt((xx - W * 0.6) ** 2 + (yy - H * 0.35) ** 2) / 520.0) ** 2)
    lit += glow[..., None] * np.array([70, 55, 60], dtype=np.float32)
    for arr, name in ((dark, "sky.png"), (lit, "sky_lit.png")):
        arr += rng.normal(0, 2.5, (H, W, 1)).astype(np.float32)
        save(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)), name)


def make_layers():
    far, mid, near = layer_trees("far"), layer_trees("mid"), layer_trees("near")
    img = render_layer(far, (8, 13, 17, 255), haze=620)
    save(img, "far.png")
    save(render_layer(far, (64, 18, 24, 255), extra_far, haze=620), "far_lit.png")
    img = render_layer(mid, (3, 5, 7, 255))
    lantern(img)
    save(img, "mid.png")
    save(render_layer(mid, (96, 24, 28, 255), extra_mid), "mid_lit.png")
    save(render_layer(near, (1, 2, 3, 255)), "near.png")
    save(render_layer(near, (44, 9, 12, 255), extra_near), "near_lit.png")


def make_rain():
    img = Image.new("RGBA", (256, 256), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for _ in range(46):
        x, y = random.randint(0, 255), random.randint(0, 255)
        ln = random.randint(16, 42)
        a = random.randint(90, 190)
        for dx, dy in ((0, 0), (256, 0), (-256, 0), (0, 256), (0, -256)):  # que cierre bien al repetirse
            d.line((x + dx, y + dy, x + dx + ln * 0.22, y + dy + ln), fill=(176, 196, 214, a), width=random.choice((1, 1, 2)))
    save(img, "rain.png")


def make_fog():
    a = (fbm(512, 256) ** 1.7 * 0.9).clip(0, 1)
    a = np.concatenate([a[:, :256], a[:, :256][:, ::-1]], axis=1)  # espejo: cierra en horizontal
    out = np.zeros((256, 512, 4), dtype=np.uint8)
    out[..., 0], out[..., 1], out[..., 2] = 86, 100, 108
    out[..., 3] = (a * 150).astype(np.uint8)
    save(Image.fromarray(out, "RGBA"), "fog.png")


def make_glow():
    yy, xx = np.mgrid[0:256, 0:256].astype(np.float32)
    r = np.sqrt((xx - 128) ** 2 + (yy - 128) ** 2) / 128.0
    a = np.clip(1.0 - r, 0, 1) ** 2.2
    out = np.zeros((256, 256, 4), dtype=np.uint8)
    out[..., 0], out[..., 1], out[..., 2] = 255, 170, 80
    out[..., 3] = (a * 255).astype(np.uint8)
    save(Image.fromarray(out, "RGBA"), "glow.png")


def make_vignette():
    yy, xx = np.mgrid[0:H, 0:W].astype(np.float32)
    nx, ny = (xx - W * 0.55) / (W * 0.6), (yy - H / 2) / (H * 0.6)
    a = np.clip(nx ** 2 * 0.7 + ny ** 2 * 0.9, 0, 1) ** 1.2 * 0.85
    a += np.clip(1.0 - xx / (W * 0.3), 0, 1) * 0.25  # mas oscuro a la izquierda: se leen los botones
    out = np.zeros((H, W, 4), dtype=np.uint8)
    out[..., 3] = (np.clip(a, 0, 1) * 255).astype(np.uint8)
    save(Image.fromarray(out, "RGBA"), "vignette.png")


if __name__ == "__main__":
    make_sky()
    make_layers()
    make_rain()
    make_fog()
    make_glow()
    make_vignette()
    print("ok")
