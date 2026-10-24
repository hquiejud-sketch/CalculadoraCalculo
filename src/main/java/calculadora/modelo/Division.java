package calculadora.modelo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** a / b */
public final class Division extends Expresion {

    private final Expresion numerador;
    private final Expresion denominador;

    public Division(Expresion numerador, Expresion denominador) {
        this.numerador = numerador;
        this.denominador = denominador;
    }

    public Expresion getNumerador() {
        return numerador;
    }

    public Expresion getDenominador() {
        return denominador;
    }

    @Override
    public double evaluar(Map<String, Double> valores) {
        return numerador.evaluar(valores) / denominador.evaluar(valores);
    }

    /** Regla del cociente: (f/g)' = (f'·g − f·g') / g² */
    @Override
    public Expresion derivar(String variable) {
        if (!denominador.contieneVariable(variable)) {
            return new Division(numerador.derivar(variable), denominador);
        }
        return new Division(
                new Resta(
                        new Producto(numerador.derivar(variable), denominador),
                        new Producto(numerador, denominador.derivar(variable))),
                new Potencia(denominador, new Constante(2)));
    }

    @Override
    protected Expresion simplificarNodo() {
        return Simplificador.simplificarProducto(this);
    }

    @Override
    public Expresion sustituir(String variable, Expresion valor) {
        return new Division(numerador.sustituir(variable, valor), denominador.sustituir(variable, valor));
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        numerador.recolectarVariables(destino);
        denominador.recolectarVariables(destino);
    }

    @Override
    public int precedencia() {
        return 2;
    }

    @Override
    public String toString() {
        return envolver(numerador, 2) + "/" + envolver(denominador, 4);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Division d && d.numerador.equals(numerador) && d.denominador.equals(denominador);
    }

    @Override
    public int hashCode() {
        return Objects.hash("/", numerador, denominador);
    }
}
