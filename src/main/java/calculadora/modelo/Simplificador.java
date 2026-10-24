package calculadora.modelo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Reglas de simplificación algebraica que usan los nodos del árbol.
 *
 * <ul>
 *   <li><b>Sumas:</b> junta términos semejantes (3x + 2x = 5x) y suma las constantes.</li>
 *   <li><b>Productos:</b> multiplica los coeficientes, junta potencias de la misma base
 *       (x·x² = x³) y cancela factores repetidos en fracciones (x³/x = x²).</li>
 *   <li><b>Potencias y funciones:</b> x^1 = x, x^0 = 1, ln(e^u) = u, sen(0) = 0 ...</li>
 *   <li><b>Forma final:</b> si el resultado es un polinomio, lo ordena de mayor a menor grado.</li>
 * </ul>
 */
public final class Simplificador {

    private Simplificador() {
    }

    // =====================================================================
    //  SUMAS Y RESTAS
    // =====================================================================

    /** Un término de una suma: coeficiente · base (base null = número solo). */
    private record Termino(double coef, Expresion base) {
        Expresion comoExpresion() {
            if (base == null) return new Constante(coef);
            if (coef == 1) return base;
            if (coef == -1) return new Negacion(base);
            return new Producto(new Constante(coef), base);
        }
    }

    static Expresion simplificarSuma(Expresion e) {
        List<Termino> terminos = new ArrayList<>();
        recolectarTerminos(e, 1.0, terminos);

        double constante = 0;
        LinkedHashMap<Expresion, Double> semejantes = new LinkedHashMap<>();
        for (Termino t : terminos) {
            if (t.base() == null) {
                constante += t.coef();
            } else {
                semejantes.merge(t.base(), t.coef(), Double::sum);
            }
        }
        if (Double.isNaN(constante)) return new Constante(Double.NaN);

        List<Termino> resultado = new ArrayList<>();
        for (Map.Entry<Expresion, Double> en : semejantes.entrySet()) {
            double c = Numeros.ajustar(en.getValue());
            if (!Numeros.casiCero(c)) resultado.add(new Termino(c, en.getKey()));
        }
        constante = Numeros.ajustar(constante);
        if (!Numeros.casiCero(constante)) resultado.add(new Termino(constante, null));
        return construirSuma(resultado);
    }

    /** Aplana sumas y restas anidadas en una lista de términos con signo. */
    private static void recolectarTerminos(Expresion e, double signo, List<Termino> salida) {
        if (e instanceof Suma s) {
            recolectarTerminos(s.getIzquierda(), signo, salida);
            recolectarTerminos(s.getDerecha(), signo, salida);
            return;
        }
        if (e instanceof Resta r) {
            recolectarTerminos(r.getIzquierda(), signo, salida);
            recolectarTerminos(r.getDerecha(), -signo, salida);
            return;
        }
        if (e instanceof Negacion n) {
            recolectarTerminos(n.getOperando(), -signo, salida);
            return;
        }
        Expresion s = e.simplificar();
        if (s instanceof Suma || s instanceof Resta || s instanceof Negacion) {
            recolectarTerminos(s, signo, salida);
            return;
        }
        if (s instanceof Constante c && c.esNumerica()) {
            salida.add(new Termino(signo * c.getValor(), null));
            return;
        }
        if (s instanceof Producto p && p.getIzquierda() instanceof Constante c && c.esNumerica()) {
            Expresion base = p.getDerecha();
            if (base instanceof Suma || base instanceof Resta) {
                // número·(a + b) dentro de una suma: se reparte para poder juntar semejantes
                recolectarTerminos(base, signo * c.getValor(), salida);
            } else {
                salida.add(new Termino(signo * c.getValor(), base));
            }
            return;
        }
        salida.add(new Termino(signo, s));
    }

    private static Expresion construirSuma(List<Termino> terminos) {
        if (terminos.isEmpty()) return Constante.CERO;
        Expresion acumulado = null;
        for (Termino t : terminos) {
            if (acumulado == null) {
                acumulado = t.comoExpresion();
            } else if (t.coef() < 0) {
                acumulado = new Resta(acumulado, new Termino(-t.coef(), t.base()).comoExpresion());
            } else {
                acumulado = new Suma(acumulado, t.comoExpresion());
            }
        }
        return acumulado;
    }

