# La Caída de los Dioses — Documento de Diseño de Progresión

Basado en: el lore que escribiste, los 5 capítulos de FTB Quests ya existentes
(`modpack-config/ftbquests/quests/chapters/`), el mod `testamentodelacarne` reconstruido,
y los 190 mods instalados en la instancia.

Este documento es el mapa de "qué ya existe" vs "qué falta" para poder decidir qué escribir
primero. No es código todavía.

---

## 0. Resumen del lore -> mecánicas

| Lore | Mecánica ya existente | Dónde |
|---|---|---|
| La Caída (guerra celestial, dioses corrompidos) | Portal al Aether requiere `lagrima_consagrada`, no agua | `ModEvents.onRightClickBlock` |
| Fragmentos de recuerdos cristalizados en la piedra | Drop 8% al picar piedra | `ModEvents.onLootTablesLoad` |
| Almas corrompidas de los dioses caídos habitando monstruos | Drop 75% en zombie/skeleton/spider/creeper | `ModEvents.onLootTablesLoad` |
| "El único poder real es la sangre, el acero, las almas" | Sistema de Pactos (Sangre=lifesteal / Acero=crit) | `ReliquiaDelApostataItem` + `ChoosePactPacket` |
| Dioses menores | 4 nombres ya definidos: **PATER, MEIDRIS, FILIS, LUNA** | Capítulo 5 (solo esqueleto) |

---

## 1. Inconsistencia a resolver: ¿2 Pactos o 3 Caminos?

El código Java que reconstruimos (`ReliquiaDelApostataItem`) ofrece **2** opciones: Pacto de
Sangre (vida -> lifesteal) y Dogma de Acero (niveles -> crit chance).

Pero el **Capítulo 2 ("La Bifurcación del Saber")**, ya escrito en FTB Quests, presenta
**3 caminos** que nacen todos de la misma quest "El Velo se Rasga" (craftear
`lente_de_la_verdad_oculta`):

1. **Sangre** -> `occultism:butcher_knife` -> ritual con tizas (`chalk_gold`/`chalk_white`) -> ("El Pacto de Sangre")
2. **Acero/Lógica** -> `create:water_wheel` -> `create:mechanical_press` -> `create:millstone` (rama 100% Create)
3. **Verbo** (camino intermedio, ninguno de los otros dos) -> `ars_nouveau:worn_notebook` -> `scribes_table` -> `novice_spell_book` + `glyph_projectile` ("Tu Primer Hechizo")

Necesito que definas cuál de estas dos lecturas es la correcta antes de tocar código:

- **(A)** Son sistemas distintos y compatibles: el "Pacto" (Java, un buff permanente elegido una
  vez con la Reliquia) es independiente de los "3 Caminos" (FTB Quests, qué árbol tecnológico/mágico
  explorás). Un jugador puede tener Pacto de Sangre y aun así especializarse en Create.
- **(B)** Hay que unificar todo en 3 Pactos: agregar un tercer botón "Pacto del Verbo" a
  `PactSelectionScreen` con su propio bonus (probablemente maná/regeneración de fuente de Ars Nouveau),
  y las 3 quests-raíz del Capítulo 2 pasan a requerir haber elegido el pacto correspondiente.

Mi recomendación es **(A)**: es menos trabajo, no rompe el código ya recuperado, y de hecho
tiene más sentido narrativo (el Pacto es "en qué creés", el Camino es "qué estudiás").

---

## 2. Estado por capítulo

### Capítulo 1 — "El Despertar sin Fe" (COMPLETO, 17 quests)
Ya cubre: refugio, primeras herramientas, `corazon_de_piedra_inerte`, `fragmento_de_memoria`,
`alma_corrupta`, armadura de cuero/hierro, mesa de encantamientos, primer yunque.
**No requiere trabajo nuevo**, salvo un pendiente:

> ⚠️ Las quests "La Forja Olvidada" (craftear `minecraft:anvil`) y "El Primer Encantamiento"
> (`minecraft:enchanting_table`) asumen que estos crafteos van a estar modificados/gateados
> (tu doc original lo decía explícitamente), pero los overrides que encontré en el `.jar`
> estaban vacíos (`{}`) — los saqué del proyecto nuevo para no romper el crafteo vanilla.
> **Falta diseñar la receta real** de yunque/mesa de encantar/mesa de alquimia si querés
> mantener ese gate de progresión.

