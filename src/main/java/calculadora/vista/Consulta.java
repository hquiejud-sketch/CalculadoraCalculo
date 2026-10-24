package calculadora.vista;

/**
 * Datos de una consulta (lo que el usuario escribió en la pantalla).
 * Se usa para el historial y para los ejemplos de la pestaña de ayuda.
 */
public class Consulta {

    public enum Tipo { DERIVADA, INDEFINIDA, DEFINIDA }

    private final String funcion;
    private final String variable;
    private final Tipo tipo;
    private final int orden;
    private final String punto;   // null si no se evalúa en un punto
    private final String limiteA;
    private final String limiteB;
    private String resumen;       // texto que se muestra en la lista

    public Consulta(String funcion, String variable, Tipo tipo, int orden, String punto, String limiteA, String limiteB) {
        this.funcion = funcion;
        this.variable = variable;
        this.tipo = tipo;
        this.orden = orden;
        this.punto = punto;
        this.limiteA = limiteA;
        this.limiteB = limiteB;
    }

    public static Consulta derivada(String funcion, String variable) {
        return new Consulta(funcion, variable, Tipo.DERIVADA, 1, null, "0", "1");
    }

    public static Consulta indefinida(String funcion, String variable) {
        return new Consulta(funcion, variable, Tipo.INDEFINIDA, 1, null, "0", "1");
    }

    public static Consulta definida(String funcion, String a, String b) {
        return new Consulta(funcion, "x", Tipo.DEFINIDA, 1, null, a, b);
    }

    public Consulta conResumen(String resumen) {
        this.resumen = resumen;
        return this;
    }

    public String getFuncion() {
        return funcion;
    }

    public String getVariable() {
        return variable;
    }

    public Tipo getTipo() {
        return tipo;
    }

    public int getOrden() {
        return orden;
    }

    public String getPunto() {
        return punto;
    }

    public String getLimiteA() {
        return limiteA;
    }

    public String getLimiteB() {
        return limiteB;
    }

    public String getResumen() {
        return resumen;
    }

    @Override
    public String toString() {
        return resumen != null ? resumen : funcion;
    }
}
