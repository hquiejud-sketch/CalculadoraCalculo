package calculadora.modelo;

import java.util.Locale;

/** Utilidades para comparar y mostrar números decimales sin "basura" de redondeo. */
public final class Numeros {

    private Numeros() {
    }

    /** true si el número es entero (tolera errores mínimos como 3.9999999999). */
    public static boolean esEntero(double v) {
        return Double.isFinite(v)
                && Math.abs(v) < 1e15
                && Math.abs(v - Math.rint(v)) < 1e-9 * Math.max(1, Math.abs(v));
    }

    public static boolean casiCero(double v) {
        return Math.abs(v) < 1e-12;
    }

    public static boolean casiIguales(double a, double b, double tolerancia) {
        return Math.abs(a - b) <= tolerancia * Math.max(1, Math.max(Math.abs(a), Math.abs(b)));
    }

    /** Redondea a entero o a una fracción simple cuando el número está a un error de redondeo de serlo. */
    public static double ajustar(double v) {
        if (!Double.isFinite(v)) return v;
        if (esEntero(v)) return Math.rint(v) + 0.0;
        Fraccion f = Fraccion.aproximar(v, 1000, 1e-9);
        return f != null ? f.valor() : v;
    }

    /** Muestra enteros sin decimales, fracciones simples como a/b y el resto con 6 decimales. */
    public static String formatear(double v) {
        if (Double.isNaN(v)) return "indefinido";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (esEntero(v)) return Long.toString(Math.round(v));
        Fraccion f = Fraccion.aproximar(v);
        if (f != null) return f.toString();
        return decimal(v);
    }

    /** Siempre en forma decimal (máximo 6 decimales). */
    public static String decimal(double v) {
        if (Double.isNaN(v)) return "indefinido";
        if (Double.isInfinite(v)) return v > 0 ? "∞" : "-∞";
        if (esEntero(v)) return Long.toString(Math.round(v));
        int decimales = Math.abs(v) >= 1e-4 ? 6 : 12;
        String s = String.format(Locale.US, "%." + decimales + "f", v);
        if (s.contains(".")) {
            s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        }
        return s.equals("-0") ? "0" : s;
    }
}
