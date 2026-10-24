package calculadora.modelo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** a · b */
public final class Producto extends Expresion {

    private final Expresion izquierda;
    private final Expresion derecha;

    public Producto(Expresion izquierda, Expresion derecha) {
        this.izquierda = izquierda;
        this.derecha = derecha;
    }

    public Expresion getIzquierda() {
        return izquierda;
    }

    public Expresion getDerecha() {
        return derecha;
    }

    @Override
    public double evaluar(Map<String, Double> valores) {
        return izquierda.evaluar(valores) * derecha.evaluar(valores);
    }

    /** Regla del producto: (f·g)' = f'·g + f·g'  (si un factor es constante: (k·f)' = k·f'). */
    @Override
    public Expresion derivar(String variable) {
        boolean dependeIzq = izquierda.contieneVariable(variable);
        boolean dependeDer = derecha.contieneVariable(variable);
        if (!dependeIzq && !dependeDer) return Constante.CERO;
        if (!dependeIzq) return new Producto(izquierda, derecha.derivar(variable));
        if (!dependeDer) return new Producto(izquierda.derivar(variable), derecha);
        return new Suma(
                new Producto(izquierda.derivar(variable), derecha),
                new Producto(izquierda, derecha.derivar(variable)));
    }

    @Override
    protected Expresion simplificarNodo() {
        return Simplificador.simplificarProducto(this);
    }

    @Override
    public Expresion sustituir(String variable, Expresion valor) {
        return new Producto(izquierda.sustituir(variable, valor), derecha.sustituir(variable, valor));
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        izquierda.recolectarVariables(destino);
        derecha.recolectarVariables(destino);
    }

    @Override
    public int precedencia() {
        return 2;
    }

    @Override
    public String toString() {
        // Caso típico después de simplificar: coeficiente numérico a la izquierda (3x, x/2, 5x^2/3)
        if (izquierda instanceof Constante c && c.esNumerica()) {
            double k = c.getValor();
            Expresion arriba = derecha;
            Expresion abajo = null;
            if (derecha instanceof Division d) {
                arriba = d.getNumerador();
                abajo = d.getDenominador();
            }
            Fraccion f = Numeros.esEntero(k) ? null : Fraccion.aproximar(k);
            if (f != null) {
                String numerador = coeficientePor(f.getNumerador(), arriba);
                Expresion denominador = abajo == null
                        ? new Constante(f.getDenominador())
                        : new Producto(new Constante(f.getDenominador()), abajo);
                return numerador + "/" + envolver(denominador, 4);
            }
            if (abajo != null) {
                return coeficientePor(k, arriba) + "/" + envolver(abajo, 4);
            }
            return coeficientePor(k, derecha);
        }
        String a = envolver(izquierda, 2);
        String b = (derecha instanceof Negacion || esNumeroNegativo(derecha))
                ? "(" + derecha + ")"
                : envolver(derecha, 2);
        if (seEscribenJuntos(izquierda, derecha)) {
            return a + b; // xy, πx
        }
        return a + "·" + b;
    }

    /** Escribe un coeficiente delante de una expresión: 3x, -x, 2(x + 1), 3·2^x. */
    static String coeficientePor(double k, Expresion e) {
        if (e.esUno()) return Numeros.formatear(k);
        String s = envolver(e, 2);
        if (k == 1) return s;
        if (k == -1) return "-" + s;
        String numero = Numeros.formatear(k);
        char primero = s.isEmpty() ? ' ' : s.charAt(0);
        if (Character.isLetter(primero) || primero == '(' || primero == '√' || primero == '|') {
            return numero + s;
        }
        return numero + "·" + s;
    }

    private static boolean seEscribenJuntos(Expresion a, Expresion b) {
        boolean bEsVariable = b instanceof Variable
                || (b instanceof Potencia p && p.getBase() instanceof Variable);
        return (a instanceof Variable && b instanceof Variable)
                || (a.equals(Constante.PI) && bEsVariable);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Producto p && p.izquierda.equals(izquierda) && p.derecha.equals(derecha);
    }

    @Override
    public int hashCode() {
        return Objects.hash("*", izquierda, derecha);
    }
}
