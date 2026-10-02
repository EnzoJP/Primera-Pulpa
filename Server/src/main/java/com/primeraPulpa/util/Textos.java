package com.primeraPulpa.util;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Utilidades de texto para el bot de Telegram: normalización (minúsculas sin
 * tildes) y matching difuso para interpretar nombres libres de mix o clientes.
 *
 * Ejemplo: "trop con mani" ≈ "Tropical con Maní" (0.83 de similitud).
 */
public final class Textos {

    private Textos() {
    }

    /**
     * Normaliza un texto: minúsculas, sin acentos ni signos, espacios colapsados.
     * "Tropical con Maní" → "tropical con mani"
     */
    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String t = Normalizer.normalize(texto.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        t = t.replaceAll("\\p{M}", "");
        t = t.replaceAll("[^\\p{L}\\p{N}]+", " ");
        return t.trim().replaceAll("\\s+", " ");
    }

    /**
     * Palabras de un texto ya normalizado.
     */
    public static List<String> tokens(String texto) {
        List<String> lista = new ArrayList<>();
        for (String parte : normalizar(texto).split(" ")) {
            if (!parte.isEmpty()) {
                lista.add(parte);
            }
        }
        return lista;
    }

    /**
     * Similitud 0..1 entre dos palabras: edición de Levenshtein + bonus si una
     * es prefijo de la otra ("trop" → "tropical" = 0.5).
     */
    public static double similitudTokens(String a, String b) {
        if (a != null && a.equals(b)) {
            return 1.0;
        }
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) {
            return 0.0;
        }
        int max = Math.max(a.length(), b.length());
        double ratio = 1.0 - (double) levenshtein(a, b) / max;
        if (a.startsWith(b) || b.startsWith(a)) {
            double prefijo = (double) Math.min(a.length(), b.length()) / max;
            ratio = Math.max(ratio, prefijo);
        }
        return ratio;
    }

    /**
     * Similitud difusa 0..1 entre un nombre del catálogo y un texto libre.
     * Recorre las palabras del texto contra las del catálogo respetando el
     * orden y penaliza levemente las palabras del catálogo que quedaron sin
     * usar (ej. "Mix ... Clásico").
     */
    public static double similitudFuzzy(String catalogo, String consulta) {
        List<String> c = tokens(catalogo);
        List<String> q = tokens(consulta);
        if (c.isEmpty() || q.isEmpty()) {
            return 0.0;
        }

        int j = 0;
        int consumidos = 0;
        double suma = 0.0;
        for (String qt : q) {
            double mejor = 0.0;
            int mejorIdx = -1;
            for (int i = j; i < c.size(); i++) {
                double s = similitudTokens(qt, c.get(i));
                if (s > mejor) {
                    mejor = s;
                    mejorIdx = i;
                }
            }
            if (mejor >= 0.3) {
                suma += mejor;
                consumidos++;
                j = mejorIdx + 1;
            }
        }

        if (consumidos == 0) {
            return 0.0;
        }
        double promedio = suma / q.size();
        double excedente = c.size() - consumidos;
        double factor = consumidos / (consumidos + excedente * 0.35);
        return promedio * factor;
    }

    private static int levenshtein(String a, String b) {
        int[] prev = new int[b.length() + 1];
        int[] curr = new int[b.length() + 1];
        for (int j = 0; j <= b.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= a.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= b.length(); j++) {
                int cost = a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] tmp = prev;
            prev = curr;
            curr = tmp;
        }
        return prev[b.length()];
    }
}