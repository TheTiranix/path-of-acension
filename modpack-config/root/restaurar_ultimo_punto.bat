@echo off
rem RESPALDO MANUAL: normalmente no hace falta correr esto. Cuando cae el grupo entero, el mod ya
rem restaura el mundo solo apenas la partida se corta (ver ClientWorldRestore), sin cerrar el juego.
rem Este script queda solo por si esa restauracion automatica llegara a fallar (por ejemplo, si el
rem juego se cerro del todo antes de que terminara): restaura el mundo al ultimo punto de guardado a
rem mano. Si no hay nada pendiente, no hace nada: es seguro correrlo igual, por las dudas.
python "%~dp0restore_after_wipe.py" "%~dp0"
echo.
pause
