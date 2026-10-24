package calculadora.modelo;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** base ^ exponente  (también representa raíces: √x = x^(1/2), y exponenciales: e^x). */
public final class Potencia extends Expresion {

    private final Expresion base;
    private final Expresion exponente;

    public Potencia(Expresion base, Expresion exponente) {
        this.base = base;
        this.exponente = exponente;
    }

    public Expresion getBase() {
        return base;
    }

    public Expresion getExponente() {
        return exponente;
    }

    @Override
    public double evaluar(Map<String, Double> valores) {
        return potenciaReal(base.evaluar(valores), exponente.evaluar(valores));
    }

    /** Como Math.pow, pero permite raíces impares de negativos: (-8)^(1/3) = -2. */
    public static double potenciaReal(double b, double e) {
        if (b < 0 && !Numeros.esEntero(e)) {
            Fraccion f = Fraccion.aproximar(e);
            if (f != null && f.getDenominador() % 2 == 1) {
                double r = Math.pow(-b, e);
                return f.getNumerador() % 2 == 0 ? r : -r;
            }
            return Double.NaN;
        }
        return Math.pow(b, e);
    }

    /**
     * Tres casos:
     * <ul>
     *   <li>exponente constante  → regla de la potencia: (uⁿ)' = n·uⁿ⁻¹·u'</li>
     *   <li>base constante       → exponencial: (aᵘ)' = aᵘ·ln(a)·u'</li>
     *   <li>ambos variables      → derivación logarítmica: (uᵛ)' = uᵛ·(v'·ln(u) + v·u'/u)</li>
     * </ul>
     */
    @Override
    public Expresion derivar(String variable) {
        boolean baseVariable = base.contieneVariable(variable);
        boolean exponenteVariable = exponente.contieneVariable(variable);
        if (!baseVariable && !exponenteVariable) {
            return Constante.CERO;
        }
        if (!exponenteVariable) {
            return new Producto(
                    new Producto(exponente, new Potencia(base, new Resta(exponente, Constante.UNO))),
                    base.derivar(variable));
        }
        if (!baseVariable) {
            return new Producto(
                    new Producto(this, new Funcion(TipoFuncion.LN, base)),
                    exponente.derivar(variable));
        }
        return new Producto(this, new Suma(
                new Producto(exponente.derivar(variable), new Funcion(TipoFuncion.LN, base)),
                new Division(new Producto(exponente, base.derivar(variable)), base)));
    }

    @Override
    protected Expresion simplificarNodo() {
        return Simplificador.simplificarPotencia(this);
    }

    @Override
    public Expresion sustituir(String variable, Expresion valor) {
        return new Potencia(base.sustituir(variable, valor), exponente.sustituir(variable, valor));
    }

    @Override
    protected void recolectarVariables(Set<String> destino) {
        base.recolectarVariables(destino);
        exponente.recolectarVariables(destino);
    }

    @Override
    public int precedencia() {
        return 4;
    }

    @Override
    public String toString() {
        if (exponente instanceof Constante k && k.esNumerica() && Math.abs(k.getValor() - 0.5) < 1e-12) {
            String b = base.toString();
            boolean atomica = base instanceof Variable || (base instanceof Constante c && c.precedencia() == 5);
            return "√" + (atomica ? b : "(" + b + ")");
        }
        boolean exponenteSimple = exponente instanceof Variable
                || (exponente instanceof Constante k
                    && (!k.esNumerica() || (k.getValor() >= 0 && k.precedencia() == 5)));
        String e = exponenteSimple ? exponente.toString() : "(" + exponente + ")";
        return envolver(base, 5) + "^" + e;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Potencia p && p.base.equals(base) && p.exponente.equals(exponente);
    }

    @Override
    public int hashCode() {
        return Objects.hash("^", base, exponente);
    }
}
