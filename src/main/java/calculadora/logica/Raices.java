package calculadora.logica;

import calculadora.modelo.Polinomio;

/** Encuentra todas las raíces (reales y complejas) de un polinomio. */
public final class Raices {

    private Raices() {
    }

    public static Complejo evaluar(Polinomio p, Complejo z) {
        Complejo r = new Complejo(0, 0);
        for (int i = p.grado(); i >= 0; i--) {
            r = r.multiplicar(z).sumar(new Complejo(p.coef(i), 0));
        }
        return r;
    }

    /**
     * Grado 1 y 2 con fórmula; grado ≥ 3 con el método de Durand–Kerner
     * (todas las raíces a la vez) y luego se pulen con Newton.
     */
    public static Complejo[] calcular(Polinomio p) {
        int n = p.grado();
        if (n < 1) return new Complejo[0];
        if (n == 1) return new Complejo[]{new Complejo(-p.coef(0) / p.coef(1), 0)};
        if (n == 2) {
            double a = p.coef(2), b = p.coef(1), c = p.coef(0);
            double disc = b * b - 4 * a * c;
            if (disc >= 0) {
                double s = Math.sqrt(disc);
                // fórmula estable (evita restar números casi iguales)
                double q = -0.5 * (b + Math.copySign(s, b));
                double r1 = q / a;
                double r2 = q != 0 ? c / q : -b / (2 * a) - s / (2 * a);
                return new Complejo[]{new Complejo(r1, 0), new Complejo(r2, 0)};
            }
            double re = -b / (2 * a), im = Math.sqrt(-disc) / (2 * a);
            return new Complejo[]{new Complejo(re, Math.abs(im)), new Complejo(re, -Math.abs(im))};
        }

        double lider = p.coef(n);
        double cota = 1;
        for (int i = 0; i < n; i++) cota = Math.max(cota, 1 + Math.abs(p.coef(i) / lider));
        Complejo[] z = new Complejo[n];
        for (int k = 0; k < n; k++) {
            double angulo = 2 * Math.PI * k / n + 0.4;
            z[k] = new Complejo(0.5 * cota * Math.cos(angulo), 0.5 * cota * Math.sin(angulo));
        }
        Complejo liderC = new Complejo(lider, 0);
        for (int iter = 0; iter < 2000; iter++) {
            double cambio = 0;
            for (int k = 0; k < n; k++) {
                Complejo numerador = evaluar(p, z[k]).dividir(liderC);
                Complejo denominador = new Complejo(1, 0);
                for (int j = 0; j < n; j++) {
                    if (j != k) denominador = denominador.multiplicar(z[k].restar(z[j]));
                }
                Complejo paso = numerador.dividir(denominador);
                z[k] = z[k].restar(paso);
                cambio = Math.max(cambio, paso.modulo());
            }
            if (cambio < 1e-15) break;
        }
        Polinomio derivada = p.derivar();
        for (int k = 0; k < n; k++) {
            for (int i = 0; i < 5; i++) {
                Complejo d = evaluar(derivada, z[k]);
                if (d.modulo() < 1e-14) break;
                z[k] = z[k].restar(evaluar(p, z[k]).dividir(d));
            }
            if (Math.abs(z[k].im()) < 1e-10 * Math.max(1, Math.abs(z[k].re()))) {
                z[k] = new Complejo(z[k].re(), 0);
            }
        }
        return z;
    }
}
