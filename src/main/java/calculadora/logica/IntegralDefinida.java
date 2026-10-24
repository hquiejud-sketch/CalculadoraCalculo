package calculadora.logica;

import calculadora.modelo.Constante;
import calculadora.modelo.Expresion;
import calculadora.modelo.Numeros;
import calculadora.modelo.Resta;
import calculadora.modelo.Simplificador;

import java.util.HashSet;
import java.util.Set;

/**
 * ∫ₐᵇ f(x) dx.
 *
 * <p>Primero busca la antiderivada F y aplica el Teorema Fundamental del Cálculo: F(b) − F(a).
 * Si no existe una antiderivada elemental, aproxima con la regla de Simpson.
 * Antes revisa que la función sea continua en [a, b] (si no, la integral es impropia).</p>
 */
public class IntegralDefinida extends Operacion {

    private final Expresion limiteInferior;
    private final Expresion limiteSuperior;

    public IntegralDefinida(String variable, Expresion limiteInferior, Expresion limiteSuperior) {
        super(variable);
        this.limiteInferior = limiteInferior;
        this.limiteSuperior = limiteSuperior;
    }

    public Expresion getLimiteInferior() {
        return limiteInferior;
    }

    public Expresion getLimiteSuperior() {
        return limiteSuperior;
    }

    public double getA() {
        return limiteInferior.valorConstante();
    }

    public double getB() {
        return limiteSuperior.valorConstante();
    }

    @Override
    public String getNombre() {
        return "Integral definida";
    }

    @Override
    public ResultadoCalculo ejecutar(Expresion funcion) throws ErrorCalculo {
        double a = valorNumerico(limiteInferior, "El límite inferior a");
        double b = valorNumerico(limiteSuperior, "El límite superior b");
        ResultadoCalculo r = new ResultadoCalculo(this, funcion);

        Set<String> otras = new HashSet<>(funcion.variables());
        otras.remove(variable);
        boolean unaVariable = otras.isEmpty();

        Integrador integrador = new Integrador();
        Expresion antiderivada = integrador.integrar(funcion, variable);
        r.setResultado(antiderivada);

        if (a == b) {
            r.setValorExacto(Constante.CERO);
            r.setValorNumerico(0.0);
            r.agregarMetodo("Si a = b, la integral vale 0");
            return r;
        }

        IntegradorNumerico numerico = new IntegradorNumerico();
        IntegradorNumerico.Resultado aproximacion = null;
        if (unaVariable) {
            Double salto = numerico.buscarDiscontinuidad(
                    IntegradorNumerico.comoFuncion(funcion, variable), Math.min(a, b), Math.max(a, b));
            if (salto != null) {
                r.setValido(false);
                r.agregarAviso("La función no es continua en " + variable + " ≈ " + Numeros.decimal(salto)
                        + ", que está dentro del intervalo. Es una integral impropia: no se puede usar F(b) − F(a).");
                return r;
            }
            aproximacion = numerico.integrar(IntegradorNumerico.comoFuncionSegura(funcion, variable), a, b);
        }

        if (antiderivada != null) {
            Expresion exacto = Simplificador.formaFinal(new Resta(
                    antiderivada.sustituir(variable, limiteSuperior),
                    antiderivada.sustituir(variable, limiteInferior)), variable);
            r.setValorExacto(exacto);
            if (exacto.esConstante()) r.setValorNumerico(exacto.valorConstante());
            integrador.getMetodos().forEach(r::agregarMetodo);
            r.agregarMetodo("Teorema fundamental del cálculo: F(b) − F(a)");
            if (aproximacion != null && aproximacion.convergio()) {
                r.setVerificacion(aproximacion.valor());
                Double exactoNumerico = r.getValorNumerico();
                if (exactoNumerico != null && !Numeros.casiIguales(exactoNumerico, aproximacion.valor(), 1e-6)) {
                    r.agregarAviso("El valor numérico de comprobación no coincide; revisa que la función sea continua en el intervalo.");
                }
            }
            return r;
        }

        if (aproximacion == null) {
            throw new ErrorCalculo("No se encontró una antiderivada y la función tiene otras variables "
                    + otras + ", así que no se puede aproximar con números.");
        }
        if (!aproximacion.convergio()) {
            r.setValido(false);
            r.agregarAviso("La integral no converge en ese intervalo.");
            return r;
        }
        r.setValorNumerico(aproximacion.valor());
        r.setAproximado(true);
        r.agregarMetodo("Integración numérica: regla de Simpson adaptativa");
        r.agregarAviso("No existe una antiderivada con funciones elementales (o no se encontró); "
                + "el resultado es una aproximación numérica.");
        return r;
    }
}
