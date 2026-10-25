"""Texturas de la interfaz del jugador (HudOverrides), de estilo frio y limpio: marcos finos y translucidos rojos, rombo central chico, casillas
sueltas. Las piezas "tintables" (gemas, resplandor, casilla elegida) son blancas y el codigo las pinta con el color de la
raza del jugador. Cada textura lleva su .mcmeta con blur, porque se dibuja a 1/4 de su tamano (4 px de textura por pixel de interfaz).
Se dibuja a 8x y se reduce a 4x para que los bordes salgan suaves.
Uso: python tools/gen_hud.py"""
import os

from PIL import Image, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tcorigenes", "textures", "gui", "hud")
K = 8  # supersampling
TEX = 4  # pixeles de textura por pixel de interfaz

STEEL = (146, 20, 32, 255)
STEEL_DK = (74, 12, 20, 255)
NAVY = (14, 4, 7, 240)
NAVY2 = (30, 8, 12, 210)
CYAN = (196, 36, 50, 255)
ICE = (200, 84, 90, 255)
WHITE = (255, 255, 255, 255)
CLEAR = (0, 0, 0, 0)

STALE = ["badge.png", "hotbar_plate.png", "icon_xp.png", "boss.png", "boss_crest.png"]  # texturas de la version anterior


def canvas(w, h):
    return Image.new("RGBA", (int(w * K), int(h * K)), CLEAR)


def pts(points):
    return [(x * K, y * K) for x, y in points]


def poly(d, points, fill):
    d.polygon(pts(points), fill=fill)


def rect(d, x0, y0, x1, y1, fill):
    d.rectangle((x0 * K, y0 * K, x1 * K - 1, y1 * K - 1), fill=fill)


def circle(d, cx, cy, r, fill):
    d.ellipse(((cx - r) * K, (cy - r) * K, (cx + r) * K, (cy + r) * K), fill=fill)


def diamond(cx, cy, r):
    return [(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)]


def chamfer(x0, y0, x1, y1, c):
    """Rectangulo con las cuatro esquinas cortadas en diagonal."""
    return [(x0 + c, y0), (x1 - c, y0), (x1, y0 + c), (x1, y1 - c), (x1 - c, y1), (x0 + c, y1), (x0, y1 - c), (x0, y0 + c)]


def write_meta(name):
    with open(os.path.join(OUT, name + ".mcmeta"), "w", encoding="utf-8") as f:
        f.write('{"texture": {"blur": true}}\n')


def save(im, name, w, h):
    os.makedirs(OUT, exist_ok=True)
    im.resize((int(w * TEX), int(h * TEX)), Image.LANCZOS).save(os.path.join(OUT, name))
    write_meta(name)
    print(name, int(w * TEX), int(h * TEX))


def frame(w, h, name, hole, c):
    """Marco fino: contorno de acero con el borde de arriba mas claro, fondo rojo oscuro translucido, hueco oscuro y marcas rojas en los extremos."""
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    poly(d, chamfer(0, 0, w, h, c), STEEL)
    poly(d, chamfer(0.9, 0.9, w - 0.9, h - 0.9, max(0.4, c - 0.8)), NAVY2)
    x0, y0, x1, y1 = hole
    rect(d, x0, y0, x1, y1, NAVY)
    rect(d, x0, y0, x1, y0 + 0.8, (0, 0, 0, 120))                 # sombra interior de arriba
    rect(d, c + 2, 0, w - c - 2, 0.7, ICE)                          # filo claro del borde de arriba
    mid = h / 2
    for tx in (1.15, w - 1.95):                                      # marcas rojas de los extremos
        rect(d, tx, mid - 1.6, tx + 0.8, mid + 1.6, CYAN)
    save(im, name, w, h)


