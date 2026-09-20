// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.client.gui;

import com.tcorigenes.tcorigenes.core.Race;
import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
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
        return line("+ ", ChatFormatting.GREEN, text, ChatFormatting.WHITE);
    }

    private static Component minus(String text) {
        return line("- ", ChatFormatting.RED, text, ChatFormatting.WHITE);
    }

    private static Component note(String text) {
        return line("* ", ChatFormatting.GOLD, text, ChatFormatting.YELLOW);
    }

    private static Component line(String mark, ChatFormatting markColor, String text, ChatFormatting textColor) {
        MutableComponent c = Component.literal(mark).withStyle(markColor);
        return c.append(Component.literal(text).withStyle(textColor));
    }

    // ------------------------------------------------------------------ razas
    public static String tagline(Race race) {
        return switch (race) {
            case HUMANO -> "La raza neutra: sin favores, sin maldiciones.";
            case HEREJE -> "Los dioses lo ignoran. Él aprendió a ver dónde duele.";
            case DEVOTO -> "Criado bajo la mirada de los dioses creadores.";
            case DEMONIO -> "Sangre de fuego y cuernos del inframundo.";
            case ANGEL -> "Alas de luz caídas del paraíso.";
            case SIERVO_DE_LA_LUNA -> "Sirve a Luna: la noche es su reino.";
            case ENDER_WARRIOR -> "Un guerrero del vacío. El agua es su enemiga.";
            case MALNACIDO -> "Nacido maldito, deforme y odiado por todos.";
            case STONE_GIANT -> "Un coloso de piedra: lento, pero casi imparable.";
        };
    }

    public static List<Component> lines(Race race) {
        List<Component> l = new ArrayList<>();
        switch (race) {
            case HUMANO -> {
                l.add(note("Sin bonificaciones ni penalizaciones."));
                l.add(note("No tiene el favor de ningún dios al empezar, pero tampoco tiene límites: puedes ganar el favor de cualquiera."));
            }
            case HEREJE -> {
                l.add(plus("+5% de esquive."));
                l.add(plus("+10% de velocidad de movimiento, de ataque y de carga de arcos."));
                l.add(plus("Expertiz anatómica: ve una marca roja en los puntos débiles de los enemigos (4 s). Golpearla hace +45% de daño y 5% del daño ignora la armadura."));
                l.add(plus("Encantar cuesta 50% menos experiencia."));
                l.add(minus("Los dioses lo ignoran: su favor nunca supera 0."));
            }
            case DEVOTO -> {
                l.add(plus("+10% de curación recibida (de toda fuente)."));
                l.add(plus("+1 de Suerte."));
                l.add(plus("Refleja 10% del daño recibido a quien lo ataca."));
                l.add(plus("Empieza con el favor de los dioses creadores."));
            }
            case DEMONIO -> {
                l.add(plus("Inmune al fuego, la lava y el magma."));
                l.add(plus("+5% de daño y +15% de resistencia al fuego elemental."));
                l.add(plus("Cada golpe suma 10% de daño de fuego."));
                l.add(plus("7% de la armadura enemiga se ignora."));
                l.add(plus("El calor extremo no lo afecta."));
                l.add(minus("-40% de resistencia a la luz."));
            }
            case ANGEL -> {
                l.add(plus("No recibe daño por caída y planea al caer."));
                l.add(plus("+10% de resistencia a la luz."));
                l.add(plus("Cada golpe suma 10% de daño de luz."));
                l.add(note("Alas visibles en el personaje."));
            }
            case SIERVO_DE_LA_LUNA -> {
                l.add(plus("+10% de resistencia lunar."));
                l.add(plus("Bajo la luna (de noche y a cielo abierto): +10% de daño, +5% de velocidad de ataque y de carga, visión nocturna, regeneración de 3% de vida por segundo y 10% de daño lunar extra."));
                l.add(plus("Puede guardar energía lunar en un colgante."));
                l.add(minus("-20% de resistencia a la luz."));
                l.add(note("Antenas visibles en el personaje."));
            }
            case ENDER_WARRIOR -> {
                l.add(plus("+10% de vida, +15% de resistencia al retroceso, +10% de resistencia ender."));
                l.add(plus("+20% de esquive de flechas y +10% de alcance de ataque."));
                l.add(plus("Es 0.5 bloques más alto. 5% de su daño se convierte en ender."));
                l.add(plus("Habilidad racial (J): teletransporte de 20 bloques hacia donde miras. Recarga: 4 min."));
                l.add(plus("Los Endermans no se enfurecen al mirarlos."));
                l.add(minus("El agua le hace daño: nadar, la lluvia (20%, se reduce con armadura completa) y beber (4 de daño mágico) lo dañan. La sed siempre está llena."));
                l.add(minus("-10% de velocidad de ataque y de carga. -20% de resistencia al agua. Pasa más hambre."));
                l.add(note("Para nadar sin daño necesita: escafandra, pechera, pantalón, botas, guantes en ambas manos (o los del Aether) y un brazalete. La poción de Enzima Acuática también lo protege."));
            }
            case MALNACIDO -> {
                l.add(plus("+10% de vida."));
                l.add(plus("El veneno, el daño mágico y el Wither no lo dañan: lo curan."));
                l.add(minus("-20% de velocidad de ataque y de carga, -15% de velocidad de movimiento."));
                l.add(minus("-35% de probabilidad de acertar los golpes."));
                l.add(minus("Las facciones lo odian y su favor nunca supera 0."));
                l.add(note("El Anillo de Purificación rompe la maldición: sin penalizaciones, +15% de daño, críticos mucho más fiables y Regeneración II permanente."));
            }
            case STONE_GIANT -> {
                l.add(plus("+20% de vida."));
                l.add(plus("Recibe 5% menos de todo el daño."));
                l.add(plus("25% más alto y ancho. 10% de su daño se convierte en tierra."));
                l.add(minus("-10% de velocidad de movimiento, de ataque y de carga."));
            }
        }
        return l;
    }

    // ----------------------------------------------------------------- clases
    public static String tagline(PlayerClass playerClass) {
        return switch (playerClass) {
            case RITUALISTA_ARCANO -> "Maestro de la magia y los rituales.";
            case BERSERKER -> "Furia y sangre en el cuerpo a cuerpo.";
            case GUERRERO_ANIMA -> "Un solo arma, un solo espíritu.";
            case ESCUDERO -> "Muro inquebrantable del grupo.";
            case ARQUERO -> "Muerte a distancia, ojo de halcón.";
            default -> "";
        };
    }

    public static List<Component> lines(PlayerClass playerClass) {
        List<Component> l = new ArrayList<>();
        switch (playerClass) {
            case RITUALISTA_ARCANO -> {
                l.add(plus("Es la única clase con acceso pleno a la magia."));
                l.add(minus("-10% de daño físico y -20% de vida, hasta consagrar el Matrimonio de Carne."));
                l.add(minus("Límite de destreza de armas: 50 entre ambas manos (sin armas de dos manos ni escudos grandes). Tras el matrimonio puede superarlo con -10% de daño y -15% de velocidad de ataque."));
            }
            case BERSERKER -> {
                l.add(plus("+25% de daño cuerpo a cuerpo."));
                l.add(plus("+15% de daño crítico y 10% menos de probabilidad de no hacer crítico."));
                l.add(note("Habilidad (G): Furia. Durante 20 s pierdes 3% de tu vida máxima cada segundo (5 veces) a cambio de +50% de daño y +20% de crítico. Recarga: 3 min."));
            }
            case GUERRERO_ANIMA -> {
                l.add(plus("+10% de velocidad de ataque, +10% de daño y +10% de vida."));
                l.add(plus("10% menos de probabilidad de no hacer crítico."));
                l.add(plus("Su espada ánima evoluciona con él."));
                l.add(minus("Solo puede usar su espada ánima: ningún otro arma, arco ni ballesta."));
                l.add(note("Habilidad (G): Espíritu Ánima. Golpe de energía a los enemigos cercanos y +Velocidad II durante 15 s. Recarga: 4 min."));
            }
            case ESCUDERO -> {
                l.add(plus("+20% de vida."));
                l.add(plus("Recibe 20% menos de todo el daño."));
                l.add(note("Habilidad (G): Guardia Total. Eres invulnerable durante 10 s. Recarga: 3 min."));
            }
            case ARQUERO -> {
                l.add(plus("+100% de daño con arco y ballesta."));
                l.add(plus("+50% de probabilidad y de daño crítico, solo con proyectiles."));
                l.add(plus("+10% de velocidad de movimiento."));
                l.add(minus("Pasar 50 de destreza en armas es posible, pero con -10% de daño y -15% de velocidad de ataque y de carga."));
                l.add(note("Habilidad (G): Ojo de Halcón. Marca los puntos débiles de todos los enemigos a 30 bloques durante 20 s: los proyectiles que los aciertan hacen +40% de daño y 15% ignora la armadura. Recarga: 45 s."));
            }
            default -> {
            }
        }
        return l;
    }
}
