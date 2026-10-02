// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.core.Tr;
import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;

/** Textos e iconos de las pantallas de eleccion de raza y clase. Solo cliente: los numeros de aca
 *  documentan lo que ya aplican RaceAttributeManager / ClassAttributeManager / ModEvents; si se
 *  cambia un balance alla, hay que actualizarlo aca tambien. */
public final class OriginInfo {
    private OriginInfo() {
    }

    public static ResourceLocation icon(Race race) {
        String name = switch (race) {
            case HUMANO -> "humano";
            case HEREJE -> "hereje";
            case DEVOTO -> "devoto";
            case DEMONIO -> "demonio";
            case ANGEL -> "angel";
            case SIERVO_DE_LA_LUNA -> "siervo";
            case ENDER_WARRIOR -> "ender";
            case MALNACIDO -> "malnacido";
            case STONE_GIANT -> "stone_giant";
            case AUTOMATA -> "automata";
        };
        return new ResourceLocation("tcorigenes", "textures/gui/origin/" + name + ".png");
    }

    public static ResourceLocation icon(PlayerClass playerClass) {
        String name = switch (playerClass) {
            case RITUALISTA_ARCANO -> "ritualista";
            case BERSERKER -> "berserker";
            case GUERRERO_ANIMA -> "anima";
            case ESCUDERO -> "escudero";
            case ARQUERO -> "arquero";
            default -> "humano";
        };
        return new ResourceLocation("tcorigenes", "textures/gui/origin/" + name + ".png");
    }

    // ------------------------------------------------------------------ lineas
    private static Component plus(String text) {
        return line("+ ", 0x6FA35A, text, 0xDDD0C8);
    }

    private static Component minus(String text) {
        return line("- ", 0xC0392B, text, 0xDDD0C8);
    }

    private static Component note(String text) {
        return line("* ", 0xC9A24A, text, 0xD9C58A);
    }

    private static Component line(String mark, int markColor, String text, int textColor) {
        MutableComponent c = Component.literal(mark).withStyle(Style.EMPTY.withColor(markColor).withBold(true));
        return c.append(Component.literal(text).withStyle(Style.EMPTY.withColor(textColor)));
    }

    // ------------------------------------------------------------------ razas
    public static String tagline(Race race) {
        return switch (race) {
            case HUMANO -> Tr.s("La raza neutra: sin favores, sin maldiciones.");
            case HEREJE -> Tr.s("Los dioses lo ignoran. Él aprendió a ver dónde duele.");
            case DEVOTO -> Tr.s("Criado bajo la mirada de los dioses creadores.");
            case DEMONIO -> Tr.s("Sangre de fuego y cuernos del inframundo.");
            case ANGEL -> Tr.s("Alas de luz caídas del paraíso.");
            case SIERVO_DE_LA_LUNA -> Tr.s("Sirve a Luna: la noche es su reino.");
            case ENDER_WARRIOR -> Tr.s("Un guerrero del vacío. El agua es su enemiga.");
            case MALNACIDO -> Tr.s("Nacido maldito, deforme y odiado por todos.");
            case STONE_GIANT -> Tr.s("Un coloso de piedra: lento, pero casi imparable.");
            case AUTOMATA -> Tr.s("Un gigante de engranajes al servicio de Deiros. La magia no puede tocarlo.");
        };
    }

