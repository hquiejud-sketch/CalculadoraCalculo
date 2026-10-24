package calculadora.modelo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** a − b */
public final class Resta extends Expresion {

    private final Expresion izquierda;
    private final Expresion derecha;

    public Resta(Expresion izquierda, Expresion derecha) {
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
        return izquierda.evaluar(valores) - derecha.evaluar(valores);
    }

    /** Regla de la resta: (f − g)' = f' − g' */
    @Override
    public Expresion derivar(String variable) {
        return new Resta(izquierda.derivar(variable), derecha.derivar(variable));
    }

    @Override
    protected Expresion simplificarNodo() {
        return Simplificador.simplificarSuma(this);
    }

    @Override
    public Expresion sustituir(String variable, Expresion valor) {
        return new Resta(izquierda.sustituir(variable, valor), derecha.sustituir(variable, valor));
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        izquierda.recolectarVariables(destino);
        derecha.recolectarVariables(destino);
    }

    @Override
    public int precedencia() {
        return 1;
    }

    @Override
    public String toString() {
        String a = izquierda.toString();
        if (derecha instanceof Negacion n) {
            return a + " + " + envolver(n.getOperando(), 2);
        }
        if (esNumeroNegativo(derecha)) {
            return a + " + " + Numeros.formatear(-((Constante) derecha).getValor());
        }
        return a + " - " + envolver(derecha, 2);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Resta r && r.izquierda.equals(izquierda) && r.derecha.equals(derecha);
    }

    @Override
    public int hashCode() {
        return Objects.hash("-", izquierda, derecha);
    }
}