### Capítulo 2 — "La Bifurcación del Saber" (COMPLETO, 10 quests, 3 ramas)
Ver sección 1. Las 3 ramas ya están bien diseñadas y no necesitan más quests, solo:
- Confirmar sistema de Pactos (A o B).
- Falta un `reward_tables` para "El Pacto de Sangre" (`table_id: 18760584328938450`... ah,
  ese es en cap.3) y para "El Verbo" (`table_id: 8915080205900394279`) — están referenciados
  pero hay que verificar que existan en `reward_tables/choose.snbt` etc.

### Capítulo 3 — "Abriendo los Viejos Caminos" (MUY COMPLETO, 27 quests)
Esta es la fase "mid-game" real del pack:
- **Twilight Forest completo**: portal ritual, Naga, Lich (+ `iceandfire:lich_staff`),
  Minotauro, Hydra, Ur-Ghast, Knight Phantom, Alpha Yeti, Snow Queen, Quest Ram — o sea
  **prácticamente todos los jefes de Twilight Forest 4.3 ya están cableados**.
- **Ice and Fire**: Ice Dragon -> Fire Dragon (rewards con `apotheosis:gem`, cambios de estación
  vía `/season set`, integración con SereneSeasons).
- **Mowzie's Mobs**: Ferrous Wroughtnaut -> `nucleo_de_automata` -> `buscador_de_ecos` (esto
  ata directamente con nuestro item ya recuperado).
- **Blood Magic**: ritual completo desde `sacrificialdagger` hasta el altar.
- **Nether**: obsidiana, fire resistance, wither skeletons, glowstone -> lleva directo al
  Cap. 4 (portal al Aether).

**No requiere quests nuevas.** Sí requiere que el loot/drop real de esos bosses (Naga, Lich,
Hydra, Wroughtnaut, dragones) esté bien engachado — eso ya lo maneja cada mod nativamente,
no hace falta código nuestro salvo el `foliaath_seed_pool` que ya está en `ModEvents`.

### Capítulo 4 — "La Ascensión Sacra" (ESQUELETO, 1 sola quest)
Solo tiene la quest de cruzar al Aether. **Esto es lo más corto y lo que más urge expandir**:
el Aether es el "paraíso construido por el Padre de la Carne" — pero no hay ninguna quest
para lo que pasa *dentro* del Aether (bosses de Aether: Valkyrie Queen, Sun Spirit; recursos
como Ambrosium, Zanite, Gravitite; Aether tools/armor). Esto es terreno vacío total.

### Capítulo 5 — "Dioses Menores" (SOLO ESQUELETO, 15 quests sin contenido)
Los 4 dioses ya tienen nombre pero cero mecánica:
- **PATER** ("Padre") — el lore dice que el Padre de la Carne construyó el Aether. Candidato
  natural: atado a Aether + Create (orden, ingeniería divina).
- **MEIDRIS** — nombre sin raíz latina obvia (posible referencia a "Meid-" ~ doncella en
  gótico, o simplemente inventado). Candidato: Ars Nouveau (magia arcana, "el Verbo").
- **FILIS** ("Hijo/a" en latín) — Candidato: Blood Magic/Occultism (sacrificio, el hijo
  sacrificado).
- **LUNA** — Candidato obvio: Twilight Forest (crepúsculo eterno) o SereneSeasons/noche,
  dado que ya asociaste estaciones (`/season set`) al arco de Twilight Forest/Ice and Fire.

Cada dios necesitaría: una quest de "revelación" (ya existe el checkmark), una elección
de fe (mecánica tipo Origins/Apoli o un segundo `ChoosePactPacket`-like), y un set de
bendiciones/maldiciones. Esto es el capítulo con más trabajo de diseño pendiente.

---

## 2.5 Segundo mod recuperado: `tcorigenes` (sistema de razas propio)

