package calculadora.logica;

/** Error que se le muestra al usuario (dato inválido, límite no numérico, etc.). */
public class ErrorCalculo extends Exception {

    public ErrorCalculo(String mensaje) {
        super(mensaje);
    }
}
//manejo error
