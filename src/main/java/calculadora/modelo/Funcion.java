package calculadora.modelo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Una función aplicada a un argumento: sen(u), ln(u), |u| ... */
public final class Funcion extends Expresion {

    private final TipoFuncion tipo;
    private final Expresion argumento;

    public Funcion(TipoFuncion tipo, Expresion argumento) {
        this.tipo = tipo;
        this.argumento = argumento;
    }

    public TipoFuncion getTipo() {
        return tipo;
    }

    public Expresion getArgumento() {
        return argumento;
    }

    @Override
    public double evaluar(Map<String, Double> valores) {
        return tipo.aplicar(argumento.evaluar(valores));
    }

    /** Regla de la cadena: [f(u)]' = f'(u) · u' */
    @Override
    public Expresion derivar(String variable) {
        if (!argumento.contieneVariable(variable)) {
            return Constante.CERO;
        }
        return new Producto(tipo.derivadaExterna(argumento), argumento.derivar(variable));
    }

    @Override
    protected Expresion simplificarNodo() {
        return Simplificador.simplificarFuncion(this);
    }

    @Override
    public Expresion sustituir(String variable, Expresion valor) {
        return new Funcion(tipo, argumento.sustituir(variable, valor));
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        argumento.recolectarVariables(destino);
    }

    @Override
    public int precedencia() {
        return 5;
    }

    @Override
    public String toString() {
        if (tipo == TipoFuncion.ABS) {
            return "|" + argumento + "|";
        }
        if (tipo == TipoFuncion.LN && argumento instanceof Funcion f && f.tipo == TipoFuncion.ABS) {
            return "ln" + argumento; // ln|x|
        }
        return tipo.getNombre() + "(" + argumento + ")";
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Funcion f && f.tipo == tipo && f.argumento.equals(argumento);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, argumento);
    }
}
