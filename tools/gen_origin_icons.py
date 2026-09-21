"""Genera los iconos (32x32, pixel art) de razas y clases para las pantallas de eleccion.

Uso:  python tools/gen_origin_icons.py
Salida: src/main/resources/assets/tcorigenes/textures/gui/origin/<nombre>.png
"""
import math
import os
from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources",
                   "assets", "tcorigenes", "textures", "gui", "origin")
S = 32
OUTLINE = (20, 14, 22, 255)


def canvas():
    img = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    return img, ImageDraw.Draw(img)


def outline(img, color=OUTLINE):
    """Borde de 1 px alrededor de todo lo dibujado."""
    px = img.load()
    edge = []
    for y in range(S):
        for x in range(S):
            if px[x, y][3] == 0:
                for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                    nx, ny = x + dx, y + dy
                    if 0 <= nx < S and 0 <= ny < S and px[nx, ny][3] > 0:
                        edge.append((x, y))
                        break
    for x, y in edge:
        px[x, y] = color
    return img


def mirror_poly(points):
    return [(S - 1 - x, y) for x, y in points]


def save(img, name):
    os.makedirs(OUT, exist_ok=True)
    outline(img).save(os.path.join(OUT, name + ".png"))


# ------------------------------------------------------------------ razas
def humano():
    img, d = canvas()
    d.ellipse((10, 4, 21, 15), fill=(232, 190, 150, 255))
    d.ellipse((10, 4, 21, 8), fill=(110, 70, 40, 255))
    d.rectangle((13, 10, 14, 11), fill=(40, 30, 30, 255))
    d.rectangle((18, 10, 19, 11), fill=(40, 30, 30, 255))
    d.polygon([(5, 30), (6, 21), (11, 17), (21, 17), (26, 21), (27, 30)], fill=(70, 110, 170, 255))
    d.rectangle((14, 16, 17, 18), fill=(232, 190, 150, 255))
    save(img, "humano")


def hereje():
    img, d = canvas()
    red, dark = (215, 30, 30, 255), (140, 10, 10, 255)
    d.ellipse((5, 5, 26, 26), outline=red, width=3)
    d.ellipse((12, 12, 19, 19), fill=red)
    d.ellipse((14, 14, 17, 17), fill=(255, 190, 170, 255))
    for a, b in (((15, 0), (16, 8)), ((15, 23), (16, 31)), ((0, 15), (8, 16)), ((23, 15), (31, 16))):
        d.rectangle((a[0], a[1], b[0], b[1]), fill=dark)
    save(img, "hereje")


def devoto():
    img, d = canvas()
    gold, light, shade = (240, 200, 60, 255), (255, 240, 150, 255), (190, 140, 30, 255)
    d.rectangle((13, 6, 18, 29), fill=gold)
    d.rectangle((6, 12, 25, 17), fill=gold)
    d.rectangle((13, 6, 14, 29), fill=light)
    d.rectangle((6, 12, 25, 13), fill=light)
    d.rectangle((17, 14, 18, 29), fill=shade)
    d.ellipse((10, 0, 21, 5), outline=(255, 250, 200, 255), width=1)
    save(img, "devoto")


def demonio():
    img, d = canvas()
    horn, hi, base = (185, 25, 25, 255), (240, 90, 60, 255), (90, 15, 15, 255)
    left = [(4, 29), (4, 20), (7, 12), (12, 4), (12, 11), (10, 17), (12, 22), (13, 29)]
    d.polygon(left, fill=horn)
    d.polygon(mirror_poly(left), fill=horn)
    for pts in ([(6, 24), (6, 19), (9, 13), (11, 8)], ):
        d.line(pts, fill=hi, width=1)
        d.line(mirror_poly(pts), fill=hi, width=1)
    d.rectangle((4, 27, 27, 30), fill=base)
    save(img, "demonio")


def angel():
    img, d = canvas()
    white, shade, gold = (250, 250, 255, 255), (185, 200, 225, 255), (255, 215, 70, 255)
    wing = [(14, 14), (6, 5), (1, 8), (1, 16), (5, 22), (10, 26), (14, 24)]
    d.polygon(wing, fill=white)
    d.polygon(mirror_poly(wing), fill=white)
    for pts in ([(13, 15), (4, 9)], [(13, 18), (3, 15)], [(13, 21), (5, 20)], [(13, 23), (8, 25)]):
        d.line(pts, fill=shade, width=1)
        d.line(mirror_poly(pts), fill=shade, width=1)
    d.ellipse((9, 1, 22, 7), outline=gold, width=2)
    save(img, "angel")


