// Copyright (c) 2026 Agustin (TheTiranix). All rights reserved. See LICENSE.txt.
package com.tcorigenes.tcorigenes.progression;

import com.tcorigenes.tcorigenes.playerclass.PlayerClass;
import java.util.ArrayList;
import java.util.List;

/**
 * Forma del arbol de cada clase: dos caminos que salen de la raiz (abajo) y llegan a la piedra clave (arriba) dibujando el contorno de un objeto:
 * Berserker una espada, Escudero un escudo, Arquero un arco con su cuerda, Ritualista un libro abierto y Guerrero Anima una llama de alma.
 * Las coordenadas estan en "unidades de nodo" con y hacia arriba; se reparten los nodos a distancia pareja a lo largo de cada contorno.
 */
final class SkillShapes {
    /** Distancia minima entre nodos vecinos (en unidades de nodo): 1 es una celda completa. */
    private static final double SPACING = 1.25;

    private SkillShapes() {
    }

    record Paths(List<double[]> a, List<double[]> b, double[] apex) {
    }

    private static double[][] mirror(double[][] right) {
        double[][] left = new double[right.length][];
        for (int i = 0; i < right.length; i++) {
            left[i] = new double[] {-right[i][0], right[i][1]};
        }
        return left;
    }

    private static double[][] points(double... xy) {
        double[][] result = new double[xy.length / 2][];
        for (int i = 0; i < result.length; i++) {
            result[i] = new double[] {xy[i * 2], xy[i * 2 + 1]};
        }
        return result;
    }

    private static double length(double[][] poly) {
        double total = 0.0;
        for (int i = 1; i < poly.length; i++) {
            total += Math.hypot(poly[i][0] - poly[i - 1][0], poly[i][1] - poly[i - 1][1]);
        }
        return total;
    }

    /** n puntos repartidos a distancia pareja a lo largo del contorno, sin contar el primero (la raiz) ni el ultimo (la clave). */
    private static List<double[]> sample(double[][] poly, int n, double scale) {
        double total = length(poly);
        List<double[]> result = new ArrayList<>();
        for (int k = 1; k <= n; k++) {
            double target = total * k / (n + 1);
            double walked = 0.0;
            for (int i = 1; i < poly.length; i++) {
                double seg = Math.hypot(poly[i][0] - poly[i - 1][0], poly[i][1] - poly[i - 1][1]);
                if (walked + seg >= target) {
                    double t = seg == 0.0 ? 0.0 : (target - walked) / seg;
                    result.add(new double[] {(poly[i - 1][0] + (poly[i][0] - poly[i - 1][0]) * t) * scale,
                            (poly[i - 1][1] + (poly[i][1] - poly[i - 1][1]) * t) * scale});
                    break;
                }
                walked += seg;
            }
        }
        return result;
    }

    static Paths of(PlayerClass playerClass, int n) {
        double[][] a;
        double[][] b;
        switch (playerClass) {
            case BERSERKER -> { // espada: pomo, empuñadura, guarda y hoja que termina en punta
                b = points(0, 0, 2.5, 0, 2.5, 2, 1.3, 3, 1.3, 10, 7, 10, 7, 12, 3, 12, 3, 46, 0, 54);
                a = mirror(b);
            }
            case ESCUDERO -> { // escudo: punta abajo, hombros anchos y borde superior con muesca
                b = points(0, 0, 7, 5, 14, 12, 19, 22, 19, 38, 19, 44, 10, 48, 0, 44);
                a = mirror(b);
            }
            case ARQUERO -> { // arco: un brazo curvo y la cuerda recta
                a = points(0, 0, 4, 6, 7, 14, 8, 24, 7, 34, 4, 42, 0, 48);
                b = points(0, 0, 0, 48);
            }
            case RITUALISTA_ARCANO -> { // libro abierto: dos paginas con el lomo en el medio
                b = points(0, 0, 9, -2, 19, 0, 23, 4, 23, 30, 19, 34, 10, 33, 0, 28);
                a = mirror(b);
            }
            default -> { // llama de alma (Guerrero Anima)
                b = points(0, 0, 4, 5, 9, 13, 11, 24, 9, 34, 5, 41, 3, 47, 0, 54);
                a = mirror(b);
            }
        }
        double scale = (n + 1) * SPACING / Math.min(length(a), length(b));
        double[][] end = a;
        return new Paths(sample(a, n, scale), sample(b, n, scale),
                new double[] {end[end.length - 1][0] * scale, end[end.length - 1][1] * scale});
    }
}