    // =====================================================================
    //  PRODUCTOS, DIVISIONES Y NEGATIVOS
    // =====================================================================

    static Expresion simplificarProducto(Expresion e) {
        double[] coeficiente = {1.0};
        List<Expresion> numerador = new ArrayList<>();
        List<Expresion> denominador = new ArrayList<>();
        recolectarFactores(e, false, coeficiente, numerador, denominador);
        if (!Double.isFinite(coeficiente[0])) return new Constante(Double.NaN);
        if (coeficiente[0] == 0) return Constante.CERO;

        LinkedHashMap<Expresion, Expresion> arriba = agrupar(numerador);
        LinkedHashMap<Expresion, Expresion> abajo = agrupar(denominador);
        cancelar(arriba, abajo);

        List<Expresion> factoresArriba = construirFactores(arriba, coeficiente, false);
        List<Expresion> factoresAbajo = construirFactores(abajo, coeficiente, true);
        if (!Double.isFinite(coeficiente[0])) return new Constante(Double.NaN);
        if (coeficiente[0] == 0) return Constante.CERO;
        factoresArriba.sort(ORDEN);
        factoresAbajo.sort(ORDEN);

        Expresion n = multiplicar(factoresArriba);
        Expresion d = multiplicar(factoresAbajo);
        Expresion resto = d == null ? n : new Division(n == null ? Constante.UNO : n, d);
        double c = Numeros.ajustar(coeficiente[0]);
        if (resto == null) return new Constante(c);
        if (c == 1) return resto;
        if (c == -1) return new Negacion(resto);
        return new Producto(new Constante(c), resto);
    }

    /** Aplana productos/divisiones: separa el coeficiente numérico y los factores de arriba y de abajo. */
    private static void recolectarFactores(Expresion e, boolean invertido, double[] coef,
                                           List<Expresion> arriba, List<Expresion> abajo) {
        if (e instanceof Producto p) {
            recolectarFactores(p.getIzquierda(), invertido, coef, arriba, abajo);
            recolectarFactores(p.getDerecha(), invertido, coef, arriba, abajo);
            return;
        }
        if (e instanceof Division d) {
            recolectarFactores(d.getNumerador(), invertido, coef, arriba, abajo);
            recolectarFactores(d.getDenominador(), !invertido, coef, arriba, abajo);
            return;
        }
        if (e instanceof Negacion n) {
            coef[0] = -coef[0];
            recolectarFactores(n.getOperando(), invertido, coef, arriba, abajo);
            return;
        }
        Expresion s = e.simplificar();
        if (s instanceof Producto || s instanceof Division || s instanceof Negacion) {
            recolectarFactores(s, invertido, coef, arriba, abajo);
            return;
        }
        if (s instanceof Constante c && c.esNumerica()) {
            coef[0] = invertido ? coef[0] / c.getValor() : coef[0] * c.getValor();
            return;
        }
        // x^(-n) se pasa al otro lado como x^n
        if (s instanceof Potencia p && p.getExponente() instanceof Constante k && k.esNumerica() && k.getValor() < 0) {
            Expresion invertida = new Potencia(p.getBase(), new Constante(-k.getValor())).simplificar();
            recolectarFactores(invertida, !invertido, coef, arriba, abajo);
            return;
        }
        (invertido ? abajo : arriba).add(s);
    }

    /** Junta potencias de la misma base sumando exponentes: x²·x = x³, eˣ·e²ˣ = e³ˣ. */
    private static LinkedHashMap<Expresion, Expresion> agrupar(List<Expresion> factores) {
        LinkedHashMap<Expresion, Expresion> mapa = new LinkedHashMap<>();
        for (Expresion f : factores) {
            Expresion base = f;
            Expresion exponente = Constante.UNO;
            if (f instanceof Potencia p) {
                base = p.getBase();
                exponente = p.getExponente();
            }
            Expresion previo = mapa.get(base);
            mapa.put(base, previo == null ? exponente : new Suma(previo, exponente).simplificar());
        }
        return mapa;
    }

