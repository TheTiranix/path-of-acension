// Este script se ejecuta cuando el servidor carga todas las recetas del juego.
ServerEvents.recipes(event => {

    // Es una buena práctica añadir un mensaje a la consola del juego.
    // Así, al iniciar, puedes ver en los logs que tu script se está ejecutando.
    console.info('[Path of Ascension] Aplicando reglas del Capítulo 1...');

    // --- 1. HACEMOS LA MADERA MÁS ESCASA ---
    // Eliminamos la receta estándar que convierte 1 tronco en 4 tablones.
    // El '#' antes de 'minecraft:planks' es muy poderoso: es una "etiqueta" (tag)
    // que agrupa a TODOS los tipos de tablones (roble, abedul, etc.).
    // Así, con una sola línea, bloqueamos todas las recetas de tablones a la vez.
    event.remove({ mod: 'minecraft', output: '#minecraft:planks', input: '#minecraft:logs' }); // solo los de vanilla: las maderas de otros mods conservan su receta (si no, sus troncos no se pueden convertir en tablones ni en mesa de crafteo)

    // Ahora creamos nuestras propias reglas, una por cada tipo de madera.
    // 'event.shapeless' crea una receta que no importa el orden en la mesa de crafteo.
    // La nueva regla es: 1 Tronco -> 2 Tablones.
    // 1 tronco/leño (con o sin corteza, normal o sin descortezar) -> 2 tablones, para cada madera de vanilla. Se usan las etiquetas de cada madera
    // (#minecraft:oak_logs incluye oak_log, oak_wood, stripped_oak_log y stripped_oak_wood): con solo el tronco el "Leño de roble" no servia.
    ['oak', 'spruce', 'birch', 'jungle', 'acacia', 'dark_oak', 'cherry', 'mangrove'].forEach(wood => {
        event.shapeless('2x minecraft:' + wood + '_planks', ['#minecraft:' + wood + '_logs']).id('tcorigenes:planks_from_' + wood);
    });
    ['crimson', 'warped'].forEach(nether => {
        event.shapeless('2x minecraft:' + nether + '_planks', ['#minecraft:' + nether + '_stems']).id('tcorigenes:planks_from_' + nether);
    });

    // --- 2. INICIO DE LA SENDA DEL ACERO ---
// Eliminamos la receta original de la Aleación de Andesita, el primer paso en Create.
event.remove({ output: 'create:andesite_alloy' });

// Creamos una nueva receta que vincula la tecnología con nuestro lore.
// Ahora, para hacer la aleación, se necesita la "memoria" de un mundo olvidado.
event.shapeless('2x create:andesite_alloy', [
    'minecraft:andesite',
    'minecraft:iron_nugget',
    'testamentodelacarne:fragmento_de_memoria' // El catalizador
]);

event.shaped('testamentodelacarne:corazon_de_piedra_inerte', [
  'SFS',
  'F F',
  'SFS'
], {
  S: 'minecraft:stone',
  F: 'testamentodelacarne:fragmento_de_memoria'
});

});