    public static List<Component> lines(Race race) {
        List<Component> l = new ArrayList<>();
        switch (race) {
            case HUMANO -> {
                l.add(note(Tr.s("Sin bonificaciones ni penalizaciones.")));
                l.add(note(Tr.s("No tiene el favor de ningún dios al empezar, pero tampoco tiene límites: puedes ganar el favor de cualquiera.")));
            }
            case HEREJE -> {
                l.add(plus(Tr.s("+5% de esquive.")));
                l.add(plus(Tr.s("+10% de velocidad de movimiento, de ataque y de carga de arcos.")));
                l.add(plus(Tr.s("Expertiz anatómica: cada 10 s ve una marca roja en un punto débil (3 s para acertarla). Golpearla hace +45% de daño y 5% del daño ignora la armadura.")));
                l.add(plus(Tr.s("Encantar cuesta 50% menos experiencia.")));
                l.add(minus(Tr.s("Los dioses lo ignoran: su favor nunca supera 0.")));
            }
            case DEVOTO -> {
                l.add(plus(Tr.s("+50% de curación recibida (de toda fuente).")));
                l.add(plus(Tr.s("+1 de Suerte.")));
                l.add(plus(Tr.s("Refleja 10% del daño recibido a quien lo ataca.")));
                l.add(plus(Tr.s("Empieza con el favor de los dioses creadores.")));
            }
            case DEMONIO -> {
                l.add(plus(Tr.s("Inmune al fuego, la lava y el magma.")));
                l.add(plus(Tr.s("+5% de daño y +15% de resistencia al fuego elemental.")));
                l.add(plus(Tr.s("Cada golpe suma 10% de daño de fuego.")));
                l.add(plus(Tr.s("7% de la armadura enemiga se ignora.")));
                l.add(plus(Tr.s("El calor extremo no lo afecta.")));
                l.add(minus(Tr.s("-40% de resistencia a la luz.")));
            }
            case ANGEL -> {
                l.add(plus(Tr.s("No recibe daño por caída y planea al caer.")));
                l.add(plus(Tr.s("+10% de resistencia a la luz.")));
                l.add(plus(Tr.s("Cada golpe suma 10% de daño de luz.")));
                l.add(note(Tr.s("Alas visibles en el personaje.")));
            }
            case SIERVO_DE_LA_LUNA -> {
                l.add(plus(Tr.s("+10% de resistencia lunar.")));
                l.add(plus(Tr.s("Bajo la luna (de noche y a cielo abierto): +10% de daño, +5% de velocidad de ataque y de carga, visión nocturna, regeneración de 3% de vida por segundo y 10% de daño lunar extra.")));
                l.add(plus(Tr.s("Puede guardar energía lunar en un colgante.")));
                l.add(minus(Tr.s("-20% de resistencia a la luz.")));
                l.add(note(Tr.s("Antenas visibles en el personaje.")));
            }
            case ENDER_WARRIOR -> {
                l.add(plus(Tr.s("+10% de vida, +15% de resistencia al retroceso, +10% de resistencia ender.")));
                l.add(plus(Tr.s("+20% de esquive de flechas y +10% de alcance de ataque.")));
                l.add(plus(Tr.s("Es 0.5 bloques más alto. 5% de su daño se convierte en ender.")));
                l.add(plus(Tr.s("Habilidad racial (J): teletransporte de 20 bloques hacia donde miras. Recarga: 4 min.")));
                l.add(plus(Tr.s("Los Endermans no se enfurecen al mirarlos.")));
                l.add(minus(Tr.s("El agua le hace daño: nadar, la lluvia (20%, se reduce con armadura completa) y beber (4 de daño mágico) lo dañan. La sed siempre está llena.")));
                l.add(minus(Tr.s("-10% de velocidad de ataque y de carga. -20% de resistencia al agua. Pasa más hambre.")));
                l.add(note(Tr.s("Para nadar sin daño necesita: escafandra, pechera, pantalón, botas, guantes en ambas manos (o los del Aether) y un brazalete. La poción de Enzima Acuática también lo protege.")));
            }
            case MALNACIDO -> {
                l.add(plus(Tr.s("+10% de vida.")));
                l.add(plus(Tr.s("El veneno, el daño mágico y el Wither no lo dañan: lo curan.")));
                l.add(minus(Tr.s("-20% de velocidad de ataque y de carga, -15% de velocidad de movimiento.")));
                l.add(minus(Tr.s("-35% de probabilidad de acertar los golpes.")));
                l.add(minus(Tr.s("Las facciones lo odian y su favor nunca supera 0.")));
                l.add(note(Tr.s("El Anillo de Purificación rompe la maldición: sin penalizaciones, +15% de daño, críticos mucho más fiables y Regeneración II permanente.")));
            }
            case STONE_GIANT -> {
                l.add(plus(Tr.s("+20% de vida.")));
                l.add(plus(Tr.s("Recibe 5% menos de todo el daño.")));
                l.add(plus(Tr.s("25% más alto y ancho. 10% de su daño se convierte en tierra.")));
                l.add(minus(Tr.s("-10% de velocidad de movimiento, de ataque y de carga.")));
            }
            case AUTOMATA -> {
                l.add(plus(Tr.s("+14% de vida, +10% de daño.")));
                l.add(plus(Tr.s("+25% de resistencia a tierra y aire. 10% de su daño se convierte en aire.")));
                l.add(plus(Tr.s("Habilidad racial (J, recarga 60 s): 20 s de +50% de daño de aire y -35% de velocidad.")));
                l.add(plus(Tr.s("Con la habilidad activa, 50% de sus golpes lanzan al enemigo por el aire (daño de caída y 1 s paralizado).")));
                l.add(plus(Tr.s("La armadura de Create y Mekanism le da mucha más protección que a cualquier otra raza.")));
                l.add(plus(Tr.s("Empieza con mucho favor de Deiros: es su sirviente.")));
                l.add(minus(Tr.s("-8% de velocidad. -40% de resistencia a fuego y agua elemental.")));
                l.add(minus(Tr.s("Prohibido usar magia: poder de hechizo en 0.")));
                l.add(note(Tr.s("25% más alto y ancho, con engranajes en los hombros (metal con el árbol al máximo).")));
            }
        }
        return l;
    }

