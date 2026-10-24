package calculadora.logica;

import calculadora.modelo.Constante;
import calculadora.modelo.Division;
import calculadora.modelo.Expresion;
import calculadora.modelo.Funcion;
import calculadora.modelo.Negacion;
import calculadora.modelo.Potencia;
import calculadora.modelo.Producto;
import calculadora.modelo.Resta;
import calculadora.modelo.Suma;
import calculadora.modelo.TipoFuncion;
import calculadora.modelo.Variable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Convierte el texto que escribe el usuario (por ejemplo "3x^2 + 5x") en un árbol de {@link Expresion}.
 *
 * <p>Funciona en dos pasos:</p>
 * <ol>
 *   <li><b>Análisis léxico (tokenizar):</b> separa el texto en piezas: números, nombres, operadores y paréntesis.</li>
 *   <li><b>Análisis sintáctico (descenso recursivo):</b> arma el árbol respetando la jerarquía de operaciones.</li>
 * </ol>
 *
 * <pre>
 * expresion := termino (('+' | '-') termino)*
 * termino   := unario (('*' | '/') unario | factor implícito)*      ← 3x significa 3·x
 * unario    := '-' unario | potencia
 * potencia  := primario ('^' unario)?                              ← 2^3^2 = 2^(3^2)
 * primario  := número | variable | π | e | función | '(' expresion ')' | '|' expresion '|' | '√' argumento
 * </pre>
 */
public class AnalizadorSintactico {

    private enum Tipo { NUMERO, NOMBRE, MAS, MENOS, POR, ENTRE, POTENCIA, ABRE, CIERRA, BARRA, RAIZ, FIN }

    private record Token(Tipo tipo, String texto, int posicion) {
    }

    /** Nombres de funciones aceptados (en español y en inglés). */
    private static final Map<String, TipoFuncion> FUNCIONES = new HashMap<>();
    private static final List<String> NOMBRES = new ArrayList<>();

    static {
        registrar(TipoFuncion.SEN, "sen", "sin");
        registrar(TipoFuncion.COS, "cos");
        registrar(TipoFuncion.TAN, "tan", "tg");
        registrar(TipoFuncion.COT, "cot", "ctg", "cotg");
        registrar(TipoFuncion.SEC, "sec");
        registrar(TipoFuncion.CSC, "csc", "cosec");
        registrar(TipoFuncion.ASEN, "asen", "asin", "arcsen", "arcsin");
        registrar(TipoFuncion.ACOS, "acos", "arccos");
        registrar(TipoFuncion.ATAN, "atan", "arctan", "arctg");
        registrar(TipoFuncion.SENH, "senh", "sinh");
        registrar(TipoFuncion.COSH, "cosh");
        registrar(TipoFuncion.TANH, "tanh");
        registrar(TipoFuncion.LN, "ln");
        registrar(TipoFuncion.LOG, "log");
        registrar(TipoFuncion.ABS, "abs");
        NOMBRES.add("exp");
        NOMBRES.add("sqrt");
        NOMBRES.add("raiz");
        NOMBRES.add("pi");
        NOMBRES.sort(Comparator.comparingInt(String::length).reversed()); // primero los más largos
    }

    private static void registrar(TipoFuncion tipo, String... nombres) {
        for (String n : nombres) {
            FUNCIONES.put(n, tipo);
            NOMBRES.add(n);
        }
    }

    private List<Token> tokens;
    private int indice;
    private int dentroDeValorAbsoluto;

    /** Punto de entrada: texto → árbol de expresión. */
    public Expresion analizar(String texto) throws ErrorSintaxis {
        if (texto == null || texto.isBlank()) {
            throw new ErrorSintaxis("Escribe una función, por ejemplo 3x^2 + 5x.", 0);
        }
        tokens = tokenizar(texto);
        indice = 0;
        dentroDeValorAbsoluto = 0;
        Expresion resultado = expresion();
        if (actual().tipo() != Tipo.FIN) {
            throw new ErrorSintaxis("No se esperaba \"" + actual().texto() + "\" en esa posición.", actual().posicion());
        }
        return resultado;
    }

    // ------------------------------------------------------------------ análisis léxico

