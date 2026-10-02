// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.intro;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Preguntas de la introduccion (estilo Fear and Hunger): las respuestas suman puntos ocultos a razas y clases y al final el servidor
 * asigna la mas afin. Las razas especiales (Ender Warrior, Malnacido, Gigante Rocoso y Automata) son caminos secretos: hace falta
 * coincidir en varias preguntas para que alguna supere el umbral. Las preguntas 0-4 deciden la raza y las 5-8 la clase.
 */
public final class IntroContent {
    public static final int RACE_QUESTIONS = 5;
    private static final int SPECIAL_THRESHOLD = 5;

    public record Option(String text, Map<Race, Integer> races, Map<PlayerClass, Integer> classes) {
    }

    public record Question(String prompt, List<Option> options) {
    }

    public record Result(Race race, PlayerClass playerClass) {
    }

    public static final List<Question> QUESTIONS = new ArrayList<>();

    private IntroContent() {
    }

    private static Option race(String text, String weights) {
        Map<Race, Integer> map = new EnumMap<>(Race.class);
        for (String part : weights.split(",")) {
            String[] kv = part.trim().split(":");
            map.put(Race.valueOf(kv[0]), Integer.parseInt(kv[1]));
        }
        return new Option(text, map, Map.of());
    }

    private static Option cls(String text, String weights) {
        Map<PlayerClass, Integer> map = new EnumMap<>(PlayerClass.class);
        for (String part : weights.split(",")) {
            String[] kv = part.trim().split(":");
            map.put(PlayerClass.valueOf(kv[0]), Integer.parseInt(kv[1]));
        }
        return new Option(text, Map.of(), map);
    }

    static {
        QUESTIONS.add(new Question("Alzás la vista. El cielo está vacío. ¿Qué sentís?", List.of(
                race("Rabia. Nos dejaron solos entre sus restos.", "HEREJE:3,DEMONIO:1"),
                race("Fe. Algún día van a volver.", "DEVOTO:3,ANGEL:1"),
                race("Nada. El cielo es solo cielo.", "HUMANO:3,AUTOMATA:2"),
                race("Atracción. Algo allá arriba me llama.", "ANGEL:3,ENDER_WARRIOR:1"),
                race("Un vacío. Lo que hay detrás de las estrellas me mira.", "ENDER_WARRIOR:2,MALNACIDO:1"))));
        QUESTIONS.add(new Question("La luna sigue allí. ¿Cómo la mirás?", List.of(
                race("Le rezo a Luna: su noche me cuida.", "SIERVO_DE_LA_LUNA:3,DEVOTO:1"),
                race("Le temo. La oscuridad esconde cosas.", "HUMANO:2,MALNACIDO:1"),
                race("Es una puerta. Del otro lado hay un vacío esperando.", "ENDER_WARRIOR:2,SIERVO_DE_LA_LUNA:1"),
                race("Es una roca fría que gira. Nada más.", "AUTOMATA:2,HUMANO:1"),
                race("Me ignora, como todo lo demás.", "HEREJE:2,MALNACIDO:2"))));
        QUESTIONS.add(new Question("Los recuerdos de los dioses caídos se cristalizaron en la piedra, y sus almas corruptas habitan en los monstruos. ¿Qué harías con ellos?", List.of(
                race("Los devoraría. El poder es poder.", "DEMONIO:3,HEREJE:1"),
                race("Dejaría que descansen en paz.", "DEVOTO:3,ANGEL:1"),
                race("Me haría de piedra: lento, firme, eterno.", "STONE_GIANT:3"),
                race("Los desarmaría con método hasta entender cómo funcionan.", "AUTOMATA:3,HUMANO:1"),
                race("A mí también me trataron como a un resto. Que se pudran todos.", "MALNACIDO:3,HEREJE:1"))));
        QUESTIONS.add(new Question("El Padre de la Carne construyó el Aether y nos dejó la carne. ¿Qué pensás de Él?", List.of(
                race("Quiero subir y ver su obra.", "ANGEL:3,DEVOTO:1"),
                race("Le debo todo. Le serviré.", "DEVOTO:3"),
                race("Un dios que abandona a sus hijos merece caer.", "HEREJE:3,DEMONIO:1"),
                race("La carne es lo único real. Lo demás son excusas.", "MALNACIDO:2,STONE_GIANT:1,HUMANO:1"),
                race("No es mi problema.", "HUMANO:3,AUTOMATA:1"))));
        QUESTIONS.add(new Question("Pater, Meidris, Filis y Luna siguen pidiendo ofrendas. ¿Contra quién alzarías la mano?", List.of(
                race("Contra Pater, el Padre.", "HEREJE:2,DEMONIO:2"),
                race("Contra Meidris, la del Verbo.", "AUTOMATA:2,STONE_GIANT:1,DEMONIO:1"),
                race("Contra Filis, la del sacrificio.", "ANGEL:2,DEVOTO:1,STONE_GIANT:1"),
                race("Contra Luna, la de la noche.", "ANGEL:2,DEMONIO:1,ENDER_WARRIOR:1"),
                race("Contra ninguno. Yo elijo con quién caminar.", "HUMANO:2,DEVOTO:2"))));

        QUESTIONS.add(new Question("De noche, en un camino sin luz, oís pasos que no son los tuyos. ¿Qué hacés?", List.of(
                cls("Salgo a su encuentro. Que sepan que estoy acá.", "BERSERKER:3"),
                cls("Me quedo firme en mi lugar y dejo que vengan a mí.", "ESCUDERO:3,GUERRERO_ANIMA:1"),
                cls("Busco un punto alto desde donde ver sin ser visto.", "ARQUERO:3"),
                cls("Cierro los ojos y escucho. Alguien camina conmigo, aunque no lo vea.", "GUERRERO_ANIMA:3,RITUALISTA_ARCANO:1"),
                cls("Marco el suelo con un trazo antes de que lleguen.", "RITUALISTA_ARCANO:3"))));
        QUESTIONS.add(new Question("Un anciano te ofrece un regalo para el viaje. ¿Cuál aceptás?", List.of(
                cls("Un hacha pesada, para abrir caminos.", "BERSERKER:3"),
                cls("Un escudo viejo, abollado de tanto proteger a otros.", "ESCUDERO:3"),
                cls("Un arco ligero y una docena de flechas.", "ARQUERO:3"),
                cls("Un libro sin título, con páginas que nunca terminan.", "RITUALISTA_ARCANO:3"),
                cls("Un nombre: el de alguien que ya no está, para repetirlo en cada paso.", "GUERRERO_ANIMA:3,RITUALISTA_ARCANO:1"))));
        QUESTIONS.add(new Question("Perdés a un compañero en combate. ¿Cómo lo vivís?", List.of(
                cls("Con furia. Alguien va a pagar.", "BERSERKER:3"),
                cls("Me prometo ser el muro del próximo: nadie más cae delante de mí.", "ESCUDERO:3"),
                cls("Lo repaso una y otra vez: debí haberlo visto venir desde más lejos.", "ARQUERO:3"),
                cls("Busco la forma de traerlo de vuelta, cueste lo que cueste.", "RITUALISTA_ARCANO:3,GUERRERO_ANIMA:1"),
                cls("Lo cargo conmigo. Cada día me pesa y cada día me hace más fuerte.", "GUERRERO_ANIMA:3"))));
        QUESTIONS.add(new Question("¿Qué es lo que más te asusta de verdad?", List.of(
                cls("Quedarme quieto mientras el mundo arde.", "BERSERKER:3"),
                cls("Que alguien caiga por mi culpa.", "ESCUDERO:3"),
                cls("Que me alcancen antes de poder responder.", "ARQUERO:3"),
                cls("No entender lo que hay del otro lado.", "RITUALISTA_ARCANO:3"),
                cls("Quedarme solo. Solo de verdad.", "GUERRERO_ANIMA:3"))));
    }