def badge_icon(name, color, draw_shape):
    """Icono de 10x10 con contorno oscuro y brillo arriba a la izquierda (se dibuja a 7x7 dentro de la barra)."""
    mask = Image.new("L", (10 * K, 10 * K), 0)
    draw_shape(ImageDraw.Draw(mask))
    outline = mask.filter(ImageFilter.MaxFilter(K // 2 * 2 + 1))
    base = Image.new("RGBA", mask.size, CLEAR)
    base.paste((10, 14, 22, 255), mask=outline)
    body = Image.new("RGBA", mask.size, color + (255,))
    shade = Image.new("RGBA", mask.size, CLEAR)
    sd = ImageDraw.Draw(shade)
    sd.rectangle((0, 5.2 * K, 10 * K, 10 * K), fill=(0, 0, 0, 60))
    sd.ellipse((1.2 * K, 1.0 * K, 4.2 * K, 3.4 * K), fill=(255, 255, 255, 120))
    body = Image.alpha_composite(body, shade)
    base.paste(body, mask=mask)
    base.resize((10 * TEX, 10 * TEX), Image.LANCZOS).save(os.path.join(OUT, name))
    write_meta(name)
    print(name, 10 * TEX, 10 * TEX)


def heart(d):
    circle(d, 3.3, 3.5, 2.5, 255)
    circle(d, 6.7, 3.5, 2.5, 255)
    d.polygon(pts([(0.9, 4.4), (9.1, 4.4), (5, 9.3)]), fill=255)


def drumstick(d):
    circle(d, 6.3, 3.7, 3.0, 255)
    d.line(pts([(4.3, 5.7), (1.7, 8.3)]), fill=255, width=int(1.5 * K))
    circle(d, 1.3, 7.5, 1.0, 255)
    circle(d, 2.5, 8.7, 1.0, 255)


def drop(d):
    circle(d, 5, 6.1, 3.3, 255)
    d.polygon(pts([(5, 0.5), (2.2, 5.0), (7.8, 5.0)]), fill=255)


def sparkle(d):
    d.polygon(pts([(5, 0.4), (6.1, 3.9), (9.6, 5), (6.1, 6.1), (5, 9.6), (3.9, 6.1), (0.4, 5), (3.9, 3.9)]), fill=255)


def bolt(d):
    d.polygon(pts([(6.4, 0.3), (2.0, 5.5), (4.6, 5.5), (3.3, 9.7), (8.0, 4.0), (5.4, 4.0)]), fill=255)


GEMS = ((15, 2.9), (15, 27.1), (2.9, 15), (27.1, 15))


def emblem(name, gem_name, glow_name):
    """Rombo central de 30x30: borde de acero, anillo fino, hueco claro para el icono de la raza y engarces en las cuatro puntas (gemas aparte)."""
    S = 30
    c = S / 2
    im = canvas(S, S)
    d = ImageDraw.Draw(im)
    poly(d, diamond(c, c, c), STEEL)
    poly(d, diamond(c, c, c - 1.0), NAVY2)
    poly(d, diamond(c, c, c - 2.6), STEEL_DK)
    poly(d, diamond(c, c, c - 3.1), (48, 16, 22, 255))   # mas claro que las barras para que se lea el icono
    d.line(pts([(c, 0.5), (0.5, c)]), fill=ICE, width=int(0.5 * K))   # filo claro arriba a la izquierda
    for gx, gy in GEMS:
        circle(d, gx, gy, 1.9, STEEL)
        circle(d, gx, gy, 1.35, NAVY)
    save(im, name, S, S)

    gem = canvas(S, S)
    g = ImageDraw.Draw(gem)
    for gx, gy in GEMS:
        circle(g, gx, gy, 1.05, WHITE)
    save(gem, gem_name, S, S)

    glow = Image.radial_gradient("L").resize((44 * TEX, 44 * TEX), Image.LANCZOS)   # 0 al centro, 255 en el borde
    img = Image.new("RGBA", glow.size, (255, 255, 255, 0))
    img.putalpha(glow.point(lambda v: int(max(0, 255 - v * 1.45) * 0.55)))
    img.save(os.path.join(OUT, glow_name))
    write_meta(glow_name)
    print(glow_name, glow.size)


def slot(name):
    """Casilla suelta: contorno fino de acero oscuro, fondo rojo oscuro translucido y filo claro arriba."""
    S = 18
    im = canvas(S, S)
    d = ImageDraw.Draw(im)
    poly(d, chamfer(0, 0, S, S, 1.4), STEEL_DK)
    poly(d, chamfer(0.9, 0.9, S - 0.9, S - 0.9, 0.8), (18, 5, 8, 180))
    rect(d, 2, 0, S - 2, 0.7, STEEL)
    rect(d, 1, 1, S - 1, 2, (0, 0, 0, 90))
    save(im, name, S, S)


def selected(name):
    """Realce de la casilla elegida (tintable): marco blanco fino con esquinas marcadas y centro transparente."""
    S = 22
    im = canvas(S, S)
    d = ImageDraw.Draw(im)
    poly(d, chamfer(0, 0, S, S, 1.8), (255, 255, 255, 235))
    poly(d, chamfer(1.3, 1.3, S - 1.3, S - 1.3, 1.0), (255, 255, 255, 70))
    poly(d, chamfer(2.1, 2.1, S - 2.1, S - 2.1, 0.7), CLEAR)
    for x, y in ((0, 0), (S - 3, 0), (0, S - 3), (S - 3, S - 3)):
        rect(d, x, y, x + 3, y + 3, WHITE)
    save(im, name, S, S)


def slot_big(name):
    """Casilla de la mano secundaria: mas grande, doble contorno de acero y gemas rojas en las esquinas."""
    S = 24
    im = canvas(S, S)
    d = ImageDraw.Draw(im)
    poly(d, chamfer(0, 0, S, S, 2.4), STEEL)
    poly(d, chamfer(1.0, 1.0, S - 1.0, S - 1.0, 1.6), (24, 7, 11, 235))
    poly(d, chamfer(1.8, 1.8, S - 1.8, S - 1.8, 1.0), STEEL_DK)
    poly(d, chamfer(2.4, 2.4, S - 2.4, S - 2.4, 0.7), (18, 5, 8, 195))
    rect(d, 4, 0, S - 4, 0.7, ICE)
    for x, y in ((2.0, 2.0), (S - 2.0, 2.0), (2.0, S - 2.0), (S - 2.0, S - 2.0)):
        poly(d, diamond(x, y, 1.1), CYAN)
    save(im, name, S, S)


for stale in STALE:
    for suffix in ("", ".mcmeta"):
        path = os.path.join(OUT, stale + suffix)
        if os.path.exists(path):
            os.remove(path)

frame(92, 11, "bar.png", (3, 2, 89, 9), 2.0)
frame(45, 11, "bar_half.png", (3, 2, 42, 9), 2.0)
badge_icon("icon_health.png", (240, 88, 106), heart)
badge_icon("icon_food.png", (240, 176, 80), drumstick)
badge_icon("icon_thirst.png", (88, 200, 248), drop)
badge_icon("icon_mana.png", (160, 128, 255), sparkle)
badge_icon("icon_stamina.png", (96, 224, 168), bolt)
emblem("emblem.png", "emblem_gem.png", "emblem_glow.png")
slot("slot.png")
selected("slot_selected.png")
slot_big("slot_big.png")


GLYPHS = "0123456789/:."
CELL_W, CELL_H = 7, 9  # celda de cada caracter en px de interfaz (el digito mide 5x7 y el resto es margen para el contorno)
STROKE = 1.0           # grosor del trazo
OUTLINE = 0.8          # contorno oscuro


def _mirror(path):
    return [(5 - x, 7 - y) for x, y in path]


# Digitos de aspecto antiguo, angulares, como tallados, parecidos a la muestra del usuario (cuña afilada, 2 con barra, 3 con curva, 6 con marca,
# 8 tachado, 0 con cuernos). Cada glifo es una lista de trazos (lineas) en una caja de 5x7; ("fill", puntos) es una forma rellena.
_SIX = [(4.4, 0), (1.5, 0), (0, 1.5), (0, 5.5), (1.5, 7), (3.5, 7), (5, 5.5), (5, 4), (3.5, 3), (0, 3.4)]
STROKES = {
    "0": [[(1.2, 0), (3.8, 0), (5, 1.6), (5, 5.4), (3.8, 7), (1.2, 7), (0, 5.4), (0, 1.6), (1.2, 0)], [(1.0, -0.7), (1.0, 0.4)], [(4.0, -0.7), (4.0, 0.4)]],
    "1": [("fill", [(1.3, 0), (3.7, 0), (3.0, 5.4), (2.5, 7), (2.0, 5.4)])],
    "2": [[(0, 1.4), (1.4, 0), (3.6, 0), (5, 1.4), (5, 2.8), (0, 7), (5, 7)], [(0.9, 4.2), (4.1, 4.2)]],
    "3": [[(0.2, 0.2), (4.8, 0.2), (1.8, 3.0), (3.8, 3.0), (5, 4.2), (5, 5.6), (3.8, 7), (1.0, 7), (0, 6.2)]],
    "4": [[(4.4, 7), (4.4, 0.2), (1.2, 4.8), (5, 4.8)]],
    "5": [[(4.8, 0.2), (0.6, 0.2), (0, 3.3), (3.6, 2.6), (5, 4), (5, 5.8), (3.8, 7), (0, 7)]],
    "6": [_SIX, [(-0.5, 0.9), (1.0, 0.9)]],
    "7": [[(0, 2), (1.6, 0), (3.8, 0), (4.8, 1.4), (3.0, 3.6), (2.4, 7)]],
    "8": [[(1.2, 0), (3.8, 0), (5, 1.2), (5, 2.4), (3.8, 3.5), (1.2, 3.5), (0, 2.4), (0, 1.2), (1.2, 0)],
          [(1.2, 3.5), (3.8, 3.5), (5, 4.7), (5, 5.8), (3.8, 7), (1.2, 7), (0, 5.8), (0, 4.7), (1.2, 3.5)], [(0.3, 6.6), (4.7, 0.4)]],
    "9": [_mirror(_SIX), [(5.5, 6.1), (4.0, 6.1)]],
    "/": [[(0.2, 7), (4.8, 0)]],
    ":": [[(2.5, 1.9), (2.5, 2.7)], [(2.5, 4.9), (2.5, 5.7)]],
    ".": [[(2.5, 6.2), (2.5, 7)]],
}


def digits(name):
    """Atlas de los numeros del HUD: 0-9 / : . en glifos angulares de aspecto antiguo, blancos con contorno oscuro marcado (se tinen por codigo)."""
    im = Image.new("RGBA", (CELL_W * K * len(GLYPHS), CELL_H * K), CLEAR)
    ox, oy = (CELL_W - 5) / 2, (CELL_H - 7) / 2
    r = STROKE * K / 2
    for i, c in enumerate(GLYPHS):
        mask = Image.new("L", (CELL_W * K, CELL_H * K), 0)
        md = ImageDraw.Draw(mask)
        for path in STROKES[c]:
            if isinstance(path, tuple):   # forma rellena
                md.polygon([((x + ox) * K, (y + oy) * K) for x, y in path[1]], fill=255)
                continue
            xy = [((x + ox) * K, (y + oy) * K) for x, y in path]
            md.line(xy, fill=255, width=int(STROKE * K), joint="curve")
            for px, py in (xy[0], xy[-1]):   # extremos redondos para que no queden cortes feos
                md.ellipse((px - r, py - r, px + r, py + r), fill=255)
        outline = mask.filter(ImageFilter.MaxFilter(int(OUTLINE * K) * 2 + 1))
        cell = Image.new("RGBA", mask.size, CLEAR)
        cell.paste((10, 2, 4, 255), mask=outline)
        cell.paste(WHITE, mask=mask)
        im.alpha_composite(cell, (i * CELL_W * K, 0))
    save(im, name, CELL_W * len(GLYPHS), CELL_H)


digits("digits.png")
