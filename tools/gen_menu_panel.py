"""Genera el panel de texto y el lema del menu principal (se superponen en la capa del layout de FancyMenu).

Uso:  python tools/gen_menu_panel.py
Salida: modpack-config/fancymenu/assets/menu_panel.png y menu_tagline.png
Para cambiar el texto del panel, editar TITLE y BODY y volver a correrlo.
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "modpack-config", "fancymenu", "assets")
FONTS = "C:/Windows/Fonts/"

TITLE = "¿CUÁL ES EL OBJETIVO DE ESTE MODPACK?"
BODY = ("Eres un Apóstata sin dios. Elige una Senda (Sangre, Acero o Carne) y caza las 12 Llaves del Fin del Mundo. "
        "Rituales, sacrificios, estructuras únicas y jefes te esperan en un mundo opresivo donde sobrevivir no basta: "
        "hay que trascender a la divinidad.")
TAGLINE = "UN APÓSTATA SIN DIOS"


def wrap(draw, text, font, width):
    lines, line = [], ""
    for word in text.split():
        trial = (line + " " + word).strip()
        if draw.textlength(trial, font=font) <= width:
            line = trial
        else:
            lines.append(line)
            line = word
    return lines + [line]


def panel():
    w, h, pad = 960, 600, 44
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(img)
    d.rounded_rectangle((6, 6, w - 7, h - 7), 14, fill=(14, 6, 8, 205), outline=(120, 14, 18, 255), width=4)
    d.rounded_rectangle((18, 18, w - 19, h - 19), 8, outline=(60, 10, 12, 255), width=2)
    title_font = ImageFont.truetype(FONTS + "georgiab.ttf", 40)
    body_font = ImageFont.truetype(FONTS + "georgiai.ttf", 38)
    y = pad
    for line in wrap(d, TITLE, title_font, w - 2 * pad):
        d.text((pad, y), line, font=title_font, fill=(200, 30, 34, 255))
        y += 52
    d.line((pad, y + 6, w - pad, y + 6), fill=(120, 14, 18, 255), width=3)
    y += 28
    for line in wrap(d, BODY, body_font, w - 2 * pad):
        d.text((pad, y), line, font=body_font, fill=(226, 206, 206, 255))
        y += 50
    img.save(os.path.join(OUT, "menu_panel.png"))


def tagline():
    font = ImageFont.truetype(FONTS + "georgiab.ttf", 44)
    text = " ".join(TAGLINE)  # espaciado entre letras
    w, h = 900, 90
    glow = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    ImageDraw.Draw(glow).text((10, 14), text, font=font, fill=(170, 20, 24, 255))
    glow = glow.filter(ImageFilter.GaussianBlur(6))
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    img.alpha_composite(glow)
    ImageDraw.Draw(img).text((10, 14), text, font=font, fill=(214, 60, 60, 255))
    img.save(os.path.join(OUT, "menu_tagline.png"))


if __name__ == "__main__":
    panel()
    tagline()
    print("ok")
