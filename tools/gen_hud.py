"""Texturas de la interfaz ornamentada del jugador (HudOverrides): marcos de barra y de jefe, medallones con icono, emblema alado del nivel,
placa y marcos de la barra rapida. Estetica gotica del pack: bronce, oro y carmesi. Las piezas marcadas "tintable" son blancas y el codigo
las pinta con el color de la raza del jugador.
Se dibuja a 8x y se reduce a 4x (4 px de textura por pixel de interfaz) para que los bordes salgan suaves.
Uso: python tools/gen_hud.py"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tcorigenes", "textures", "gui", "hud")
K = 8  # supersampling
TEX = 4  # pixeles de textura por pixel de interfaz

DARK = (12, 7, 11, 255)
DEEP = (30, 16, 20, 255)
BRONZE = (84, 52, 28, 255)
GOLD = (214, 168, 92, 255)
LIGHT = (255, 238, 180, 255)
CRIMSON = (178, 30, 46, 255)
WHITE = (255, 255, 255, 255)


def canvas(w, h):
    return Image.new("RGBA", (int(w * K), int(h * K)), (0, 0, 0, 0))


def pts(points):
    return [(x * K, y * K) for x, y in points]


def poly(d, points, fill, edge=None, edge_w=0.0):
    """Poligono relleno; con "edge" lleva un borde del grosor dado (en pixeles de interfaz)."""
    if edge is not None:
        n = max(1, int(edge_w * K))
        for dx, dy in ((-n, 0), (n, 0), (0, -n), (0, n), (-n, -n), (n, n), (-n, n), (n, -n)):
            d.polygon([(x + dx, y + dy) for x, y in pts(points)], fill=edge)
    d.polygon(pts(points), fill=fill)


def rect(d, x0, y0, x1, y1, fill):
    d.rectangle((x0 * K, y0 * K, x1 * K - 1, y1 * K - 1), fill=fill)


def circle(d, cx, cy, r, fill=None, outline=None, width=1.0):
    d.ellipse(((cx - r) * K, (cy - r) * K, (cx + r) * K, (cy + r) * K), fill=fill, outline=outline, width=int(width * K))


def diamond(cx, cy, r):
    return [(cx, cy - r), (cx + r, cy), (cx, cy + r), (cx - r, cy)]


def save(im, name, w, h):
    os.makedirs(OUT, exist_ok=True)
    im.resize((int(w * TEX), int(h * TEX)), Image.LANCZOS).save(os.path.join(OUT, name))
    print(name, int(w * TEX), int(h * TEX))


def plate_points(w, h, tip, inset):
    """Placa con las puntas en flecha a los dos lados."""
    cy = h / 2
    return [(inset, cy), (inset + tip, inset + 0.5), (w - inset - tip, inset + 0.5), (w - inset, cy), (w - inset - tip, h - inset - 0.5), (inset + tip, h - inset - 0.5)]


def ornate_bar(w, h, name, hole_y0=4, hole_y1=11):
    """Marco horizontal: placa gotica de puntas en flecha, filete dorado con muescas, hueco oscuro y gemas carmesi en las puntas."""
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    poly(d, plate_points(w, h, 3.2, 0), GOLD)
    poly(d, plate_points(w, h, 3.2, 1.1), BRONZE)
    rect(d, 3, hole_y0, w - 3, hole_y1, DARK)
    rect(d, 3, hole_y0, w - 3, hole_y0 + 1, (0, 0, 0, 130))            # sombra interior arriba
    rect(d, 3, hole_y1 - 0.5, w - 3, hole_y1, (255, 230, 170, 60))     # brillo del borde de abajo
    x = 9.0
    while x < w - 8:                                                    # muescas del filete superior
        rect(d, x, 1.6, x + 0.9, 2.6, LIGHT)
        x += 9.0
    rect(d, 7, 1.5, w - 7, 2.0, (255, 238, 180, 150))
    for ex in (1.9, w - 1.9):
        poly(d, diamond(ex, h / 2, 1.5), CRIMSON, GOLD, 0.25)
    save(im, name, w, h)


def badge(size, name):
    """Medallon redondo donde va el icono de cada recurso."""
    im = canvas(size, size)
    d = ImageDraw.Draw(im)
    c = size / 2
    circle(d, c, c, c, GOLD)
    circle(d, c, c, c - 0.9, BRONZE)
    circle(d, c, c, c - 1.7, None, GOLD, 0.35)
    circle(d, c, c, c - 2.2, DARK)
    d.arc(((c - c + 1.2) * K, (c - c + 1.2) * K, (size - 1.2) * K, (size - 1.2) * K), 200, 290, fill=LIGHT, width=int(0.5 * K))
    for dx, dy in ((0, -c + 0.5), (0, c - 0.5), (-c + 0.5, 0), (c - 0.5, 0)):
        circle(d, c + dx, c + dy, 0.55, LIGHT)
    save(im, name, size, size)


def icon(name, color, draw_shape):
    """Icono de 10x10 con contorno oscuro y un brillo arriba a la izquierda."""
    mask = Image.new("L", (10 * K, 10 * K), 0)
    draw_shape(ImageDraw.Draw(mask))
    outline = mask.filter(ImageFilter.MaxFilter(K // 2 * 2 + 1))
    base = Image.new("RGBA", mask.size, (0, 0, 0, 0))
    base.paste((18, 8, 12, 255), mask=outline)
    body = Image.new("RGBA", mask.size, color + (255,))
    shade = Image.new("RGBA", mask.size, (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    sd.rectangle((0, 5.2 * K, 10 * K, 10 * K), fill=(0, 0, 0, 70))            # mitad de abajo mas oscura
    sd.ellipse((1.2 * K, 1.0 * K, 4.2 * K, 3.4 * K), fill=(255, 255, 255, 110))  # brillo
    body = Image.alpha_composite(body, shade)
    base.paste(body, mask=mask)
    base.resize((10 * TEX, 10 * TEX), Image.LANCZOS).save(os.path.join(OUT, name))
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


def star(d):
    p = []
    for i in range(10):
        r = 4.7 if i % 2 == 0 else 2.0
        a = -math.pi / 2 + i * math.pi / 5
        p.append((5 + r * math.cos(a), 5.2 + r * math.sin(a)))
    d.polygon(pts(p), fill=255)


def emblem(name, gem_name, glow_name):
    """Emblema del nivel de 48x44: rombo de doble borde, alas cortas, corona arriba y colgante abajo. Las gemas van aparte (tintables)."""
    W, H = 48, 44
    cx, cy, r = 24, 22, 15
    im = canvas(W, H)
    d = ImageDraw.Draw(im)
    for side in (1, -1):                                  # alas: plumas en sierra a cada lado
        wing = [(cx + side * 13, cy), (cx + side * 22.5, cy - 9.5), (cx + side * 20.5, cy - 6.2), (cx + side * 23.6, cy - 5.4), (cx + side * 20.8, cy - 2.4),
                (cx + side * 23.4, cy - 1.0), (cx + side * 20.4, cy + 1.2), (cx + side * 22.2, cy + 3.6), (cx + side * 17.5, cy + 4.0)]
        poly(d, wing, GOLD, BRONZE, 0.4)
        poly(d, [(cx + side * 14, cy - 0.5), (cx + side * 20.6, cy - 6.6), (cx + side * 19.0, cy - 2.0), (cx + side * 16.5, cy + 1.6)], (150, 100, 46, 255))
    poly(d, diamond(cx, cy, r + 1.2), BRONZE)
    poly(d, diamond(cx, cy, r), GOLD)
    poly(d, diamond(cx, cy, r - 1.5), BRONZE)
    poly(d, diamond(cx, cy, r - 2.7), (44, 30, 38, 255))   # mas claro que el resto para que se lea el icono de la raza
    d.polygon(pts(diamond(cx, cy, r - 4.4)), outline=CRIMSON, width=int(0.5 * K))
    crown = [(19.2, 7.2), (20.2, 2.6), (22.4, 5.2), (24, 0.6), (25.6, 5.2), (27.8, 2.6), (28.8, 7.2)]
    poly(d, crown, GOLD, BRONZE, 0.35)
    circle(d, 24, 3.4, 0.6, LIGHT)
    poly(d, [(24, 37.4), (26.8, 40.2), (24, 43.6), (21.2, 40.2)], BRONZE)
    poly(d, [(24, 38.2), (26.0, 40.2), (24, 42.8), (22.0, 40.2)], GOLD)
    for gx, gy in ((24, 9.6), (24, 34.4), (9.6, 22), (38.4, 22)):    # engarces de las gemas
        circle(d, gx, gy, 2.1, GOLD)
        circle(d, gx, gy, 1.5, DARK)
    save(im, name, W, H)

    gem = canvas(W, H)
    g = ImageDraw.Draw(gem)
    for gx, gy in ((24, 9.6), (24, 34.4), (9.6, 22), (38.4, 22)):
        circle(g, gx, gy, 1.35, WHITE)
    poly(g, [(24, 38.6), (25.6, 40.2), (24, 42.2), (22.4, 40.2)], WHITE)
    circle(g, 24, 3.4, 0.5, WHITE)
    save(gem, gem_name, W, H)

    glow = Image.radial_gradient("L").resize((56 * TEX, 56 * TEX), Image.LANCZOS)   # 0 al centro, 255 en el borde
    alpha = glow.point(lambda v: int(max(0, 255 - v * 1.45) * 0.55))
    img = Image.new("RGBA", glow.size, (255, 255, 255, 0))
    img.putalpha(alpha)
    img.save(os.path.join(OUT, glow_name))
    print(glow_name, glow.size)


def hotbar_plate(name):
    """Placa detras de la barra rapida: puntas en flecha, doble filete dorado, fondo oscuro y gemas en las puntas."""
    W, H = 200, 28
    im = canvas(W, H)
    d = ImageDraw.Draw(im)
    poly(d, plate_points(W, H, 6.5, 0), GOLD)
    poly(d, plate_points(W, H, 6.5, 1.1), BRONZE)
    poly(d, plate_points(W, H, 6.5, 2.1), (14, 8, 12, 245))
    rect(d, 12, 1.0, W - 12, 1.7, LIGHT)
    for ex in (3.4, W - 3.4):
        poly(d, diamond(ex, H / 2, 1.8), CRIMSON, GOLD, 0.3)
    save(im, name, W, H)


def slot(name):
    """Casilla de la barra rapida: fondo oscuro con borde fino de bronce y esquinas doradas en L."""
    S = 20
    im = canvas(S, S)
    d = ImageDraw.Draw(im)
    rect(d, 0, 0, S, S, BRONZE)
    rect(d, 1, 1, S - 1, S - 1, (16, 9, 13, 235))
    rect(d, 1, 1, S - 1, 2, (0, 0, 0, 120))
    for sx, sy in ((0, 0), (1, 0), (0, 1), (1, 1)):
        x0 = 0 if sx == 0 else S
        y0 = 0 if sy == 0 else S
        dx = 1 if sx == 0 else -1
        dy = 1 if sy == 0 else -1
        d.rectangle((min(x0, x0 + dx * 2.6) * K, min(y0, y0 + dy * 1.0) * K, max(x0, x0 + dx * 2.6) * K - 1, max(y0, y0 + dy * 1.0) * K - 1), fill=GOLD)
        d.rectangle((min(x0, x0 + dx * 1.0) * K, min(y0, y0 + dy * 2.6) * K, max(x0, x0 + dx * 1.0) * K - 1, max(y0, y0 + dy * 2.6) * K - 1), fill=GOLD)
    save(im, name, S, S)


def selected(name):
    """Realce del slot elegido (tintable): marco blanco con esquinas en rombo y centro transparente."""
    S = 24
    im = canvas(S, S)
    d = ImageDraw.Draw(im)
    rect(d, 0, 0, S, S, WHITE)
    rect(d, 1.2, 1.2, S - 1.2, S - 1.2, (200, 200, 200, 255))
    rect(d, 2.2, 2.2, S - 2.2, S - 2.2, (0, 0, 0, 0))
    for x, y in ((1.2, 1.2), (S - 1.2, 1.2), (1.2, S - 1.2), (S - 1.2, S - 1.2)):
        poly(d, diamond(x, y, 2.3), WHITE)
    save(im, name, S, S)


def crest(name):
    """Adorno de la barra de jefe: rombo central con cuernos curvos hacia arriba."""
    W, H = 44, 18
    im = canvas(W, H)
    d = ImageDraw.Draw(im)
    cx, cy = W / 2, 10
    for side in (1, -1):
        horn = [(cx + side * 5, cy + 1), (cx + side * 12, cy + 2.5), (cx + side * 19.5, cy - 3), (cx + side * 21.5, cy - 8.5),
                (cx + side * 17.5, cy - 5.2), (cx + side * 11.5, cy - 1.8), (cx + side * 6, cy - 2)]
        poly(d, horn, GOLD, BRONZE, 0.4)
    poly(d, diamond(cx, cy, 8.2), BRONZE)
    poly(d, diamond(cx, cy, 7.2), GOLD)
    poly(d, diamond(cx, cy, 5.8), DARK)
    poly(d, diamond(cx, cy, 3.6), CRIMSON, LIGHT, 0.25)
    save(im, name, W, H)


ornate_bar(92, 15, "bar.png")
ornate_bar(190, 15, "boss.png")
badge(14, "badge.png")
icon("icon_health.png", (232, 52, 66), heart)
icon("icon_food.png", (228, 152, 62), drumstick)
icon("icon_thirst.png", (72, 164, 236), drop)
icon("icon_mana.png", (156, 124, 255), sparkle)
icon("icon_stamina.png", (112, 226, 124), bolt)
icon("icon_xp.png", (252, 216, 104), star)
emblem("emblem.png", "emblem_gem.png", "emblem_glow.png")
hotbar_plate("hotbar_plate.png")
slot("slot.png")
selected("slot_selected.png")
crest("boss_crest.png")
