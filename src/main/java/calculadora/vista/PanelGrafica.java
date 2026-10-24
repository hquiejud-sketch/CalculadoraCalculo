package calculadora.vista;

import calculadora.modelo.Expresion;
import calculadora.modelo.Numeros;

import javax.swing.JPanel;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Dibuja f(x) y su derivada o antiderivada, el área de la integral definida
 * y la recta tangente. Se puede hacer zoom con la rueda y mover arrastrando.
 */
public class PanelGrafica extends JPanel {

    private static final Color COLOR_AREA = new Color(234, 108, 20, 55);
    private static final Color CUADRICULA = new Color(236, 239, 244);
    private static final Color EJES = new Color(148, 160, 178);

    private Expresion f;
    private Expresion g;
    private Expresion tangente;
    private String etiquetaF = "f(x)";
    private String etiquetaG;
    private String etiquetaTangente;
    private String variable = "x";
    private Double areaDesde;
    private Double areaHasta;
    private Double puntoX;
    private String mensaje = "La gráfica aparece aquí cuando calculas algo.";

    private double xMin = -10, xMax = 10, yMin = -10, yMax = 10;
    private Point inicioArrastre;
    private double[] vistaAlArrastrar;
    private Point raton;

    public PanelGrafica() {
        setBackground(Color.WHITE);
        MouseAdapter control = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                inicioArrastre = e.getPoint();
                vistaAlArrastrar = new double[]{xMin, xMax, yMin, yMax};
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (inicioArrastre == null || f == null) return;
                double dx = (e.getX() - inicioArrastre.x) * (vistaAlArrastrar[1] - vistaAlArrastrar[0]) / getWidth();
                double dy = (e.getY() - inicioArrastre.y) * (vistaAlArrastrar[3] - vistaAlArrastrar[2]) / getHeight();
                xMin = vistaAlArrastrar[0] - dx;
                xMax = vistaAlArrastrar[1] - dx;
                yMin = vistaAlArrastrar[2] + dy;
                yMax = vistaAlArrastrar[3] + dy;
                raton = e.getPoint();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                inicioArrastre = null;
            }

