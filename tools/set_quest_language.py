#!/usr/bin/env python3
"""Cambia el idioma de las quests del Atlas del Mundo Caido (FTB Quests de esta version no traduce por jugador).

Uso:  python tools/set_quest_language.py en|es <carpeta de la instancia>
Copia los capitulos atlas_*.snbt y chapter_groups.snbt del idioma elegido a config/ftbquests/quests de la instancia.
(El mod en si cambia solo con el idioma del juego.)
"""
import os
import shutil
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))


def main():
    if len(sys.argv) != 3 or sys.argv[1] not in ("en", "es"):
        print(__doc__)
        return 1
    lang, instance = sys.argv[1], sys.argv[2]
    src = os.path.join(ROOT, "modpack-config", "ftbquests_en" if lang == "en" else "ftbquests", "quests")
    dst = os.path.join(instance, "config", "ftbquests", "quests")
    os.makedirs(os.path.join(dst, "chapters"), exist_ok=True)
    count = 0
    for name in os.listdir(os.path.join(src, "chapters")):
        if name.startswith("atlas_"):
            shutil.copy(os.path.join(src, "chapters", name), os.path.join(dst, "chapters", name))
            count += 1
    shutil.copy(os.path.join(src, "chapter_groups.snbt"), os.path.join(dst, "chapter_groups.snbt"))
    print("%d capitulos copiados (%s). Reabre el mundo para ver los cambios." % (count, lang))
    return 0


if __name__ == "__main__":
    sys.exit(main())
