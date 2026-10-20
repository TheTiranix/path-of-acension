"""Escena original del menu principal, en capas para el efecto de profundidad (parallax) de FancyMenu.

Un lago al anochecer con abetos, hierba naranja y una mesa de madera con un farol en primer plano, dibujado en una grilla de 480x270 y
ampliado a 1920x1080 con pixeles grandes (estilo bloques). Todo se dibuja por codigo: no usa arte de ningun juego ni modpack.

Capas (de atras hacia adelante): menu_bg.png (cielo, luna, bosque lejano y lago, opaca), menu_l1_far.png (abetos de la orilla),
menu_l2_mid.png (hierba), menu_l3_near.png (mesa con farol y hierba del frente). Las de alfa se estiran a la pantalla en el layout de
FancyMenu (modpack-config/fancymenu/customization/titulo_tetrico.txt), donde se ajusta la intensidad de cada una.

Uso:  python tools/gen_menu_scene.py
Salida: modpack-config/fancymenu/assets/
"""
import os
import random

import numpy as np
from PIL import Image, ImageChops, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(__file__), "..", "modpack-config", "fancymenu", "assets")
GW, GH, S = 480, 270, 4  # grilla chica y factor de ampliacion
random.seed(20261031)
rng = np.random.default_rng(20261031)


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def save(img, name):
    img.resize((GW * S, GH * S), Image.NEAREST).save(os.path.join(ROOT, name))


def sky():
    img = Image.new("RGB", (GW, GH))
    d = ImageDraw.Draw(img)
    stops = [(0.0, (8, 10, 36)), (0.45, (34, 30, 88)), (0.62, (110, 52, 110)), (0.72, (226, 112, 92))]
    for y in range(GH):
        t = y / 160.0
        t = min(t, 1.0)
        for (t0, c0), (t1, c1) in zip(stops, stops[1:]):
            if t0 <= t <= t1:
                d.line((0, y, GW, y), fill=lerp(c0, c1, (t - t0) / (t1 - t0)))
    for _ in range(140):  # estrellas
        x, y = random.randrange(GW), random.randrange(100)
        v = random.randint(150, 255)
        d.point((x, y), fill=(v, v, min(255, v + 10)))
    mx, my = 332, 46  # luna cuadrada, como en el juego
    d.rectangle((mx - 15, my - 15, mx + 14, my + 14), fill=(238, 214, 206))
    d.rectangle((mx - 9, my - 6, mx - 4, my - 1), fill=(206, 176, 176))
    d.rectangle((mx + 3, my + 4, mx + 9, my + 9), fill=(206, 176, 176))
    return img


