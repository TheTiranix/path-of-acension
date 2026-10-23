"""Capitulo 2 - Todo comienza con pequenos pasos. Los 45 pasos del borrador de ChatGPT, reordenados con las reglas del modpack:
el dark metal (mesa tier 2, lente de la verdad oculta) va ANTES de la netherita, porque en el pack la netherita y la gravitite mejoran piezas de dark metal;
el Aether se abre con la Lagrima Consagrada (Ars Nouveau) y no con agua; los reinos aun no estan implementados (misiones narrativas)."""
from gen_quests import adv, biome, check, dim, give, item, kill, q, struct, xp

FILENAME = "capitulo_2_pequenos_pasos"
TITLE = "II · Todo comienza con pequeños pasos"
ICON = "minecraft:nether_star"
KICKER = "Capítulo II"
PREFIX = "c2"
BG_SRC = "cap2_fondo.png"
BG_OUT = "cap2_bg.png"
BG_BRIGHT = 0.6


def r(n, *gives):
    return [xp(n)] + [give(*g) if isinstance(g, tuple) else give(g) for g in gives]


SECTIONS = [
    {"name": "Bajo la tierra", "quests": [
        q("c2_realms", "Los reinos olvidados", "Nueve reinos sostienen lo que queda del mundo: de Páter, de Filis, de Meidris, de Deiros, de la Luna, y uno sin dios. Aún no sabes cómo llegar a ellos.",
          [check("Conozco la existencia de los reinos")], r(20), icon="minecraft:map", kind="lore"),
        q("c2_dintro", "Mazmorras", "Entre las ruinas del viejo mundo duermen cámaras selladas, con monstruos y botín. Pocas se abren sin pelea.", [check("Entendí qué es una mazmorra")], r(20),
          icon="minecraft:iron_bars", kind="lore"),
        q("c2_dfind", "Una mazmorra", "Bajo tierra, un hueco con ruido de huesos. Encuentra tu primera mazmorra.", [struct("betterdungeons:small_dungeon")], r(40), kind="place"),
        q("c2_dprep", "Prepararse para descender", "Provisiones y un arco: dentro no hay lugar para improvisar.", [item("minecraft:cooked_beef", 16), item("minecraft:bow")], r(30), kind="craft"),
        q("c2_dloot", "Botín de los caídos", "Los esqueletos guardan cámaras enteras. Explora una mazmorra de huesos y llévate lo tuyo.", [struct("betterdungeons:skeleton_dungeon")],
          r(60, ("minecraft:diamond", 1), ("minecraft:golden_apple", 1)), kind="place"),
    ]},
    {"name": "Hierro, diamante y obsidiana", "quests": [
        q("c2_iron", "Herramientas básicas", "Hierro para hacha y pala: lo básico para no depender de la suerte.", [item("minecraft:iron_axe"), item("minecraft:iron_shovel")], r(30), kind="craft"),
        q("c2_flint", "Pedernal y mechero", "Grava, pedernal y hierro: la chispa que hará falta para abrir un portal.", [item("minecraft:flint_and_steel")], r(30), kind="craft"),
        q("c2_diamond", "Brillo bajo la roca", "Los diamantes viven en lo hondo, cerca de la lava. No bajes sin agua a mano.", [item("minecraft:diamond", 3)], r(60), tip="En este pack los diamantes aparecen menos y más profundo."),
        q("c2_dpick", "Un pico de diamante", "Con él cae hasta la obsidiana.", [item("minecraft:diamond_pickaxe")], r(50), kind="craft"),
        q("c2_obs", "Piedra de la noche", "Agua sobre lava quieta da obsidiana. Reúne diez bloques.", [item("minecraft:obsidian", 10)], r(40)),
    ]},
    {"name": "El Nether", "quests": [
        q("c2_portal", "Un portal entre mundos", "Marco de obsidiana, un chispazo, y la puerta se abre hacia el infierno.", [check("Levanté el portal")], r(40), icon="minecraft:obsidian", kind="lore"),
        q("c2_nether", "Más allá del fuego", "Cruza. El aire quema, el cielo es roca.", [dim("minecraft:the_nether")], r(80, ("minecraft:golden_carrot", 8)), kind="place"),
        q("c2_biomes", "Los biomas del infierno", "Bosques carmesí y distorsionados, valles de almas y deltas de basalto. Recórrelos.",
          [biome("minecraft:crimson_forest"), biome("minecraft:warped_forest"), biome("minecraft:soul_sand_valley"), biome("minecraft:basalt_deltas")], r(80), kind="place"),
        q("c2_fort", "La fortaleza", "Piedra negra y esqueletos con calavera. Una fortaleza del Nether.", [struct("betterfortresses:fortress")], r(60), kind="place"),
        q("c2_blaze", "Varas de blaze", "Los blazes custodian lo que arde. Sus varas sirven de combustible y de ingrediente.", [item("minecraft:blaze_rod", 4)], r(50), tip="Búscalos dentro de la fortaleza."),
        q("c2_bastion", "El bastión", "Un bastión remanente: oro, piglins y un cofre que vale la pelea.", [struct("minecraft:bastion_remnant")], r(60), kind="place"),
        q("c2_piglin", "Tesoros de piglins", "Los piglins cambian oro por cosas raras. Reúne una buena cantidad.", [item("minecraft:gold_ingot", 16)], r(40)),
    ]},
    {"name": "Una mesa de otro nivel", "quests": [
        q("c2_osm", "Osmio", "Un metal pesado y plateado. Hará falta para una mesa que aguanta aleaciones oscuras.", [item("mekanism:ingot_osmium", 4)], r(40)),
        q("c2_lens", "La lente de la verdad oculta", "Cuatro fragmentos de memoria y una perla de ender dan una lente que revela lo escondido.", [item("testamentodelacarne:lente_de_la_verdad_oculta")], r(60), kind="craft",
          tip="Receta: perla de ender al centro, cuatro Fragmentos de Memoria en cruz."),
        q("c2_t2", "Una mesa de otro nivel", "El metal oscuro, la netherita y la gravitite no se trabajan en una mesa común. Esta sí.", [item("testamentodelacarne:tier2_crafting_table")], r(80), kind="craft",
          tip="Receta: osmio, lente, pólvora y una mesa de crafteo."),
        q("c2_dmpick", "La primera pieza oscura", "Con tus lingotes de dark metal y la mesa nueva, forja un pico: el primer eslabón del ascenso.", [item("testamentodelacarne:dark_metal_pickaxe")], r(80), kind="craft",
          tip="Se craftea solo en la mesa tier 2."),
    ]},
    {"name": "Netherita", "quests": [
        q("c2_debris", "Escombros ancestrales", "En lo hondo del infierno, restos de una era anterior. Se minan con pico de diamante.", [item("minecraft:ancient_debris", 4)], r(60),
          tip="Busca entre Y 8 y 22 con explosiones o minería en tira."),
        q("c2_scrap", "Fragmentos de netherita", "En el horno, el escombro se rompe en láminas.", [item("minecraft:netherite_scrap", 4)], r(60)),
        q("c2_ingot", "Un lingote de netherita", "Cuatro fragmentos y cuatro lingotes de oro: el metal más duro del mundo.", [item("minecraft:netherite_ingot")], r(80), kind="craft"),
        q("c2_template", "Una plantilla de mejora", "Los bastiones guardan moldes de herrería. Consigue la plantilla de netherita.", [item("minecraft:netherite_upgrade_smithing_template")], r(60),
          tip="Cofres de bastión."),
        q("c2_dup", "Duplicar la plantilla", "Un molde, un bloque de netherrack y siete diamantes dan dos.", [item("minecraft:netherite_upgrade_smithing_template", 2)], r(60), kind="craft"),
        q("c2_up", "Mejorar a netherita", "En este mundo la netherita no empieza de cero: mejora una pieza de dark metal con ella, en la mesa tier 2.", [item("minecraft:netherite_pickaxe")], r(100), kind="craft",
          tip="Un lingote para herramientas y armas; cuatro en cruz para armaduras."),
        q("c2_gear", "Crafteos básicos de netherita", "Arma y armadura: ya puedes enfrentar lo que habita los infiernos.", [item("minecraft:netherite_sword"), item("minecraft:netherite_chestplate")], r(120), kind="craft"),
    ]},
    {"name": "El Infierno hostil", "quests": [
        q("c2_ndun", "Una mazmorra infernal", "Bajo el basalto hay cámaras selladas por quien te precedió. Encuentra una.", [struct("betterdungeons:small_nether_dungeon")], r(60), kind="place"),
        q("c2_nloot", "Sus recompensas", "Los esqueletos del Nether no son comunes: tienen calaveras.", [kill("minecraft:wither_skeleton", 3)], r(60), tip="Se encuentran en fortalezas y mazmorras infernales."),
        q("c2_nboss", "Los primeros jefes", "Dos estructuras guardan lo peor del Nether: una arena en llamas y una forja de almas.", [struct("cataclysm:burning_arena"), struct("cataclysm:soul_black_smith")],
          r(80), kind="place"),
        q("c2_nbosskill", "Derrotar a los jefes", "El Ignis y la Monstruosidad de Netherita. Si caen, el infierno es tuyo.", [kill("cataclysm:ignis"), kill("cataclysm:netherite_monstrosity")],
          r(200, ("minecraft:golden_apple", 3)), kind="boss", tip="Ven con armadura de netherita y pociones de resistencia al fuego."),
    ]},
    {"name": "El cielo", "quests": [
        q("c2_tear1", "Una lágrima del infierno", "Los ghasts lloran lágrimas que valen más que el oro. Consigue una.", [item("minecraft:ghast_tear")], r(40), tip="Mata ghasts con arco a distancia."),
        q("c2_app", "Un aparato de encantamiento", "Para forjar una Lágrima Consagrada hace falta un aparato de Ars Nouveau y gemas de fuente.", [item("ars_nouveau:enchanting_apparatus"), item("ars_nouveau:source_gem", 1)], r(60), kind="craft"),
        q("c2_tear2", "La Lágrima Consagrada", "Un llanto forjado con pura devoción: lágrima de ghast, fragmento de memoria, diamante, gema de fuente y agua.", [item("testamentodelacarne:lagrima_consagrada")], r(120),
          kind="craft", tip="En el aparato de encantamiento: lágrima de ghast al centro; en los pedestales, un Fragmento de Memoria, un diamante, una gema de fuente y un cubo de agua."),
        q("c2_frame", "El marco de luz", "Diez bloques de piedra luminosa en forma de puerta, hasta donde alcance el cielo.", [item("minecraft:glowstone", 10)], r(40), tip="Pide polvo de glowstone del Nether."),
        q("c2_aportal", "Un portal al paraíso", "Haz clic derecho con la lágrima sobre un bloque del marco. El portal se enciende.", [check("Encendí el portal del Aether")], r(60), icon="minecraft:glowstone", kind="lore"),
        q("c2_aether", "El cielo del paraíso", "Cruza. Verás lo que quedó del cielo de los dioses.", [dim("aether:the_aether")], r(150, ("minecraft:golden_carrot", 8)), kind="place"),
        q("c2_abio", "Primeros pasos entre nubes", "Reúne lo que da el cielo: fragmentos de ambrosía y troncos de raíz de cielo.", [item("aether:ambrosium_shard", 4), item("aether:skyroot_log", 4)], r(60)),
    ]},
    {"name": "Las mazmorras del Aether", "quests": [
        q("c2_bronze", "La mazmorra de bronce", "Un laberinto flotante vigilado por un coloso de piedra.", [struct("aether:bronze_dungeon")], r(60), kind="place"),
        q("c2_slider", "El Slider", "Una piedra con ojos que embiste. Esquiva y golpea.", [kill("aether:slider")], r(150, ("minecraft:golden_apple", 1)), kind="boss",
          tip="Ven con armadura buena: hace mucho daño por contacto."),
        q("c2_silver", "La mazmorra de plata", "Más arriba, otra, con los ecos de las valquirias.", [struct("aether:silver_dungeon")], r(80), kind="place"),
        q("c2_valk", "La reina valquiria", "La más fiera de las valquirias. Cuidado con su lanza.", [kill("aether:valkyrie_queen")], r(200, ("minecraft:golden_apple", 2)), kind="boss"),
        q("c2_gold", "La mazmorra de oro", "La cima del Aether. Aquí empieza lo sagrado.", [struct("aether:gold_dungeon")], r(100), kind="place"),
        q("c2_sun", "El espíritu del sol", "El centinela final del cielo. Sus llamas queman lo que toca.", [kill("aether:sun_spirit")], r(300, ("minecraft:golden_apple", 3)), kind="boss"),
        q("c2_acraft", "Crafteos del cielo", "Zanite para el pico y la espada: tu herramienta de cielo.", [item("aether:zanite_pickaxe"), item("aether:zanite_sword")], r(80), kind="craft"),
    ]},
    {"name": "Los demás reinos", "quests": [
        q("c2_realms2", "Los reinos pendientes", "Hay más reinos por descubrir. El mundo apenas empieza a mostrarse.", [check("Estoy listo para seguir buscando")], r(100), icon="minecraft:map", kind="lore"),
        q("c2_final", "Un poder desconocido", "El metal oscuro, la netherita y la gravitite no son un final: son la base de algo más. Hay metales aún más viejos, de fuego y de hielo.", [check("Lo comprendo")],
          r(500, ("minecraft:golden_apple", 5)), icon="minecraft:netherite_ingot", kind="final", tip="Capítulo III: los dioses mismos."),
    ]},
]