    /** Cancela bases que están arriba y abajo: x³/x = x². */
    private static void cancelar(Map<Expresion, Expresion> arriba, Map<Expresion, Expresion> abajo) {
        for (Expresion base : new ArrayList<>(arriba.keySet())) {
            Expresion exponenteAbajo = abajo.get(base);
            if (exponenteAbajo == null) continue;
            Expresion diferencia = new Resta(arriba.get(base), exponenteAbajo).simplificar();
            arriba.remove(base);
            abajo.remove(base);
            if (diferencia instanceof Constante k && k.esNumerica()) {
                if (k.getValor() > 0) arriba.put(base, diferencia);
                else if (k.getValor() < 0) abajo.put(base, new Constante(-k.getValor()));
            } else {
                arriba.put(base, diferencia);
            }
        }
    }

    private static List<Expresion> construirFactores(Map<Expresion, Expresion> mapa, double[] coef, boolean abajo) {
        List<Expresion> lista = new ArrayList<>();
        for (Map.Entry<Expresion, Expresion> en : mapa.entrySet()) {
            Expresion exponente = en.getValue();
            if (exponente.esCero()) continue;
            Expresion factor = exponente.esUno() ? en.getKey() : new Potencia(en.getKey(), exponente).simplificar();
            if (factor instanceof Constante c && c.esNumerica()) {
                coef[0] = abajo ? coef[0] / c.getValor() : coef[0] * c.getValor();
                continue;
            }
            lista.add(factor);
        }
        return lista;
    }

    private static Expresion multiplicar(List<Expresion> factores) {
        Expresion acumulado = null;
        for (Expresion f : factores) {
            acumulado = acumulado == null ? f : new Producto(acumulado, f);
        }
        return acumulado;
    }

    /** Orden de los factores al escribir: constantes, variables, exponenciales, funciones, el resto. */
    private static final Comparator<Expresion> ORDEN = Comparator
            .comparingInt(Simplificador::categoria)
            .thenComparing(f -> (f instanceof Potencia p ? p.getBase() : f).toString());

    private static int categoria(Expresion f) {
        Expresion base = f instanceof Potencia p ? p.getBase() : f;
        if (base instanceof Constante) {
            return f instanceof Potencia p && !p.getExponente().esConstante() ? 2 : 0;
        }
        if (base instanceof Variable) return 1;
        if (base instanceof Funcion) return 3;
        return 4;
    }

    // =====================================================================
    //  POTENCIAS
    // =====================================================================

    static Expresion simplificarPotencia(Potencia p) {
        Expresion b = p.getBase().simplificar();
        Expresion x = p.getExponente().simplificar();

        if (x instanceof Constante k && k.esNumerica()) {
            double n = k.getValor();
            if (n == 0) return Constante.UNO;
            if (n == 1) return b;
            if (b instanceof Constante cb && cb.esNumerica()) {
                double r = Potencia.potenciaReal(cb.getValor(), n);
                boolean bonito = Numeros.esEntero(n) || Numeros.esEntero(r) || Fraccion.aproximar(r, 100, 1e-12) != null;
                if (Double.isFinite(r) && bonito) return new Constante(Numeros.ajustar(r));
            }
            if (b.esCero() && n > 0) return Constante.CERO;
            if (b.esUno()) return Constante.UNO;
            // (x^m)^n = x^(m·n)
            if (b instanceof Potencia bp && bp.getExponente() instanceof Constante m && m.esNumerica()) {
                return new Potencia(bp.getBase(), new Constante(m.getValor() * n)).simplificar();
            }
            // (k·u)^n = k^n · u^n
            if (b instanceof Producto bp && bp.getIzquierda() instanceof Constante c && c.esNumerica()
                    && (c.getValor() > 0 || Numeros.esEntero(n))) {
                return simplificarProducto(new Producto(
                        new Potencia(c, x), new Potencia(bp.getDerecha(), x)));
            }
            // (−u)^n con n entero
            if (b instanceof Negacion ng && Numeros.esEntero(n)) {
                Expresion positiva = new Potencia(ng.getOperando(), x).simplificar();
                return Math.round(n) % 2 == 0 ? positiva : new Negacion(positiva).simplificar();
            }
        }
        if (b.esUno()) return Constante.UNO;
        // e^(ln u) = u
        if (b.equals(Constante.E) && x instanceof Funcion f && f.getTipo() == TipoFuncion.LN) {
            return f.getArgumento();
        }
        // (a^u)^v = a^(u·v) cuando a es una constante positiva (por ejemplo (e^x)^2 = e^(2x))
        if (b instanceof Potencia bp && bp.getBase() instanceof Constante a && a.getValor() > 0) {
            return new Potencia(a, new Producto(bp.getExponente(), x)).simplificar();
        }
        return new Potencia(b, x);
    }

