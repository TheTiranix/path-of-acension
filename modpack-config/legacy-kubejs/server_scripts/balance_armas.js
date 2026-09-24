// Balance de armas (pedidos de alejandr0): recetas eliminadas y recetas cambiadas.
ServerEvents.recipes(event => {
  // Tipos de arma repetidos entre Variant Tools y Basic Weapons (y picas / hachas grandes / hachas de mano).
  const removidos = ["vtaw_mw:wooden_pike", "vtaw_mw:wooden_greataxe", "vtaw_mw:wooden_handaxe", "vtaw_mw:wooden_glaive", "basicweapons:wooden_dagger", "vtaw_mw:stone_pike", "vtaw_mw:stone_greataxe", "vtaw_mw:stone_handaxe", "vtaw_mw:stone_glaive", "basicweapons:stone_dagger", "vtaw_mw:iron_pike", "vtaw_mw:iron_greataxe", "vtaw_mw:iron_handaxe", "vtaw_mw:iron_glaive", "basicweapons:iron_dagger", "vtaw_mw:golden_pike", "vtaw_mw:golden_greataxe", "vtaw_mw:golden_handaxe", "vtaw_mw:golden_glaive", "basicweapons:golden_dagger", "vtaw_mw:diamond_pike", "vtaw_mw:diamond_greataxe", "vtaw_mw:diamond_handaxe", "vtaw_mw:diamond_glaive", "basicweapons:diamond_dagger", "vtaw_mw:netherite_pike", "vtaw_mw:netherite_greataxe", "vtaw_mw:netherite_handaxe", "vtaw_mw:netherite_glaive", "basicweapons:netherite_dagger"];
  removidos.forEach(id => {
    event.remove({ output: id });
    event.remove({ input: id });
  });
  // Recetas de compat de las dagas de basic weapons con farmers delight
  event.remove({ id: /basicweapons:compat\/.*dagger.*/ });

  // ---- Born in Chaos: bloque de diamante en lugar de un ingrediente, y cambios pedidos
  const bic = 'born_in_chaos_v1:';
  event.remove({ output: bic + 'sharpened_dark_metal_sword' });
  event.shaped(bic + 'sharpened_dark_metal_sword', [' I ', 'DBD', ' H '], {
    I: bic + 'dark_metal_ingot', B: 'minecraft:diamond_block', D: 'minecraft:diamond', H: bic + 'bone_handle'
  });
  event.remove({ output: bic + 'soul_cutlass' });
  event.shaped(bic + 'soul_cutlass', [' AD', 'AI ', ' H '], {
    A: bic + 'nightmare_claw', D: 'minecraft:diamond', I: bic + 'dark_metal_ingot', H: bic + 'bone_handle'
  });
  event.remove({ output: bic + 'frostbitten_blade' });
  event.shaped(bic + 'frostbitten_blade', [' PP', 'PIP', 'HX '], {
    P: bic + 'permafrost_shard', I: bic + 'dark_metal_ingot', H: bic + 'bone_handle', X: 'minecraft:diamond_block'
  });
  event.remove({ output: bic + 'nightmare_scythe' });
  event.shaped(bic + 'nightmare_scythe', ['CXI', '  H', ' H '], {
    C: bic + 'nightmare_claw', X: 'minecraft:diamond_block', I: bic + 'dark_metal_ingot', H: bic + 'bone_handle'
  });
  event.remove({ output: bic + 'intoxicating_dagger' });
  event.shaped(bic + 'intoxicating_dagger', ['DX', 'DI', ' H'], {
    D: bic + 'intoxicating_decoction', X: 'minecraft:diamond_block', I: bic + 'dark_metal_ingot', H: bic + 'bone_handle'
  });
  event.remove({ output: bic + 'spider_bite_sword' });
  event.shaped(bic + 'spider_bite_sword', ['MIM', 'MXM', ' H '], {
    M: bic + 'spider_mandible', I: bic + 'dark_metal_ingot', X: 'minecraft:diamond_block', H: bic + 'bone_handle'
  });
  event.remove({ output: bic + 'great_reaper_axe' });
  event.shaped(bic + 'great_reaper_axe', ['IXI', 'IH ', ' H '], {
    I: bic + 'dark_metal_ingot', X: 'minecraft:diamond_block', H: bic + 'bone_handle'
  });

  // ---- Ice and Fire: la Phantasmal Blade pide ademas 2 lingotes de netherite
  event.remove({ id: 'iceandfire:ghost_sword' });
  event.shapeless('iceandfire:ghost_sword', [
    'iceandfire:dragonbone_sword', 'iceandfire:ghost_ingot', 'minecraft:netherite_ingot', 'minecraft:netherite_ingot'
  ]);
});
