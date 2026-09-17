ServerEvents.recipes(event => {
  // Elimina la receta original si no lo has hecho
  event.remove({ output: 'bloodmagic:largebloodstonebrick' });

  // Crea una nueva receta que usa la semilla del Foliaath y tu Corazón Inerte
  event.shaped('bloodmagic:largebloodstonebrick', [
    'BSB',
    'SFS',
    'BSB'
  ], {
    B: 'bloodmagic:weakbloodshard',
    S: 'minecraft:stone_bricks',
    F: 'mowzies_mobs:foliaath_seed' // El botín del jefe
  });


   event.shapeless(
    'testamentodelacarne:nucleo_de_automata',
    [ 'mowzies_mobs:wrought_axe' ] // <-- ID correcto que encontraste
);

   // Receta sin forma: 1 Alma Corrupta + 4 de Adoquín (Cobblestone)
  // da como resultado 1 Corazón de Piedra.
  event.shapeless(
    'testamentodelacarne:corazon_de_piedra_inerte', // Resultado
    [
      'testamentodelacarne:alma_corrupta',
      'minecraft:cobblestone',
      'minecraft:cobblestone',
      'minecraft:cobblestone',
      'minecraft:cobblestone',
    ]
  );

  // Ejemplo: Modificar una receta en el Altar de Sangre
  // Esto hace que una pechera de diamante requiera una Lágrima Consagrada en un altar T3
  event.custom({
    type: 'bloodmagic:altar',
    input: { item: 'testamentodelacarne:lagrima_consagrada' },
    output: { item: 'minecraft:diamond_chestplate' },
    upgradeLevel: 2, // Nivel de Altar requerido (0=T1, 1=T2, 2=T3...)
    altarSyphon: 5000, // LP requerido
    consumptionRate: 5,
    drainRate: 5
  });


    event.remove({ output: '#minecraft:planks', input: '#minecraft:logs' });

  // Crea una nueva receta menos eficiente: 1 tronco -> 2 tablones
  // Esto hace que la madera sea un recurso más valioso al principio.
  event.shapeless('2x minecraft:oak_planks', ['minecraft:oak_log']);
  event.shapeless('2x minecraft:birch_planks', ['minecraft:birch_log']);

  event.remove({ output: 'mekanism:steel_casing' });

  // Crea la nueva receta
  event.shaped('mekanism:steel_casing', [
    'SGS',
    'GOG',
    'SGS'
  ], {
    S: 'mekanism:ingot_steel',
    G: 'minecraft:glass',
    O: 'testamentodelacarne:engranaje_arcano' // Tu componente clave
  });

  event.custom({
      type: 'ars_nouveau:enchanting_apparatus',
      reagent: [{ item: 'minecraft:ghast_tear' }], // El ítem del pedestal central
      pedestalItems: [ // Los 4 ítems de los pedestales de alrededor
          { item: { item: 'testamentodelacarne:fragmento_de_memoria' } },
          { item: { item: 'minecraft:diamond' } },
          { item: { item: 'ars_nouveau:source_gem' } },
          { item: { item: 'minecraft:water_bucket' } }
      ],
      output: { item: 'testamentodelacarne:lagrima_consagrada' }
  });

    // --- 4. LAS HERRAMIENTAS DEL APÓSTATA ---
    // Eliminamos las recetas de herramientas de piedra de vanilla
    event.remove({ output: 'minecraft:stone_axe' });
    event.remove({ output: 'minecraft:stone_pickaxe' });
    event.remove({ output: 'minecraft:stone_shovel' });
    event.remove({ output: 'minecraft:stone_hoe' });

    // Creamos las nuevas recetas que requieren el Corazón de Piedra
    
    // Receta para el Hacha de Piedra
    event.shaped('minecraft:stone_axe', [
      'SC',
      'S ',
      'S ' 
    ], {
      S: 'minecraft:stick',
      C: 'testamentodelacarne:corazon_de_piedra_inerte'
    });

    // Receta para el Pico de Piedra
    event.shaped('minecraft:stone_pickaxe', [
      'CCC',
      ' S ',
      ' S ' 
    ], {
      S: 'minecraft:stick',
      C: 'testamentodelacarne:corazon_de_piedra_inerte'
    });
    
    // --- CÓDIGO AÑADIDO ---

    // Receta para la Pala de Piedra
    event.shaped('minecraft:stone_shovel', [
      'C',
      'S',
      'S'
    ], {
      S: 'minecraft:stick',
      C: 'testamentodelacarne:corazon_de_piedra_inerte'
    });

    // Receta para la Azada de Piedra
    event.shaped('minecraft:stone_hoe', [
      'CC',
      ' S',
      ' S'
    ], {
      S: 'minecraft:stick',
      C: 'testamentodelacarne:corazon_de_piedra_inerte'
    });


    // --- PASO 1: Chasis de Engranaje de Precisión (Create) ---
    event.recipes.create.pressing(
        'testamentodelacarne:chasis_engranaje', // CORREGIDO: Ahora es un ítem de tu mod
        [
            'create:brass_ingot',
            'testamentodelacarne:corazon_de_piedra_inerte'
        ]
    );

    // --- PASO 2: Gema de Sangre Imbuida (Blood Magic) ---
    event.recipes.bloodmagic.altar('testamentodelacarne:gema_sangre_imbuida', 'minecraft:diamond') // CORREGIDO
        .upgradeLevel(1)
        .altarSyphon(2000)
        .consumptionRate(5)
        .drainRate(5);

    // --- PASO 4: Ensamblaje Final del Engranaje Arcano ---
    event.shaped(
        'testamentodelacarne:engranaje_arcano', 
        [
            ' L ',
            'SCS',
            ' G '
        ],
        {
            L: 'testamentodelacarne:lagrima_consagrada',
            C: 'testamentodelacarne:chasis_engranaje',  // CORREGIDO
            G: 'testamentodelacarne:gema_sangre_imbuida', // CORREGIDO
            S: 'minecraft:amethyst_shard'
        }
    );

});

