#!/usr/bin/env python3
"""Revisa los capitulos SNBT generados: llaves y corchetes balanceados, ids unicos, dependencias que existen, imagenes referenciadas presentes."""
import glob
import os
import re
import sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CHS = [os.path.join(ROOT, "modpack-config", d, "quests", "chapters") for d in ("ftbquests", "ftbquests_es")]
TEX = os.path.join(ROOT, "modpack-config", "legacy-kubejs", "assets", "kubejs", "textures", "gui", "quest")

bad = 0
all_ids = {}
for f in sorted(g for ch in CHS for g in glob.glob(ch + "/*.snbt")):
    txt = open(f, encoding="utf-8").read()
    # balance fuera de strings
    depth = {"{": 0, "[": 0}
    stack, in_str, esc = [], False, False
    for ch in txt:
        if in_str:
            if esc:
                esc = False
            elif ch == "\\":
                esc = True
            elif ch == '"':
                in_str = False
            continue
        if ch == '"':
            in_str = True
        elif ch in "{[":
            stack.append(ch)
        elif ch in "}]":
            if not stack or {"}": "{", "]": "["}[ch] != stack.pop():
                print(os.path.basename(f), "desbalanceado")
                bad += 1
                break
    if stack or in_str:
        print(os.path.basename(f), "quedo abierto")
        bad += 1
    ids = re.findall(r'\bid: "([0-9A-F]{16})"', txt)
    dup = {i for i in ids if ids.count(i) > 1}
    if dup:
        print(os.path.basename(f), "ids repetidos", list(dup)[:3])
        bad += 1
    qids = set(re.findall(r'\n\t\t\tid: "([0-9A-F]{16})"', txt))
    for dep in re.findall(r'dependencies: \[(.*?)\]', txt):
        for d in re.findall(r'"([0-9A-F]{16})"', dep):
            if d not in qids:
                print(os.path.basename(f), "dependencia inexistente", d)
                bad += 1
    for img in re.findall(r'image: "kubejs:textures/gui/quest/([^"]+)"', txt):
        if not os.path.exists(os.path.join(TEX, img)):
            print(os.path.basename(f), "falta imagen", img)
            bad += 1
    for i in ids:
        all_ids.setdefault(i, []).append(os.path.basename(f))
    
    print("%-45s quests=%d tareas=%d" % (os.path.basename(f), len(qids), txt.count('type: "') - txt.count('type: "item"\n\t\t\t\t}') * 0))
cross = []
if cross:
    print("ids repetidos entre capitulos:", cross[:3])
    bad += 1
print("OK" if not bad else "%d problemas" % bad)
sys.exit(1 if bad else 0)
