package calculadora.logica;

import calculadora.modelo.Constante;
import calculadora.modelo.Division;
import calculadora.modelo.Expresion;
import calculadora.modelo.Funcion;
import calculadora.modelo.Negacion;
import calculadora.modelo.Numeros;
import calculadora.modelo.Polinomio;
import calculadora.modelo.Potencia;
import calculadora.modelo.Producto;
import calculadora.modelo.Resta;
import calculadora.modelo.Simplificador;
import calculadora.modelo.Suma;
import calculadora.modelo.TipoFuncion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static calculadora.modelo.Ops.*;

/**
 * Busca una antiderivada F(x) tal que F'(x) = f(x), usando las mismas técnicas que se ven en Cálculo.
 *
 * <p>Las técnicas se prueban en este orden:</p>
 * <ol>
 *   <li>Integral de una constante.</li>
 *   <li>Regla de la potencia (polinomios, término a término).</li>
 *   <li>Linealidad: se separa la suma en integrales más pequeñas.</li>
 *   <li>Sacar las constantes multiplicativas.</li>
 *   <li>Funciones racionales: división de polinomios + fracciones parciales.</li>
 *   <li>Raíces de cuadráticas: completar el cuadrado (asen, ln).</li>
 *   <li>Tabla de integrales con sustitución u = g(x).</li>
 *   <li>e^(ax)·sen(bx) y e^(ax)·cos(bx).</li>
 *   <li>Integración por partes (método tabular y LIATE).</li>
 *   <li>Expandir productos y volver a intentar.</li>
 * </ol>
 *
 * <p>Toda respuesta se comprueba numéricamente (derivando F y comparando con f) antes de aceptarla.</p>
 */
public class Integrador {

    private static final int PROFUNDIDAD_MAXIMA = 6;
    private static final int PASOS_MAXIMOS = 20000;
    /** Puntos de prueba (no enteros para no caer justo en una asíntota). */
    private static final double[] PUNTOS = {0.3172, 0.7215, 1.2731, 1.8463, 2.5128, -0.4389, -1.1567,
            -2.3341, 3.0717, 0.0913, -0.8123, 1.5539, 4.2911, 5.8317, 7.6123, 10.3719, -3.7151, -6.1337};

    private final Set<String> metodos = new LinkedHashSet<>();
    private int pasos;

    /** Una forma posible de ver un factor como F(g(x)): interior = g(x), antiderivada = F(u). */
    private record Candidato(Expresion interior, Function<Expresion, Expresion> antiderivada) {
    }

    /** Devuelve la antiderivada (sin la constante C) o null si no se encontró. */
    public Expresion integrar(Expresion f, String variable) {
        metodos.clear();
        pasos = 0;
        Expresion g = f.simplificar();
        Expresion resultado;
        try {
            resultado = intentar(g, variable, 0);
        } catch (StackOverflowError | ArithmeticException e) {
            resultado = null;
        }
        if (resultado == null) {
            metodos.clear();
            return null;
        }
        resultado = Simplificador.formaFinal(resultado, variable);
        if (!esAntiderivada(resultado, g, variable)) {
            metodos.clear();
            return null;
        }
        return resultado;
    }

    /** Técnicas que se usaron en la última integral (para mostrarlas en pantalla). */
    public List<String> getMetodos() {
        return new ArrayList<>(metodos);
    }

    private void usar(String metodo) {
        metodos.add(metodo);
    }

    /** Ejecuta una técnica; si falla, se olvidan los métodos que había anotado. */
    private Expresion probar(Supplier<Expresion> tecnica) {
        Set<String> antes = new LinkedHashSet<>(metodos);
        Expresion r = tecnica.get();
        if (r == null) {
            metodos.clear();
            metodos.addAll(antes);
        }
        return r;
    }

