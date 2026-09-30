// Mesas de crafteo con nivel (pedido de alejandr0). Las armaduras de dark metal ya no se hacen en la mesa de herreria (mejorando
// una de netherite con la placa): se craftean de la forma comun en la mesa tier 2 (recetas dark_metal_armor_* del mod).
ServerEvents.recipes(event => {
  ['_2', '_3', '_4', '_5'].forEach(suffix => {
    event.remove({ id: 'born_in_chaos_v1:armor_plate_from_dark_metal_k' + suffix });
  });
});

// Armas, herramientas y armaduras de black steel, dragonsteel y arcane (Amethyst Rapier, Spellbreaker) solo se craftean en la mesa
// tier 3 (5x5): se sacan las recetas de 3x3 originales; las nuevas estan en el mod (recetas tier3_shaped).
ServerEvents.recipes(event => {
  ["cataclysm:black_steel_sword", "cataclysm:black_steel_axe", "cataclysm:black_steel_pickaxe", "cataclysm:black_steel_shovel", "cataclysm:black_steel_hoe", "irons_spellbooks:amethyst_rapier", "irons_spellbooks:spellbreaker", "iceandfire:dragonsteel_fire_sword", "iceandfire:dragonsteel_fire_pickaxe", "iceandfire:dragonsteel_fire_axe", "iceandfire:dragonsteel_fire_shovel", "iceandfire:dragonsteel_fire_hoe", "iceandfire:dragonsteel_fire_helmet", "iceandfire:dragonsteel_fire_chestplate", "iceandfire:dragonsteel_fire_leggings", "iceandfire:dragonsteel_fire_boots", "iceandfire:dragonsteel_ice_sword", "iceandfire:dragonsteel_ice_pickaxe", "iceandfire:dragonsteel_ice_axe", "iceandfire:dragonsteel_ice_shovel", "iceandfire:dragonsteel_ice_hoe", "iceandfire:dragonsteel_ice_helmet", "iceandfire:dragonsteel_ice_chestplate", "iceandfire:dragonsteel_ice_leggings", "iceandfire:dragonsteel_ice_boots", "iceandfire:dragonsteel_lightning_sword", "iceandfire:dragonsteel_lightning_pickaxe", "iceandfire:dragonsteel_lightning_axe", "iceandfire:dragonsteel_lightning_shovel", "iceandfire:dragonsteel_lightning_hoe", "iceandfire:dragonsteel_lightning_helmet", "iceandfire:dragonsteel_lightning_chestplate", "iceandfire:dragonsteel_lightning_leggings", "iceandfire:dragonsteel_lightning_boots"].forEach(output => {
    event.remove({ output: output, type: 'minecraft:crafting_shaped' });
  });
});
