package calculadora.modelo;

/** Fracción p/q. Sirve para mostrar 0.5 como 1/2 o 0.3333333333 como 1/3. */
public final class Fraccion {

    private final long numerador;
    private final long denominador;

    public Fraccion(long numerador, long denominador) {
        if (denominador < 0) {
            numerador = -numerador;
            denominador = -denominador;
        }
        this.numerador = numerador;
        this.denominador = denominador;
    }

    public static Fraccion aproximar(double x) {
        return aproximar(x, 1000, 1e-10);
    }

    /**
     * Busca la fracción más simple cercana a x usando fracciones continuas.
     * Devuelve null si no hay una con denominador ≤ maxDenominador.
     */
    public static Fraccion aproximar(double x, long maxDenominador, double tolerancia) {
        if (!Double.isFinite(x) || Math.abs(x) > 1e9) return null;
        long signo = x < 0 ? -1 : 1;
        double y = Math.abs(x);
        long h0 = 0, h1 = 1, k0 = 1, k1 = 0;
        double b = y;
        for (int i = 0; i < 64; i++) {
            long a = (long) Math.floor(b);
            long h2 = a * h1 + h0;
            long k2 = a * k1 + k0;
            if (k2 > maxDenominador) break;
            h0 = h1;
            h1 = h2;
            k0 = k1;
            k1 = k2;
            if (Math.abs(y - (double) h1 / k1) <= tolerancia * Math.max(1, y)) {
                return new Fraccion(signo * h1, k1);
            }
            double parteFraccionaria = b - a;
            if (parteFraccionaria < 1e-15) break;
            b = 1 / parteFraccionaria;
        }
        return null;
    }

    public long getNumerador() {
        return numerador;
    }

    public long getDenominador() {
        return denominador;
    }

    public double valor() {
        return (double) numerador / denominador;
    }

    @Override
    public String toString() {
        return denominador == 1 ? Long.toString(numerador) : numerador + "/" + denominador;
    }
}
