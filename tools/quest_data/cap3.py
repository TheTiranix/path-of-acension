"""Capitulo 3 - Conociendo a los dioses. Twilight Forest, Alex's Caves y los metales Fiery / Wintry.
Alex's Caves en este pack no es un portal: son biomas bajo tierra del Overworld (ProgressionTier lo cuenta al pisar un bioma de alexscaves),
asi que esa linea usa biomas en vez de una dimension. Fiery y Wintry (equipo) se forjan en la mesa de nivel 3 (CraftingGate)."""
from gen_quests import biome, check, dim, give, item, kill, q, struct, xp

FILENAME = "capitulo_3_conociendo_a_los_dioses"
TITLE = "III · Conociendo a los dioses"
ICON = "minecraft:ender_eye"
KICKER = "Capítulo III"
PREFIX = "c3"
BG_SRC = None
BG_GRADIENT = ((8, 14, 30), (26, 8, 34))
BG_ACCENT = (110, 210, 150)
BG_OUT = "cap3_bg.png"


def r(n, *gives):
    return [xp(n)] + [give(*g) if isinstance(g, tuple) else give(g) for g in gives]


SECTIONS = [
    {"name": "El camino hacia lo desconocido", "quests": [
        q("c3_mats", "El camino hacia lo desconocido", "Un bosque que existe fuera del tiempo. Para llegar, un charco de agua, flores a su alrededor y un diamante.", [item("minecraft:water_bucket"), item("minecraft:diamond")], r(60),
          tip="Pozo de agua de 2x2 rodeado de plantas, flores o setas."),
        q("c3_portal", "Un portal entre mundos", "Arroja el diamante al agua. Si el bosque te acepta, el charco brilla.", [check("Activé el portal")], r(60), icon="minecraft:diamond", kind="lore"),
        q("c3_lore1", "Los dioses olvidados", "Meidris dejó aquí lo que no cabía en el mundo: humanoides, magia vieja, elfos de luz. El bosque conserva su memoria.", [check("Leí la memoria del bosque")], r(40), icon="minecraft:book", kind="lore"),
        q("c3_enter", "El bosque crepuscular", "El cielo se vuelve verde y el aire huele a hojas mojadas. Cruza.", [dim("twilightforest:twilight_forest")], r(150, ("minecraft:golden_carrot", 8)), kind="place"),
        q("c3_biomes", "Un mundo que explorar", "Bosque, pantano, bosque denso: cada rincón tiene sus reglas.", [biome("twilightforest:forest"), biome("twilightforest:dense_forest"), biome("twilightforest:swamp")], r(80), kind="place"),
        q("c3_fauna", "Habitantes de otro mundo", "Capuchas rojas y lobos hostiles recorren el crepúsculo.", [kill("twilightforest:redcap"), kill("twilightforest:hostile_wolf")], r(60)),
        q("c3_res", "Recursos de tierras lejanas", "La madera de hierro: la dimensión te la da, el horno la termina.", [item("twilightforest:ironwood_ingot", 2)], r(60)),
        q("c3_maps", "Preparativos para la cacería", "Los mapas mágicos muestran lo que el crepúsculo esconde.", [item("twilightforest:magic_map")], r(60), kind="craft"),
    ]},
    {"name": "La Naga y el Lich", "quests": [
        q("c3_nagas", "La serpiente del bosque", "En un patio de piedra, una serpiente gigante espera. Encuentra su guarida.", [struct("twilightforest:naga_courtyard")], r(60), kind="place"),
        q("c3_naga", "La Naga", "Rápida, y sus embestidas duelen. Dale batalla.", [kill("twilightforest:naga")], r(200, ("minecraft:golden_apple", 2)), kind="boss"),
        q("c3_scale", "Escamas de una bestia", "Con sus escamas se forja armadura de Naga.", [item("twilightforest:naga_scale", 4)], r(80)),
        q("c3_lichs", "La torre del hechicero", "Una torre alta, con ilusiones por dentro. El Twilight Lich la habita.", [struct("twilightforest:lich_tower")], r(80), kind="place"),
        q("c3_lich", "El rey de las ilusiones", "Clones, relámpagos y bombas. Sobrevive y derríbalo.", [kill("twilightforest:lich")], r(300, ("minecraft:golden_apple", 3)), kind="boss"),
        q("c3_lichloot", "Reliquias de magia antigua", "El cetro del Lich guarda un fragmento de lo que fue magia verdadera.", [item("twilightforest:twilight_scepter")], r(100), tip="También cae el cetro de drenaje y el de zombis."),
    ]},
    {"name": "El laberinto y la Hidra", "quests": [
        q("c3_maze", "El laberinto subterráneo", "Bajo el bosque, un laberinto de piedra. El Minotauro guarda lo del fondo.", [struct("twilightforest:labyrinth")], r(80), kind="place"),
        q("c3_mino", "El Minoshroom", "Un minotauro hongo. Si caes, el laberinto no te suelta.", [kill("twilightforest:minoshroom")], r(300, ("minecraft:golden_apple", 3)), kind="boss"),
        q("c3_hlair", "La hidra de fuego", "En una caverna de lava, una bestia de muchas cabezas.", [struct("twilightforest:hydra_lair")], r(100), kind="place"),
        q("c3_hydra", "La bestia de las múltiples cabezas", "Corta cabeza tras cabeza. Cuidado con su aliento de fuego.", [kill("twilightforest:hydra")], r(400, ("minecraft:golden_apple", 4)), kind="boss",
          tip="Lleva resistencia al fuego."),
        q("c3_blood", "Sangre ardiente", "La sangre de la hidra es el corazón de lo que se forja con fuego eterno.", [item("twilightforest:fiery_blood", 2)], r(100)),
    ]},
    {"name": "Fuego eterno", "quests": [
        q("c3_fiery", "El poder del fuego", "Sangre de hidra y hierro: así se forja el fiery, un metal que nunca se enfría.", [item("twilightforest:fiery_ingot", 4)], r(150), kind="craft"),
        q("c3_t3", "La mesa de los dioses", "Lo que ahora forjas ya no cabe en la mesa de metal oscuro. Esta la sigue.", [item("testamentodelacarne:tier3_crafting_table")], r(150), kind="craft",
          tip="Mesa de nivel 3 (5x5): arcane, black steel, dragonsteel, fiery y wintry."),
        q("c3_ftools", "Herramientas de fuego eterno", "El pico y la espada del fiery, tibios al tacto.", [item("twilightforest:fiery_pickaxe"), item("twilightforest:fiery_sword")], r(200), kind="craft"),
        q("c3_fgear", "Armaduras y armas ardientes", "Casco y coraza: nada que arda te toca.", [item("twilightforest:fiery_helmet"), item("twilightforest:fiery_chestplate")], r(250), kind="craft"),
    ]},
    {"name": "La Torre Oscura y los caballeros", "quests": [
        q("c3_dtower", "La torre oscura", "Una torre que huele a alma y azufre. Los ghasts guardan sus pisos.", [struct("twilightforest:dark_tower")], r(100), kind="place",
          tip="No confundir con la Torre Oscura de Born in Chaos (capítulo I)."),
        q("c3_urg", "El Ur-Ghast", "Un ghast descomunal. El techo de la torre es su arena.", [kill("twilightforest:ur_ghast")], r(400, ("minecraft:golden_apple", 4)), kind="boss"),
        q("c3_kstr", "Los caballeros fantasma", "Una fortaleza donde aún se escuchan armaduras sin dueño.", [struct("twilightforest:knight_stronghold")], r(100), kind="place"),
        q("c3_phantom", "El guardián de la fortaleza", "Fantasmas con armadura. Derriba a los caballeros.", [kill("twilightforest:knight_phantom", 3)], r(400, ("minecraft:golden_apple", 4)), kind="boss"),
    ]},
    {"name": "Invierno eterno", "quests": [
        q("c3_snow", "El frío de las montañas", "Más allá del bosque, la nieve y el hielo. Cruza los biomas fríos.", [biome("twilightforest:snowy_forest"), biome("twilightforest:glacier")], r(100), kind="place"),
        q("c3_ycave", "La cueva del yeti", "Bajo el hielo, una cueva con pieles y garras.", [struct("twilightforest:yeti_cave")], r(100), kind="place"),
        q("c3_alpha", "El Alpha Yeti", "Un gigante peludo y furioso. Lanza hielo y embiste.", [kill("twilightforest:alpha_yeti")], r(400, ("minecraft:golden_apple", 4)), kind="boss"),
        q("c3_wintry", "El poder del invierno", "Hielo y plata: el wintry, un metal que no se derrite.", [item("testamentodelacarne:wintry_ingot", 4)], r(150), kind="craft"),
        q("c3_wtools", "Herramientas de hielo eterno", "Tan frías que el aire a su alrededor se vuelve niebla.", [item("testamentodelacarne:wintry_sword")], r(200), kind="craft"),
        q("c3_wgear", "Armaduras y armas congeladas", "Casco y coraza helados: protegen de todo lo que quema.", [item("testamentodelacarne:wintry_helmet"), item("testamentodelacarne:wintry_chestplate")], r(250), kind="craft"),
        q("c3_palace", "El palacio de la Reina de las Nieves", "Un castillo de aurora. Todo lo que la rodea es hielo vivo.", [struct("twilightforest:aurora_palace")], r(100), kind="place"),
        q("c3_queen", "La Reina de las Nieves", "La señora del invierno. Su corte de cristal no perdona.", [kill("twilightforest:snow_queen")], r(500, ("minecraft:golden_apple", 5)), kind="boss"),
    ]},
    {"name": "Gigantes y desafíos finales", "quests": [
        q("c3_troll", "Los secretos de los gigantes", "Cavernas de trolls y mineros gigantes. Lo que guardan no es poco.", [struct("twilightforest:troll_cave")], r(120), kind="place"),
        q("c3_castle", "El último desafío del bosque", "Un castillo que solo se abre con el trofeo de cada jefe.", [struct("twilightforest:final_castle")], r(300), kind="place"),
        q("c3_prize", "El precio de la victoria", "Cada jefe dejó algo: armas, armaduras, reliquias. Revisa lo que puedes forjar con ello.", [check("Revisé las recompensas de los jefes")], r(120), icon="minecraft:chest", kind="lore"),
    ]},
    {"name": "Cavernas primordiales", "quests": [
        q("c3_cavehow", "Un nuevo mundo bajo tierra", "Más allá de la capa de roca hay cavernas con clima y bestias propios. No hay puerta: hay que bajar.", [check("Quiero bajar")], r(60), icon="minecraft:glow_berries", kind="lore",
          tip="Alex's Caves son biomas subterráneos del Overworld."),
        q("c3_cavemat", "Preparar el descenso", "Antorchas, comida y una manzana de oro: nadie vuelve del fondo con las manos vacías.", [item("minecraft:torch", 32), item("minecraft:golden_apple")], r(60), kind="craft"),
        q("c3_prim", "El mundo primitivo", "Una caverna de selva y huesos. Aquí el tiempo se detuvo hace millones de años.", [biome("alexscaves:primordial_caves")], r(200, ("minecraft:golden_carrot", 8)), kind="place"),
        q("c3_lore2", "Ecos de una era olvidada", "Lo que ves no es obra de dioses sino de la carne: una era antes de las razas, de los reinos, de los nombres.", [check("Escuché los ecos")], r(60), icon="minecraft:bone", kind="lore"),
        q("c3_dino", "Gigantes de un mundo perdido", "Un cuenco de huesos descomunales. Aquí caminaron bestias de otro tiempo.", [struct("alexscaves:dino_bowl")], r(100), kind="place"),
        q("c3_book", "El conocimiento de las cavernas", "Un compendio que se llena a medida que descubres a sus habitantes.", [item("alexscaves:cave_book")], r(80), kind="craft"),
        q("c3_cres", "Recursos de la antigüedad", "Ámbar, hueso pesado: lo que la caverna entrega a quien la recorre.", [item("alexscaves:amber", 4), item("alexscaves:heavy_bone", 2)], r(80)),
        q("c3_beasts", "Criaturas prehistóricas", "Raptores veloces y triceratops con coraza.", [kill("alexscaves:vallumraptor"), kill("alexscaves:grottoceratops")], r(120)),
        q("c3_titan", "Los colosos primitivos", "Un tiranosaurio de la edad antigua. Reza antes de acercarte.", [kill("alexscaves:luxtructosaurus")], r(500, ("minecraft:golden_apple", 5)), kind="boss",
          tip="Lleva armadura completa de metal nuevo y pociones."),
        q("c3_surv", "Guía de supervivencia primitiva", "Lleva luz, comida y un plan de retirada. Las cavernas devoran a los distraídos.", [check("Entendí los riesgos")], r(60), icon="minecraft:campfire", kind="lore"),
    ]},
    {"name": "Los dioses antiguos", "quests": [
        q("c3_relics", "Reliquias de las cavernas", "Un par de reliquias antiguas, para quien sepa leerlas.", [item("alexscaves:dinosaur_chop", 2)], r(80)),
        q("c3_power", "El poder de los dioses antiguos", "Superaste el crepúsculo y las cavernas. Lo que antes eran mitos, hoy son nombres que has derrotado.", [check("Estoy listo")], r(200), icon="minecraft:totem_of_undying", kind="lore"),
        q("c3_know", "El conocimiento prohibido", "Fuego y hielo, juntos: dos metales para dos extremos. Muestra que los tienes.", [item("twilightforest:fiery_ingot"), item("testamentodelacarne:wintry_ingot")], r(250), kind="craft"),
        q("c3_final", "Algo mucho más antiguo", "Los dioses y las bestias de estos mundos no son el principio. Son el eco de algo anterior a todos.", [check("Lo entiendo")], r(1000, ("minecraft:golden_apple", 8)),
          icon="minecraft:ender_eye", kind="final"),
    ]},
]