    private Expresion intentar(Expresion f, String v, int profundidad) {
        if (profundidad > PROFUNDIDAD_MAXIMA || ++pasos > PASOS_MAXIMOS) return null;
        Expresion g = f.simplificar();

        // 1. ∫k dx = kx
        if (!g.contieneVariable(v)) {
            usar("Integral de una constante: ∫k dx = kx");
            return mul(g, var(v));
        }
        // 2. Polinomio: ∫xⁿ dx = xⁿ⁺¹/(n+1)
        Polinomio p = Polinomio.desde(g, v);
        if (p != null) {
            usar("Regla de la potencia: ∫xⁿ dx = xⁿ⁺¹/(n+1)");
            return p.integrar().aExpresion(v);
        }
        // 3. Linealidad
        List<Expresion> terminos = new ArrayList<>();
        terminos(g, false, terminos);
        if (terminos.size() > 1) {
            return probar(() -> linealidad(terminos, v, profundidad));
        }
        // 4. Sacar constantes
        List<Expresion> factores = new ArrayList<>();
        aplanar(g, false, factores);
        List<Expresion> constantes = new ArrayList<>();
        List<Expresion> variables = new ArrayList<>();
        for (Expresion x : factores) {
            (x.contieneVariable(v) ? variables : constantes).add(x);
        }
        Expresion k = producto(constantes).simplificar();
        if (!constantes.isEmpty() && !k.esUno()) {
            return probar(() -> {
                Expresion r = intentar(producto(variables), v, profundidad);
                if (r == null) return null;
                usar("Sacar la constante: ∫k·f dx = k·∫f dx");
                return mul(k, r);
            });
        }
        Expresion r;
        if ((r = probar(() -> racional(g, v))) != null) return r;
        if ((r = probar(() -> raizCuadratica(variables, v))) != null) return r;
        if ((r = probar(() -> sustitucion(variables, v))) != null) return r;
        if ((r = probar(() -> exponencialPorTrigonometrica(variables, v))) != null) return r;
        if ((r = probar(() -> porPartes(variables, v, profundidad))) != null) return r;
        if ((r = probar(() -> identidadTrigonometrica(variables, v, profundidad))) != null) return r;
        return probar(() -> expandir(g, v, profundidad));
    }

    // ------------------------------------------------------------------ 3. linealidad

    private Expresion linealidad(List<Expresion> terminos, String v, int profundidad) {
        List<Expresion> partes = new ArrayList<>();
        for (Expresion t : terminos) {
            Expresion r = intentar(t, v, profundidad);
            if (r == null) return null;
            partes.add(r);
        }
        usar("Linealidad: ∫(f ± g) dx = ∫f dx ± ∫g dx");
        return sumar(partes);
    }

    // ------------------------------------------------------------------ 5. funciones racionales

    private Expresion racional(Expresion g, String v) {
        if (!(g instanceof Division d)) return null;
        Polinomio numerador = Polinomio.desde(d.getNumerador(), v);
        Polinomio denominador = Polinomio.desde(d.getDenominador(), v);
        if (numerador == null || denominador == null || denominador.grado() < 1) return null;

        List<Expresion> partes = new ArrayList<>();
        Polinomio[] cocienteYResiduo = numerador.dividir(denominador);
        if (!cocienteYResiduo[0].esCero()) {
            usar("División de polinomios");
            partes.add(cocienteYResiduo[0].integrar().aExpresion(v));
        }
        if (!cocienteYResiduo[1].esCero()) {
            Expresion propia = fraccionPropia(cocienteYResiduo[1], denominador, v);
            if (propia == null) return null;
            partes.add(propia);
        }
        return sumar(partes);
    }

