package calculadora.modelo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Un número (2, 0.5, -3) o una constante con nombre (π, e). */
public final class Constante extends Expresion {

    public static final Constante CERO = new Constante(0);
    public static final Constante UNO = new Constante(1);
    public static final Constante PI = new Constante(Math.PI, "π");
    public static final Constante E = new Constante(Math.E, "e");

    private final double valor;
    /** null si es un número normal; "π" o "e" si es una constante con nombre. */
    private final String nombre;

    public Constante(double valor) {
        this(valor, null);
    }

    private Constante(double valor, String nombre) {
        this.valor = valor == 0 ? 0.0 : valor; // evita mostrar "-0"
        this.nombre = nombre;
    }

    public double getValor() {
        return valor;
    }

    /** true si es un número escrito con dígitos (no π ni e). */
    public boolean esNumerica() {
        return nombre == null;
    }

    public boolean esEntero() {
        return esNumerica() && Numeros.esEntero(valor);
    }

    @Override
    public double evaluar(Map<String, Double> valores) {
        return valor;
    }

    @Override
    public Expresion derivar(String variable) {
        return CERO; // la derivada de una constante es 0
    }

    @Override
    protected Expresion simplificarNodo() {
        return this;
    }

    @Override
    public Expresion sustituir(String variable, Expresion nuevo) {
        return this;
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        // una constante no tiene variables
    }

    @Override
    public int precedencia() {
        if (nombre != null) return 5;
        if (valor < 0) return 3;
        if (!Numeros.esEntero(valor) && Fraccion.aproximar(valor) != null) return 2; // se muestra como a/b
        return 5;
    }

    @Override
    public String toString() {
        return nombre != null ? nombre : Numeros.formatear(valor);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Constante c && Objects.equals(nombre, c.nombre) && Double.compare(valor, c.valor) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nombre, valor);
    }
}
