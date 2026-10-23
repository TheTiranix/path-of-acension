#!/usr/bin/env python3
"""Cambia el idioma de las quests (FTB Quests de esta version no traduce por jugador).

Uso:  python tools/set_quest_language.py en|es <carpeta de la instancia>
Copia los capitulos y chapter_groups.snbt del idioma elegido (modpack-config/ftbquests = ingles, modpack-config/ftbquests_es = espanol) a
config/ftbquests/quests de la instancia. Cierra el juego antes: si esta abierto, el editor de quests pisa los archivos al guardar.
(Los ids son los mismos en los dos idiomas: el progreso no se pierde. El mod en si cambia solo con el idioma del juego.)
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
    src = os.path.join(ROOT, "modpack-config", "ftbquests_es" if lang == "es" else "ftbquests", "quests")
    dst = os.path.join(instance, "config", "ftbquests", "quests")
    os.makedirs(os.path.join(dst, "chapters"), exist_ok=True)
    for old in os.listdir(os.path.join(dst, "chapters")):
        if old.endswith(".snbt"):
            os.remove(os.path.join(dst, "chapters", old))
    count = 0
    for name in os.listdir(os.path.join(src, "chapters")):
        shutil.copy(os.path.join(src, "chapters", name), os.path.join(dst, "chapters", name))
        count += 1
    shutil.copy(os.path.join(src, "chapter_groups.snbt"), os.path.join(dst, "chapter_groups.snbt"))
    print("%d capitulos copiados (%s). Reabre el mundo para ver los cambios." % (count, lang))
    return 0


if __name__ == "__main__":
    sys.exit(main())