def siervo():
    img, d = canvas()
    moon = Image.new("RGBA", (S, S), (0, 0, 0, 0))
    md = ImageDraw.Draw(moon)
    md.ellipse((3, 8, 27, 32), fill=(205, 220, 255, 255))
    cut = Image.new("L", (S, S), 0)
    ImageDraw.Draw(cut).ellipse((10, 4, 34, 28), fill=255)
    px, cp = moon.load(), cut.load()
    for y in range(S):
        for x in range(S):
            if cp[x, y]:
                px[x, y] = (0, 0, 0, 0)
    img.alpha_composite(moon)
    d.line([(9, 12), (6, 5), (4, 2)], fill=(120, 130, 190, 255), width=1)
    d.line([(14, 9), (15, 3), (17, 1)], fill=(120, 130, 190, 255), width=1)
    d.ellipse((2, 0, 5, 3), fill=(255, 250, 200, 255))
    d.ellipse((16, 0, 19, 2), fill=(255, 250, 200, 255))
    save(img, "siervo")


def ender():
    img, d = canvas()
    d.rounded_rectangle((2, 6, 29, 25), radius=3, fill=(22, 10, 32, 255))
    for x0 in (6, 19):
        d.rectangle((x0, 13, x0 + 6, 17), fill=(235, 90, 255, 255))
        d.rectangle((x0 + 1, 14, x0 + 5, 16), fill=(255, 190, 255, 255))
    for p in ((4, 9), (27, 22), (8, 23), (24, 9), (15, 21)):
        d.point(p, fill=(170, 60, 220, 255))
    save(img, "ender")


def malnacido():
    img, d = canvas()
    bone, shade = (150, 195, 115, 255), (95, 140, 75, 255)
    d.ellipse((6, 3, 25, 21), fill=bone)
    d.rectangle((10, 17, 21, 27), fill=bone)
    d.rectangle((10, 20, 21, 27), fill=shade)
    d.rectangle((10, 20, 21, 20), fill=bone)
    d.rectangle((9, 10, 13, 15), fill=(20, 40, 20, 255))
    d.rectangle((18, 10, 22, 15), fill=(20, 40, 20, 255))
    d.polygon([(15, 16), (17, 16), (16, 19)], fill=(20, 40, 20, 255))
    for x in (12, 15, 18):
        d.line([(x, 22), (x, 27)], fill=(20, 40, 20, 255), width=1)
    d.line([(20, 4), (18, 8), (20, 11)], fill=(60, 100, 50, 255), width=1)
    save(img, "malnacido")


def stone_giant():
    img, d = canvas()
    d.polygon([(3, 25), (2, 15), (8, 6), (19, 3), (28, 11), (29, 23), (22, 29), (9, 29)], fill=(125, 125, 132, 255))
    d.polygon([(8, 6), (19, 3), (28, 11), (17, 13), (10, 12)], fill=(165, 165, 172, 255))
    d.polygon([(22, 29), (29, 23), (28, 11), (20, 17), (21, 24)], fill=(88, 88, 96, 255))
    d.line([(11, 14), (14, 19), (12, 24)], fill=(60, 60, 68, 255), width=1)
    d.line([(20, 17), (17, 22), (19, 27)], fill=(60, 60, 68, 255), width=1)
    save(img, "stone_giant")


# ---------------------------------------------------------------- clases
def ritualista():
    img, d = canvas()
    col, glow = (170, 70, 220, 255), (235, 170, 255, 255)
    d.ellipse((3, 3, 28, 28), outline=col, width=2)
    pts = [(15.5 + 11 * math.cos(math.radians(-90 + 72 * i)),
            15.5 + 11 * math.sin(math.radians(-90 + 72 * i))) for i in range(5)]
    for i in range(5):
        d.line([pts[i], pts[(i + 2) % 5]], fill=glow, width=1)
    d.ellipse((13, 13, 18, 18), fill=col)
    save(img, "ritualista")


