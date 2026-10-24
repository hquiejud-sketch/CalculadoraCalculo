package calculadora;

import calculadora.logica.AnalizadorSintactico;
import calculadora.logica.Derivada;
import calculadora.logica.ErrorSintaxis;
import calculadora.logica.IntegralDefinida;
import calculadora.logica.IntegralIndefinida;
import calculadora.logica.ResultadoCalculo;
import calculadora.modelo.Expresion;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pruebas automáticas: en IntelliJ, clic derecho sobre esta clase → Run 'CalculadoraTest'. */
class CalculadoraTest {

    private final AnalizadorSintactico analizador = new AnalizadorSintactico();

    private Expresion leer(String texto) throws Exception {
        return analizador.analizar(texto);
    }

    private String derivar(String f, String variable, int orden) throws Exception {
        return new Derivada(variable, orden, null).ejecutar(leer(f)).getResultado().toString();
    }

    private Expresion integrar(String f, String variable) throws Exception {
        return new IntegralIndefinida(variable).ejecutar(leer(f)).getResultado();
    }

    private ResultadoCalculo definida(String f, String a, String b) throws Exception {
        return new IntegralDefinida("x", leer(a), leer(b)).ejecutar(leer(f));
    }

    /** Comprueba F'(x) = f(x) con una derivada numérica en varios puntos. */
    private void esAntiderivada(Expresion antiderivada, String f) throws Exception {
        assertNotNull(antiderivada, "no se encontró antiderivada de " + f);
        Expresion funcion = leer(f);
        for (double x : new double[]{0.37, 0.91, 1.43, 2.17}) {
            double h = 1e-5;
            double numerica = (antiderivada.evaluar("x", x + h) - antiderivada.evaluar("x", x - h)) / (2 * h);
            assertEquals(funcion.evaluar("x", x), numerica, 1e-5, "F' ≠ f en x = " + x);
        }
    }

    // ------------------------------------------------------------------ analizador

    @Test
    void entiendeMultiplicacionImplicitaYFunciones() throws Exception {
        assertEquals("3x^2 + 5x", leer("3x^2+5x").toString());
        assertEquals(10.0, leer("2sen(pi/2)·5").valorConstante(), 1e-12);
        assertEquals(8.0, leer("x^3").evaluar("x", 2), 1e-12);
        assertEquals(3.0, leer("√(x + 5)").evaluar("x", 4), 1e-12);
        assertEquals(2.0, leer("|x - 4|").evaluar("x", 2), 1e-12);
    }

    @Test
    void detectaErroresDeEscritura() {
        assertThrows(ErrorSintaxis.class, () -> leer("(x + 1"));
        assertThrows(ErrorSintaxis.class, () -> leer("3x +"));
        assertThrows(ErrorSintaxis.class, () -> leer("x $ 2"));
        assertThrows(ErrorSintaxis.class, () -> leer(""));
    }

    // ------------------------------------------------------------------ derivadas

    @Test
    void derivadasBasicas() throws Exception {
        assertEquals("6x + 5", derivar("3x^2 + 5x", "x", 1)); // ejemplo del enunciado
        assertEquals("sen(x) + x·cos(x)", derivar("x·sen(x)", "x", 1));
        assertEquals("2e^(2x)", derivar("e^(2x)", "x", 1));
        assertEquals("1/x", derivar("ln(x)", "x", 1));
        assertEquals("sec(x)^2", derivar("tan(x)", "x", 1));
        assertEquals("20x·(x^2 + 1)^9", derivar("(x^2 + 1)^10", "x", 1));
    }

    @Test
    void derivadasDeOrdenSuperiorYParciales() throws Exception {
        assertEquals("6x", derivar("x^3", "x", 2));
        assertEquals("x^2 + 9y^2", derivar("x^2·y + 3y^3", "y", 1));
    }

    @Test
    void derivadaEnUnPunto() throws Exception {
        ResultadoCalculo r = new Derivada("x", 1, leer("2")).ejecutar(leer("3x^2 + 5x"));
        assertEquals(17.0, r.getValorNumerico(), 1e-12);
    }

    // ------------------------------------------------------------------ integrales

    @Test
    void integralesIndefinidas() throws Exception {
        assertEquals("x^3 + 5x^2/2", integrar("3x^2 + 5x", "x").toString());
        assertEquals("atan(x)", integrar("1/(x^2 + 1)", "x").toString());
        assertEquals("sen(x^2)", integrar("2x·cos(x^2)", "x").toString());
        assertEquals("y^3 - y^2", integrar("3y^2 - 2y", "y").toString());
        for (String f : new String[]{"x·e^x", "x^2·sen(x)", "x·ln(x)", "(x + 1)/(x^2 - 4)", "sen(x)^2",
                "sen(x)^3", "e^x·cos(x)", "√(1 - x^2)", "1/(x^3 - x)", "tan(x)", "ln(x^2 + 1)"}) {
            esAntiderivada(integrar(f, "x"), f);
        }
    }

    @Test
    void integralesDefinidas() throws Exception {
        assertEquals(4.0, definida("2x", "0", "2").getValorNumerico(), 1e-12); // ejemplo del enunciado
        assertEquals(Math.PI / 4, definida("1/(x^2 + 1)", "0", "1").getValorNumerico(), 1e-12);
        assertEquals(2.0, definida("sen(x)", "0", "pi").getValorNumerico(), 1e-12);
        assertEquals("π/4", definida("1/(x^2 + 1)", "0", "1").getValorExacto().toString());
    }

    @Test
    void integralSinAntiderivadaUsaMetodoNumerico() throws Exception {
        ResultadoCalculo r = definida("e^(-x^2)", "0", "1");
        assertTrue(r.isAproximado());
        assertEquals(0.746824132812427, r.getValorNumerico(), 1e-9);
    }

    @Test
    void detectaIntegralImpropia() throws Exception {
        ResultadoCalculo r = definida("1/x^2", "-1", "1");
        assertFalse(r.isValido());
    }
}
