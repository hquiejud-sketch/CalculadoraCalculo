package calculadora.modelo;

/**
 * Atajos para construir expresiones con menos código.
 * Ejemplo: {@code suma(mul(num(3), pot(var("x"), 2)), var("x"))} representa 3x^2 + x.
 */
public final class Ops {

    private Ops() {
    }

    public static Expresion num(double valor) {
        return new Constante(valor);
    }

    public static Expresion var(String nombre) {
        return new Variable(nombre);
    }

    public static Expresion suma(Expresion a, Expresion b) {
        return new Suma(a, b);
    }

    public static Expresion resta(Expresion a, Expresion b) {
        return new Resta(a, b);
    }

    public static Expresion mul(Expresion a, Expresion b) {
        return new Producto(a, b);
    }

    public static Expresion div(Expresion a, Expresion b) {
        return new Division(a, b);
    }

    public static Expresion pot(Expresion base, Expresion exponente) {
        return new Potencia(base, exponente);
    }

    public static Expresion pot(Expresion base, double exponente) {
        return new Potencia(base, new Constante(exponente));
    }

    public static Expresion neg(Expresion a) {
        return new Negacion(a);
    }

    public static Expresion fn(TipoFuncion tipo, Expresion argumento) {
        return new Funcion(tipo, argumento);
    }

    /** ln|u| */
    public static Expresion lnAbs(Expresion u) {
        return new Funcion(TipoFuncion.LN, new Funcion(TipoFuncion.ABS, u));
    }
}
