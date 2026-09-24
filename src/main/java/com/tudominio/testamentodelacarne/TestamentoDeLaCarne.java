// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tudominio.testamentodelacarne;

import com.mojang.logging.LogUtils;
import com.tudominio.testamentodelacarne.networking.packet.Networking;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.PathPackResources;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.locating.IModFile;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.slf4j.Logger;

/**
 * Recuperada por decompilacion de mods/examplemod-1.0.0.jar (build del 2025-07-19) y reescrita
 * con mappings oficiales legibles. Logica y valores preservados 1:1 respecto al original.
 */
@Mod(TestamentoDeLaCarne.MODID)
public class TestamentoDeLaCarne {
    public static final String MODID = "testamentodelacarne";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<CreativeModeTab> TESTAMENTO_TAB = CREATIVE_MODE_TABS.register(
            "testamento_tab",
            () -> CreativeModeTab.builder()
                    .icon(() -> new ItemStack((ItemLike) ModItems.FRAGMENTO_DE_MEMORIA.get()))
                    .title(Component.translatable("creativetab.testamento_tab"))
                    .displayItems((params, output) -> {
                        output.accept((ItemLike) ModItems.FRAGMENTO_DE_MEMORIA.get());
                        output.accept((ItemLike) ModItems.CORAZON_DE_PIEDRA_INERTE.get());
                        output.accept((ItemLike) ModItems.ENGRANAJE_ARCANO.get());
                        output.accept((ItemLike) ModItems.LENTE_DE_LA_VERDAD_OCULTA.get());
                        output.accept((ItemLike) ModItems.LAGRIMA_CONSAGRADA.get());
                        output.accept((ItemLike) ModItems.BUSCADOR_DE_ECOS.get());
                        output.accept((ItemLike) ModItems.NUCLEO_DE_AUTOMATA.get());
                        output.accept((ItemLike) ModItems.ALMA_CORRUPTA.get());
                        output.accept((ItemLike) ModItems.RELIQUIA_DEL_APOSTATA.get());
                        output.accept((ItemLike) ModItems.CHASIS_ENGRANAJE.get());
                        output.accept((ItemLike) ModItems.GEMA_SANGRE_IMBUIDA.get());
                        output.accept((ItemLike) ModItems.ESPADA_ANIMA_1.get());
                        output.accept((ItemLike) ModItems.ESPADA_ANIMA_2.get());
                        output.accept((ItemLike) ModItems.ESPADA_ANIMA_3.get());
                        output.accept((ItemLike) ModItems.ESPADA_ANIMA_DEMONIO_1.get());
                        output.accept((ItemLike) ModItems.ESPADA_ANIMA_DEMONIO_2.get());
                        output.accept((ItemLike) ModItems.ESPADA_ANIMA_DEMONIO_3.get());
                        output.accept((ItemLike) ModItems.DARK_METAL_PICKAXE.get());
                        output.accept((ItemLike) ModItems.PRISMA_CONVERTIDOR.get());
                        MaterialWeapons.ALL.forEach(weapon -> output.accept((ItemLike) weapon.get()));
                        // Armas de otros mods que no aparecian en el creativo.
                        for (String extra : new String[] {"cataclysm:zweiender", "cataclysm:final_fractal"}) {
                            var item = net.minecraftforge.registries.ForgeRegistries.ITEMS.getValue(
                                    net.minecraft.resources.ResourceLocation.tryParse(extra));
                            if (item != null && item != net.minecraft.world.item.Items.AIR) {
                                output.accept((ItemLike) item);
                            }
                        }
                    })
                    .build()
    );

    public TestamentoDeLaCarne() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addPackFinders);
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Networking::register);
        event.enqueueWork(() -> net.minecraftforge.common.TierSortingRegistry.registerTier(
                com.tudominio.testamentodelacarne.util.ModTiers.DARK_METAL_TIER,
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("testamentodelacarne", "dark_metal"),
                java.util.List.of(net.minecraft.world.item.Tiers.NETHERITE), java.util.List.of()));
    }

    /**
     * Expone el datapack embebido en este mod (advancements, functions, recetas propias)
     * como un pack de servidor activable via "/datapack enable mod:testamentodelacarne".
     */
    private void addPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            IModFile modFile = ModList.get().getModFileById(MODID).getFile();
            event.addRepositorySource(packConsumer -> {
                Pack pack = Pack.readMetaAndCreate(
                        "builtin/" + MODID,
                        Component.literal("Recursos de El Testamento"),
                        true,
                        path -> new PathPackResources(modFile.getFileName() + ":" + path, modFile.findResource(path), true),
                        PackType.SERVER_DATA,
                        Pack.Position.TOP,
                        PackSource.BUILT_IN
                );
                if (pack != null) {
                    packConsumer.accept(pack);
                }
            });
        }
    }
}
