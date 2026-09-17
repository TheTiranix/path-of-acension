// Archivo: kubejs/server_scripts/activar_datapack.js
// Versión corregida con el comando correcto

PlayerEvents.loggedIn(event => {
    // Solo se ejecuta la primera vez que un jugador entra al mundo
    if (!event.player.stages.has('datapack_activado')) {
        event.player.stages.add('datapack_activado');
        
        // --- COMANDO CORREGIDO ---
        // Usamos "mod:testamentodelacarne" para apuntar al data pack dentro de tu mod.
        event.server.runCommandSilent('datapack enable "mod:testamentodelacarne"');
        
        // Forzamos un reload para que los cambios se apliquen inmediatamente al primer login
        event.server.runCommandSilent('reload');
    }
});