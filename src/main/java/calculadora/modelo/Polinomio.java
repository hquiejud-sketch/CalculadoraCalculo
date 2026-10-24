package calculadora.modelo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Polinomio con coeficientes numéricos: c0 + c1·x + c2·x² + ... + cn·xⁿ.
 *
 * <p>Igual que en el ejemplo del enunciado, cada polinomio sabe derivarse e
 * integrarse a sí mismo término por término. También se usa para dividir
 * polinomios y para las fracciones parciales.</p>
 */
public final class Polinomio {

    private static final int GRADO_MAXIMO = 200;

    /** c[i] es el coeficiente de x^i. */
    private final double[] c;

    public Polinomio(double... coeficientes) {
        double escala = 0;
        for (double v : coeficientes) escala = Math.max(escala, Math.abs(v));
        int n = coeficientes.length;
        while (n > 1 && Math.abs(coeficientes[n - 1]) <= 1e-13 * Math.max(1, escala)) n--;
        c = n == 0 ? new double[]{0} : Arrays.copyOf(coeficientes, n);
    }

    public static Polinomio constante(double k) {
        return new Polinomio(k);
    }

    public static Polinomio x() {
        return new Polinomio(0, 1);
    }

    public int grado() {
        return c.length - 1;
    }

    public double coef(int i) {
        return i >= 0 && i < c.length ? c[i] : 0;
    }

    public double[] coeficientes() {
        return c.clone();
    }

    public boolean esCero() {
        return c.length == 1 && Numeros.casiCero(c[0]);
    }

    public Polinomio sumar(Polinomio otro) {
        double[] r = new double[Math.max(c.length, otro.c.length)];
        for (int i = 0; i < r.length; i++) r[i] = coef(i) + otro.coef(i);
        return new Polinomio(r);
    }

    public Polinomio restar(Polinomio otro) {
        return sumar(otro.escalar(-1));
    }

    public Polinomio escalar(double k) {
        double[] r = c.clone();
        for (int i = 0; i < r.length; i++) r[i] *= k;
        return new Polinomio(r);
    }

    public Polinomio multiplicar(Polinomio otro) {
        double[] r = new double[c.length + otro.c.length - 1];
        for (int i = 0; i < c.length; i++)
            for (int j = 0; j < otro.c.length; j++)
                r[i + j] += c[i] * otro.c[j];
        return new Polinomio(r);
    }

    public Polinomio potencia(int n) {
        Polinomio resultado = constante(1);
        for (int i = 0; i < n; i++) resultado = resultado.multiplicar(this);
        return resultado;
    }

    /** d/dx (c·xⁿ) = n·c·xⁿ⁻¹ para cada término. */
    public Polinomio derivar() {
        if (c.length == 1) return constante(0);
        double[] r = new double[c.length - 1];
        for (int i = 1; i < c.length; i++) r[i - 1] = i * c[i];
        return new Polinomio(r);
    }

    /** ∫ c·xⁿ dx = c·xⁿ⁺¹/(n+1) para cada término (sin la constante C). */
    public Polinomio integrar() {
        double[] r = new double[c.length + 1];
        for (int i = 0; i < c.length; i++) r[i + 1] = c[i] / (i + 1);
        return new Polinomio(r);
    }

    /** Evalúa con el método de Horner. */
    public double evaluar(double x) {
        double r = 0;
        for (int i = c.length - 1; i >= 0; i--) r = r * x + c[i];
        return r;
    }

    /** División larga: devuelve {cociente, residuo}. */
    public Polinomio[] dividir(Polinomio divisor) {
        if (divisor.esCero()) throw new ArithmeticException("División entre el polinomio cero");
        int n = grado(), m = divisor.grado();
        if (n < m) return new Polinomio[]{constante(0), this};
        double[] resto = c.clone();
        double[] q = new double[n - m + 1];
        for (int i = n - m; i >= 0; i--) {
            q[i] = resto[i + m] / divisor.c[m];
            for (int j = 0; j <= m; j++) resto[i + j] -= q[i] * divisor.c[j];
        }
        return new Polinomio[]{new Polinomio(q), new Polinomio(Arrays.copyOf(resto, Math.max(m, 1)))};
    }

