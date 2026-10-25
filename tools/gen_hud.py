"""Texturas de la interfaz ornamentada del jugador (HudOverrides): marco de barra, marco de barra de jefe, rombo del nivel y marcos de la barra rapida.
Se dibuja a 8x y se reduce a 4x (la textura tiene 4 px por pixel de interfaz) para que los bordes salgan suaves.
Uso: python tools/gen_hud.py"""
import os

from PIL import Image, ImageDraw

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "tcorigenes", "textures", "gui", "hud")
K = 8  # supersampling
TEX = 4  # pixeles de textura por pixel de interfaz

DARK = (14, 8, 12, 255)
BRONZE = (74, 46, 26, 255)
GOLD = (206, 160, 84, 255)
LIGHT = (255, 232, 170, 255)
CRIMSON = (176, 30, 44, 255)


def canvas(w, h):
    return Image.new("RGBA", (w * K, h * K), (0, 0, 0, 0))


def rect(d, x0, y0, x1, y1, fill):
    d.rectangle((x0 * K, y0 * K, x1 * K - 1, y1 * K - 1), fill=fill)


def diamond(d, cx, cy, r, fill, outline=None):
    pts = [(cx * K, (cy - r) * K), ((cx + r) * K, cy * K), (cx * K, (cy + r) * K), ((cx - r) * K, cy * K)]
    d.polygon(pts, fill=fill, outline=outline)


def save(im, name, w, h):
    os.makedirs(OUT, exist_ok=True)
    im.resize((w * TEX, h * TEX), Image.LANCZOS).save(os.path.join(OUT, name))
    print(name, w * TEX, h * TEX)


def ornate_bar(w, h, name):
    """Marco horizontal: borde bronce, filete dorado, hueco oscuro de 3 px de margen y rombos carmesi en las puntas."""
    im = canvas(w, h)
    d = ImageDraw.Draw(im)
    rect(d, 0, 2, w, h - 2, BRONZE)
    rect(d, 1, 3, w - 1, h - 3, GOLD)
    rect(d, 2, 4, w - 2, h - 4, BRONZE)
    rect(d, 3, 4, w - 3, h - 4, DARK)
    rect(d, 3, 4, w - 3, 5, (0, 0, 0, 120))          # sombra interior arriba
    rect(d, 6, 2, w - 6, 2.7, LIGHT)                  # brillo del filete superior
    for sx in (0, w):
        diamond(d, sx + (3 if sx == 0 else -3), h / 2, 4.2, BRONZE)
        diamond(d, sx + (3 if sx == 0 else -3), h / 2, 3.4, GOLD)
        diamond(d, sx + (3 if sx == 0 else -3), h / 2, 2.0, CRIMSON)
    save(im, name, w, h)


def emblem(size, name):
    """Rombo del nivel: doble borde dorado, centro oscuro, filete carmesi y puntas."""
    im = canvas(size, size)
    d = ImageDraw.Draw(im)
    c = size / 2
    diamond(d, c, c, c - 0.5, BRONZE)
    diamond(d, c, c, c - 1.6, GOLD)
    diamond(d, c, c, c - 3.0, BRONZE)
    diamond(d, c, c, c - 4.0, DARK)
    diamond(d, c, c, c - 5.6, None, CRIMSON)
    for dx, dy in ((0, -c + 1.2), (0, c - 1.2), (-c + 1.2, 0), (c - 1.2, 0)):
        diamond(d, c + dx, c + dy, 1.7, LIGHT)
    save(im, name, size, size)


def slot(size, name):
    im = canvas(size, size)
    d = ImageDraw.Draw(im)
    rect(d, 0, 0, size, size, BRONZE)
    rect(d, 1, 1, size - 1, size - 1, GOLD)
    rect(d, 2, 2, size - 2, size - 2, DARK)
    rect(d, 2, 2, size - 2, 3, (0, 0, 0, 110))
    for x, y in ((0, 0), (size - 2, 0), (0, size - 2), (size - 2, size - 2)):
        rect(d, x, y, x + 2, y + 2, LIGHT)
    save(im, name, size, size)


def selected(size, name):
    """Realce del slot elegido: borde dorado brillante con esquinas carmesi y centro transparente."""
    im = canvas(size, size)
    d = ImageDraw.Draw(im)
    rect(d, 0, 0, size, size, CRIMSON)
    rect(d, 1, 1, size - 1, size - 1, LIGHT)
    rect(d, 2, 2, size - 2, size - 2, GOLD)
    rect(d, 3, 3, size - 3, size - 3, (0, 0, 0, 0))
    for x, y in ((0, 0), (size - 3, 0), (0, size - 3), (size - 3, size - 3)):
        rect(d, x, y, x + 3, y + 3, CRIMSON)
        rect(d, x + 1, y + 1, x + 2, y + 2, LIGHT)
    save(im, name, size, size)


ornate_bar(92, 15, "bar.png")
ornate_bar(190, 15, "boss.png")
emblem(32, "emblem.png")
slot(20, "slot.png")
selected(24, "slot_selected.png")