    /** ∫ R(x)/D(x) dx con grado(R) < grado(D). */
    private Expresion fraccionPropia(Polinomio residuo, Polinomio denominador, String v) {
        Expresion x = var(v);
        int n = denominador.grado();
        if (n == 1) {
            usar("Integral de 1/u: ∫ du/u = ln|u|");
            double coeficiente = Numeros.ajustar(residuo.coef(0) / denominador.coef(1));
            return mul(num(coeficiente), lnAbs(denominador.aExpresion(v)));
        }
        Complejo[] raices = Raices.calcular(denominador);
        if (sonDistintas(raices)) {
            // R/D = Σ A/(x − r)  con  A = R(r)/D'(r)
            usar("Fracciones parciales");
            Polinomio derivada = denominador.derivar();
            List<Expresion> partes = new ArrayList<>();
            for (Complejo z : raices) {
                Complejo a = Raices.evaluar(residuo, z).dividir(Raices.evaluar(derivada, z));
                if (z.im() == 0) {
                    double raiz = Numeros.ajustar(z.re());
                    partes.add(mul(num(Numeros.ajustar(a.re())), lnAbs(resta(x, num(raiz)))));
                } else if (z.im() > 0) {
                    // par de raíces complejas α ± βi → término con ln y con atan
                    double alfa = Numeros.ajustar(z.re()), beta = Numeros.ajustar(z.im());
                    double s = Numeros.ajustar(a.re()), t = Numeros.ajustar(a.im());
                    Polinomio cuadratico = new Polinomio(alfa * alfa + beta * beta, -2 * alfa, 1);
                    if (!Numeros.casiCero(s)) {
                        partes.add(mul(num(s), fn(TipoFuncion.LN, cuadratico.aExpresion(v))));
                    }
                    if (!Numeros.casiCero(t)) {
                        partes.add(mul(num(-2 * t), fn(TipoFuncion.ATAN, div(resta(x, num(alfa)), num(beta)))));
                    }
                }
            }
            return sumar(partes);
        }
        // Una sola raíz repetida: D = a·(x − h)ⁿ  → sustitución w = x − h
        double h = 0;
        for (Complejo z : raices) {
            if (Math.abs(z.im()) > 1e-6) return null;
            h += z.re();
        }
        h = Numeros.ajustar(h / n);
        Polinomio esperado = new Polinomio(-h, 1).potencia(n).escalar(denominador.coef(n));
        if (!denominador.casiIgual(esperado, 1e-7)) return null;
        Expresion w = resta(x, num(h)).simplificar();
        usar("Sustitución w = " + w);
        Polinomio enW = residuo.trasladar(h);
        double lider = denominador.coef(n);
        List<Expresion> partes = new ArrayList<>();
        for (int k = 0; k <= enW.grado(); k++) {
            double ck = Numeros.ajustar(enW.coef(k) / lider);
            if (Numeros.casiCero(ck)) continue;
            int exponente = k - n;
            if (exponente == -1) {
                partes.add(mul(num(ck), lnAbs(w)));
            } else {
                partes.add(mul(num(ck / (exponente + 1)), pot(w, exponente + 1)));
            }
        }
        return sumar(partes);
    }

    private static boolean sonDistintas(Complejo[] raices) {
        for (int i = 0; i < raices.length; i++) {
            for (int j = i + 1; j < raices.length; j++) {
                double distancia = raices[i].restar(raices[j]).modulo();
                if (distancia < 1e-4 * Math.max(1, raices[i].modulo())) return false;
            }
        }
        return true;
    }

    // ------------------------------------------------------------------ 6. raíces de cuadráticas

