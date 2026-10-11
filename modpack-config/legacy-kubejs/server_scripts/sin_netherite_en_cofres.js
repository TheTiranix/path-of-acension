// Sin netherite ni ancient debris en los cofres fuera del Nether (pedido de alejandr0): el netherite solo se consigue en el Nether.
// Vale para los cofres del overworld, del End y de las dimensiones de los mods (los del Nether quedan como estan).
LootJS.modifiers(event => {
  event.addLootTypeModifier(LootType.CHEST)
    .anyDimension(
      "minecraft:overworld", "minecraft:the_end", "twilightforest:twilight_forest", "aether:the_aether", "bloodmagic:dungeon",
      "valoria:the_valoria", "testamentodelacarne:alex_caves", "macabre:the_pit", "macabre:limbo", "tcorigenes:heaven")
    .removeLoot(/.*netherite.*/)
    .removeLoot("minecraft:ancient_debris");
});
