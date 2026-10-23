#!/usr/bin/env python3
"""Cambia el idioma de un capitulo de FTB Quests ya guardado (por ejemplo el que se acomodo a mano en el editor del juego) sin tocar posiciones,
dependencias, formas ni recompensas: solo reemplaza titulo, descripcion, titulo de las tareas "checkmark", titulo del capitulo y los carteles de seccion
(c1_sN.png -> c1_sN_<idioma>.png). Las misiones se reconocen por su id (el mismo en los dos idiomas)."""
import re

from gen_quests import hid, s


def localize_saved(text, filename, texts, chapter_title, banner_prefix, lang, aliases=None):
    """texts: {key: (title, lore, tip, check_title)} en el idioma pedido.
    aliases: {titulo en cualquier idioma: key}, para las misiones a las que el editor del juego les dio un id nuevo (copiar/pegar)."""
    by_id = {hid(filename, key): v for key, v in texts.items()}

    def fix_block(m):
        blk = m.group(0)
        qid = re.search(r'\n\t\t\tid: "([0-9A-F]{16})"', blk)
        if not qid:
            return blk
        found = by_id.get(qid.group(1))
        if found is None and aliases:  # id nuevo: se reconoce por el titulo viejo (en cualquier idioma)
            old_title = re.search(r'\n\t\t\ttitle: "(.*?)"(?=\n)', blk)
            key = aliases.get(old_title.group(1)) if old_title else None
            found = texts.get(key) if key else None
        if found is None:
            return blk
        title, lore, tip, check_title = found
        lines = [lore] + (["&7" + tip] if tip else [])
        blk = re.sub(r'\n\t\t\ttitle: ".*?"(?=\n)', lambda _: "\n\t\t\ttitle: " + s(title), blk, count=1)
        blk = re.sub(r'\n\t\t\tdescription: \[.*?\]\n', lambda _: "\n\t\t\tdescription: [" + " ".join(s(x) for x in lines) + "]\n", blk, count=1, flags=re.S)
        if check_title:  # las misiones con tilde manual tienen una sola tarea, con titulo propio (4 o 5 tabulaciones segun el guardado)
            blk = re.sub(r'(\n\t{4,5}title: )".*?"(?=\n)', lambda mm: mm.group(1) + s(check_title), blk)
        return blk

    out = re.sub(r"\n\t\t\{\n.*?\n\t\t\}(?=\n)", fix_block, text, flags=re.S)
    out = re.sub(r'\n\ttitle: ".*?"(?=\n)', lambda _: "\n\ttitle: " + s(chapter_title), out, count=1)
    return re.sub(r'(kubejs:textures/gui/quest/%s_s\d+)(?:_(?:en|es))?\.png' % re.escape(banner_prefix), lambda m: m.group(1) + "_" + lang + ".png", out)