    /** ∫ √(ax² + bx + c) dx  y  ∫ 1/√(ax² + bx + c) dx completando el cuadrado. */
    private Expresion raizCuadratica(List<Expresion> factores, String v) {
        if (factores.size() != 1 || !(factores.get(0) instanceof Potencia p)) return null;
        if (!(p.getExponente() instanceof Constante k) || !k.esNumerica()) return null;
        double n = k.getValor();
        if (Math.abs(Math.abs(n) - 0.5) > 1e-12) return null;
        Polinomio q = Polinomio.desde(p.getBase(), v);
        if (q == null || q.grado() != 2) return null;

        double a2 = q.coef(2), b = q.coef(1), c = q.coef(0);
        double h = Numeros.ajustar(-b / (2 * a2));
        double resto = Numeros.ajustar(c - b * b / (4 * a2)); // Q = a2·(x − h)² + resto
        Expresion w = resta(var(v), num(h));
        Expresion raizQ = pot(q.aExpresion(v), 0.5);
        double a = Math.sqrt(Math.abs(a2));
        boolean negativo = a2 < 0;
        if (negativo && resto <= 0) return null;

        Expresion especial = negativo
                ? fn(TipoFuncion.ASEN, div(mul(num(a), w), num(Math.sqrt(resto))))
                : lnAbs(suma(mul(num(a), w), raizQ));
        if (n < 0) {
            usar("Completar el cuadrado: " + (negativo ? "∫ du/√(a² − u²) = asen(u/a)" : "∫ du/√(u² ± a²) = ln|u + √(u² ± a²)|"));
        } else {
            usar("Completar el cuadrado: " + (negativo ? "∫ √(a² − u²) du" : "∫ √(u² ± a²) du"));
        }
        if (n < 0) {
            return mul(num(1 / a), especial);
        }
        return suma(div(mul(w, raizQ), num(2)), mul(num(resto / (2 * a)), especial));
    }

    // ------------------------------------------------------------------ 7. tabla + sustitución

    private Expresion sustitucion(List<Expresion> factores, String v) {
        for (int i = 0; i < factores.size(); i++) {
            Expresion fi = factores.get(i);
            Expresion resto = productoExcepto(factores, i);
            for (Candidato c : candidatos(fi, v)) {
                Expresion derivadaInterior = c.interior().derivar(v).simplificar();
                if (derivadaInterior.esCero()) continue;
                Expresion k = cocienteConstante(resto, derivadaInterior, v); // resto = k·g'(x) ?
                if (k == null) continue;
                Expresion antiderivada = c.antiderivada().apply(c.interior());
                if (antiderivada == null) continue;
                if (c.interior().equals(var(v))) {
                    usar("Tabla de integrales");
                } else {
                    usar("Sustitución u = " + c.interior());
                }
                return k.esUno() ? antiderivada : mul(k, antiderivada);
            }
        }
        // ¿La función completa es k·(derivada de uno de sus factores)?  (sec·tan → sec)
        Expresion todo = producto(factores);
        for (Expresion fi : factores) {
            if (!(fi instanceof Funcion)) continue;
            Expresion d = fi.derivar(v).simplificar();
            if (d.esCero()) continue;
            Expresion k = cocienteConstante(todo, d, v);
            if (k != null) {
                usar("Reconocer una derivada: ∫ f'(x) dx = f(x)");
                return mul(k, fi);
            }
        }
        return null;
    }

    /** Formas de ver un factor como F(u) con u = g(x). */
    private List<Candidato> candidatos(Expresion fi, String v) {
        List<Candidato> lista = new ArrayList<>();
        if (fi instanceof Funcion f && f.getTipo().integral(var(v)) != null) {
            lista.add(new Candidato(f.getArgumento(), u -> f.getTipo().integral(u)));
        }
        if (fi instanceof Potencia p) {
            Expresion base = p.getBase();
            Expresion exponente = p.getExponente();
            if (exponente instanceof Constante k && k.esNumerica() && base.contieneVariable(v)) {
                double n = k.getValor();
                if (base instanceof Funcion bf) {
                    TipoFuncion tipo = bf.getTipo();
                    TipoFuncion reciproca = tipo.reciproca();
                    if (Math.abs(n - 2) < 1e-12 && tipo.integralCuadrado(var(v)) != null) {
                        lista.add(new Candidato(bf.getArgumento(), tipo::integralCuadrado));
                    }
                    if (Math.abs(n + 1) < 1e-12 && reciproca != null) {
                        lista.add(new Candidato(bf.getArgumento(), reciproca::integral));
                    }
                    if (Math.abs(n + 2) < 1e-12 && reciproca != null && reciproca.integralCuadrado(var(v)) != null) {
                        lista.add(new Candidato(bf.getArgumento(), reciproca::integralCuadrado));
                    }
                }
                // ∫uⁿ du = uⁿ⁺¹/(n+1)  ó  ln|u| si n = −1
                lista.add(new Candidato(base, u -> Math.abs(n + 1) < 1e-12
                        ? lnAbs(u)
                        : div(pot(u, n + 1), num(n + 1))));
            }
            if (!base.contieneVariable(v) && base.esConstante() && exponente.contieneVariable(v)) {
                // ∫aᵘ du = aᵘ/ln(a)   (si a = e queda eᵘ)
                lista.add(new Candidato(exponente, u -> div(pot(base, u), fn(TipoFuncion.LN, base))));
            }
        }
        lista.add(new Candidato(fi, u -> div(pot(u, 2), num(2)))); // ∫u du = u²/2
        return lista;
    }

