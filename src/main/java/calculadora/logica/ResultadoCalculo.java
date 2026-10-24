package calculadora.logica;

import calculadora.modelo.Expresion;

import java.util.ArrayList;
import java.util.List;

/** Todo lo que produce una operación: el resultado, los métodos usados y las advertencias. */
public class ResultadoCalculo {

    private final Operacion operacion;
    private final Expresion funcion;
    /** Derivada o antiderivada (puede ser null si no se encontró). */
    private Expresion resultado;
    /** Valor exacto de f'(a) o de la integral definida (por ejemplo e^2 - 1). */
    private Expresion valorExacto;
    private Double valorNumerico;
    /** true si el valor se obtuvo con un método numérico. */
    private boolean aproximado;
    /** Valor numérico de comprobación (Simpson) para la integral definida. */
    private Double verificacion;
    /** false si la operación no se pudo completar (por ejemplo, integral impropia). */
    private boolean valido = true;
    private final List<String> metodos = new ArrayList<>();
    private final List<String> avisos = new ArrayList<>();

    public ResultadoCalculo(Operacion operacion, Expresion funcion) {
        this.operacion = operacion;
        this.funcion = funcion;
    }

    public Operacion getOperacion() {
        return operacion;
    }

    public Expresion getFuncion() {
        return funcion;
    }

    public Expresion getResultado() {
        return resultado;
    }

    public void setResultado(Expresion resultado) {
        this.resultado = resultado;
    }

    public Expresion getValorExacto() {
        return valorExacto;
    }

    public void setValorExacto(Expresion valorExacto) {
        this.valorExacto = valorExacto;
    }

    public Double getValorNumerico() {
        return valorNumerico;
    }

    public void setValorNumerico(Double valorNumerico) {
        this.valorNumerico = valorNumerico;
    }

    public boolean isAproximado() {
        return aproximado;
    }

    public void setAproximado(boolean aproximado) {
        this.aproximado = aproximado;
    }

    public Double getVerificacion() {
        return verificacion;
    }

    public void setVerificacion(Double verificacion) {
        this.verificacion = verificacion;
    }

    public boolean isValido() {
        return valido;
    }

    public void setValido(boolean valido) {
        this.valido = valido;
    }

    public List<String> getMetodos() {
        return metodos;
    }

    public void agregarMetodo(String metodo) {
        if (!metodos.contains(metodo)) metodos.add(metodo);
    }

    public List<String> getAvisos() {
        return avisos;
    }

    public void agregarAviso(String aviso) {
        avisos.add(aviso);
    }
}
