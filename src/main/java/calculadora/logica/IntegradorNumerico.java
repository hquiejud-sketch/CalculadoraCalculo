package calculadora.logica;

import calculadora.modelo.Expresion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.DoubleUnaryOperator;

/**
 * Integración numérica con la regla de Simpson adaptativa.
 *
 * <p>Se usa cuando no existe una antiderivada con funciones elementales (por ejemplo e^(-x²))
 * y también para comprobar el resultado exacto de las integrales definidas.</p>
 */
public class IntegradorNumerico {

    /** Valor aproximado y si el método logró la precisión pedida. */
    public record Resultado(double valor, boolean convergio) {
    }

    private static final int PROFUNDIDAD_MAXIMA = 48;
    private static final long EVALUACIONES_MAXIMAS = 3_000_000;

    private boolean fallo;
    private long evaluaciones;

    /** Convierte la expresión en una función de una variable f(x). */
    public static DoubleUnaryOperator comoFuncion(Expresion e, String variable) {
        Map<String, Double> valores = new HashMap<>();
        return x -> {
            valores.put(variable, x);
            return e.evaluar(valores);
        };
    }

    /** Como {@link #comoFuncion}, pero rellena huecos removibles (sen(x)/x en x = 0) con el valor de los lados. */
    public static DoubleUnaryOperator comoFuncionSegura(Expresion e, String variable) {
        DoubleUnaryOperator f = comoFuncion(e, variable);
        return x -> {
            double y = f.applyAsDouble(x);
            if (Double.isFinite(y)) return y;
            double d = 1e-7 * Math.max(1, Math.abs(x));
            double izquierda = f.applyAsDouble(x - d);
            double derecha = f.applyAsDouble(x + d);
            if (Double.isFinite(izquierda) && Double.isFinite(derecha)) return (izquierda + derecha) / 2;
            if (Double.isFinite(izquierda)) return izquierda;
            if (Double.isFinite(derecha)) return derecha;
            return Double.NaN;
        };
    }

    /** ∫ₐᵇ f(x) dx */
    public Resultado integrar(DoubleUnaryOperator f, double a, double b) {
        if (a == b) return new Resultado(0, true);
        if (a > b) {
            Resultado r = integrar(f, b, a);
            return new Resultado(-r.valor(), r.convergio());
        }
        fallo = false;
        evaluaciones = 0;
        int partes = 64; // se divide primero el intervalo para no perder oscilaciones
        double h = (b - a) / partes;
        double tolerancia = 1e-10 * Math.max(1, Math.abs(estimacionRapida(f, a, b))) / partes;
        double total = 0;
        for (int i = 0; i < partes; i++) {
            double x0 = a + i * h;
            double x1 = i == partes - 1 ? b : a + (i + 1) * h;
            double m = (x0 + x1) / 2;
            double f0 = evaluar(f, x0), fm = evaluar(f, m), f1 = evaluar(f, x1);
            double simpson = (x1 - x0) / 6 * (f0 + 4 * fm + f1);
            total += adaptativo(f, x0, x1, f0, fm, f1, simpson, tolerancia, PROFUNDIDAD_MAXIMA);
        }
        return new Resultado(total, !fallo && Double.isFinite(total));
    }

    /** Si el error estimado es grande, parte el intervalo a la mitad y repite en cada lado. */
    private double adaptativo(DoubleUnaryOperator f, double a, double b, double fa, double fm, double fb,
                              double entero, double tolerancia, int profundidad) {
        double m = (a + b) / 2;
        double mIzq = (a + m) / 2, mDer = (m + b) / 2;
        double fIzq = evaluar(f, mIzq), fDer = evaluar(f, mDer);
        double izquierda = (m - a) / 6 * (fa + 4 * fIzq + fm);
        double derecha = (b - m) / 6 * (fm + 4 * fDer + fb);
        double diferencia = izquierda + derecha - entero;
        if (Math.abs(diferencia) <= 15 * tolerancia) {
            return izquierda + derecha + diferencia / 15;
        }
        if (profundidad <= 0 || evaluaciones > EVALUACIONES_MAXIMAS || Double.isNaN(diferencia)) {
            fallo = true;
            return izquierda + derecha + diferencia / 15;
        }
        return adaptativo(f, a, m, fa, fIzq, fm, izquierda, tolerancia / 2, profundidad - 1)
                + adaptativo(f, m, b, fm, fDer, fb, derecha, tolerancia / 2, profundidad - 1);
    }

    private double evaluar(DoubleUnaryOperator f, double x) {
        evaluaciones++;
        return f.applyAsDouble(x);
    }