    /** Si a/b no depende de la variable, devuelve ese cociente; si no, null. */
    private Expresion cocienteConstante(Expresion a, Expresion b, String v) {
        Expresion q = div(a, b).simplificar();
        if (!q.contieneVariable(v)) return q;
        if (q.variables().size() != 1) return null;
        // el simplificador no siempre ve que es constante: se revisa numéricamente
        double referencia = Double.NaN;
        int validos = 0;
        for (double x : PUNTOS) {
            double y = q.evaluar(v, x);
            if (!Double.isFinite(y)) continue;
            if (validos == 0) {
                referencia = y;
            } else if (!Numeros.casiIguales(y, referencia, 1e-9)) {
                return null;
            }
            validos++;
        }
        return validos >= 4 ? num(Numeros.ajustar(referencia)) : null;
    }

    // ------------------------------------------------------------------ 8. e^(ax)·sen(bx)

    private Expresion exponencialPorTrigonometrica(List<Expresion> factores, String v) {
        if (factores.size() != 2) return null;
        for (int i = 0; i < 2; i++) {
            if (!(factores.get(i) instanceof Potencia exp) || !(factores.get(1 - i) instanceof Funcion trig)) continue;
            if (!exp.getBase().esConstante() || exp.getBase().contieneVariable(v)) continue;
            if (trig.getTipo() != TipoFuncion.SEN && trig.getTipo() != TipoFuncion.COS) continue;
            Polinomio lineal1 = Polinomio.desde(exp.getExponente(), v);
            Polinomio lineal2 = Polinomio.desde(trig.getArgumento(), v);
            if (lineal1 == null || lineal2 == null || lineal1.grado() != 1 || lineal2.grado() != 1) continue;
            double a = lineal1.coef(1) * Math.log(exp.getBase().valorConstante());
            double b = lineal2.coef(1);
            Expresion sen = fn(TipoFuncion.SEN, trig.getArgumento());
            Expresion cos = fn(TipoFuncion.COS, trig.getArgumento());
            Expresion cuerpo = trig.getTipo() == TipoFuncion.SEN
                    ? resta(mul(num(a), sen), mul(num(b), cos))
                    : suma(mul(num(a), cos), mul(num(b), sen));
            usar("Integración por partes dos veces (la integral se repite)");
            return div(mul(exp, cuerpo), num(a * a + b * b));
        }
        return null;
    }

    // ------------------------------------------------------------------ 9. por partes

