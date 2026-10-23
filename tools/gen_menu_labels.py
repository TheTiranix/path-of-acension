#!/usr/bin/env python3
"""Genera las etiquetas de los botones del menu principal como imagenes con la letra Cinzel (licencia OFL): no dependen del sistema de fuentes del juego.
Salida: src/main/resources/assets/tcorigenes/textures/gui/menu/btn_<id>_<idioma>.png (+ .mcmeta con filtro lineal para que escalen suaves).
Todas miden 1200x96 con el texto centrado; MenuButtons las dibuja a la altura del boton. Idiomas: es, en (otros idiomas usan la letra normal del juego)."""
import json
import os

from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONT = os.path.join(ROOT, "src", "main", "resources", "assets", "tcorigenes", "font", "cinzel.ttf")
OUT = os.path.join(ROOT, "src", "main", "resources", "assets", "tcorigenes", "textures", "gui", "menu")
LABELS = {
    "es": {"sp": "UN JUGADOR", "mp": "MULTIJUGADOR", "opt": "OPCIONES", "mods": "MODS", "quit": "SALIR"},
    "en": {"sp": "SINGLEPLAYER", "mp": "MULTIPLAYER", "opt": "OPTIONS", "mods": "MODS", "quit": "QUIT"},
}
W, H, SIZE, TRACK = 1200, 96, 70, 0.14


def main():
    os.makedirs(OUT, exist_ok=True)
    font = ImageFont.truetype(FONT, SIZE)
    probe = ImageDraw.Draw(Image.new("RGB", (4, 4)))
    for lang, labels in LABELS.items():
        for key, text in labels.items():
            widths = [probe.textlength(c, font=font) for c in text]
            total = sum(widths) + TRACK * SIZE * (len(text) - 1)
            im = Image.new("RGBA", (W, H), (255, 255, 255, 0))
            d = ImageDraw.Draw(im)
            x = (W - total) / 2
            for c, cw in zip(text, widths):
                d.text((x, 12), c, font=font, fill=(255, 255, 255, 255))  # blanco: el color (crema, blanco al pasar el mouse) se aplica al dibujarla
                x += cw + TRACK * SIZE
            path = os.path.join(OUT, "btn_%s_%s.png" % (key, lang))
            im.save(path, optimize=True)
            with open(path + ".mcmeta", "w", encoding="utf-8") as f:
                json.dump({"texture": {"blur": True}}, f)
            print(path, "%d px de texto" % total)


if __name__ == "__main__":
    main()
