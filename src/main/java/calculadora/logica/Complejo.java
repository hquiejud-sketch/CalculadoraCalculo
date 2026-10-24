package calculadora.logica;

/** Número complejo a + bi (se usa para encontrar las raíces de un polinomio). */
public record Complejo(double re, double im) {

    public Complejo sumar(Complejo o) {
        return new Complejo(re + o.re, im + o.im);
    }

    public Complejo restar(Complejo o) {
        return new Complejo(re - o.re, im - o.im);
    }

    public Complejo multiplicar(Complejo o) {
        return new Complejo(re * o.re - im * o.im, re * o.im + im * o.re);
    }

    public Complejo dividir(Complejo o) {
        double d = o.re * o.re + o.im * o.im;
        return new Complejo((re * o.re + im * o.im) / d, (im * o.re - re * o.im) / d);
    }

    public double modulo() {
        return Math.hypot(re, im);
    }
}
