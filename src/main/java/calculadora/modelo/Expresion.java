package calculadora.modelo;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Clase base del árbol de expresiones matemáticas (patrón de diseño Composite).
 *
 * <p>Cada nodo del árbol (constante, variable, suma, producto, potencia, función...)
 * es un objeto que sabe <b>evaluarse</b>, <b>derivarse</b>, <b>simplificarse</b> y
 * <b>mostrarse</b> a sí mismo. Por ejemplo, {@code 3x^2 + 5x} se guarda como:</p>
 *
 * <pre>
 *            Suma
 *          /      \
 *     Producto   Producto
 *     /    \      /    \
 *    3   Potencia 5     x
 *         /   \
 *        x     2
 * </pre>
 *
 * <p>Gracias al polimorfismo, para derivar todo el árbol basta con llamar
 * {@code raiz.derivar("x")}: cada nodo aplica su propia regla (suma, producto,
 * cadena...) y llama a la derivada de sus hijos.</p>
 */
public abstract class Expresion {

    /** Marca para no simplificar dos veces el mismo nodo (los nodos son inmutables). */
    private boolean simplificada;

    /** Calcula el valor numérico usando los valores de las variables. */
    public abstract double evaluar(Map<String, Double> valores);

    /** Devuelve la derivada de esta expresión respecto a la variable indicada. */
    public abstract Expresion derivar(String variable);

    /** Cada subclase implementa aquí sus reglas de simplificación. */
    protected abstract Expresion simplificarNodo();

    /** Reemplaza la variable por otra expresión (se usa para evaluar F(b) − F(a)). */
    public abstract Expresion sustituir(String variable, Expresion valor);

    /** Agrega al conjunto los nombres de las variables que aparecen. */
    protected abstract void recolectarVariables(Set<String> destino);

    /**
     * Precedencia para decidir dónde poner paréntesis al imprimir:
     * 1 = suma/resta, 2 = producto/división, 3 = negativo, 4 = potencia, 5 = número/variable/función.
     */
    public abstract int precedencia();

    @Override public abstract boolean equals(Object otro);
    @Override public abstract int hashCode();
    @Override public abstract String toString();

    /** Simplifica la expresión (método plantilla: guarda el resultado para no repetir trabajo). */
    public final Expresion simplificar() {
        if (simplificada) {
            return this;
        }
        Expresion resultado = simplificarNodo();
        resultado.simplificada = true;
        return resultado;
    }

    /** Evalúa con una sola variable, por ejemplo f(2). */
    public double evaluar(String variable, double valor) {
        Map<String, Double> valores = new HashMap<>();
        valores.put(variable, valor);
        return evaluar(valores);
    }

    public Set<String> variables() {
        Set<String> conjunto = new LinkedHashSet<>();
        recolectarVariables(conjunto);
        return conjunto;
    }

    public boolean contieneVariable(String variable) {
        return variables().contains(variable);
    }

    /** true si no tiene variables (por ejemplo 2, π o ln(3)). */
    public boolean esConstante() {
        return variables().isEmpty();
    }

    public double valorConstante() {
        return evaluar(Collections.emptyMap());
    }

    public boolean esCero() {
        return this instanceof Constante c && c.esNumerica() && c.getValor() == 0;
    }

    public boolean esUno() {
        return this instanceof Constante c && c.esNumerica() && c.getValor() == 1;
    }

    /** Pone paréntesis al hijo si su precedencia es menor que la mínima pedida. */
    protected static String envolver(Expresion hijo, int precedenciaMinima) {
        String texto = hijo.toString();
        return hijo.precedencia() < precedenciaMinima ? "(" + texto + ")" : texto;
    }

    protected static boolean esNumeroNegativo(Expresion e) {
        return e instanceof Constante c && c.esNumerica() && c.getValor() < 0;
    }
}
