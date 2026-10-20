"""Genera el lema del menu principal (se superpone en la capa del layout de FancyMenu).

Uso:  python tools/gen_menu_panel.py
Salida: modpack-config/fancymenu/assets/menu_tagline.png
"""
import os

from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = os.path.join(os.path.dirname(__file__), "..", "modpack-config", "fancymenu", "assets")
FONTS = "C:/Windows/Fonts/"
TAGLINE = "UN APÓSTATA SIN DIOS"


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
    tagline()
    print("ok")