Encontré y reconstruí también `mods/tcorigenes.jar` — un mod aparte (no usa Origins/Apoli,
que están instalados pero sin tocar) con 8 razas propias vía Capability + GUI (`Orbe de
Orígenes`). Ya vive en el mismo proyecto Gradle, empaquetado junto a `testamentodelacarne`
(ver `mods.toml`, ahora con 2 bloques `[[mods]]`).

**Estado real de las 8 razas** (contradice un poco la sensación de "vacío" — 7 de 8 sí tienen
algo, pero todo son modificadores de atributos sueltos, sin identidad más profunda):

| Raza | Mecánica implementada |
|---|---|
| Humano | Ninguna (neutral, es la base) — esto es intencional |
| Hereje | +5% velocidad, +5% velocidad de ataque |
| Devoto | Regen pasiva (1 HP/2.5s) + curación recibida x1.5 |
| Demonio | +10% daño de ataque, inmune a fuego/lava |
| Ángel | Inmune a daño de caída |
| Siervo de la Luna | +10% daño y +10% armadura, pero SOLO de noche a cielo abierto (se quita de día) |
| Ender Warrior | +15% vida máxima, pero +20% daño de ahogo (débil al agua, como los Endermen); al elegirlo recibe una Espada Ánima maldita |
| Malnacido | -15% velocidad, -20% velocidad de ataque, +10% vida; el veneno cura en vez de dañar; inmune a inanición |

**Confirmado, tal cual dijiste:** no hay ningún skill tree por raza — el único skill tree que
existe es el genérico de los Pactos (`pacto_iniciado`/`pacto_de_sangre`/`dogma_de_acero`, en
`testamentodelacarne`). Las razas son 100% atributos planos, sin árbol de progresión propio.

**Otro cabo suelto que encontré:** hay una clave de traducción `creativetab.tcorigenes_tab`
en el lang, pero el código nunca llegó a crear esa pestaña — el Orbe de Orígenes quedó
metido en una pestaña vanilla cualquiera. Evidencia de que se planeó una pestaña propia y
se abandonó a mitad de camino.

**Sobre la Sanidad (`SanityJS` + `sanitydim`):** no encontré ni una línea de código en
ninguno de los 2 mods recuperados que la toque. Confirma lo que decías: nunca se llegó a
integrar. Es terreno 100% libre — no hay que deshacer nada, solo diseñar desde cero.

---

## 2.6 IMPLEMENTADO: nodo de skill tree exclusivo por raza

Siguiendo tu elección (extender el árbol existente en vez de un sistema paralelo nuevo),
ya agregué 7 nodos nuevos a `main_tree.json` (Humano no tiene nodo — sigue siendo la raza
neutra), con el mismo mecanismo que ya funcionaba para los Pactos: advancement
(`minecraft:impossible`, solo se otorga por comando) -> `mcfunction` -> `skilltree unlock`.

| Raza | Nodo | Bonus extra en el árbol (además del automático de Java) |
|---|---|---|
| Hereje | `raza_hereje` | +5% velocidad de movimiento |
| Devoto | `raza_devoto` | +2 corazones de vida máxima |
| Demonio | `raza_demonio` | +0.5 daño de ataque plano |
| Ángel | `raza_angel` | +1 punto de armadura |
| Siervo de la Luna | `raza_siervo_de_la_luna` | +1 punto de armadura (fijo, complementa el bonus nocturno dinámico de Java) |
| Ender Warrior | `raza_ender_warrior` | +2 corazones de vida máxima |
| Malnacido | `raza_malnacido` | +5% velocidad de ataque |

Cableado en `ChooseRacePacket` y `SetRaceCommand` (nueva clase `RaceSkillGrant`) — se
otorga automáticamente al elegir la raza, igual que pasa con los Pactos.

**Limitación conocida:** si un jugador cambia de raza (con `/setrace` o un segundo Orbe),
el nodo de la raza anterior no se revoca — se acumularían bonuses de varias razas. Los
Pactos evitan esto con el flag `pacto_elegido` (uno solo por partida); para razas no
apliqué el mismo candado porque no sabía si querías permitir cambiar de raza libremente.
Decime si querés que lo bloquee igual (una raza por partida) o que el nodo viejo se
revoque al cambiar.