    private Expresion porPartes(List<Expresion> factores, String v, int profundidad) {
        for (int i = 0; i < factores.size(); i++) {
            Expresion t = factores.get(i);
            Polinomio polinomio = Polinomio.desde(productoExcepto(factores, i).simplificar(), v);
            if (polinomio == null) continue;

            // Método tabular: ∫P(x)·T(x) dx = P·T₁ − P'·T₂ + P''·T₃ − ...
            if (esCiclica(t, v) && polinomio.grado() >= 1) {
                List<Expresion> integrales = new ArrayList<>();
                Expresion actual = t;
                for (int k = 0; k <= polinomio.grado(); k++) {
                    actual = intentar(actual, v, profundidad + 1);
                    if (actual == null) return null;
                    actual = actual.simplificar();
                    integrales.add(actual);
                }
                List<Expresion> terminos = new ArrayList<>();
                Polinomio derivada = polinomio;
                for (int k = 0; k <= polinomio.grado(); k++) {
                    Expresion termino = mul(derivada.aExpresion(v), integrales.get(k));
                    terminos.add(k % 2 == 0 ? termino : neg(termino));
                    derivada = derivada.derivar();
                }
                usar("Integración por partes (método tabular)");
                return sumar(terminos);
            }
            // LIATE: u = ln, log, asen, acos, atan  y  dv = P(x) dx  →  ∫u dv = u·v − ∫v du
            if (t instanceof Funcion f && esLogaritmicaOInversa(f.getTipo())) {
                Expresion vPrima = polinomio.integrar().aExpresion(v);
                Expresion du = t.derivar(v).simplificar();
                Expresion restoIntegral = intentar(mul(vPrima, du).simplificar(), v, profundidad + 1);
                if (restoIntegral == null) return null;
                usar("Integración por partes: ∫u dv = u·v − ∫v du");
                return resta(mul(vPrima, t), restoIntegral);
            }
        }
        return null;
    }

    private static boolean esCiclica(Expresion t, String v) {
        if (t instanceof Potencia p && p.getBase().esConstante()) {
            Polinomio l = Polinomio.desde(p.getExponente(), v);
            return l != null && l.grado() == 1;
        }
        if (t instanceof Funcion f) {
            TipoFuncion tipo = f.getTipo();
            boolean ciclica = tipo == TipoFuncion.SEN || tipo == TipoFuncion.COS
                    || tipo == TipoFuncion.SENH || tipo == TipoFuncion.COSH;
            Polinomio l = Polinomio.desde(f.getArgumento(), v);
            return ciclica && l != null && l.grado() == 1;
        }
        return false;
    }

    private static boolean esLogaritmicaOInversa(TipoFuncion t) {
        return t == TipoFuncion.LN || t == TipoFuncion.LOG || t == TipoFuncion.ASEN
                || t == TipoFuncion.ACOS || t == TipoFuncion.ATAN;
    }

    // ------------------------------------------------------------------ 10. expandir

    private Expresion expandir(Expresion g, String v, int profundidad) {
        List<Expresion> expandidos = Simplificador.expandirTerminos(g);
        if (expandidos.size() < 2) return null;
        Expresion suma = sumar(expandidos).simplificar();
        List<Expresion> separados = new ArrayList<>();
        terminos(suma, false, separados);
        if (separados.size() < 2) return null;
        Expresion r = intentar(suma, v, profundidad + 1);
        if (r == null) return null;
        usar("Expandir el producto y separar en términos");
        return r;
    }

    // ------------------------------------------------------------------ identidades trigonométricas

    /**
     * Potencias de seno y coseno:
     * impar  → sen³(u) = sen(u)·(1 − cos²(u))       (luego sustitución u = cos)
     * par    → sen⁴(u) = ((1 − cos(2u))/2)²          (reducción de potencia)
     */
    private Expresion identidadTrigonometrica(List<Expresion> factores, String v, int profundidad) {
        for (int i = 0; i < factores.size(); i++) {
            if (!(factores.get(i) instanceof Potencia p) || !(p.getBase() instanceof Funcion f)) continue;
            if (f.getTipo() != TipoFuncion.SEN && f.getTipo() != TipoFuncion.COS) continue;
            if (!(p.getExponente() instanceof Constante k) || !k.esEntero() || k.getValor() < 3 || k.getValor() > 8) continue;
            int n = (int) Math.round(k.getValor());
            Expresion u = f.getArgumento();
            boolean esSeno = f.getTipo() == TipoFuncion.SEN;
            Expresion reescrito;
            if (n % 2 == 1) {
                Expresion otra = fn(esSeno ? TipoFuncion.COS : TipoFuncion.SEN, u);
                reescrito = mul(f, pot(resta(num(1), pot(otra, 2)), (n - 1) / 2.0));
                usar("Identidad: sen² + cos² = 1");
            } else {
                Expresion cosDoble = fn(TipoFuncion.COS, mul(num(2), u));
                Expresion mitad = div(esSeno ? resta(num(1), cosDoble) : suma(num(1), cosDoble), num(2));
                reescrito = pot(mitad, n / 2.0);
                usar("Reducción de potencia: sen²(u) = (1 − cos(2u))/2");
            }
            List<Expresion> nuevos = new ArrayList<>(factores);
            nuevos.set(i, reescrito);
            Expresion expandido = sumar(Simplificador.expandirTerminos(producto(nuevos))).simplificar();
            return intentar(expandido, v, profundidad + 1);
        }
        return null;
    }