    // =====================================================================
    //  FUNCIONES
    // =====================================================================

    static Expresion simplificarFuncion(Funcion f) {
        Expresion a = f.getArgumento().simplificar();
        TipoFuncion t = f.getTipo();
        if (a.esConstante()) {
            double r = t.aplicar(a.valorConstante());
            if (Double.isFinite(r) && Numeros.esEntero(r)) return new Constante(Math.rint(r));
            if (t == TipoFuncion.ABS && a instanceof Constante c && c.esNumerica()) return new Constante(Math.abs(c.getValor()));
            // atan(1) = π/4, asen(1/2) = π/6 ...
            if (t == TipoFuncion.ASEN || t == TipoFuncion.ACOS || t == TipoFuncion.ATAN) {
                Fraccion vecesPi = Fraccion.aproximar(r / Math.PI, 12, 1e-12);
                if (vecesPi != null) return new Producto(new Constante(vecesPi.valor()), Constante.PI);
            }
        }
        // ln(e^u) = u
        if (t == TipoFuncion.LN && a instanceof Potencia p && p.getBase().equals(Constante.E)) {
            return p.getExponente();
        }
        if (t == TipoFuncion.ABS) {
            if (a instanceof Negacion n) return new Funcion(TipoFuncion.ABS, n.getOperando()).simplificar();
            if (noNegativo(a)) return a; // |x² + 1| = x² + 1
        }
        // paridad: cos(−u) = cos(u), sen(−u) = −sen(u)
        if (a instanceof Negacion n) {
            if (t == TipoFuncion.COS || t == TipoFuncion.SEC || t == TipoFuncion.COSH) {
                return new Funcion(t, n.getOperando()).simplificar();
            }
            if (t == TipoFuncion.SEN || t == TipoFuncion.TAN || t == TipoFuncion.SENH || t == TipoFuncion.TANH) {
                return new Negacion(new Funcion(t, n.getOperando())).simplificar();
            }
        }
        return new Funcion(t, a);
    }

    /** true si la expresión nunca es negativa (para quitar el valor absoluto: ln|x² + 1| = ln(x² + 1)). */
    static boolean noNegativo(Expresion e) {
        if (e instanceof Constante c) return c.getValor() >= 0;
        if (e instanceof Funcion f) return f.getTipo() == TipoFuncion.ABS || f.getTipo() == TipoFuncion.COSH;
        if (e instanceof Potencia p) {
            if (p.getBase() instanceof Constante c && c.getValor() > 0) return true;
            if (p.getExponente() instanceof Constante k && k.esNumerica()) {
                double n = k.getValor();
                if (Numeros.esEntero(n) && Math.round(n) % 2 == 0) return true;
                Fraccion fr = Fraccion.aproximar(n);
                return fr != null && fr.getDenominador() % 2 == 0; // raíz cuadrada
            }
            return false;
        }
        if (e instanceof Suma s) return noNegativo(s.getIzquierda()) && noNegativo(s.getDerecha());
        if (e instanceof Producto p) return noNegativo(p.getIzquierda()) && noNegativo(p.getDerecha());
        if (e instanceof Division d) return noNegativo(d.getNumerador()) && noNegativo(d.getDenominador());
        if (e.variables().size() == 1) {
            String v = e.variables().iterator().next();
            Polinomio p = Polinomio.desde(e, v);
            if (p != null && p.grado() == 2) {
                double a = p.coef(2), b = p.coef(1), c = p.coef(0);
                return a > 0 && b * b - 4 * a * c < 0;
            }
        }
        return false;
    }

    // =====================================================================
    //  FORMA FINAL PARA MOSTRAR
    // =====================================================================