            @Override
            public void mouseWheelMoved(MouseWheelEvent e) {
                if (f == null) return;
                double factor = e.getWheelRotation() > 0 ? 1.15 : 1 / 1.15;
                double cx = aX(e.getX()), cy = aY(e.getY());
                xMin = cx + (xMin - cx) * factor;
                xMax = cx + (xMax - cx) * factor;
                yMin = cy + (yMin - cy) * factor;
                yMax = cy + (yMax - cy) * factor;
                repaint();
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    ajustarVista();
                    repaint();
                }
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                raton = e.getPoint();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                raton = null;
                repaint();
            }
        };
        addMouseListener(control);
        addMouseMotionListener(control);
        addMouseWheelListener(control);
    }

    /** Muestra una o dos funciones; el área, el punto y la tangente son opcionales (null). */
    public void mostrar(String variable, Expresion f, String etiquetaF, Expresion g, String etiquetaG,
                        Double areaDesde, Double areaHasta, Double puntoX, Expresion tangente, String etiquetaTangente) {
        this.variable = variable;
        this.f = f;
        this.etiquetaF = etiquetaF;
        this.g = g;
        this.etiquetaG = etiquetaG;
        this.areaDesde = areaDesde;
        this.areaHasta = areaHasta;
        this.puntoX = puntoX;
        this.tangente = tangente;
        this.etiquetaTangente = etiquetaTangente;
        this.mensaje = null;
        ajustarVista();
        repaint();
    }

    public void mostrarMensaje(String mensaje) {
        this.f = null;
        this.g = null;
        this.tangente = null;
        this.mensaje = mensaje;
        repaint();
    }

    // ------------------------------------------------------------------ coordenadas

    private double aPixelX(double x) {
        return (x - xMin) / (xMax - xMin) * getWidth();
    }

    private double aPixelY(double y) {
        return getHeight() - (y - yMin) / (yMax - yMin) * getHeight();
    }

    private double aX(int px) {
        return xMin + px * (xMax - xMin) / getWidth();
    }

    private double aY(int py) {
        return yMin + (getHeight() - py) * (yMax - yMin) / getHeight();
    }

    private double evaluar(Expresion e, double x) {
        try {
            return e.evaluar(variable, x);
        } catch (RuntimeException ex) {
            return Double.NaN;
        }
    }

    /** Elige un rango que muestre bien las curvas (ignora valores extremos). */
    private void ajustarVista() {
        if (f == null) return;
        if (areaDesde != null && areaHasta != null) {
            double a = Math.min(areaDesde, areaHasta), b = Math.max(areaDesde, areaHasta);
            double margen = Math.max(1, (b - a) * 0.4);
            xMin = a - margen;
            xMax = b + margen;
        } else if (puntoX != null) {
            xMin = puntoX - 3;
            xMax = puntoX + 3;
        } else {
            xMin = -6;
            xMax = 6;
        }
        List<Double> valores = new ArrayList<>();
        for (Expresion e : new Expresion[]{f, g}) {
            if (e == null) continue;
            for (int i = 0; i <= 400; i++) {
                double y = evaluar(e, xMin + (xMax - xMin) * i / 400);
                if (Double.isFinite(y)) valores.add(y);
            }
        }
        if (valores.isEmpty()) {
            yMin = -10;
            yMax = 10;
            return;
        }
        double[] ys = valores.stream().mapToDouble(Double::doubleValue).toArray();
        Arrays.sort(ys);
        double bajo = ys[(int) (ys.length * 0.05)];
        double alto = ys[(int) Math.min(ys.length - 1, ys.length * 0.95)];
        if (areaDesde != null) {
            bajo = Math.min(bajo, 0);
            alto = Math.max(alto, 0);
        }
        if (bajo > 0 && bajo < 0.6 * (alto - bajo)) bajo = 0;
        if (alto < 0 && -alto < 0.6 * (alto - bajo)) alto = 0;
        if (alto - bajo < 1e-9) {
            bajo -= 1;
            alto += 1;
        }
        double margen = (alto - bajo) * 0.15;
        yMin = bajo - margen;
        yMax = alto + margen;
    }

    // ------------------------------------------------------------------ dibujo

    @Override
    protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g2 = (Graphics2D) g0.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, getWidth(), getHeight());

        if (f == null) {
            g2.setColor(Estilo.TEXTO_SUAVE);
            g2.setFont(Estilo.ui(Font.PLAIN, 14f));
            String texto = mensaje == null ? "" : mensaje;
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2, getHeight() / 2);
            g2.dispose();
            return;
        }
        dibujarCuadricula(g2);
        if (areaDesde != null && areaHasta != null) dibujarArea(g2);
        if (tangente != null) {
            dibujarCurva(g2, tangente, Estilo.VERDE, new BasicStroke(1.8f, BasicStroke.CAP_ROUND,
                    BasicStroke.JOIN_ROUND, 1, new float[]{7, 6}, 0));
        }
        dibujarCurva(g2, f, Estilo.AZUL, new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        if (g != null) {
            dibujarCurva(g2, g, Estilo.ACENTO, new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        }
        if (puntoX != null) {
            double y = evaluar(f, puntoX);
            if (Double.isFinite(y)) {
                int px = (int) aPixelX(puntoX), py = (int) aPixelY(y);
                g2.setColor(Color.WHITE);
                g2.fillOval(px - 6, py - 6, 12, 12);
                g2.setColor(Estilo.VERDE);
                g2.setStroke(new BasicStroke(2.5f));
                g2.drawOval(px - 6, py - 6, 12, 12);
            }
        }
        dibujarLeyenda(g2);
        dibujarCoordenadas(g2);
        g2.dispose();
    }

    private void dibujarCuadricula(Graphics2D g2) {
        int w = getWidth(), h = getHeight();
        double pasoX = paso(xMax - xMin, Math.max(2, w / 90));
        double pasoY = paso(yMax - yMin, Math.max(2, h / 70));
        g2.setFont(Estilo.ui(Font.PLAIN, 11f));
        FontMetrics fm = g2.getFontMetrics();

        g2.setStroke(new BasicStroke(1f));
        g2.setColor(CUADRICULA);
        for (double x = Math.ceil(xMin / pasoX) * pasoX; x <= xMax; x += pasoX) {
            int px = (int) Math.round(aPixelX(x));
            g2.drawLine(px, 0, px, h);
        }
        for (double y = Math.ceil(yMin / pasoY) * pasoY; y <= yMax; y += pasoY) {
            int py = (int) Math.round(aPixelY(y));
            g2.drawLine(0, py, w, py);
        }
        // ejes (si no se ven, las etiquetas se pegan al borde)
        int ejeY = (int) Math.round(aPixelY(0));
        int ejeX = (int) Math.round(aPixelX(0));
        g2.setColor(EJES);
        g2.setStroke(new BasicStroke(1.4f));
        if (ejeY >= 0 && ejeY <= h) g2.drawLine(0, ejeY, w, ejeY);
        if (ejeX >= 0 && ejeX <= w) g2.drawLine(ejeX, 0, ejeX, h);

        int yEtiquetas = Math.max(fm.getAscent() + 2, Math.min(h - 4, ejeY + fm.getAscent() + 4));
        g2.setColor(Estilo.TEXTO_SUAVE);
        for (double x = Math.ceil(xMin / pasoX) * pasoX; x <= xMax; x += pasoX) {
            if (Math.abs(x) < pasoX / 2) continue;
            String t = numeroCorto(x, pasoX);
            int px = (int) Math.round(aPixelX(x));
            int inicio = px - fm.stringWidth(t) / 2;
            if (inicio < 2 || inicio + fm.stringWidth(t) > w - 2) continue; // no cortar etiquetas en el borde
            g2.drawString(t, inicio, yEtiquetas);
        }
        int xEtiquetas = Math.max(4, Math.min(w - 40, ejeX + 6));
        for (double y = Math.ceil(yMin / pasoY) * pasoY; y <= yMax; y += pasoY) {
            if (Math.abs(y) < pasoY / 2) continue;
            String t = numeroCorto(y, pasoY);
            int py = (int) Math.round(aPixelY(y));
            g2.drawString(t, xEtiquetas, py + fm.getAscent() / 2 - 1);
        }
    }

    private void dibujarArea(Graphics2D g2) {
        double a = Math.min(areaDesde, areaHasta), b = Math.max(areaDesde, areaHasta);
        int muestras = 600;
        Path2D zona = new Path2D.Double();
        double cero = aPixelY(0);
        zona.moveTo(aPixelX(a), cero);
        for (int i = 0; i <= muestras; i++) {
            double x = a + (b - a) * i / muestras;
            double y = evaluar(f, x);
            if (!Double.isFinite(y)) continue;
            double py = Math.max(-5000, Math.min(5000 + getHeight(), aPixelY(y)));
            zona.lineTo(aPixelX(x), py);
        }
        zona.lineTo(aPixelX(b), cero);
        zona.closePath();
        g2.setColor(COLOR_AREA);
        g2.fill(zona);
        g2.setColor(new Color(234, 108, 20, 150));
        g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1, new float[]{4, 4}, 0));
        for (double x : new double[]{a, b}) {
            double y = evaluar(f, x);
            if (Double.isFinite(y)) {
                g2.drawLine((int) aPixelX(x), (int) cero, (int) aPixelX(x), (int) aPixelY(y));
            }
        }
    }

    private void dibujarCurva(Graphics2D g2, Expresion e, Color color, Stroke trazo) {
        int w = getWidth(), h = getHeight();
        int muestras = Math.max(200, w * 2);
        Path2D camino = new Path2D.Double();
        boolean dibujando = false;
        double anteriorPy = 0;
        for (int i = 0; i <= muestras; i++) {
            double x = xMin + (xMax - xMin) * i / muestras;
            double y = evaluar(e, x);
            if (!Double.isFinite(y)) {
                dibujando = false;
                continue;
            }
            double px = aPixelX(x), py = aPixelY(y);
            boolean fuera = py < -3 * h || py > 4 * h;
            boolean salto = dibujando && Math.abs(py - anteriorPy) > 2.5 * h; // asíntota vertical
            if (fuera || salto) {
                dibujando = false;
                if (fuera) continue;
            }
            if (!dibujando) {
                camino.moveTo(px, py);
                dibujando = true;
            } else {
                camino.lineTo(px, py);
            }
            anteriorPy = py;
        }
        g2.setColor(color);
        g2.setStroke(trazo);
        g2.draw(camino);
    }

    private void dibujarLeyenda(Graphics2D g2) {
        List<String> textos = new ArrayList<>();
        List<Color> colores = new ArrayList<>();
        textos.add(etiquetaF);
        colores.add(Estilo.AZUL);
        if (g != null && etiquetaG != null) {
            textos.add(etiquetaG);
            colores.add(Estilo.ACENTO);
        }
        if (tangente != null && etiquetaTangente != null) {
            textos.add(etiquetaTangente);
            colores.add(Estilo.VERDE);
        }
        if (areaDesde != null) {
            textos.add("Área bajo la curva");
            colores.add(new Color(234, 108, 20, 140));
        }
        g2.setFont(Estilo.mate(Font.PLAIN, 13.5f));
        FontMetrics fm = g2.getFontMetrics();
        int ancho = 0;
        for (String t : textos) ancho = Math.max(ancho, fm.stringWidth(t));
        int alto = textos.size() * 22 + 12;
        int x = 14, y = 14;
        g2.setColor(new Color(255, 255, 255, 235));
        g2.fill(new RoundRectangle2D.Double(x, y, ancho + 50, alto, 12, 12));
        g2.setColor(Estilo.BORDE);
        g2.setStroke(new BasicStroke(1f));
        g2.draw(new RoundRectangle2D.Double(x, y, ancho + 50, alto, 12, 12));
        for (int i = 0; i < textos.size(); i++) {
            int fila = y + 18 + i * 22;
            g2.setColor(colores.get(i));
            g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.drawLine(x + 12, fila - 4, x + 32, fila - 4);
            g2.setColor(Estilo.TINTA);
            g2.drawString(textos.get(i), x + 40, fila);
        }
    }

    private void dibujarCoordenadas(Graphics2D g2) {
        g2.setFont(Estilo.ui(Font.PLAIN, 11.5f));
        FontMetrics fm = g2.getFontMetrics();
        String ayuda = "Rueda: zoom  ·  Arrastrar: mover  ·  Doble clic: centrar";
        g2.setColor(Estilo.TEXTO_SUAVE);
        g2.drawString(ayuda, 14, getHeight() - 12);
        if (raton == null) return;
        double x = aX(raton.x);
        double y = evaluar(f, x);
        String texto = variable + " = " + Numeros.decimal(redondear(x)) + "    "
                + etiquetaF + " = " + (Double.isFinite(y) ? Numeros.decimal(redondear(y)) : "—");
        int ancho = fm.stringWidth(texto) + 20;
        int px = getWidth() - ancho - 12, py = getHeight() - 34;
        g2.setColor(new Color(24, 26, 35, 225));
        g2.fill(new RoundRectangle2D.Double(px, py, ancho, 24, 10, 10));
        g2.setColor(Color.WHITE);
        g2.drawString(texto, px + 10, py + 16);
        if (Double.isFinite(y)) {
            int cx = (int) aPixelX(x), cy = (int) aPixelY(y);
            g2.setColor(Estilo.AZUL);
            g2.fillOval(cx - 4, cy - 4, 8, 8);
        }
    }

    private static double redondear(double v) {
        return Math.round(v * 1e4) / 1e4;
    }

    /** Paso "bonito" para la cuadrícula: 1, 2 o 5 por una potencia de 10. */
    private static double paso(double rango, int divisiones) {
        double bruto = rango / divisiones;
        double magnitud = Math.pow(10, Math.floor(Math.log10(bruto)));
        double normal = bruto / magnitud;
        double base = normal < 1.5 ? 1 : normal < 3 ? 2 : normal < 7 ? 5 : 10;
        return base * magnitud;
    }

    private static String numeroCorto(double v, double paso) {
        int decimales = (int) Math.max(0, -Math.floor(Math.log10(paso)));
        return String.format(Locale.US, "%." + decimales + "f", v).replace("-", "−");
    }
}
