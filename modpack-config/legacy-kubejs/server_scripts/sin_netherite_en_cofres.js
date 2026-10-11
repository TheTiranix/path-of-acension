// Sin netherite ni ancient debris en los cofres del overworld (pedido de alejandr0). El resto de las dimensiones no cambian.
LootJS.modifiers(event => {
  event.addLootTypeModifier(LootType.CHEST)
    .anyDimension("minecraft:overworld")
    .removeLoot(/.*netherite.*/)
    .removeLoot("minecraft:ancient_debris");
});
