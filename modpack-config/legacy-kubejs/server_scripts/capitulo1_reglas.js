// Este script se ejecuta cuando el servidor carga todas las recetas del juego.
ServerEvents.recipes(event => {

    // Es una buena práctica añadir un mensaje a la consola del juego.
    // Así, al iniciar, puedes ver en los logs que tu script se está ejecutando.
    console.info('[Testamento de la Carne] Aplicando reglas del Capítulo 1...');

    // --- 1. HACEMOS LA MADERA MÁS ESCASA ---
    // Eliminamos la receta estándar que convierte 1 tronco en 4 tablones.
    // El '#' antes de 'minecraft:planks' es muy poderoso: es una "etiqueta" (tag)
    // que agrupa a TODOS los tipos de tablones (roble, abedul, etc.).
    // Así, con una sola línea, bloqueamos todas las recetas de tablones a la vez.
    event.remove({ output: '#minecraft:planks', input: '#minecraft:logs' });

    // Ahora creamos nuestras propias reglas, una por cada tipo de madera.
    // 'event.shapeless' crea una receta que no importa el orden en la mesa de crafteo.
    // La nueva regla es: 1 Tronco -> 2 Tablones.
    event.shapeless('2x minecraft:oak_planks', ['minecraft:oak_log']);
    event.shapeless('2x minecraft:spruce_planks', ['minecraft:spruce_log']);
    event.shapeless('2x minecraft:birch_planks', ['minecraft:birch_log']);
    event.shapeless('2x minecraft:jungle_planks', ['minecraft:jungle_log']);
    event.shapeless('2x minecraft:acacia_planks', ['minecraft:acacia_log']);
    event.shapeless('2x minecraft:dark_oak_planks', ['minecraft:dark_oak_log']);
    event.shapeless('2x minecraft:cherry_planks', ['minecraft:cherry_log']);
    event.shapeless('2x minecraft:mangrove_planks', ['minecraft:mangrove_log']);

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