    private static double estimacionRapida(DoubleUnaryOperator f, double a, double b) {
        int n = 200;
        double h = (b - a) / n, suma = 0;
        for (int i = 0; i <= n; i++) {
            double y = f.applyAsDouble(a + i * h);
            if (!Double.isFinite(y)) continue;
            double peso = (i == 0 || i == n) ? 1 : (i % 2 == 1 ? 4 : 2);
            suma += peso * y;
        }
        return suma * h / 3;
    }

    /**
     * Busca un punto del intervalo donde la función no es continua (asíntota vertical o hueco no removible).
     * Devuelve ese punto o null si la función parece continua en [a, b].
     */
    public Double buscarDiscontinuidad(DoubleUnaryOperator f, double a, double b) {
        int n = 2000;
        double[] xs = new double[n + 1];
        double[] ys = new double[n + 1];
        for (int i = 0; i <= n; i++) {
            xs[i] = a + (b - a) * i / n;
            ys[i] = f.applyAsDouble(xs[i]);
        }
        for (int i = 0; i <= n; i++) {
            if (!Double.isFinite(ys[i]) && !esRemovible(f, xs[i], a, b)) return xs[i];
        }
        // Máximos locales de |f|: si al acercarse el valor sigue creciendo sin límite, hay una asíntota
        List<Integer> candidatos = new ArrayList<>();
        for (int i = 0; i <= n; i++) {
            double y = Math.abs(ys[i]);
            if (!Double.isFinite(y)) continue;
            double izquierda = i > 0 ? Math.abs(ys[i - 1]) : -1;
            double derecha = i < n ? Math.abs(ys[i + 1]) : -1;
            if ((!Double.isFinite(izquierda) || y >= izquierda) && (!Double.isFinite(derecha) || y >= derecha)) {
                candidatos.add(i);
            }
        }
        candidatos.sort((i, j) -> Double.compare(Math.abs(ys[j]), Math.abs(ys[i])));
        for (int c = 0; c < Math.min(12, candidatos.size()); c++) {
            int i = candidatos.get(c);
            double desde = xs[Math.max(0, i - 1)], hasta = xs[Math.min(n, i + 1)];
            double polo = creceSinLimite(f, desde, hasta, a, b);
            if (!Double.isNaN(polo)) return redondearPolo(polo, b - a);
        }
        return null;
    }

    /** Si |f| crece sin límite al acercarse, devuelve dónde (asíntota); si no, NaN. */
    private double creceSinLimite(DoubleUnaryOperator f, double desde, double hasta, double a, double b) {
        double[] mejor = maximo(f, desde, hasta);
        if (Double.isNaN(mejor[0])) return (desde + hasta) / 2;
        double inicial = mejor[1];
        double ancho = hasta - desde;
        for (int z = 0; z < 8; z++) {
            ancho /= 10;
            double[] m = maximo(f, Math.max(a, mejor[0] - ancho), Math.min(b, mejor[0] + ancho));
            if (Double.isNaN(m[0])) return mejor[0];
            mejor = m;
        }
        return mejor[1] > 1e3 && mejor[1] > 1e6 * inicial ? mejor[0] : Double.NaN;
    }

    private static double redondearPolo(double x, double ancho) {
        double paso = Math.pow(10, Math.floor(Math.log10(Math.max(ancho, 1e-12))) - 6);
        return Math.round(x / paso) * paso + 0.0;
    }

    /** {x donde |f| es máximo, ese máximo} con 41 muestras. */
    private static double[] maximo(DoubleUnaryOperator f, double desde, double hasta) {
        double mejorX = Double.NaN, mejorY = -1;
        for (int i = 0; i <= 40; i++) {
            double x = desde + (hasta - desde) * i / 40;
            double y = Math.abs(f.applyAsDouble(x));
            if (Double.isFinite(y) && y > mejorY) {
                mejorY = y;
                mejorX = x;
            }
        }
        return new double[]{mejorX, mejorY};
    }

    /** Un hueco es removible si a los lados la función se acerca a un mismo valor finito. */
    private static boolean esRemovible(DoubleUnaryOperator f, double x, double a, double b) {
        double d = 1e-6 * Math.max(1, Math.abs(x));
        Double izquierda = null, derecha = null;
        if (x - 2 * d >= a) {
            double y1 = f.applyAsDouble(x - d), y2 = f.applyAsDouble(x - 2 * d);
            if (!cercanos(y1, y2)) return false;
            izquierda = y1;
        }
        if (x + 2 * d <= b) {
            double y1 = f.applyAsDouble(x + d), y2 = f.applyAsDouble(x + 2 * d);
            if (!cercanos(y1, y2)) return false;
            derecha = y1;
        }
        if (izquierda == null && derecha == null) return false;
        return izquierda == null || derecha == null || cercanos(izquierda, derecha);
    }

    private static boolean cercanos(double y1, double y2) {
        return Double.isFinite(y1) && Double.isFinite(y2) && Math.abs(y1 - y2) <= 1e-3 * Math.max(1, Math.abs(y1));
    }
}