**Posiciones:** los 7 nodos quedaron en una fila en `y:-450` (arriba del clúster de
Pactos, que está en `y:0/217`), separados del árbol base de PassiveSkillTree para no
pisar ningún nodo del mod. Iconos vanilla elegidos por temática (pluma=velocidad,
tótem=vida, blaze=fuego, élitro=ángel, membrana de phantom=luna, ojo de ender=ender,
carne podrida=malnacido) — son placeholders fáciles de cambiar por texturas propias después.

---

## 4. Diseño de Sanidad (SanityJS + sanitydim) — SOLO DISEÑO, sin código todavía

Nunca se tocó, así que no hay nada que romper. Propuesta inicial, a discutir:

### 4.1 Qué baja la cordura
- Estar en oscuridad total de noche (ya hay precedente: `Siervo de la Luna` ya chequea
  `world.isNight() && canSeeSky`, se puede reusar esa misma lógica).
- Ver un mob hostil "de terror" (candidatos: `entomophobia`, `sculkhorde`, mobs de
  `scary_mobs_and_Bosses`) a corta distancia.
- Estar cerca del portal roto al Aether antes de repararlo (temática: "ver la herida
  divina" te rompe la cabeza).
- Un tick fijo dentro de dimensiones "malditas" (¿el propio `sanitydim` una vez adentro,
  o Twilight Forest de noche, o el Nether?).
- Alma Corrupta en el inventario por mucho tiempo sin usarla (efecto secundario del ítem:
  "cargar con las almas te pesa").

### 4.2 Qué sube/recupera la cordura
- Dormir en una cama dentro de un refugio cerrado (ya es mecánica vanilla — solo hay que
  enganchar el evento).
- Estar cerca de una fogata/luz consagrada (podría ser un uso nuevo para `lagrima_consagrada`
  o `corazon_de_piedra_inerte` colocado como bloque-baliza, si se craftea algo así).
- Completar ciertas quests de FTB Quests (dar cordura como reward, igual que dan xp).
- Elegir un Pacto (Sangre/Acero) — tiene sentido narrativo: "creer en algo" te estabiliza.

### 4.3 Qué pasa en cordura 0 (el castigo — usando `sanitydim`)
Propuesta: al llegar a 0, teletransporte forzado (o portal que se abre solo) a `sanitydim`
por un tiempo fijo, con un debuff fuerte (ceguera/lentitud/hambre) hasta que el jugador
sobrevive X tiempo o encuentra una "salida" — mecánicamente similar a como Cataclysm o
Ice and Fire usan arenas de bosses, pero acá es una dimensión-castigo, no una pelea.
Alternativa más suave: alucinaciones (mobs falsos, sonidos, texto en pantalla) sin
teletransportar, reservando `sanitydim` solo para cordura 0 sostenida por mucho tiempo.

### 4.4 Preguntas que necesito que respondas antes de escribir código
1. ¿La cordura es un stat nuevo (barra propia, lo que ya trae `SanityJS`) o querés
   reusar hambre/saturación con otro nombre?
2. ¿El castigo en 0 es punitivo duro (perder progreso, ir a `sanitydim` a la fuerza) o
   más "cosmético" (sustos, distorsión visual) al principio y solo escala fuerte más
   adelante en la partida?
3. ¿Sanidad aplica desde el Capítulo 1 (Despertar) o recién desde que el jugador "abre
   los ojos" (Cap. 2, `lente_de_la_verdad_oculta`)? Esto último tiene más sentido con el
   lore ("mis nuevos sentidos perciben...").

---

## 5. Mods instalados que TODAVÍA no aparecen en ninguna quest

Relevantes para el lore de terror/cordura que mencionaste al principio (Fear & Hunger) y que
hoy no están en ningún capítulo:

- **`SanityJS` + `sanitydim`** — cordura y una dimensión de pesadilla. Sin usar. Candidato
  natural para "castigo" si el jugador falla un pacto, o para el Capítulo 5 (visiones de los
  dioses menores).
- **`Origins` + `origins-classes`** — razas/clases mencionadas en tu pedido original. Sin
  usar en ninguna quest todavía. Podría reemplazar o complementar el sistema de Pactos.
- **`Tombstone`, `entomophobia`, `spookydoors`, `sculkhorde`** — terror ambiental, sin
  quests asociadas.
- **`Relics` / `relics_in_chaos`** — aparecen como *rewards* (`holy_locket`, `jellyfish_necklace`)
  pero no como objetivos de quest propios.
- **`FTB Chunks/Teams`** — instalados pero no vi lógica de claims en las quests; probablemente
  para multiplayer/co-op, no requiere diseño narrativo.

---

## 7. IMPLEMENTADO: mod de Daño Elemental + razas recableadas con tus valores exactos

Nuevo mod `elementaldamage` (tercer mod en el mismo jar, `com.tudominio.elementaldamage`):

- **5 tipos de daño custom** (datapack, `data/elementaldamage/damage_type/`): `light`,
  `fire_elemental`, `water_elemental`, `lunar`, `ender_elemental`. Son independientes del
  fuego/agua vanilla — un item/hechizo que quiera hacer "daño de fuego elemental" tiene que
  usar `ElementalDamageSource.hurt(...)` explícitamente, no el fuego normal de Minecraft.
- **7 atributos nuevos** (`ModAttributes`): `dodge_chance`, `arrow_dodge_chance`,
  `resist_light/fire/water/lunar/ender` (positivo = % menos daño de ese tipo, negativo =
  % más daño = debilidad). Registrados en `Player` vía `EntityAttributeModificationEvent`.
- **`ElementalDamageEvents`**: en cada `LivingHurtEvent` sobre un jugador, primero tira el
  esquive (distinto atributo si el golpe viene de una flecha), y si no esquivó, aplica la
  resistencia/debilidad según el tipo de daño de la fuente.
- **`RaceAttributeManager` reescrito** con tus números exactos: Hereje (3% esquive, +10%
  velocidad), Demonio (+15% daño, -40% resist. luz, +15% resist. fuego), Ángel (+10% resist.
  luz), Siervo de la Luna (-20% resist. luz, +15% resist. lunar, además del buff nocturno
  dinámico que ya existía), Ender Warrior (+15% vida, +10% esquive de flechas, -40% resist.
  agua, +15% resist. ender), Malnacido (-20% daño, -10% velocidad, +10% vida). Devoto: la
  curación +10% ya está (afecta TODA curación: natural, pociones, efectos, robo de vida).

### 7.1 Lo que quedó explícitamente para después (y por qué)

Cada ítem de esta lista es, como mínimo, un sistema nuevo — no un atributo que se pueda
sumar en una línea:

| Pendiente | De quién | Por qué es su propio sistema |
|---|---|---|
| Puntos débiles (marcador visual, 45%+5% real, aparece cada 10s) | Hereje, Arquero | Necesita raycasting contra el modelo del enemigo + overlay renderizado en el HUD (`RenderGuiEvent` o un `LayeredDraw`) + un timer por-entidad. |
| Reflejo de 3% de daño | Devoto | Se puede hacer, pero hay que decidir si refleja igual contra explosiones/proyectiles/daño sin atacante — lo dejé afuera para no adivinar la regla. |
| Cola/cuernos/alas visuales, "30% menos colorido" | Demonio/Ángel/Siervo Luna | Requiere modelo/textura de jugador custom (capa extra tipo `PlayerRenderer` o Geckolib), no es código de gameplay. |
| Planeo al caer | Ángel | Ya existe algo así en Aether/Curios (alas) — hay que decidir si reusamos ese sistema o escribimos uno propio. |
| Batería de energía lunar | Siervo de la Luna | Es un ítem nuevo con NBT de carga, más una fuente de "energía lunar" que cargarlo (¿de noche pasivamente? ¿con un rayo?). |
| Teletransporte 20 bloques / 4 min | Ender Warrior | Habilidad con keybind + cooldown + validación de que el destino sea seguro (no meterte en una pared). |
| Errar golpes 20% / Anillo de purificación que saca la maldición | Malnacido | Interceptar el propio ataque del jugador (no la defensa) + un ítem-anillo con estado equipado que reescribe todos los bonuses de la raza. |
| **Los 5 Orígenes de clase completos** (Ritualista, Berserker, Guerrero Ánima, Escudero, Arquero) | Todos | Cada uno necesita: keybind propio, sistema de habilidad con cooldown + HUD, y en el caso del Guerrero Ánima un ítem con fases de evolución atado a la raza del jugador (la espada ánima ya existe en `testamentodelacarne`, pero evolucionarla por fases + atarla a un espíritu invocable sin hitbox es var las 3 features anteriores combinadas). El Ritualista además necesita el "Matrimonio de Carne" (un ritual social/multijugador) — esa es una decisión de diseño en sí misma, no solo código. |

Mi sugerencia: la próxima vuelta la dedicamos a **uno** de estos sistemas de base —el más
reutilizable es el de **habilidad con keybind + cooldown + HUD**, porque lo necesitan
Berserker, Escudero, Ender Warrior y el espíritu del Guerrero Ánima por igual. Una vez que
ese esqueleto exista, cada clase/raza lo reusa y se vuelve mucho más rápido de escribir.

---

## 7.2 IMPLEMENTADO: esqueleto genérico de Habilidad (keybind + cooldown + HUD)

Nuevo paquete `com.tcorigenes.tcorigenes.ability`:

- **`PlayerAbility`**: interfaz mínima (`id`, `cooldownTicks`, `icon`, `activate(ServerPlayer)`).
  Cualquier habilidad futura (Berserker, Escudero, teletransporte del Ender Warrior,
  invocación del Guerrero Ánima) solo tiene que implementar esto y registrarse en
  `AbilityRegistry` — el resto ya está resuelto.
- **`AbilityCooldownManager`**: cooldown **server-autoritativo** en memoria (por jugador, por
  id de habilidad). Se pierde al reiniciar el server — si en algún momento hace falta que
  sobreviva un reinicio, se puede mover a la capability.
- **`PlayerAbilityLoadoutProvider`**: capability nueva (mismo patrón que `PlayerRaceProvider`)
  que guarda qué habilidad tiene equipada cada jugador. Hoy apunta siempre a la habilidad de
  prueba; la fase de clases la va a pisar según lo que elija cada jugador, igual que
  `ChooseRacePacket` hace con la raza.
- **Red**: `ActivateAbilityPacket` (cliente -> servidor, sin payload — el servidor decide
  qué habilidad según la capability, nunca confía en el cliente) y
  `AbilityCooldownSyncPacket` (servidor -> cliente, confirma si se activó o resincroniza el
  cooldown real si el cliente se desfasó por lag).
- **Cliente**: keybind `G` (`AbilityKeyBindings`, tecla configurable desde Opciones ->
  Controles -> "La Caída de los Dioses"), `ClientAbilityCooldowns` (cuenta regresiva visual)
  y `AbilityHudOverlay` (ícono 16x16 abajo al centro con el tiempo restante encima).
- **Habilidad de prueba**: "Grito de Guerra" (`AbilityRegistry.registerDefaults()`) — Fuerza I
  por 5s, sonido y partículas, cooldown 30s. Sirve para validar el circuito completo end to
  end; se reemplaza cuando definamos las habilidades reales de cada clase.

**Limitación conocida del HUD:** hoy solo muestra la última habilidad usada (alcanza para 1
habilidad por jugador). Si en algún momento un jugador puede tener más de una habilidad
activa a la vez (ej. una de raza + una de clase), el overlay hay que expandirlo a una fila
de íconos en vez de uno solo.

---

## 8. Propuesta de orden de trabajo (actualizado)

1. Confirmar sección 1 (Pactos vs Caminos).
2. Diseñar las recetas de yunque/mesa de encantar/alquimia del Capítulo 1 (rápido, acota un
   hueco ya detectado).
3. Expandir Capítulo 4 (Aether) — es el más corto y desbloquea contenido ya instalado
   (mod Aether completo) sin depender de decisiones de lore nuevas.
4. Diseñar Capítulo 5 (los 4 dioses) — el trabajo más grande, necesita tu visto bueno de
   qué representa cada dios antes de escribir una sola línea.
5. Decidir si Sanity/Origins entran en esta vuelta o quedan para una fase 2.

Decime con cuál arrancamos.