    /**
     * Simplifica y además ordena los polinomios (6x + 5 en vez de 5 + 6x).
     * Si expandir alarga la expresión (por ejemplo 20x(x² + 1)^9) se deja factorizada.
     */
    public static Expresion formaFinal(Expresion e, String variable) {
        Expresion simplificada = e.simplificar();
        Expresion mejor = canonica(simplificada, variable);
        // a veces expandir los productos deja algo más corto: e^x·(x + 1) - e^x  →  x·e^x
        List<Expresion> terminos = expandirTerminos(simplificada);
        if (terminos.size() > 1) {
            Expresion expandida = canonica(sumar(terminos).simplificar(), variable);
            if (expandida.toString().length() < mejor.toString().length()) {
                mejor = expandida;
            }
        }
        return mejor;
    }

    private static Expresion canonica(Expresion s, String v) {
        Expresion alternativa = canonicaHijos(s, v);
        Polinomio p = Polinomio.desde(s, v);
        if (p != null) {
            Expresion polinomio = p.aExpresion(v);
            if (polinomio.toString().length() < alternativa.toString().length()) {
                return polinomio;
            }
        }
        return alternativa;
    }

    /**
     * Reparte productos sobre sumas: (a + b)·c → a·c + b·c. También (a + b)/c y (a + b)² … (a + b)⁴.
     * Devuelve la lista de términos (sin simplificar).
     */
    public static List<Expresion> expandirTerminos(Expresion e) {
        if (e instanceof Suma s) return unir(expandirTerminos(s.getIzquierda()), expandirTerminos(s.getDerecha()));
        if (e instanceof Resta r) return unir(expandirTerminos(r.getIzquierda()), negar(expandirTerminos(r.getDerecha())));
        if (e instanceof Negacion n) return negar(expandirTerminos(n.getOperando()));
        if (e instanceof Producto p) return combinar(expandirTerminos(p.getIzquierda()), expandirTerminos(p.getDerecha()));
        if (e instanceof Division d) {
            List<Expresion> lista = new ArrayList<>();
            for (Expresion t : expandirTerminos(d.getNumerador())) lista.add(new Division(t, d.getDenominador()));
            return lista;
        }
        if (e instanceof Potencia p && p.getExponente() instanceof Constante k && k.esEntero()
                && k.getValor() >= 2 && k.getValor() <= 4
                && (p.getBase() instanceof Suma || p.getBase() instanceof Resta)) {
            List<Expresion> base = expandirTerminos(p.getBase());
            List<Expresion> r = base;
            for (int i = 1; i < (int) k.getValor(); i++) r = combinar(r, base);
            return r;
        }
        List<Expresion> uno = new ArrayList<>();
        uno.add(e);
        return uno;
    }

    private static List<Expresion> unir(List<Expresion> a, List<Expresion> b) {
        List<Expresion> r = new ArrayList<>(a);
        r.addAll(b);
        return r;
    }

    private static List<Expresion> negar(List<Expresion> a) {
        List<Expresion> r = new ArrayList<>();
        for (Expresion t : a) r.add(new Negacion(t));
        return r;
    }

    private static List<Expresion> combinar(List<Expresion> a, List<Expresion> b) {
        List<Expresion> r = new ArrayList<>();
        if (a.size() * b.size() > 64) {
            r.add(new Producto(sumar(a), sumar(b)));
            return r;
        }
        for (Expresion x : a) for (Expresion y : b) r.add(new Producto(x, y));
        return r;
    }

    public static Expresion sumar(List<Expresion> terminos) {
        Expresion r = null;
        for (Expresion t : terminos) r = r == null ? t : new Suma(r, t);
        return r == null ? Constante.CERO : r;
    }

    private static Expresion canonicaHijos(Expresion s, String v) {
        if (s instanceof Division d) return new Division(canonica(d.getNumerador(), v), canonica(d.getDenominador(), v));
        if (s instanceof Producto p) return new Producto(canonica(p.getIzquierda(), v), canonica(p.getDerecha(), v));
        if (s instanceof Negacion n) return new Negacion(canonica(n.getOperando(), v));
        if (s instanceof Suma x) return new Suma(canonica(x.getIzquierda(), v), canonica(x.getDerecha(), v));
        if (s instanceof Resta r) return new Resta(canonica(r.getIzquierda(), v), canonica(r.getDerecha(), v));
        if (s instanceof Funcion f) return new Funcion(f.getTipo(), canonica(f.getArgumento(), v));
        if (s instanceof Potencia p) return new Potencia(canonica(p.getBase(), v), p.getExponente());
        return s;
    }
}
