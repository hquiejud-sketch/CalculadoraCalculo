package calculadora.logica;

import calculadora.modelo.Constante;
import calculadora.modelo.Division;
import calculadora.modelo.Expresion;
import calculadora.modelo.Funcion;
import calculadora.modelo.Negacion;
import calculadora.modelo.Potencia;
import calculadora.modelo.Producto;
import calculadora.modelo.Resta;
import calculadora.modelo.Simplificador;
import calculadora.modelo.Suma;
import calculadora.modelo.Variable;

import java.util.LinkedHashSet;
import java.util.Set;

/** Derivada de orden n, opcionalmente evaluada en un punto. */
public class Derivada extends Operacion {

    private final int orden;
    /** Punto donde evaluar f'(a); null si no se pide. */
    private final Expresion punto;

    public Derivada(String variable, int orden, Expresion punto) {
        super(variable);
        this.orden = Math.max(1, orden);
        this.punto = punto;
    }

    public int getOrden() {
        return orden;
    }

    public Expresion getPunto() {
        return punto;
    }

    @Override
    public String getNombre() {
        return orden == 1 ? "Derivada" : "Derivada de orden " + orden;
    }

    @Override
    public ResultadoCalculo ejecutar(Expresion funcion) throws ErrorCalculo {
        ResultadoCalculo r = new ResultadoCalculo(this, funcion);
        Expresion actual = funcion.simplificar();
        for (int i = 0; i < orden; i++) {
            // cada nodo se deriva con su propia regla y luego se simplifica el resultado
            actual = Simplificador.formaFinal(actual.derivar(variable), variable);
        }
        r.setResultado(actual);

        Set<String> reglas = new LinkedHashSet<>();
        detectarReglas(funcion, variable, reglas);
        reglas.forEach(r::agregarMetodo);
        if (!funcion.contieneVariable(variable)) {
            r.agregarMetodo("Derivada de una constante: (k)' = 0");
            r.agregarAviso("La función no depende de " + variable + ", por eso su derivada es 0.");
        }

        if (punto != null) {
            valorNumerico(punto, "El punto de evaluación");
            Expresion valor = Simplificador.formaFinal(actual.sustituir(variable, punto), variable);
            r.setValorExacto(valor);
            if (valor.esConstante()) {
                double numero = valor.valorConstante();
                r.setValorNumerico(numero);
                if (!Double.isFinite(numero)) {
                    r.agregarAviso("La derivada no está definida en ese punto.");
                }
            }
        }
        return r;
    }

    /** Revisa el árbol para decir qué reglas de derivación se aplicaron. */
    private static void detectarReglas(Expresion e, String v, Set<String> reglas) {
        if (!e.contieneVariable(v)) {
            return; // las partes constantes no aportan reglas
        }
        if (e instanceof Suma s) {
            reglas.add("Regla de la suma: (f + g)' = f' + g'");
            detectarReglas(s.getIzquierda(), v, reglas);
            detectarReglas(s.getDerecha(), v, reglas);
        } else if (e instanceof Resta s) {
            reglas.add("Regla de la suma: (f + g)' = f' + g'");
            detectarReglas(s.getIzquierda(), v, reglas);
            detectarReglas(s.getDerecha(), v, reglas);
        } else if (e instanceof Negacion n) {
            detectarReglas(n.getOperando(), v, reglas);
        } else if (e instanceof Producto p) {
            boolean ambos = p.getIzquierda().contieneVariable(v) && p.getDerecha().contieneVariable(v);
            reglas.add(ambos ? "Regla del producto: (f·g)' = f'·g + f·g'" : "Constante por función: (k·f)' = k·f'");
            detectarReglas(p.getIzquierda(), v, reglas);
            detectarReglas(p.getDerecha(), v, reglas);
        } else if (e instanceof Division d) {
            if (d.getDenominador().contieneVariable(v)) {
                reglas.add("Regla del cociente: (f/g)' = (f'·g − f·g')/g²");
            }
            detectarReglas(d.getNumerador(), v, reglas);
            detectarReglas(d.getDenominador(), v, reglas);
        } else if (e instanceof Potencia p) {
            boolean base = p.getBase().contieneVariable(v), exponente = p.getExponente().contieneVariable(v);
            if (base && exponente) {
                reglas.add("Derivación logarítmica: (uᵛ)' = uᵛ·(v'·ln u + v·u'/u)");
            } else if (base) {
                reglas.add("Regla de la potencia: (uⁿ)' = n·uⁿ⁻¹·u'");
            } else {
                reglas.add(p.getBase().equals(Constante.E) ? "Exponencial: (eᵘ)' = eᵘ·u'" : "Exponencial: (aᵘ)' = aᵘ·ln(a)·u'");
            }
            Expresion interior = base ? p.getBase() : p.getExponente();
            if (!(interior instanceof Variable)) reglas.add("Regla de la cadena: [f(g(x))]' = f'(g(x))·g'(x)");
            detectarReglas(p.getBase(), v, reglas);
            detectarReglas(p.getExponente(), v, reglas);
        } else if (e instanceof Funcion f) {
            reglas.add("Derivada de " + f.getTipo().getNombre() + "(u)");
            if (!(f.getArgumento() instanceof Variable)) reglas.add("Regla de la cadena: [f(g(x))]' = f'(g(x))·g'(x)");
            detectarReglas(f.getArgumento(), v, reglas);
        }
    }
}