    /** Orden de preferencia ante empates (las razas secretas solo cuentan si superan el umbral). */
    private static final Race[] TIE_ORDER = {Race.HEREJE, Race.DEVOTO, Race.DEMONIO, Race.ANGEL, Race.SIERVO_DE_LA_LUNA,
            Race.ENDER_WARRIOR, Race.MALNACIDO, Race.STONE_GIANT, Race.AUTOMATA, Race.HUMANO};

    private static boolean isSecret(Race race) {
        return race == Race.ENDER_WARRIOR || race == Race.MALNACIDO || race == Race.STONE_GIANT || race == Race.AUTOMATA;
    }

    /** Raza y clase que dan estas respuestas, o null si el arreglo no es valido. */
    public static Result compute(int[] answers) {
        if (answers == null || answers.length != QUESTIONS.size()) {
            return null;
        }
        Map<Race, Integer> raceScore = new EnumMap<>(Race.class);
        Map<PlayerClass, Integer> classScore = new EnumMap<>(PlayerClass.class);
        for (int q = 0; q < answers.length; q++) {
            List<Option> options = QUESTIONS.get(q).options();
            if (answers[q] < 0 || answers[q] >= options.size()) {
                return null;
            }
            Option option = options.get(answers[q]);
            option.races().forEach((r, w) -> raceScore.merge(r, w, Integer::sum));
            option.classes().forEach((c, w) -> classScore.merge(c, w, Integer::sum));
        }
        Race race = Race.HUMANO;
        int best = 0;
        for (Race candidate : TIE_ORDER) {
            int score = raceScore.getOrDefault(candidate, 0);
            if (isSecret(candidate) && score < SPECIAL_THRESHOLD) {
                continue;
            }
            if (score > best) {
                best = score;
                race = candidate;
            }
        }
        PlayerClass chosen = PlayerClass.BERSERKER;
        int bestClass = -1;
        for (PlayerClass candidate : PlayerClass.values()) {
            if (candidate == PlayerClass.NINGUNA || candidate == PlayerClass.RITUALISTA_ARCANO && race == Race.AUTOMATA) {
                continue; // el Automata tiene prohibido usar magia
            }
            int score = classScore.getOrDefault(candidate, 0);
            if (score > bestClass) {
                bestClass = score;
                chosen = candidate;
            }
        }
        return new Result(race, chosen);
    }
}