    private List<Token> tokenizar(String s) throws ErrorSintaxis {
        List<Token> lista = new ArrayList<>();
        int p = 0;
        while (p < s.length()) {
            char c = s.charAt(p);
            if (Character.isWhitespace(c)) {
                p++;
                continue;
            }
            if (Character.isDigit(c) || (c == '.' && p + 1 < s.length() && Character.isDigit(s.charAt(p + 1)))) {
                int inicio = p;
                boolean conPunto = false;
                while (p < s.length()) {
                    char d = s.charAt(p);
                    boolean separadorDecimal = (d == '.' || d == ',') && !conPunto
                            && p + 1 < s.length() && Character.isDigit(s.charAt(p + 1));
                    if (Character.isDigit(d)) {
                        p++;
                    } else if (separadorDecimal) {
                        conPunto = true;
                        p++;
                    } else {
                        break;
                    }
                }
                lista.add(new Token(Tipo.NUMERO, s.substring(inicio, p).replace(',', '.'), inicio));
                continue;
            }
            if (Character.isLetter(c)) {
                int inicio = p;
                while (p < s.length() && Character.isLetter(s.charAt(p))) p++;
                separarPalabra(s.substring(inicio, p).toLowerCase(Locale.ROOT), inicio, lista);
                continue;
            }
            switch (c) {
                case '+' -> lista.add(new Token(Tipo.MAS, "+", p));
                case '-', '−' -> lista.add(new Token(Tipo.MENOS, "-", p));
                case '*', '·', '×' -> lista.add(new Token(Tipo.POR, "*", p));
                case '/', '÷' -> lista.add(new Token(Tipo.ENTRE, "/", p));
                case '^' -> lista.add(new Token(Tipo.POTENCIA, "^", p));
                case '(', '[', '{' -> lista.add(new Token(Tipo.ABRE, "(", p));
                case ')', ']', '}' -> lista.add(new Token(Tipo.CIERRA, ")", p));
                case '|' -> lista.add(new Token(Tipo.BARRA, "|", p));
                case '√' -> lista.add(new Token(Tipo.RAIZ, "√", p));
                case '²', '³' -> {
                    lista.add(new Token(Tipo.POTENCIA, "^", p));
                    lista.add(new Token(Tipo.NUMERO, c == '²' ? "2" : "3", p));
                }
                default -> throw new ErrorSintaxis("El símbolo \"" + c + "\" no es válido.", p);
            }
            p++;
        }
        lista.add(new Token(Tipo.FIN, "fin", s.length()));
        return lista;
    }

    /** "xsen" → x, sen   ·   "senx" → sen, x   ·   "pix" → pi, x */
    private void separarPalabra(String palabra, int inicio, List<Token> lista) throws ErrorSintaxis {
        int k = 0;
        while (k < palabra.length()) {
            String resto = palabra.substring(k);
            String encontrado = null;
            for (String nombre : NOMBRES) {
                if (resto.startsWith(nombre)) {
                    encontrado = nombre;
                    break;
                }
            }
            if (encontrado == null) {
                char letra = resto.charAt(0);
                if (letra == 'π' || (letra >= 'a' && letra <= 'z')) {
                    encontrado = String.valueOf(letra);
                } else {
                    throw new ErrorSintaxis("La letra \"" + letra + "\" no es válida.", inicio + k);
                }
            }
            lista.add(new Token(Tipo.NOMBRE, encontrado, inicio + k));
            k += encontrado.length();
        }
    }

    // ------------------------------------------------------------------ análisis sintáctico

    private Token actual() {
        return tokens.get(indice);
    }

    private Token avanzar() {
        return tokens.get(indice++);
    }

    private void esperar(Tipo tipo, String mensaje) throws ErrorSintaxis {
        if (actual().tipo() != tipo) {
            throw new ErrorSintaxis(mensaje, actual().posicion());
        }
        avanzar();
    }

    private Expresion expresion() throws ErrorSintaxis {
        Expresion e = termino();
        while (actual().tipo() == Tipo.MAS || actual().tipo() == Tipo.MENOS) {
            boolean esSuma = avanzar().tipo() == Tipo.MAS;
            Expresion t = termino();
            e = esSuma ? new Suma(e, t) : new Resta(e, t);
        }
        return e;
    }

    private Expresion termino() throws ErrorSintaxis {
        Expresion e = unario();
        while (true) {
            Tipo t = actual().tipo();
            if (t == Tipo.POR) {
                avanzar();
                e = new Producto(e, unario());
            } else if (t == Tipo.ENTRE) {
                avanzar();
                e = new Division(e, unario());
            } else if (iniciaFactor(actual())) {
                e = new Producto(e, potencia()); // multiplicación implícita: 3x, 2sen(x), (x+1)(x-1)
            } else {
                return e;
            }
        }
    }

