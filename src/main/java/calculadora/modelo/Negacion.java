package calculadora.modelo;

import java.util.Map;
import java.util.Set;

/** −a (signo menos delante de una expresión). */
public final class Negacion extends Expresion {

    private final Expresion operando;

    public Negacion(Expresion operando) {
        this.operando = operando;
    }

    public Expresion getOperando() {
        return operando;
    }

    @Override
    public double evaluar(Map<String, Double> valores) {
        return -operando.evaluar(valores);
    }

    /** (−f)' = −f' */
    @Override
    public Expresion derivar(String variable) {
        return new Negacion(operando.derivar(variable));
    }

    @Override
    protected Expresion simplificarNodo() {
        return Simplificador.simplificarProducto(this);
    }

    @Override
    public Expresion sustituir(String variable, Expresion valor) {
        return new Negacion(operando.sustituir(variable, valor));
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        operando.recolectarVariables(destino);
    }

    @Override
    public int precedencia() {
        return 3;
    }

    @Override
    public String toString() {
        String s = operando.toString();
        boolean sinParentesis = operando.precedencia() >= 2
                && !(operando instanceof Negacion)
                && !esNumeroNegativo(operando);
        return "-" + (sinParentesis ? s : "(" + s + ")");
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Negacion n && n.operando.equals(operando);
    }

    @Override
    public int hashCode() {
        return 31 * operando.hashCode() + 7;
    }
}
