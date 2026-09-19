package com.tcorigenes.tcorigenes;

import com.tcorigenes.tcorigenes.ability.AbilityRegistry;
import com.tcorigenes.tcorigenes.block.ModBlocks;
import com.tcorigenes.tcorigenes.compat.PlayerReviveCompat;
import com.tcorigenes.tcorigenes.core.capability.event.ModCommands;
import com.tcorigenes.tcorigenes.core.capability.event.ModEvents;
import com.tcorigenes.tcorigenes.faction.ModEntityTypes;
import com.tcorigenes.tcorigenes.faction.ModStructurePieceTypes;
import com.tcorigenes.tcorigenes.faction.ModStructureTypes;
import com.tcorigenes.tcorigenes.faction.entity.FactionNpcEntity;
import com.tcorigenes.tcorigenes.item.ModItems;
import com.tcorigenes.tcorigenes.networking.Networking;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Mod separado "tcorigenes": sistema de razas propio (no usa Origins/Apoli, que estan
 * instalados en el pack pero sin usar todavia). El Orbe de Origenes abre una GUI para
 * elegir 1 de 8 razas; la raza se guarda en una Capability (PlayerRaceProvider) y otorga
 * bonos de atributos (ver RaceAttributeManager y ModEvents).
 */
@Mod(TCOrigenes.MOD_ID)
public class TCOrigenes {
    public static final String MOD_ID = "tcorigenes";
    public static final String NBT_KEY_RACE = "player_race_data";
    public static final String NBT_KEY_REGEN_TIMER = "player_regen_timer";

    public TCOrigenes() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModEntityTypes.register(modEventBus);
        ModStructurePieceTypes.register(modEventBus);
        ModStructureTypes.register(modEventBus);
        AbilityRegistry.registerDefaults();
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::addCreative);
        modEventBus.addListener(TCOrigenes::registerAttributes);
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(TCOrigenes::registerOverlays));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(TCOrigenes::registerEntityRenderers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(com.tcorigenes.tcorigenes.client.ModModelLayers::registerLayerDefinitions));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(com.tcorigenes.tcorigenes.client.RaceRenderEvents::onAddLayers));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(com.tcorigenes.tcorigenes.client.RaceRenderEvents.class));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(com.tcorigenes.tcorigenes.client.MobLevelDisplay.class));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(com.tcorigenes.tcorigenes.client.RankingButtons.class));
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> MinecraftForge.EVENT_BUS.register(com.tcorigenes.tcorigenes.client.WeaponWeightTooltip.class));
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(ModEvents.class);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
        PlayerReviveCompat.register();
        com.tcorigenes.tcorigenes.compat.MagicRestrictions.register();
    }

    private static void registerOverlays(RegisterGuiOverlaysEvent event) {
        event.registerAboveAll("ability_cooldown", new com.tcorigenes.tcorigenes.ability.client.AbilityHudOverlay());
        event.registerAboveAll("favor_hud", new com.tcorigenes.tcorigenes.favor.client.FavorHudOverlay());
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntityTypes.FACTION_NPC.get(), FactionNpcEntity.createAttributes().build());
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.FACTION_NPC.get(), com.tcorigenes.tcorigenes.faction.client.FactionNpcRenderer::new);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(Networking::register);
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.INGREDIENTS) {
            event.accept(ModItems.ORBE_DE_ORIGENES);
            event.accept(ModItems.ANILLO_DE_PURIFICACION);
            event.accept(ModItems.BATERIA_LUNAR);
            event.accept(ModItems.VINCULO_DE_CARNE);
            event.accept(ModItems.ESCAFANDRA);
            event.accept(ModItems.BRAZALETE_CUERO);
            event.accept(ModItems.BRAZALETE_HIERRO);
            event.accept(ModItems.BRAZALETE_DARK_METAL);
            event.accept(ModBlocks.ALTAR_PATER_ITEM);
            event.accept(ModBlocks.ALTAR_FILIS_ITEM);
            event.accept(ModBlocks.ALTAR_LUNA_ITEM);
            event.accept(ModBlocks.ALTAR_MEIDRIS_ITEM);
            event.accept(ModBlocks.ALTAR_DEIROS_ITEM);
            event.accept(ModBlocks.ALTAR_TEMPO_ITEM);
        }
    }
}