    /** Devuelve p(x + h). */
    public Polinomio trasladar(double h) {
        Polinomio resultado = constante(0);
        Polinomio xMasH = new Polinomio(h, 1);
        for (int i = c.length - 1; i >= 0; i--) {
            resultado = resultado.multiplicar(xMasH).sumar(constante(c[i]));
        }
        return resultado;
    }

    public boolean casiIgual(Polinomio otro, double tolerancia) {
        int n = Math.max(c.length, otro.c.length);
        for (int i = 0; i < n; i++) {
            if (!Numeros.casiIguales(coef(i), otro.coef(i), tolerancia)) return false;
        }
        return true;
    }

    /**
     * Intenta convertir una expresión en polinomio de la variable dada.
     * Devuelve null si no lo es (por ejemplo si tiene sen(x), 1/x u otra variable).
     */
    public static Polinomio desde(Expresion e, String variable) {
        if (e instanceof Constante k) {
            return k.esNumerica() ? constante(k.getValor()) : null;
        }
        if (e instanceof Variable v) {
            return v.getNombre().equals(variable) ? x() : null;
        }
        if (e instanceof Suma s) {
            Polinomio a = desde(s.getIzquierda(), variable), b = desde(s.getDerecha(), variable);
            return a == null || b == null ? null : a.sumar(b);
        }
        if (e instanceof Resta r) {
            Polinomio a = desde(r.getIzquierda(), variable), b = desde(r.getDerecha(), variable);
            return a == null || b == null ? null : a.restar(b);
        }
        if (e instanceof Negacion n) {
            Polinomio a = desde(n.getOperando(), variable);
            return a == null ? null : a.escalar(-1);
        }
        if (e instanceof Producto p) {
            Polinomio a = desde(p.getIzquierda(), variable), b = desde(p.getDerecha(), variable);
            if (a == null || b == null || a.grado() + b.grado() > GRADO_MAXIMO) return null;
            return a.multiplicar(b);
        }
        if (e instanceof Division d) {
            Polinomio a = desde(d.getNumerador(), variable), b = desde(d.getDenominador(), variable);
            if (a == null || b == null || b.grado() != 0 || Numeros.casiCero(b.coef(0))) return null;
            return a.escalar(1 / b.coef(0));
        }
        if (e instanceof Potencia p && p.getExponente() instanceof Constante k && k.esEntero()) {
            long n = Math.round(k.getValor());
            Polinomio base = desde(p.getBase(), variable);
            if (base == null || n < 0 || n * base.grado() > GRADO_MAXIMO) return null;
            return base.potencia((int) n);
        }
        return null;
    }

    /** Convierte a expresión ordenada de mayor a menor grado: 3x^2 - x + 5 (y 1 - x^2 en vez de -x^2 + 1). */
    public Expresion aExpresion(String variable) {
        List<Integer> orden = new ArrayList<>();
        for (int i = grado(); i >= 0; i--) {
            if (!Numeros.casiCero(Numeros.ajustar(c[i]))) orden.add(i);
        }
        if (orden.size() == 2 && c[orden.get(0)] < 0 && c[orden.get(1)] > 0) {
            Collections.reverse(orden);
        }
        Expresion acumulado = null;
        for (int i : orden) {
            double k = Numeros.ajustar(c[i]);
            Expresion potencia = i == 0 ? null
                    : i == 1 ? new Variable(variable)
                    : new Potencia(new Variable(variable), new Constante(i));
            double magnitud = acumulado == null ? k : Math.abs(k);
            Expresion termino;
            if (potencia == null) termino = new Constante(magnitud);
            else if (magnitud == 1) termino = potencia;
            else if (magnitud == -1) termino = new Negacion(potencia);
            else termino = new Producto(new Constante(magnitud), potencia);

            if (acumulado == null) acumulado = termino;
            else acumulado = k < 0 ? new Resta(acumulado, termino) : new Suma(acumulado, termino);
        }
        return acumulado == null ? Constante.CERO : acumulado;
    }

    @Override
    public String toString() {
        return aExpresion("x").toString();
    }
}
