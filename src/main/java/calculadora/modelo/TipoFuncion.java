package calculadora.modelo;

import static calculadora.modelo.Ops.*;

/**
 * Funciones disponibles. Cada una sabe calcularse, cuál es su derivada
 * y (si existe en la tabla) cuál es su integral.
 */
public enum TipoFuncion {
    SEN("sen"), COS("cos"), TAN("tan"), COT("cot"), SEC("sec"), CSC("csc"),
    ASEN("asen"), ACOS("acos"), ATAN("atan"),
    SENH("senh"), COSH("cosh"), TANH("tanh"),
    LN("ln"), LOG("log"), ABS("abs");

    private final String nombre;

    TipoFuncion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public double aplicar(double x) {
        return switch (this) {
            case SEN -> Math.sin(x);
            case COS -> Math.cos(x);
            case TAN -> Math.tan(x);
            case COT -> 1 / Math.tan(x);
            case SEC -> 1 / Math.cos(x);
            case CSC -> 1 / Math.sin(x);
            case ASEN -> Math.asin(x);
            case ACOS -> Math.acos(x);
            case ATAN -> Math.atan(x);
            case SENH -> Math.sinh(x);
            case COSH -> Math.cosh(x);
            case TANH -> Math.tanh(x);
            case LN -> Math.log(x);
            case LOG -> Math.log10(x);
            case ABS -> Math.abs(x);
        };
    }

    /** f'(u): la derivada "de afuera" para la regla de la cadena. */
    public Expresion derivadaExterna(Expresion u) {
        return switch (this) {
            case SEN -> fn(COS, u);
            case COS -> neg(fn(SEN, u));
            case TAN -> pot(fn(SEC, u), 2);
            case COT -> neg(pot(fn(CSC, u), 2));
            case SEC -> mul(fn(SEC, u), fn(TAN, u));
            case CSC -> neg(mul(fn(CSC, u), fn(COT, u)));
            case ASEN -> pot(resta(num(1), pot(u, 2)), -0.5);
            case ACOS -> neg(pot(resta(num(1), pot(u, 2)), -0.5));
            case ATAN -> div(num(1), suma(num(1), pot(u, 2)));
            case SENH -> fn(COSH, u);
            case COSH -> fn(SENH, u);
            case TANH -> pot(fn(COSH, u), -2);
            case LN -> div(num(1), u);
            case LOG -> div(num(1), mul(u, fn(LN, num(10))));
            case ABS -> div(u, fn(ABS, u));
        };
    }

    /** ∫ f(u) du según la tabla de integrales, o null si no hay fórmula directa. */
    public Expresion integral(Expresion u) {
        return switch (this) {
            case SEN -> neg(fn(COS, u));
            case COS -> fn(SEN, u);
            case TAN -> neg(lnAbs(fn(COS, u)));
            case COT -> lnAbs(fn(SEN, u));
            case SEC -> lnAbs(suma(fn(SEC, u), fn(TAN, u)));
            case CSC -> neg(lnAbs(suma(fn(CSC, u), fn(COT, u))));
            case ASEN -> suma(mul(u, fn(ASEN, u)), pot(resta(num(1), pot(u, 2)), 0.5));
            case ACOS -> resta(mul(u, fn(ACOS, u)), pot(resta(num(1), pot(u, 2)), 0.5));
            case ATAN -> resta(mul(u, fn(ATAN, u)), div(fn(LN, suma(num(1), pot(u, 2))), num(2)));
            case SENH -> fn(COSH, u);
            case COSH -> fn(SENH, u);
            case TANH -> fn(LN, fn(COSH, u));
            case LN -> resta(mul(u, fn(LN, u)), u);
            case LOG -> div(resta(mul(u, fn(LN, u)), u), fn(LN, num(10)));
            case ABS -> div(mul(u, fn(ABS, u)), num(2));
        };
    }

    /** ∫ f(u)² du para las funciones que tienen fórmula conocida, o null. */
    public Expresion integralCuadrado(Expresion u) {
        return switch (this) {
            case SEN -> resta(div(u, num(2)), div(fn(SEN, mul(num(2), u)), num(4)));
            case COS -> suma(div(u, num(2)), div(fn(SEN, mul(num(2), u)), num(4)));
            case TAN -> resta(fn(TAN, u), u);
            case COT -> resta(neg(fn(COT, u)), u);
            case SEC -> fn(TAN, u);
            case CSC -> neg(fn(COT, u));
            case SENH -> resta(div(fn(SENH, mul(num(2), u)), num(4)), div(u, num(2)));
            case COSH -> suma(div(fn(SENH, mul(num(2), u)), num(4)), div(u, num(2)));
            case TANH -> resta(u, fn(TANH, u));
            default -> null;
        };
    }

    /** 1/sen = csc, 1/cos = sec, 1/tan = cot ... (o null). */
    public TipoFuncion reciproca() {
        return switch (this) {
            case SEN -> CSC;
            case CSC -> SEN;
            case COS -> SEC;
            case SEC -> COS;
            case TAN -> COT;
            case COT -> TAN;
            default -> null;
        };
    }
}
