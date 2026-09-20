# Path of Ascension - todo lo que va FUERA del jar

El mod propio (`testamentodelacarne-0.1.0.jar`) trae el codigo y sus datos. Esta carpeta trae lo
demas que cambiamos del pack: **configs de mods ajenos, quests y scripts**. Asi todos juegan con
los mismos cambios. Los mods de terceros NO estan en este repo (ver `MODS.txt`).

## Como instalarlo

1. Instala los mods de `MODS.txt` (CurseForge / Modrinth). El pack usa **Just Enough Resources**
   (JER) para ver de que mob cae cada item.
2. Copia `testamentodelacarne-0.1.0.jar` (de `releases/` o de la Release de GitHub) a `mods/`.
3. Descomprime `PathOfAscension-configs-<version>.zip` (Release de GitHub) **encima de la carpeta
   de la instancia**: pisa los archivos de `config/` y `kubejs/`.
4. Abre el mundo de nuevo. Las quests y las configs de mods se leen al cargar.

## Que contiene

| Carpeta del repo | Va en la instancia | Que cambia |
|---|---|---|
| `config/toughasnails/` | `config/toughasnails/` | Sed al 10%, regeneracion sin depender de la sed, dano por temperatura extrema tras 1 dia |
| `config/apotheosis/` | `config/apotheosis/` | Bosses invasores tambien en Twilight Forest y The Aether |
| `ftbquests/` | `config/ftbquests/` | Quests, con 1 punto de habilidad en las que dan XP |
| `legacy-kubejs/` | `kubejs/` | Scripts de KubeJS |

## Para quien mantiene el repo

Si cambias una config (o cualquier cosa fuera del jar) en tu instancia, **copiala aqui y subela en
el mismo push**. Para generar el zip de una release: `python tools/make_release.py v0.2.0`.

## Licencia

El mod y los materiales de este repositorio son © 2026 Agustin (TheTiranix), todos los derechos reservados. Ver `LICENSE.txt` en la raiz. Las configs de mods ajenos que se incluyen aqui son solo los valores que elegimos; los mods en si pertenecen a sus autores (ver `MODS.txt`).