def fir(draw, x, base, h, color):
    """Abeto de bloques: capas que se ensanchan hacia abajo."""
    layers = max(4, h // 7)
    for i in range(layers):
        y = base - h + int(i * h / layers)
        half = 2 + int(i * (h / 8.0) / layers) + (1 if i else 0)
        draw.rectangle((x - half, y, x + half, y + int(h / layers) + 1), fill=color)
    draw.rectangle((x - 1, base, x + 1, base + 4), fill=(color[0] // 2, color[1] // 2, color[2] // 2))


def far_forest_and_lake():
    img = sky()
    d = ImageDraw.Draw(img)
    horizon = 150
    d.rectangle((0, horizon - 6, GW, horizon + 3), fill=(14, 20, 44))  # franja de orilla lejana
    for x in range(-5, GW + 10, 9):
        fir(d, x + random.randint(-2, 2), horizon + 2, random.randint(26, 52), (12 + random.randint(0, 6), 22 + random.randint(0, 8), 46 + random.randint(0, 8)))
    # lago: refleja el cielo mas oscuro y los abetos invertidos
    lake_top = horizon + 3
    ref = img.crop((0, lake_top - 52, GW, lake_top)).transpose(Image.FLIP_TOP_BOTTOM)
    lake = Image.new("RGB", (GW, GH - lake_top))
    lake.paste(ref.resize((GW, 52)), (0, 0))
    arr = np.asarray(lake, dtype=np.float32) * 0.55 + np.array([10, 14, 40], dtype=np.float32) * 0.45
    for y in range(arr.shape[0]):  # ondas horizontales
        if y % 3 == 0:
            arr[y] *= (1.0 + 0.16 * np.sin(np.arange(GW) / 5.0 + y))[:, None]
    img.paste(Image.fromarray(np.clip(arr, 0, 255).astype(np.uint8)), (0, lake_top))
    # niebla suave en la linea del agua
    mist = Image.new("RGBA", (GW, GH), (0, 0, 0, 0))
    md = ImageDraw.Draw(mist)
    for y in range(horizon - 4, horizon + 14):
        a = int(120 * max(0.0, 1 - abs(y - horizon - 3) / 11.0))
        md.line((0, y, GW, y), fill=(150, 130, 190, a))
    img = Image.alpha_composite(img.convert("RGBA"), mist).convert("RGB")
    save(img, "menu_bg.png")


def shore_firs():
    img = Image.new("RGBA", (GW, GH), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    for x, base, h in ((14, 196, 118), (52, 200, 84), (-6, 206, 150), (440, 198, 124), (470, 204, 150), (402, 196, 70)):
        fir(d, x, base, h, (6, 14, 28, 255))
    save(img, "menu_l1_far.png")


def grass_field():
    img = Image.new("RGBA", (GW, GH), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rectangle((0, 214, GW, GH), fill=(40, 16, 8, 255))
    palette = [(226, 112, 28), (196, 76, 20), (150, 44, 16), (240, 150, 50), (110, 30, 12)]
    for _ in range(3200):
        x = random.randrange(-2, GW)
        y = random.randrange(204, GH)
        hgt = random.randint(3, 6 + (y - 204) // 6)
        c = random.choice(palette)
        d.line((x, y, x + random.randint(-1, 1), y - hgt), fill=c + (255,), width=1 + (random.random() < 0.3))
    save(img, "menu_l2_mid.png")


def table_scene():
    img = Image.new("RGBA", (GW, GH), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    wood = [(150, 36, 28), (176, 46, 34), (126, 28, 24)]
    # tablero en perspectiva (derecha-abajo): tablones de madera roja
    top = [(250, 190), (480, 150), (480, 214), (292, 252)]
    d.polygon(top, fill=wood[0])
    for k in range(1, 6):
        t = k / 6.0
        p0 = (250 + (292 - 250) * t, 190 + (252 - 190) * t)
        p1 = (480, 150 + (214 - 150) * t)
        d.line((*p0, *p1), fill=wood[2], width=1)
    d.polygon([(250, 190), (292, 252), (292, 262), (250, 200)], fill=(96, 20, 20))  # canto frontal
    d.rectangle((266, 214, 274, GH), fill=(84, 18, 18))  # pata
    d.rectangle((420, 186, 432, GH), fill=(84, 18, 18))
    # farol sobre la mesa
    lx, ly = 440, 150
    d.rectangle((lx - 7, ly - 2, lx + 7, ly + 22), fill=(230, 150, 40))
    d.rectangle((lx - 4, ly + 2, lx + 4, ly + 18), fill=(255, 226, 120))
    d.rectangle((lx - 9, ly - 6, lx + 9, ly - 1), fill=(40, 24, 18))
    d.rectangle((lx - 9, ly + 22, lx + 9, ly + 26), fill=(40, 24, 18))
    d.rectangle((lx - 2, ly - 11, lx + 2, ly - 6), fill=(40, 24, 18))
    # fotos viejas, apoyadas en la mesa
    for (x, y, ang) in ((330, 196, -0.18), (372, 184, 0.12)):
        ph = Image.new("RGBA", (40, 28), (232, 220, 206, 255))
        pd = ImageDraw.Draw(ph)
        pd.rectangle((3, 3, 36, 24), fill=(34, 40, 90, 255))
        pd.rectangle((3, 16, 36, 24), fill=(20, 24, 40, 255))
        pd.rectangle((24, 7, 29, 12), fill=(226, 200, 190, 255))
        ph = ph.rotate(ang * 57.3, expand=True)
        img.alpha_composite(ph, (x, y))
    # un cuaderno cerrado y una vela
    d.rectangle((296, 176, 322, 190), fill=(30, 18, 16, 255))
    d.rectangle((298, 178, 320, 180), fill=(120, 24, 24, 255))
    # hierba del primer plano, borrosa en el borde inferior
    for _ in range(700):
        x, y = random.randrange(0, 300), random.randrange(236, GH)
        d.line((x, y, x + random.randint(-1, 1), y - random.randint(6, 18)), fill=random.choice([(226, 112, 28, 255), (150, 44, 16, 255), (240, 150, 50, 255)]), width=2)
    big = img.resize((GW * S, GH * S), Image.NEAREST)
    # resplandor calido del farol (a resolucion completa, suave)
    glow = Image.new("RGBA", big.size, (0, 0, 0, 0))
    gd = ImageDraw.Draw(glow)
    cx, cy = 440 * S, 162 * S
    for r, a in ((260, 30), (160, 46), (80, 70)):
        gd.ellipse((cx - r, cy - r, cx + r, cy + r), fill=(255, 150, 50, a))
    glow = glow.filter(ImageFilter.GaussianBlur(40))
    out = Image.alpha_composite(glow, big)
    out.save(os.path.join(ROOT, "menu_l3_near.png"))


if __name__ == "__main__":
    os.makedirs(ROOT, exist_ok=True)
    far_forest_and_lake()
    shore_firs()
    grass_field()
    table_scene()
    print("ok")