def berserker():
    img, d = canvas()
    d.line([(6, 28), (22, 9)], fill=(110, 70, 35, 255), width=3)
    d.polygon([(15, 3), (28, 4), (29, 14), (24, 17), (21, 11), (16, 9)], fill=(190, 195, 205, 255))
    d.polygon([(16, 9), (21, 11), (24, 17), (29, 14), (28, 4)], fill=(150, 155, 168, 255))
    d.line([(17, 5), (26, 6)], fill=(240, 245, 250, 255), width=1)
    d.rectangle((3, 27, 6, 30), fill=(150, 20, 20, 255))
    save(img, "berserker")


def anima():
    img, d = canvas()
    for r, a in ((15, 60), (12, 90)):
        d.ellipse((16 - r, 16 - r, 16 + r, 16 + r), fill=(120, 210, 255, a))
    d.rectangle((14, 2, 17, 21), fill=(190, 240, 255, 255))
    d.rectangle((14, 2, 15, 21), fill=(250, 255, 255, 255))
    d.polygon([(14, 1), (17, 1), (16, 0)], fill=(250, 255, 255, 255))
    d.rectangle((9, 21, 22, 23), fill=(70, 130, 200, 255))
    d.rectangle((14, 24, 17, 28), fill=(60, 90, 140, 255))
    d.rectangle((13, 29, 18, 30), fill=(70, 130, 200, 255))
    save(img, "anima")


def escudero():
    img, d = canvas()
    d.polygon([(4, 4), (27, 4), (27, 17), (15.5, 30), (4, 17)], fill=(150, 155, 170, 255))
    d.polygon([(7, 7), (24, 7), (24, 16), (15.5, 26), (7, 16)], fill=(65, 100, 175, 255))
    d.ellipse((10, 9, 21, 20), fill=(245, 205, 70, 255))
    d.ellipse((13, 12, 18, 17), fill=(255, 240, 160, 255))
    save(img, "escudero")


def arquero():
    img, d = canvas()
    d.arc((2, 2, 24, 29), start=90, end=270, fill=(120, 75, 35, 255), width=3)
    d.line([(13, 3), (13, 28)], fill=(235, 235, 225, 255), width=1)
    d.line([(6, 16), (28, 16)], fill=(150, 110, 60, 255), width=2)
    d.polygon([(26, 12), (31, 16), (26, 20)], fill=(200, 205, 215, 255))
    d.polygon([(6, 13), (10, 16), (6, 19), (8, 16)], fill=(220, 60, 50, 255))
    save(img, "arquero")


if __name__ == "__main__":
    for fn in (humano, hereje, devoto, demonio, angel, siervo, ender, malnacido, stone_giant,
               ritualista, berserker, anima, escudero, arquero):
        fn()
    print("ok ->", os.path.abspath(OUT))


# ------------------------------------------------------------- moneda (cara o cruz)
def coin(name, emblem):
    img, d = canvas()
    d.ellipse((2, 2, 29, 29), fill=(120, 80, 14, 255))
    d.ellipse((3, 3, 28, 28), fill=(214, 168, 46, 255))
    d.ellipse((5, 5, 26, 26), fill=(238, 196, 70, 255))
    d.ellipse((6, 6, 25, 25), outline=(170, 120, 24, 255), width=1)
    d.arc((5, 5, 26, 26), start=200, end=290, fill=(255, 236, 150, 255), width=1)
    emblem(d)
    save(img, name)


def emblem_heads(d):
    dark = (110, 70, 10, 255)
    d.polygon([(9, 20), (9, 11), (13, 15), (16, 9), (19, 15), (23, 11), (23, 20)], fill=dark)
    d.rectangle((9, 21, 23, 22), fill=dark)
    for x in (9, 16, 23):
        d.point((x, 9 if x == 16 else 10), fill=(255, 236, 150, 255))


def emblem_tails(d):
    dark = (110, 70, 10, 255)
    d.rectangle((14, 8, 17, 23), fill=dark)
    d.rectangle((9, 12, 22, 15), fill=dark)


def make_coins():
    global OUT
    saved = OUT
    OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets",
                       "tcorigenes", "textures", "gui", "coinflip")
    coin("coin_heads", emblem_heads)
    coin("coin_tails", emblem_tails)
    OUT = saved


if __name__ == "__main__":
    make_coins()
