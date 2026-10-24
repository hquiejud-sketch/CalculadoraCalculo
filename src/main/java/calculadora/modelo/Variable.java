package calculadora.modelo;

import java.util.Map;
import java.util.Set;

/** Una variable como x o y. */
public final class Variable extends Expresion {

    private final String nombre;

    public Variable(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    @Override
    public double evaluar(Map<String, Double> valores) {
        Double v = valores.get(nombre);
        return v == null ? Double.NaN : v;
    }

    /** d/dx (x) = 1  y  d/dx (y) = 0 (las demás variables se tratan como constantes). */
    @Override
    public Expresion derivar(String variable) {
        return nombre.equals(variable) ? Constante.UNO : Constante.CERO;
    }

    @Override
    protected Expresion simplificarNodo() {
        return this;
    }

    @Override
    public Expresion sustituir(String variable, Expresion valor) {
        return nombre.equals(variable) ? valor : this;
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        destino.add(nombre);
    }

    @Override
    public int precedencia() {
        return 5;
    }

    @Override
    public String toString() {
        return nombre;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Variable v && v.nombre.equals(nombre);
    }

    @Override
    public int hashCode() {
        return nombre.hashCode();
    }
}
