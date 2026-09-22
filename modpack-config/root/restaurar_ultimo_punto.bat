@echo off
rem Corre esto DESPUES de que el juego se cerro solo por una caida de grupo entera, y ANTES de
rem volver a abrirlo. Restaura el mundo al ultimo punto de guardado (ver WorldRestoreManager en el
rem mod y tools/restore_after_wipe.py). Si no hay nada pendiente, no hace nada: es seguro correrlo
rem siempre, por las dudas, antes de abrir el juego.
python "%~dp0restore_after_wipe.py" "%~dp0"
echo.
pause
