"""Capitulo 1 - El Despertar. Estructura del borrador de ChatGPT, adaptada a las reglas reales del modpack:
herramientas de piedra con Corazon de Piedra Inerte (Fragmentos de Memoria al minar piedra), Create con Fragmento de Memoria,
bronce de Mekanism con cobre, mochila de Sophisticated Backpacks, dark metal de Born in Chaos (alto horno, mesa tier 2)."""
from gen_quests import adv, biome, check, give, item, kill, q, struct, xp

FILENAME = "capitulo_1_el_despertar"
TITLE = "I · El Despertar"
ICON = "minecraft:torch"
KICKER = "Capítulo I"
PREFIX = "c1"
BG_SRC = "cap1_fondo.png"
BG_OUT = "cap1_bg.png"
BG_BRIGHT = 1.25


def r(n, *gives):
    return [xp(n)] + [give(*g) if isinstance(g, tuple) else give(g) for g in gives]


SECTIONS = [
    {"name": "El despertar", "quests": [
        q("c1_start", "El comienzo", "Abres los ojos en un mundo que ya no recuerda a sus dioses. Todo empieza aquí.", [check("Estoy despierto")],
          r(10, ("minecraft:bread", 4)), icon="minecraft:compass", kind="lore", tip="Pulsa el botón de la tarea para completarla."),
        q("c1_inv", "Un mundo desconocido", "No recuerdas cómo llegaste. Revisa lo poco que llevas encima.", [check("Revisé mi inventario")],
          r(10), icon="minecraft:chest", kind="lore", tip="Abre el inventario con la tecla E."),
        q("c1_hands", "Las manos vacías", "Antes que cualquier herramienta, tus manos. Rompe lo que la tierra te ofrece y recógelo.", [item("minecraft:dirt", 4)],
          r(10, ("minecraft:apple", 2)), tip="Mantén pulsado el clic izquierdo sobre un bloque."),
    ]},
    {"name": "Los dones de la naturaleza", "quests": [
        q("c1_log", "El primer recurso", "El bosque es tu primer aliado. Arranca un tronco a golpes.", [check("Conseguí un tronco")], r(10),
          icon="minecraft:oak_log", kind="lore", tip="Sirve el tronco de cualquier árbol."),
        q("c1_planks", "Madera trabajada", "La madera escasea: aquí un tronco rinde solo dos tablones. Úsalos con cabeza.", [check("Conseguí tablones")], r(10),
          icon="minecraft:oak_planks", kind="lore", tip="1 tronco = 2 tablones, de cualquier madera."),
        q("c1_table", "Una superficie de trabajo", "Con cuatro tablones tendrás un lugar donde construir de verdad.", [item("minecraft:crafting_table")], r(15), kind="craft"),
        q("c1_stick", "Pequeñas herramientas", "Dos tablones, uno sobre otro, dan palos. Casi todo empieza con ellos.", [item("minecraft:stick", 4)], r(10), kind="craft"),
        q("c1_wpick", "Un pico rudimentario", "Madera tosca, pero basta para arrancarle la piedra al suelo.", [item("minecraft:wooden_pickaxe")], r(15), kind="craft"),
        q("c1_stone", "La piedra bajo nuestros pies", "Mina piedra con tu pico. Algo en ella susurra.", [item("minecraft:cobblestone", 8)], r(15),
          tip="Cada bloque de piedra tiene un 8% de soltar un Fragmento de Memoria."),
        q("c1_frag", "Susurros cristalizados", "Un cristal tenue que vibra: un eco de un mundo olvidado. Reúne cuatro.", [item("testamentodelacarne:fragmento_de_memoria", 4)], r(25),
          tip="Sigue minando piedra: sale uno cada tantos bloques."),
        q("c1_heart", "El corazón palpitante", "Cuatro fragmentos alrededor de la piedra forman un Corazón de Piedra Inerte. Todo metal de este mundo pasa por él.",
          [item("testamentodelacarne:corazon_de_piedra_inerte")], r(30, ("minecraft:cobblestone", 16)), kind="craft",
          tip="Receta: piedra y Fragmento de Memoria, alternados en una mesa de crafteo (S F S / F _ F / S F S)."),
        q("c1_tools", "Herramientas de verdad", "Las herramientas de piedra de este mundo no se tallan: se forjan con corazones de piedra.",
          [item("minecraft:stone_axe"), item("minecraft:stone_shovel")], r(30), kind="craft", tip="Hacha y pala: un corazón de piedra cada una, más palos."),
    ]},
    {"name": "La primera defensa", "quests": [
        q("c1_alone", "No estamos solos", "Cuando cae el sol, la tierra rota despierta. Pasa tu primera noche sin morir.", [check("Sobreviví a mi primera noche")], r(15),
          icon="minecraft:skeleton_skull", kind="lore"),
        q("c1_sword", "El primer filo", "Un arma, aunque sea de madera, es la diferencia entre correr y resistir.", [item("minecraft:wooden_sword")], r(15), kind="craft"),
        q("c1_flesh", "La carne corrupta", "Los muertos andan. Derriba a cinco y quédate con lo que dejen.", [kill("minecraft:zombie", 5)], r(25, ("minecraft:cooked_beef", 4)),
          tip="Los zombis, esqueletos, arañas y creepers suelen soltar Almas Corruptas."),
        q("c1_soul", "Ecos de los caídos", "Lo que dejan los corruptos es moneda para los que buscan ascender. Guarda estas almas.", [item("testamentodelacarne:alma_corrupta", 3)], r(20),
          tip="Cuatro tipos de mob las sueltan: zombi, esqueleto, araña y creeper."),
        q("c1_torch", "Prepararse para la noche", "Carbón y palos: una antorcha mantiene a raya a lo que acecha en la oscuridad.", [item("minecraft:torch", 8)], r(15), kind="craft"),
    ]},
    {"name": "El metal de los mortales", "quests": [
        q("c1_spick", "Piedra y profundidad", "El cobre no cede a la madera. Forja un pico de piedra.", [item("minecraft:stone_pickaxe")], r(30), kind="craft",
          tip="Pide tres corazones de piedra: planifica tus fragmentos."),
        q("c1_rawcu", "Bajo la superficie", "Vetas verdosas en la roca: cobre. Arráncalo en bruto.", [item("minecraft:raw_copper", 4)], r(20)),
        q("c1_furnace", "El calor transforma la roca", "Ocho piedras y fuego. El horno convierte lo crudo en metal.", [item("minecraft:furnace")], r(15, ("minecraft:coal", 4)), kind="craft"),
        q("c1_cu", "El primer lingote", "Fundir el cobre da un metal blando, pero tuyo.", [item("minecraft:copper_ingot", 4)], r(20)),
        q("c1_cuuse", "Un metal con posibilidades", "El cobre no es solo adorno: protege del rayo y se acumula en bloques.", [item("minecraft:copper_block"), item("minecraft:lightning_rod")],
          r(30), kind="craft", tip="Consulta más usos con JEI: tecla R sobre el lingote."),
        q("c1_bronze", "Bronce con cobre", "Con cobre y palos se forja bronce de Mekanism: la primera armadura y herramientas de verdad.", [item("mekanismtools:bronze_pickaxe")],
          r(30, ("minecraft:cooked_beef", 4)), kind="craft", tip="Receta: tres lingotes de cobre y dos palos (como el pico de hierro)."),
    ]},
    {"name": "La fuerza del hierro", "quests": [
        q("c1_coal", "Más profundo", "Baja hasta donde el hierro duerme. Lleva carbón de sobra para fundirlo.", [item("minecraft:coal", 8)], r(20)),
        q("c1_rawfe", "Mineral sin refinar", "Hierro en bruto, rojizo y pesado. Reúne una buena carga.", [item("minecraft:raw_iron", 16)], r(25)),
        q("c1_fe", "El hierro toma forma", "En el horno, el mineral se vuelve lingote.", [item("minecraft:iron_ingot", 16)], r(30)),
        q("c1_fetool", "Un nuevo nivel de herramientas", "Pico y espada de hierro: ya no eres presa fácil.", [item("minecraft:iron_pickaxe"), item("minecraft:iron_sword")], r(40), kind="craft"),
        q("c1_armor", "Hierro que protege", "Un escudo y una coraza. Lo que los dioses olvidaron, tú lo recuerdas.", [item("minecraft:shield"), item("minecraft:iron_chestplate")], r(40), kind="craft"),
        q("c1_util", "Los secretos del hierro", "Cubos para el agua, tolvas para mover objetos. El hierro sirve para casi todo.", [item("minecraft:bucket"), item("minecraft:hopper")], r(40), kind="craft",
          tip="Mira en JEI qué más se hace con hierro: yunques, raíles, calderos..."),
        q("c1_ind", "La era industrial", "Un engranaje de Create necesita aleación de andesita, y la aleación necesita memoria. Así empieza la máquina.",
          [item("create:andesite_alloy", 2), item("create:cogwheel")], r(50), kind="craft",
          tip="Aleación: andesita, pepita de hierro y un Fragmento de Memoria."),
    ]},
    {"name": "Un lugar al que regresar", "quests": [
        q("c1_mat", "Materiales para un hogar", "Piedra para las paredes. Mucha piedra.", [item("minecraft:cobblestone", 64)], r(20)),
        q("c1_walls", "Cuatro paredes y un techo", "Levanta un refugio cerrado donde la noche no pueda entrar.", [check("Construí un refugio")], r(30), icon="minecraft:oak_door", kind="lore"),
        q("c1_light", "Que no reine la oscuridad", "Ilumina cada rincón: lo que no ves, te caza.", [item("minecraft:torch", 16)], r(20), kind="craft"),
        q("c1_bed", "Un lecho para descansar", "Tres lanas y tres tablones. Descansar también es sobrevivir.", [item("minecraft:white_bed")], r(25), kind="craft", tip="Lana blanca de oveja."),
        q("c1_sleep", "Guardar el progreso", "Duerme en tu cama: ese será tu punto de regreso.", [adv("minecraft:adventure/sleep_in_bed")], r(30, ("minecraft:golden_carrot", 4)),
          tip="Solo cuenta si duermes de verdad (de noche)."),
    ]},
    {"name": "Más allá del horizonte", "quests": [
        q("c1_call", "La llamada de otros mundos", "Se habla de nueve reinos en pie, cada uno fiel a un dios caído. Ninguno sabe de los demás.", [check("Escuché la llamada")], r(20),
          icon="minecraft:filled_map", kind="lore"),
        q("c1_prep", "Preparar la expedición", "Comida, hierro en la cabeza y paso firme. Nadie vuelve de un viaje sin provisiones.", [item("minecraft:cooked_beef", 8), item("minecraft:iron_helmet")], r(30), kind="craft"),
        q("c1_sign", "Señales de otro reino", "En el bosque oscuro la luz no llega y lo viejo respira. Busca esa penumbra.", [biome("minecraft:dark_forest")], r(40), kind="place"),
        q("c1_tower", "El primer reino", "Una torre de piedra negra se alza entre los árboles. Lo que la habita no sirve a ningún dios.", [struct("born_in_chaos_v1:dark_tower_forest")],
          r(60, ("minecraft:golden_apple", 1)), kind="place", tip="Es una estructura de bosques; puede estar lejos."),
    ]},
    {"name": "Llevar lo necesario", "quests": [
        q("c1_bp1", "El problema del espacio", "Tus bolsillos ya no dan abasto. Reúne cuero, cuerda y un cofre.", [item("minecraft:leather", 4), item("minecraft:string", 4), item("minecraft:chest")], r(20)),
        q("c1_bp2", "Un inventario más grande", "Cuero, cuerda y un cofre: una mochila que cargas a la espalda.", [item("sophisticatedbackpacks:backpack")], r(40), kind="craft"),
        q("c1_bp3", "Preparar una expedición", "Llena la mochila: comida, herramientas y recursos para días lejos de casa.", [check("Llené mi mochila")], r(20, ("minecraft:cooked_beef", 8)), icon="minecraft:bundle", kind="lore"),
    ]},
    {"name": "El océano oculta secretos", "quests": [
        q("c1_ocean", "Agua hasta donde alcanza la vista", "El mundo no termina en la costa. Encuentra el océano.", [biome("minecraft:ocean")], r(30), kind="place"),
        q("c1_boat", "Antes de zarpar", "Unos tablones sobre el agua te llevan donde los pies no llegan.", [check("Navegué en un bote")], r(20), icon="minecraft:oak_boat", kind="lore"),
        q("c1_wreck", "Lo que dejó el mar", "Cascos hundidos guardan cofres. También hay barcos piratas en el horizonte.", [struct("minecraft:shipwreck")], r(40, ("minecraft:emerald", 3)), kind="place",
          tip="Con Dungeons Arise Seven Seas aparecen naves enteras sobre el agua."),
        q("c1_dive", "El peligro de las profundidades", "Bajo el agua el aire se acaba. Come bien y no te alejes de la luz.", [item("minecraft:cooked_cod", 8)], r(25)),
    ]},
    {"name": "El metal oscuro", "quests": [
        q("c1_dm1", "Una veta imposible", "Fragmentos negros que no brillan: metal oscuro. Los sueltan los caídos de la torre y ciertos depósitos.", [item("born_in_chaos_v1:pieceofdarkmetal")], r(30),
          tip="Sale de mobs de Born in Chaos y de los depósitos de dark metal (con Fortuna dan más)."),
        q("c1_dm2", "Extracción desconocida", "Un fragmento es nada. Nueve ya son algo.", [item("born_in_chaos_v1:pieceofdarkmetal", 9)], r(40)),
        q("c1_dm3", "Un montón de oscuridad", "Nueve fragmentos, en cuadrado, forman un montón de metal oscuro.", [item("born_in_chaos_v1:pileof_dark_metal")], r(40), kind="craft",
          tip="Fórmula 3x3 en la mesa de crafteo."),
        q("c1_dm4", "Un horno más fiero", "Este metal no se funde en un horno común. Necesitas un alto horno.", [item("minecraft:blast_furnace")], r(30), kind="craft",
          tip="Receta: hierro, horno y piedra lisa."),
        q("c1_dm5", "El metal que no debería existir", "En el alto horno, el montón se vuelve lingote. Lo que sale pesa más de lo que debería.", [item("born_in_chaos_v1:dark_metal_ingot")], r(60),
          tip="Cuatrocientos ticks de fundición por montón."),
        q("c1_dm6", "Un secreto para otro día", "El metal oscuro guarda más de lo que ves. Revisa sus usos: hará falta una mesa de otro nivel.", [check("Revisé sus usos en JEI")], r(40, ("minecraft:ender_pearl", 2)),
          icon="born_in_chaos_v1:dark_metal_ingot", kind="final", tip="Todo lo hecho de dark metal se craftea solo en una mesa tier 2 (capítulo II)."),
    ]},
]
