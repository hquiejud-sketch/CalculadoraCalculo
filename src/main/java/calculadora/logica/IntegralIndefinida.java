package calculadora.logica;

import calculadora.modelo.Expresion;

/** ∫ f(x) dx = F(x) + C */
public class IntegralIndefinida extends Operacion {

    public IntegralIndefinida(String variable) {
        super(variable);
    }

    @Override
    public String getNombre() {
        return "Integral indefinida";
    }

    @Override
    public ResultadoCalculo ejecutar(Expresion funcion) {
        ResultadoCalculo r = new ResultadoCalculo(this, funcion);
        Integrador integrador = new Integrador();
        Expresion antiderivada = integrador.integrar(funcion, variable);
        if (antiderivada == null) {
            r.setValido(false);
            r.agregarAviso("No se encontró una antiderivada con funciones elementales usando las técnicas "
                    + "disponibles. Prueba con la integral definida: ahí se usa un método numérico.");
            return r;
        }
        r.setResultado(antiderivada);
        integrador.getMetodos().forEach(r::agregarMetodo);
        return r;
    }
}
