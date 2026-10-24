package calculadora.logica;

/** La función escrita tiene un error de escritura (paréntesis sin cerrar, símbolo inválido...). */
public class ErrorSintaxis extends ErrorCalculo {

    /** Posición (carácter) donde se detectó el error, para marcarlo en el campo de texto. */
    private final int posicion;

    public ErrorSintaxis(String mensaje, int posicion) {
        super(mensaje);
        this.posicion = posicion;
    }

    public int getPosicion() {
        return posicion;
    }
}
