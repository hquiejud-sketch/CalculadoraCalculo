package calculadora.vista;

import calculadora.logica.Derivada;
import calculadora.logica.IntegralDefinida;
import calculadora.logica.IntegralIndefinida;
import calculadora.logica.Operacion;
import calculadora.logica.ResultadoCalculo;
import calculadora.modelo.Constante;
import calculadora.modelo.Expresion;
import calculadora.modelo.Numeros;

/** Convierte expresiones y resultados en HTML con exponentes bonitos (x² en vez de x^2). */
public final class FormatoHtml {

    private FormatoHtml() {
    }

    /** "3x^2 + 5x" → "3x<sup>2</sup> + 5x" */
    public static String expresion(Expresion e) {
        return expresion(e.toString());
    }

    public static String expresion(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '^' && i + 1 < s.length()) {
                int j = i + 1;
                String exponente;
                if (s.charAt(j) == '(') {
                    int k = cierre(s, j);
                    exponente = s.substring(j + 1, k);
                    i = k + 1;
                } else {
                    int k = j;
                    if (Character.isDigit(s.charAt(k)) || s.charAt(k) == '.') {
                        while (k < s.length() && (Character.isDigit(s.charAt(k)) || s.charAt(k) == '.')) k++;
                    } else {
                        k++;
                    }
                    exponente = s.substring(j, k);
                    i = k;
                }
                out.append("<sup>").append(expresion(exponente)).append("</sup>");
                continue;
            }
            switch (c) {
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '&' -> out.append("&amp;");
                case '-' -> out.append('−');
                default -> out.append(c);
            }
            i++;
        }
        return out.toString();
    }

    private static int cierre(String s, int abre) {
        int nivel = 0;
        for (int k = abre; k < s.length(); k++) {
            if (s.charAt(k) == '(') nivel++;
            if (s.charAt(k) == ')' && --nivel == 0) return k;
        }
        return s.length() - 1;
    }

    /** f(x) entre paréntesis si es una suma: ∫ (3x² + 5x) dx */
    private static String agrupada(Expresion e) {
        String s = expresion(e);
        return e.precedencia() < 2 ? "(" + s + ")" : s;
    }

    private static boolean tieneOtrasVariables(Expresion f, String v) {
        return f.variables().stream().anyMatch(x -> !x.equals(v));
    }

    /** f'(x), f''(x), f⁽⁴⁾(x) o ∂f/∂x si hay otras variables. */
    public static String nombreDerivada(int orden, String v, boolean parcial) {
        if (parcial) {
            return orden == 1
                    ? "∂f/∂" + v
                    : "∂<sup>" + orden + "</sup>f/∂" + v + "<sup>" + orden + "</sup>";
        }
        String primas = orden <= 3 ? "'".repeat(orden) : "<sup>(" + orden + ")</sup>";
        return "f" + primas + "(" + v + ")";
    }

    /** Versión en texto plano (para la leyenda de la gráfica y para copiar). */
    public static String nombreDerivadaTexto(int orden, String v, boolean parcial) {
        if (parcial) return orden == 1 ? "∂f/∂" + v : "∂^" + orden + "f/∂" + v + "^" + orden;
        return "f" + (orden <= 3 ? "'".repeat(orden) : "^(" + orden + ")") + "(" + v + ")";
    }

    /** Número exacto + aproximación decimal: "e² − 1 ≈ 6.389056". */
    public static String valor(Expresion exacto, Double numero, boolean aproximado) {
        if (exacto == null) {
            return numero == null ? "?" : "≈ " + Numeros.decimal(numero);
        }
        String texto = expresion(exacto);
        if (numero == null || !Double.isFinite(numero)) return texto;
        boolean esEntero = exacto instanceof Constante c && c.esEntero();
        if (esEntero || aproximado) return texto;
        return texto + " <span class='aprox'>≈ " + Numeros.decimal(numero) + "</span>";
    }

    /** Envuelve el contenido en una página con sus estilos CSS. */
    public static String pagina(String cuerpo) {
        return "<html><head><style>" + estilos() + "</style></head><body>" + cuerpo + "</body></html>";
    }

    public static String estilos() {
        return "body { font-family: '" + Estilo.FAMILIA_MATE + "'; font-size: 16pt; color: " + Estilo.hex(Estilo.TINTA) + "; margin: 2px 4px; }"
                + ".etq { font-family: '" + Estilo.FAMILIA_UI + "'; font-size: 10.5pt; font-weight: bold; color: " + Estilo.hex(Estilo.ACENTO) + "; margin-bottom: 8px; }"
                + ".fun { font-size: 16pt; color: #334155; margin-bottom: 8px; }"
                + ".res { font-size: 21pt; font-weight: bold; color: " + Estilo.hex(Estilo.TINTA) + "; margin-bottom: 8px; }"
                + ".lim { font-size: 13pt; }"
                + ".sec { font-size: 16pt; color: #1e293b; margin-bottom: 6px; }"
                + ".aprox { font-weight: normal; color: " + Estilo.hex(Estilo.TEXTO_SUAVE) + "; }"
                + ".met { font-family: '" + Estilo.FAMILIA_UI + "'; font-size: 12pt; color: #475569; margin-top: 6px; }"
                + ".titmet { font-family: '" + Estilo.FAMILIA_UI + "'; font-size: 10pt; font-weight: bold; color: " + Estilo.hex(Estilo.TEXTO_SUAVE) + "; margin-top: 14px; }"
                + ".aviso { font-family: '" + Estilo.FAMILIA_UI + "'; font-size: 12pt; color: " + Estilo.hex(Estilo.AMBAR) + "; margin-top: 10px; }"
                + ".error { font-family: '" + Estilo.FAMILIA_UI + "'; font-size: 13pt; color: " + Estilo.hex(Estilo.ROJO) + "; margin-top: 4px; }"
                + ".ayuda { font-family: '" + Estilo.FAMILIA_UI + "'; font-size: 12pt; color: " + Estilo.hex(Estilo.TEXTO_SUAVE) + "; }";
    }

    /** Página HTML completa que se muestra en el panel de resultado. */
    public static String resultado(ResultadoCalculo r) {
        Operacion op = r.getOperacion();
        String v = op.getVariable();
        Expresion f = r.getFuncion();
        boolean parcial = tieneOtrasVariables(f, v);
        StringBuilder sb = new StringBuilder();
        sb.append("<div class='etq'>").append(op.getNombre().toUpperCase()).append(" &nbsp;·&nbsp; VARIABLE: ")
                .append(v).append("</div>");
        sb.append("<div class='fun'>").append(parcial ? "f" : "f(" + v + ")").append(" = ").append(expresion(f)).append("</div>");

        if (op instanceof Derivada d) {
            String nombre = nombreDerivada(d.getOrden(), v, parcial);
            sb.append("<div class='res'>").append(nombre).append(" = ").append(expresion(r.getResultado())).append("</div>");
            if (d.getPunto() != null && r.getValorExacto() != null) {
                String enPunto = parcial ? nombre + " en " + v + " = " + expresion(d.getPunto())
                        : nombre.replace("(" + v + ")", "(" + expresion(d.getPunto()) + ")");
                sb.append("<div class='sec'>").append(enPunto).append(" = ")
                        .append(valor(r.getValorExacto(), r.getValorNumerico(), false)).append("</div>");
            }
        } else if (op instanceof IntegralIndefinida) {
            sb.append("<div class='res'>∫ ").append(agrupada(f)).append(" d").append(v).append(" = ");
            sb.append(r.getResultado() == null ? "?" : expresion(r.getResultado()) + " + C").append("</div>");
        } else if (op instanceof IntegralDefinida id) {
            String limites = "∫<sub><span class='lim'>" + expresion(id.getLimiteInferior()) + "</span></sub><sup><span class='lim'>"
                    + expresion(id.getLimiteSuperior()) + "</span></sup> ";
            String resultado = r.isValido() ? valor(r.getValorExacto(), r.getValorNumerico(), r.isAproximado()) : "integral impropia";
            sb.append("<div class='res'>").append(limites).append(agrupada(f)).append(" d").append(v)
                    .append(resultado.startsWith("≈") ? " " : " = ").append(resultado).append("</div>");
            if (r.getResultado() != null) {
                sb.append("<div class='sec'>F(").append(v).append(") = ").append(expresion(r.getResultado())).append("</div>");
            }
            if (r.getVerificacion() != null) {
                sb.append("<div class='met'>Comprobación numérica (Simpson): ")
                        .append(Numeros.decimal(r.getVerificacion())).append("</div>");
            }
        }
        if (!r.getMetodos().isEmpty()) {
            sb.append("<div class='titmet'>").append(op instanceof Derivada ? "REGLAS USADAS" : "MÉTODOS USADOS").append("</div>");
            sb.append("<div class='met'>");
            for (int i = 0; i < r.getMetodos().size(); i++) {
                if (i > 0) sb.append("<br>");
                sb.append("• ").append(superindices(escapar(r.getMetodos().get(i))));
            }
            sb.append("</div>");
        }
        for (String aviso : r.getAvisos()) {
            sb.append("<div class='aviso'>⚠ ").append(superindices(escapar(aviso))).append("</div>");
        }
        return pagina(sb.toString());
    }

    /** Texto plano del resultado (botón Copiar). */
    public static String textoPlano(ResultadoCalculo r) {
        Operacion op = r.getOperacion();
        String v = op.getVariable();
        Expresion f = r.getFuncion();
        boolean parcial = tieneOtrasVariables(f, v);
        String fs = f.precedencia() < 2 ? "(" + f + ")" : f.toString();
        if (op instanceof Derivada d) {
            String s = nombreDerivadaTexto(d.getOrden(), v, parcial) + " = " + r.getResultado();
            if (r.getValorExacto() != null) s += "\nEn " + v + " = " + d.getPunto() + ": " + r.getValorExacto();
            return s;
        }
        if (op instanceof IntegralIndefinida) {
            return "∫ " + fs + " d" + v + " = " + (r.getResultado() == null ? "?" : r.getResultado() + " + C");
        }
        IntegralDefinida id = (IntegralDefinida) op;
        String valor = !r.isValido() ? "integral impropia"
                : r.getValorExacto() != null ? r.getValorExacto().toString()
                : r.getValorNumerico() != null ? "≈ " + Numeros.decimal(r.getValorNumerico()) : "?";
        return "∫ " + fs + " d" + v + " desde " + id.getLimiteInferior() + " hasta " + id.getLimiteSuperior()
                + (valor.startsWith("≈") ? " " : " = ") + valor;
    }

    /** Cambia los superíndices Unicode (xⁿ⁺¹) por etiquetas sup, que se ven bien en cualquier fuente. */
    public static String superindices(String s) {
        String normales = "0123456789n+-uvxa";
        String altos = "⁰¹²³⁴⁵⁶⁷⁸⁹ⁿ⁺⁻ᵘᵛˣᵃ";
        StringBuilder out = new StringBuilder();
        boolean dentro = false;
        for (char c : s.toCharArray()) {
            int i = altos.indexOf(c);
            if (i >= 0 && !dentro) {
                out.append("<sup>");
                dentro = true;
            } else if (i < 0 && dentro) {
                out.append("</sup>");
                dentro = false;
            }
            out.append(i >= 0 ? normales.charAt(i) : c);
        }
        if (dentro) out.append("</sup>");
        return out.toString();
    }

    public static String error(String mensaje) {
        return pagina("<div class='etq'>REVISA LA ENTRADA</div><div class='error'>" + escapar(mensaje)
                + "</div><div class='ayuda'><br>Ejemplos válidos: 3x^2 + 5x &nbsp;·&nbsp; x·sen(x) &nbsp;·&nbsp; e^(2x) "
                + "&nbsp;·&nbsp; 1/(x^2 + 1) &nbsp;·&nbsp; √(x + 1)</div>");
    }

    public static String bienvenida() {
        return pagina("<div class='etq'>LISTO</div>"
                + "<div class='fun'>Escribe una función, elige la operación y presiona <b>Calcular</b> (o Enter).</div>"
                + "<div class='ayuda'>Puedes usar sen, cos, tan, ln, log, √, e, π, valor absoluto |x| y potencias con ^. "
                + "En la pestaña <b>Ayuda y ejemplos</b> hay ejercicios listos para probar.</div>");
    }

    public static String escapar(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