    // ----------------------------------------------------------------- clases
    public static String tagline(PlayerClass playerClass) {
        return switch (playerClass) {
            case RITUALISTA_ARCANO -> Tr.s("Maestro de la magia y los rituales.");
            case BERSERKER -> Tr.s("Furia y sangre en el cuerpo a cuerpo.");
            case GUERRERO_ANIMA -> Tr.s("Un solo arma, un solo espíritu.");
            case ESCUDERO -> Tr.s("Muro inquebrantable del grupo.");
            case ARQUERO -> Tr.s("Muerte a distancia, ojo de halcón.");
            default -> "";
        };
    }

    public static List<Component> lines(PlayerClass playerClass) {
        List<Component> l = new ArrayList<>();
        switch (playerClass) {
            case RITUALISTA_ARCANO -> {
                l.add(plus(Tr.s("Es la única clase con acceso pleno a la magia.")));
                l.add(minus(Tr.s("-10% de daño físico y -20% de vida, hasta consagrar el Matrimonio de Carne.")));
                l.add(minus(Tr.s("Límite de destreza de armas: 75 entre ambas manos (sin armas de dos manos ni dos piezas de 50). Tras el matrimonio puede superarlo, con -10% de daño y -15% de velocidad de ataque a partir de 50.")));
            }
            case BERSERKER -> {
                l.add(plus(Tr.s("+25% de daño cuerpo a cuerpo.")));
                l.add(plus(Tr.s("+15% de daño crítico y 10% menos de probabilidad de no hacer crítico.")));
                l.add(note(Tr.s("Habilidad (G): Furia. Durante 20 s pierdes 3% de tu vida máxima cada segundo (5 veces) a cambio de +50% de daño y +20% de daño crítico. Recarga: 3 min.")));
            }
            case GUERRERO_ANIMA -> {
                l.add(plus(Tr.s("+10% de velocidad de ataque, +10% de daño y +10% de vida.")));
                l.add(plus(Tr.s("10% menos de probabilidad de no hacer crítico.")));
                l.add(plus(Tr.s("Su espada ánima evoluciona con él.")));
                l.add(minus(Tr.s("Solo puede usar su espada ánima: ningún otro arma, arco ni ballesta.")));
                l.add(note(Tr.s("Habilidad (G): Espíritu Ánima. Golpe de energía a los enemigos cercanos y +Velocidad II durante 15 s. Recarga: 4 min.")));
            }
            case ESCUDERO -> {
                l.add(plus(Tr.s("+20% de vida.")));
                l.add(plus(Tr.s("Recibe 20% menos de todo el daño.")));
                l.add(note(Tr.s("Habilidad (G): Guardia Total. Eres invulnerable durante 10 s y todos los enemigos a 10 bloques pasan a atacarte a vos. Recarga: 3 min.")));
            }
            case ARQUERO -> {
                l.add(plus(Tr.s("+100% de daño con arco y ballesta.")));
                l.add(plus(Tr.s("+50% de probabilidad y de daño crítico, solo con proyectiles.")));
                l.add(plus(Tr.s("+10% de velocidad de movimiento.")));
                l.add(minus(Tr.s("Pasar 50 de destreza en armas es posible, pero con -10% de daño y -15% de velocidad de ataque y de carga.")));
                l.add(note(Tr.s("Habilidad (G): Ojo de Halcón. Marca los puntos débiles de todos los enemigos a 30 bloques durante 20 s: los proyectiles que los aciertan hacen +40% de daño y 15% ignora la armadura. Fallar 3 flechazos la cancela antes. Recarga: 3 min.")));
            }
            default -> {
            }
        }
        return l;
    }
}
