package calculadora.logica;

import calculadora.modelo.Expresion;

/**
 * Operación que se le puede aplicar a una función (clase abstracta).
 * Cada subclase implementa {@link #ejecutar(Expresion)} a su manera (polimorfismo):
 * {@link Derivada}, {@link IntegralIndefinida} e {@link IntegralDefinida}.
 */
public abstract class Operacion {

    /** Variable respecto a la cual se deriva o integra ("x" o "y"). */
    protected final String variable;

    protected Operacion(String variable) {
        this.variable = variable;
    }

    public String getVariable() {
        return variable;
    }

    public abstract String getNombre();

    public abstract ResultadoCalculo ejecutar(Expresion funcion) throws ErrorCalculo;

    /** Comprueba que un límite o punto sea un número (acepta pi, e, 1/2, √2...). */
    protected static double valorNumerico(Expresion e, String descripcion) throws ErrorCalculo {
        if (!e.esConstante()) {
            throw new ErrorCalculo(descripcion + " debe ser un número (puedes usar pi, e, 1/2, √2...).");
        }
        double v = e.valorConstante();
        if (!Double.isFinite(v)) {
            throw new ErrorCalculo(descripcion + " no es un número válido.");
        }
        return v;
    }
}