    // ------------------------------------------------------------------ utilidades

    /** Separa una suma en sus términos con signo. */
    private static void terminos(Expresion e, boolean negativo, List<Expresion> salida) {
        if (e instanceof Suma s) {
            terminos(s.getIzquierda(), negativo, salida);
            terminos(s.getDerecha(), negativo, salida);
        } else if (e instanceof Resta r) {
            terminos(r.getIzquierda(), negativo, salida);
            terminos(r.getDerecha(), !negativo, salida);
        } else {
            salida.add(negativo ? neg(e) : e);
        }
    }

    /** Separa un producto en factores; lo que está dividiendo se guarda con exponente negativo. */
    private static void aplanar(Expresion e, boolean invertido, List<Expresion> salida) {
        if (e instanceof Producto p) {
            aplanar(p.getIzquierda(), invertido, salida);
            aplanar(p.getDerecha(), invertido, salida);
        } else if (e instanceof Division d) {
            aplanar(d.getNumerador(), invertido, salida);
            aplanar(d.getDenominador(), !invertido, salida);
        } else if (e instanceof Negacion n) {
            salida.add(num(-1));
            aplanar(n.getOperando(), invertido, salida);
        } else if (!invertido) {
            salida.add(e);
        } else if (e instanceof Constante c && c.esNumerica()) {
            salida.add(num(1 / c.getValor()));
        } else if (e instanceof Potencia p) {
            salida.add(pot(p.getBase(), neg(p.getExponente())).simplificar());
        } else {
            salida.add(pot(e, -1).simplificar());
        }
    }

    private static Expresion producto(List<Expresion> factores) {
        Expresion r = null;
        for (Expresion f : factores) r = r == null ? f : mul(r, f);
        return r == null ? Constante.UNO : r;
    }

    private static Expresion productoExcepto(List<Expresion> factores, int excluido) {
        List<Expresion> resto = new ArrayList<>(factores);
        resto.remove(excluido);
        return producto(resto);
    }

    private static Expresion sumar(List<Expresion> partes) {
        Expresion r = null;
        for (Expresion p : partes) r = r == null ? p : suma(r, p);
        return r == null ? Constante.CERO : r;
    }

    /** Comprueba F'(x) ≈ f(x) en varios puntos con una derivada numérica (diferencia central). */
    static boolean esAntiderivada(Expresion antiderivada, Expresion f, String v) {
        Map<String, Double> valores = new HashMap<>();
        Set<String> otras = new LinkedHashSet<>(antiderivada.variables());
        otras.addAll(f.variables());
        otras.remove(v);
        int k = 0;
        for (String otra : otras) valores.put(otra, 1.37 + 0.29 * k++);
        int validos = 0;
        for (double x : PUNTOS) {
            double h = 1e-5 * Math.max(1, Math.abs(x));
            valores.put(v, x);
            double fx = f.evaluar(valores);
            valores.put(v, x + h);
            double adelante = antiderivada.evaluar(valores);
            valores.put(v, x - h);
            double atras = antiderivada.evaluar(valores);
            if (!Double.isFinite(fx) || !Double.isFinite(adelante) || !Double.isFinite(atras) || Math.abs(fx) > 1e6) {
                continue;
            }
            double derivadaNumerica = (adelante - atras) / (2 * h);
            if (Math.abs(derivadaNumerica - fx) > 1e-4 * Math.max(1, Math.abs(fx))) return false;
            validos++;
        }
        return validos >= 3;
    }
}
