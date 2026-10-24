package calculadora.vista;

import java.util.List;

/** Ejercicios de ejemplo y texto de ayuda de la pestaña "Ayuda y ejemplos". */
public final class Ejemplos {

    private Ejemplos() {
    }

    public static List<Consulta> todos() {
        return List.of(
                ejemplo(Consulta.derivada("3x^2 + 5x", "x"), "Derivada", "3x^2 + 5x", "Ejemplo del enunciado"),
                ejemplo(Consulta.definida("2x", "0", "2"), "Integral definida", "2x  en [0, 2]", "Ejemplo del enunciado: resultado 4"),
                ejemplo(Consulta.derivada("x·sen(x)", "x"), "Derivada", "x·sen(x)", "Regla del producto"),
                ejemplo(Consulta.derivada("(x^2 + 1)/(x - 1)", "x"), "Derivada", "(x^2 + 1)/(x - 1)", "Regla del cociente"),
                ejemplo(Consulta.derivada("e^(2x)·cos(x)", "x"), "Derivada", "e^(2x)·cos(x)", "Producto + regla de la cadena"),
                ejemplo(new Consulta("x^3 - 2x", "x", Consulta.Tipo.DERIVADA, 1, "1", "0", "1"),
                        "Derivada en un punto", "x^3 - 2x  en x = 1", "Dibuja la recta tangente"),
                ejemplo(new Consulta("sen(2x)", "x", Consulta.Tipo.DERIVADA, 3, null, "0", "1"),
                        "Derivada de orden 3", "sen(2x)", "Derivadas sucesivas"),
                ejemplo(Consulta.derivada("x^2·y + 3y^3", "y"), "Derivada parcial", "x^2·y + 3y^3  respecto a y",
                        "La x se trata como constante"),
                ejemplo(Consulta.indefinida("2x·cos(x^2)", "x"), "Integral indefinida", "2x·cos(x^2)", "Sustitución u = x²"),
                ejemplo(Consulta.indefinida("x^2·e^x", "x"), "Integral indefinida", "x^2·e^x", "Por partes (método tabular)"),
                ejemplo(Consulta.indefinida("x·ln(x)", "x"), "Integral indefinida", "x·ln(x)", "Por partes (LIATE)"),
                ejemplo(Consulta.indefinida("(x + 1)/(x^2 - 4)", "x"), "Integral indefinida", "(x + 1)/(x^2 - 4)", "Fracciones parciales"),
                ejemplo(Consulta.indefinida("sen(x)^3", "x"), "Integral indefinida", "sen(x)^3", "Identidad sen² + cos² = 1"),
                ejemplo(Consulta.indefinida("3y^2 - 2y", "y"), "Integral indefinida", "3y^2 - 2y  respecto a y", "Variable y"),
                ejemplo(Consulta.definida("1/(x^2 + 1)", "0", "1"), "Integral definida", "1/(x^2 + 1)  en [0, 1]", "Resultado exacto: π/4"),
                ejemplo(Consulta.definida("√(1 - x^2)", "-1", "1"), "Integral definida", "√(1 - x^2)  en [-1, 1]", "Área de medio círculo: π/2"),
                ejemplo(Consulta.definida("e^(-x^2)", "0", "1"), "Integral definida", "e^(-x^2)  en [0, 1]", "Sin antiderivada: método numérico"),
                ejemplo(Consulta.definida("1/x^2", "-1", "1"), "Integral definida", "1/x^2  en [-1, 1]", "Detecta una integral impropia"));
    }

    private static Consulta ejemplo(Consulta c, String tipo, String expresion, String nota) {
        return c.conResumen("<html><font color='" + Estilo.hex(Estilo.ACENTO) + "'><b>" + tipo.toUpperCase()
                + "</b></font><br>" + FormatoHtml.expresion(expresion)
                + "<br><font color='gray' size='3'>" + nota + "</font></html>");
    }

    public static String ayudaHtml() {
        String ui = Estilo.FAMILIA_UI;
        String mate = Estilo.FAMILIA_MATE;
        String fila = "<tr><td style='padding:4px 10px 4px 0; color:#334155'>%s</td>"
                + "<td style='padding:4px 10px; font-family:" + mate + "; font-size:13pt'>%s</td>"
                + "<td style='padding:4px 0 4px 10px; color:#64748b'>%s</td></tr>";
        StringBuilder sb = new StringBuilder();
        sb.append("<html><body style='font-family:").append(ui).append("; font-size:11pt; color:#111827; margin:4px 8px'>");
        sb.append("<h2 style='font-size:15pt; margin-top:0'>Cómo escribir las funciones</h2>");
        sb.append("<table cellspacing='0'>");
        sb.append(String.format(fila, "<b>Operación</b>", "<b>Escribe</b>", "<b>Nota</b>"));
        sb.append(String.format(fila, "Suma y resta", "x + 1 &nbsp; x − 3", ""));
        sb.append(String.format(fila, "Multiplicación", "3*x &nbsp; 3x &nbsp; 2sen(x)", "Se puede omitir el *"));
        sb.append(String.format(fila, "División", "1/(x + 1)", "Usa paréntesis en el denominador"));
        sb.append(String.format(fila, "Potencia", "x^2 &nbsp; e^(2x) &nbsp; x^(1/3)", "e^(2x) ≠ e^2x"));
        sb.append(String.format(fila, "Raíz cuadrada", "√(x + 1) &nbsp; sqrt(x)", ""));
        sb.append(String.format(fila, "Valor absoluto", "|x| &nbsp; abs(x)", ""));
        sb.append(String.format(fila, "Trigonométricas", "sen cos tan cot sec csc", "También sin(x)"));
        sb.append(String.format(fila, "Inversas", "asen acos atan", "También arcsen, arctan"));
        sb.append(String.format(fila, "Hiperbólicas", "senh cosh tanh", ""));
        sb.append(String.format(fila, "Logaritmos", "ln(x) &nbsp; log(x)", "log es base 10"));
        sb.append(String.format(fila, "Constantes", "π (pi) &nbsp; e", ""));
        sb.append("</table>");
        sb.append("<h2 style='font-size:15pt'>Qué hace cada operación</h2>");
        sb.append("<p><b>Derivada:</b> de orden 1 a 5. Si marcas <i>Evaluar en</i>, calcula f'(a) y dibuja la recta tangente. ")
                .append("Si la función tiene otras letras, se toman como constantes (derivada parcial).</p>");
        sb.append("<p><b>Integral indefinida:</b> busca F(x) con potencia, sustitución, por partes, fracciones parciales, ")
                .append("identidades trigonométricas y completar el cuadrado. Cada resultado se comprueba derivándolo.</p>");
        sb.append("<p><b>Integral definida:</b> usa F(b) − F(a). Si no existe una antiderivada elemental (por ejemplo e^(−x²)) ")
                .append("la aproxima con la regla de Simpson. Si la función se va a infinito dentro del intervalo, avisa que es impropia.</p>");
        sb.append("<p><b>Límites y puntos:</b> aceptan expresiones como pi, pi/2, e, 1/3 o √2.</p>");
        sb.append("<h2 style='font-size:15pt'>Gráfica</h2>");
        sb.append("<p>Azul: f(x). Naranja: la derivada o la antiderivada. Rueda del ratón para zoom, arrastra para moverte ")
                .append("y doble clic para centrar.</p>");
        return sb.append("</body></html>").toString();
    }
}