    private boolean iniciaFactor(Token t) {
        return switch (t.tipo()) {
            case NUMERO, NOMBRE, ABRE, RAIZ -> true;
            case BARRA -> dentroDeValorAbsoluto == 0;
            default -> false;
        };
    }

    private Expresion unario() throws ErrorSintaxis {
        if (actual().tipo() == Tipo.MENOS) {
            avanzar();
            return new Negacion(unario());
        }
        if (actual().tipo() == Tipo.MAS) {
            avanzar();
            return unario();
        }
        return potencia();
    }

    private Expresion potencia() throws ErrorSintaxis {
        Expresion base = primario();
        if (actual().tipo() == Tipo.POTENCIA) {
            avanzar();
            return new Potencia(base, unario());
        }
        return base;
    }

    private Expresion primario() throws ErrorSintaxis {
        Token t = actual();
        switch (t.tipo()) {
            case NUMERO -> {
                avanzar();
                return new Constante(Double.parseDouble(t.texto()));
            }
            case ABRE -> {
                avanzar();
                Expresion e = expresion();
                esperar(Tipo.CIERRA, "Falta cerrar un paréntesis \")\".");
                return e;
            }
            case BARRA -> {
                avanzar();
                dentroDeValorAbsoluto++;
                Expresion e = expresion();
                dentroDeValorAbsoluto--;
                esperar(Tipo.BARRA, "Falta cerrar el valor absoluto con \"|\".");
                return new Funcion(TipoFuncion.ABS, e);
            }
            case RAIZ -> {
                avanzar();
                return new Potencia(argumento("√"), new Constante(0.5));
            }
            case NOMBRE -> {
                avanzar();
                return nombre(t);
            }
            case FIN -> throw new ErrorSintaxis("La expresión está incompleta.", t.posicion());
            default -> throw new ErrorSintaxis("No se esperaba \"" + t.texto() + "\" en esa posición.", t.posicion());
        }
    }

    private Expresion nombre(Token t) throws ErrorSintaxis {
        String n = t.texto();
        if (n.equals("pi") || n.equals("π")) return Constante.PI;
        if (n.equals("e")) return Constante.E;
        if (esFuncion(n)) {
            Expresion exponente = null;
            if (actual().tipo() == Tipo.POTENCIA) { // sen^2(x)
                avanzar();
                boolean negativo = actual().tipo() == Tipo.MENOS;
                if (negativo) avanzar();
                exponente = primario();
                if (negativo) exponente = new Negacion(exponente);
            }
            Expresion arg = argumento(n);
            Expresion f;
            if (n.equals("exp")) f = new Potencia(Constante.E, arg);
            else if (n.equals("sqrt") || n.equals("raiz")) f = new Potencia(arg, new Constante(0.5));
            else f = new Funcion(FUNCIONES.get(n), arg);
            return exponente == null ? f : new Potencia(f, exponente);
        }
        if (n.length() == 1) return new Variable(n);
        throw new ErrorSintaxis("No conozco \"" + n + "\".", t.posicion());
    }

    private static boolean esFuncion(String n) {
        return FUNCIONES.containsKey(n) || n.equals("exp") || n.equals("sqrt") || n.equals("raiz");
    }

    /** Argumento de una función: sen(x + 1), sen x, sen 2x, √x, ln|x| */
    private Expresion argumento(String funcion) throws ErrorSintaxis {
        if (actual().tipo() == Tipo.ABRE) {
            avanzar();
            Expresion e = expresion();
            esperar(Tipo.CIERRA, "Falta cerrar el paréntesis de " + funcion + "( ).");
            return e;
        }
        if (actual().tipo() == Tipo.BARRA && dentroDeValorAbsoluto == 0) {
            return primario();
        }
        if (!esArgumentoSimple(actual())) {
            throw new ErrorSintaxis("Falta lo que va dentro de " + funcion + ", por ejemplo " + funcion + "(x).",
                    actual().posicion());
        }
        Expresion e = potenciaSimple();
        while (esArgumentoSimple(actual())) {
            e = new Producto(e, potenciaSimple());
        }
        return e;
    }

    private boolean esArgumentoSimple(Token t) {
        return t.tipo() == Tipo.NUMERO || (t.tipo() == Tipo.NOMBRE && !esFuncion(t.texto()));
    }

    private Expresion potenciaSimple() throws ErrorSintaxis {
        Token t = avanzar();
        Expresion base = t.tipo() == Tipo.NUMERO ? new Constante(Double.parseDouble(t.texto())) : nombre(t);
        if (actual().tipo() == Tipo.POTENCIA) {
            avanzar();
            return new Potencia(base, unario());
        }
        return base;
    }
}
