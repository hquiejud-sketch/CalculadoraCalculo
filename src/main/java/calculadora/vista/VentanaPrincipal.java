package calculadora.vista;

import calculadora.logica.AnalizadorSintactico;
import calculadora.logica.Derivada;
import calculadora.logica.ErrorCalculo;
import calculadora.logica.ErrorSintaxis;
import calculadora.logica.IntegralDefinida;
import calculadora.logica.IntegralIndefinida;
import calculadora.logica.Operacion;
import calculadora.logica.ResultadoCalculo;
import calculadora.modelo.Expresion;
import calculadora.modelo.Numeros;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.List;

import static calculadora.modelo.Ops.mul;
import static calculadora.modelo.Ops.num;
import static calculadora.modelo.Ops.resta;
import static calculadora.modelo.Ops.suma;
import static calculadora.modelo.Ops.var;

/**
 * Ventana principal (JFrame) de la calculadora.
 *
 * <p>Solo se encarga de la interfaz: lee lo que escribe el usuario, crea la {@link Operacion}
 * correspondiente, la ejecuta y muestra el {@link ResultadoCalculo}. Todos los cálculos
 * viven en los paquetes {@code modelo} y {@code logica}.</p>
 */
public class VentanaPrincipal extends JFrame {

    private static final String TARJETA_DERIVADA = "derivada";
    private static final String TARJETA_INDEFINIDA = "indefinida";
    private static final String TARJETA_DEFINIDA = "definida";

    private final AnalizadorSintactico analizador = new AnalizadorSintactico();

    private final JLabel etiquetaFuncion = new JLabel("f(x) =");
    private final JTextField campoFuncion = new JTextField("3x^2 + 5x");
    private final JComboBox<String> comboVariable = new JComboBox<>(new String[]{"x", "y"});
    private final JRadioButton radioDerivada = new JRadioButton("Derivada", true);
    private final JRadioButton radioIndefinida = new JRadioButton("Integral indefinida");
    private final JRadioButton radioDefinida = new JRadioButton("Integral definida");
    private final JSpinner spinnerOrden = new JSpinner(new SpinnerNumberModel(1, 1, 5, 1));
    private final JCheckBox checkPunto = new JCheckBox("Evaluar en x =");
    private final JTextField campoPunto = campoCorto("2");
    private final JTextField campoA = campoCorto("0");
    private final JTextField campoB = campoCorto("2");
    private final CardLayout tarjetas = new CardLayout();
    private final JPanel panelOpciones = new JPanel(tarjetas);
    private final JEditorPane panelResultado = new JEditorPane();
    private final PanelGrafica grafica = new PanelGrafica();
    private final DefaultListModel<Consulta> historial = new DefaultListModel<>();
    private final JList<Consulta> listaHistorial = new JList<>(historial);
    private final JTabbedPane pestanas = new JTabbedPane();
    private final JButton botonCalcular = new BotonPrincipal("Calcular");
    private JButton botonVariable;
    private String textoCopiable = "";

    public VentanaPrincipal() {
        super("Calculadora de Cálculo — Integrales y Derivadas");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setIconImage(crearIcono());
        setMinimumSize(new Dimension(1100, 720));
        setSize(1320, 820);
        setLocationRelativeTo(null);

        JPanel raiz = new JPanel(new BorderLayout());
        raiz.setBackground(Estilo.FONDO);
        raiz.add(crearEncabezado(), BorderLayout.NORTH);
        raiz.add(crearPanelIzquierdo(), BorderLayout.WEST);
        raiz.add(crearPestanas(), BorderLayout.CENTER);
        setContentPane(raiz);

        configurarEventos();
        panelResultado.setText(FormatoHtml.bienvenida());
        getRootPane().setDefaultButton(botonCalcular); // Enter = Calcular
    }

    // =====================================================================
    //  CONSTRUCCIÓN DE LA INTERFAZ
    // =====================================================================

    private JComponent crearEncabezado() {
        JPanel encabezado = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new GradientPaint(0, 0, Estilo.OSCURO, getWidth(), 0, new Color(40, 34, 30)));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        encabezado.setBorder(new EmptyBorder(14, 22, 14, 22));

        JLabel logo = new JLabel(new javax.swing.ImageIcon(crearIcono().getScaledInstance(40, 40, java.awt.Image.SCALE_SMOOTH)));
        JLabel titulo = new JLabel("Calculadora de Cálculo");
        titulo.setFont(Estilo.ui(Font.BOLD, 21f));
        titulo.setForeground(Color.WHITE);
        JLabel subtitulo = new JLabel("Derivadas e integrales simples y definidas · variable x o y");
        subtitulo.setFont(Estilo.ui(Font.PLAIN, 12.5f));
        subtitulo.setForeground(new Color(190, 196, 208));

        JPanel textos = new JPanel(new GridLayout(2, 1));
        textos.setOpaque(false);
        textos.add(titulo);
        textos.add(subtitulo);

        JPanel izquierda = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        izquierda.setOpaque(false);
        izquierda.add(logo);
        izquierda.add(textos);

        JLabel curso = new JLabel("Ingeniería en Sistemas · Programación II");
        curso.setFont(Estilo.ui(Font.BOLD, 11.5f));
        curso.setForeground(new Color(251, 160, 90));
        curso.setBorder(new CompoundBorder(new LineBorder(new Color(251, 160, 90, 150), 1, true), new EmptyBorder(5, 12, 5, 12)));
        JPanel derecha = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        derecha.setOpaque(false);
        derecha.add(curso);

        encabezado.add(izquierda, BorderLayout.WEST);
        encabezado.add(derecha, BorderLayout.EAST);
        return encabezado;
    }

    private JComponent crearPanelIzquierdo() {
        JPanel controles = new JPanel();
        controles.setLayout(new BoxLayout(controles, BoxLayout.Y_AXIS));
        controles.setOpaque(false);
        controles.add(tarjeta("Función", crearPanelFuncion()));
        controles.add(Box.createVerticalStrut(12));
        controles.add(tarjeta("Variable y operación", crearPanelOperacion()));
        controles.add(Box.createVerticalStrut(12));
        controles.add(crearBotones());

        panelResultado.setContentType("text/html");
        panelResultado.setEditable(false);
        panelResultado.setBackground(Color.WHITE);
        panelResultado.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        JScrollPane scroll = new JScrollPane(panelResultado);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel izquierdo = new JPanel(new BorderLayout(0, 12));
        izquierdo.setBackground(Estilo.FONDO);
        izquierdo.setBorder(new EmptyBorder(14, 14, 14, 6));
        izquierdo.setPreferredSize(new Dimension(470, 10));
        izquierdo.add(controles, BorderLayout.NORTH);
        izquierdo.add(tarjeta("Resultado", scroll), BorderLayout.CENTER);
        return izquierdo;
    }

    private JComponent crearPanelFuncion() {
        etiquetaFuncion.setFont(Estilo.mate(Font.BOLD | Font.ITALIC, 19f));
        etiquetaFuncion.setForeground(Estilo.TINTA);
        campoFuncion.setFont(Estilo.mono(Font.PLAIN, 17f));
        campoFuncion.setBorder(new CompoundBorder(new LineBorder(Estilo.BORDE, 1, true), new EmptyBorder(8, 10, 8, 10)));
        campoFuncion.setToolTipText("Ejemplos: 3x^2 + 5x,  x·sen(x),  e^(2x),  1/(x^2 + 1)");

        JPanel fila = new JPanel(new BorderLayout(10, 0));
        fila.setOpaque(false);
        fila.add(etiquetaFuncion, BorderLayout.WEST);
        fila.add(campoFuncion, BorderLayout.CENTER);

        String[][] teclas = {
                {"sen", "sen()"}, {"cos", "cos()"}, {"tan", "tan()"}, {"ln", "ln()"}, {"log", "log()"},
                {"√", "√()"}, {"eˣ", "e^()"}, {"xⁿ", "^"}, {"π", "π"}, {"(", "("}, {")", ")"},
                {"|x|", "||"}, {"·", "*"}, {"x", "VARIABLE"}};
        JPanel teclado = new JPanel(new GridLayout(2, 7, 6, 6));
        teclado.setOpaque(false);
        for (String[] tecla : teclas) {
            JButton b = new JButton(tecla[0]);
            b.setFont(Estilo.mate(Font.PLAIN, 14f));
            b.setFocusable(false);
            b.setMargin(new java.awt.Insets(4, 2, 4, 2));
            String valor = tecla[1];
            b.addActionListener(e -> insertar(valor));
            if (valor.equals("VARIABLE")) botonVariable = b;
            teclado.add(b);
        }

        JPanel panel = new JPanel(new BorderLayout(0, 10));
        panel.setOpaque(false);
        panel.add(fila, BorderLayout.NORTH);
        panel.add(teclado, BorderLayout.CENTER);
        return panel;
    }

    private JComponent crearPanelOperacion() {
        ButtonGroup grupo = new ButtonGroup();
        for (JRadioButton r : List.of(radioDerivada, radioIndefinida, radioDefinida)) {
            grupo.add(r);
            r.setOpaque(false);
            r.setFont(Estilo.ui(Font.PLAIN, 13.5f));
        }
        checkPunto.setOpaque(false);
        campoPunto.setEnabled(false);
        spinnerOrden.setPreferredSize(new Dimension(58, 28));

        JLabel notaVariable = nota("las demás letras se tratan como constantes");
        JPanel filaVariable = fila(etiqueta("Variable:"), comboVariable, Box.createHorizontalStrut(6), notaVariable);
        JPanel filaOperaciones = fila(radioDerivada, radioIndefinida, radioDefinida);

        panelOpciones.setOpaque(false);
        panelOpciones.add(fila(etiqueta("Orden:"), spinnerOrden, Box.createHorizontalStrut(14), checkPunto, campoPunto),
                TARJETA_DERIVADA);
        panelOpciones.add(fila(nota("Se agrega la constante de integración C.")), TARJETA_INDEFINIDA);
        panelOpciones.add(fila(etiqueta("Desde a ="), campoA, Box.createHorizontalStrut(8), etiqueta("hasta b ="), campoB),
                TARJETA_DEFINIDA);

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setOpaque(false);
        for (JComponent c : List.of(filaVariable, filaOperaciones, panelOpciones)) {
            c.setAlignmentX(Component.LEFT_ALIGNMENT);
            panel.add(c);
        }
        return panel;
    }

    private JComponent crearBotones() {
        botonCalcular.setPreferredSize(new Dimension(150, 42));

        JButton limpiar = new JButton("Limpiar");
        limpiar.addActionListener(e -> limpiar());
        JButton copiar = new JButton("Copiar resultado");
        copiar.addActionListener(e -> copiarResultado());
        for (JButton b : List.of(limpiar, copiar)) b.setFont(Estilo.ui(Font.PLAIN, 13f));

        JPanel panel = new JPanel(new GridLayout(1, 3, 8, 0));
        panel.setOpaque(false);
        panel.add(botonCalcular);
        panel.add(limpiar);
        panel.add(copiar);
        panel.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 42));
        return panel;
    }

    private JComponent crearPestanas() {
        JPanel marcoGrafica = new JPanel(new BorderLayout());
        marcoGrafica.setBorder(new LineBorder(Estilo.BORDE));
        marcoGrafica.add(grafica);

        // Historial
        listaHistorial.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listaHistorial.setCellRenderer(new RenderizadorHtml());
        JButton borrar = new JButton("Borrar historial");
        borrar.addActionListener(e -> historial.clear());
        JPanel panelHistorial = new JPanel(new BorderLayout(0, 8));
        panelHistorial.setBorder(new EmptyBorder(10, 10, 10, 10));
        panelHistorial.add(nota("Doble clic en un cálculo para cargarlo de nuevo."), BorderLayout.NORTH);
        panelHistorial.add(new JScrollPane(listaHistorial), BorderLayout.CENTER);
        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        sur.setOpaque(false);
        sur.add(borrar);
        panelHistorial.add(sur, BorderLayout.SOUTH);

        // Ayuda y ejemplos
        DefaultListModel<Consulta> modeloEjemplos = new DefaultListModel<>();
        Ejemplos.todos().forEach(modeloEjemplos::addElement);
        JList<Consulta> listaEjemplos = new JList<>(modeloEjemplos);
        listaEjemplos.setCellRenderer(new RenderizadorHtml());
        listaEjemplos.addMouseListener(alHacerDobleClic(listaEjemplos));
        JPanel panelEjemplos = new JPanel(new BorderLayout(0, 8));
        panelEjemplos.setBorder(new EmptyBorder(10, 10, 10, 6));
        panelEjemplos.add(nota("Ejemplos: doble clic para calcularlos."), BorderLayout.NORTH);
        panelEjemplos.add(new JScrollPane(listaEjemplos), BorderLayout.CENTER);
        panelEjemplos.setPreferredSize(new Dimension(390, 10));

        JEditorPane textoAyuda = new JEditorPane("text/html", Ejemplos.ayudaHtml());
        textoAyuda.setEditable(false);
        textoAyuda.setCaretPosition(0);
        JScrollPane scrollAyuda = new JScrollPane(textoAyuda);
        scrollAyuda.setBorder(new EmptyBorder(10, 6, 10, 10));
        JPanel panelAyuda = new JPanel(new BorderLayout());
        panelAyuda.add(panelEjemplos, BorderLayout.WEST);
        panelAyuda.add(scrollAyuda, BorderLayout.CENTER);

        pestanas.setFont(Estilo.ui(Font.BOLD, 13f));
        pestanas.addTab("Gráfica", marcoGrafica);
        pestanas.addTab("Historial", panelHistorial);
        pestanas.addTab("Ayuda y ejemplos", panelAyuda);

        JPanel contenedor = new JPanel(new BorderLayout());
        contenedor.setBackground(Estilo.FONDO);
        contenedor.setBorder(new EmptyBorder(14, 8, 14, 14));
        contenedor.add(pestanas);
        return contenedor;
    }

    // =====================================================================
    //  EVENTOS
    // =====================================================================

    private void configurarEventos() {
        botonCalcular.addActionListener(e -> calcular());
        comboVariable.addActionListener(e -> actualizarVariable());
        radioDerivada.addActionListener(e -> cambiarOperacion());
        radioIndefinida.addActionListener(e -> cambiarOperacion());
        radioDefinida.addActionListener(e -> cambiarOperacion());
        checkPunto.addActionListener(e -> campoPunto.setEnabled(checkPunto.isSelected()));
        listaHistorial.addMouseListener(alHacerDobleClic(listaHistorial));
    }

    private MouseAdapter alHacerDobleClic(JList<Consulta> lista) {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && lista.getSelectedValue() != null) {
                    cargar(lista.getSelectedValue());
                    calcular();
                    pestanas.setSelectedIndex(0);
                }
            }
        };
    }

    /** Botón Calcular: texto → árbol → operación → resultado en pantalla. */
    private void calcular() {
        Consulta consulta = leerConsulta();
        try {
            Expresion funcion = analizador.analizar(consulta.getFuncion());
            Operacion operacion = crearOperacion(consulta);
            ResultadoCalculo resultado = operacion.ejecutar(funcion);

            panelResultado.setText(FormatoHtml.resultado(resultado));
            panelResultado.setCaretPosition(0);
            textoCopiable = FormatoHtml.textoPlano(resultado);
            actualizarGrafica(resultado);
            agregarAlHistorial(consulta, resultado);
        } catch (ErrorSintaxis ex) {
            mostrarError(ex.getMessage());
            campoFuncion.requestFocusInWindow();
            campoFuncion.setCaretPosition(Math.min(ex.getPosicion(), campoFuncion.getText().length()));
        } catch (ErrorCalculo ex) {
            mostrarError(ex.getMessage());
        } catch (RuntimeException ex) {
            mostrarError("No se pudo completar el cálculo (" + ex.getClass().getSimpleName() + ").");
        }
    }

    private Consulta leerConsulta() {
        Consulta.Tipo tipo = radioDerivada.isSelected() ? Consulta.Tipo.DERIVADA
                : radioIndefinida.isSelected() ? Consulta.Tipo.INDEFINIDA : Consulta.Tipo.DEFINIDA;
        return new Consulta(campoFuncion.getText().trim(), variable(), tipo, (Integer) spinnerOrden.getValue(),
                checkPunto.isSelected() ? campoPunto.getText().trim() : null,
                campoA.getText().trim(), campoB.getText().trim());
    }

    /** Polimorfismo: según la opción elegida se crea un tipo distinto de Operacion. */
    private Operacion crearOperacion(Consulta c) throws ErrorCalculo {
        return switch (c.getTipo()) {
            case DERIVADA -> new Derivada(c.getVariable(), c.getOrden(),
                    c.getPunto() == null ? null : leerNumero(c.getPunto(), "El punto de evaluación"));
            case INDEFINIDA -> new IntegralIndefinida(c.getVariable());
            case DEFINIDA -> new IntegralDefinida(c.getVariable(),
                    leerNumero(c.getLimiteA(), "El límite inferior a"),
                    leerNumero(c.getLimiteB(), "El límite superior b"));
        };
    }

    private Expresion leerNumero(String texto, String nombre) throws ErrorCalculo {
        try {
            return analizador.analizar(texto);
        } catch (ErrorSintaxis e) {
            throw new ErrorCalculo(nombre + " no es válido: " + e.getMessage());
        }
    }

    private void actualizarGrafica(ResultadoCalculo r) {
        Operacion op = r.getOperacion();
        String v = op.getVariable();
        Expresion f = r.getFuncion();
        boolean unaVariable = f.variables().stream().allMatch(v::equals)
                && (r.getResultado() == null || r.getResultado().variables().stream().allMatch(v::equals));
        if (!unaVariable) {
            grafica.mostrarMensaje("La gráfica solo está disponible para funciones de una variable (" + v + ").");
            return;
        }
        String nombreF = "f(" + v + ")";
        if (op instanceof Derivada d) {
            Double x0 = null;
            Expresion tangente = null;
            String etiquetaTangente = null;
            if (d.getPunto() != null && d.getOrden() == 1 && r.getValorNumerico() != null) {
                x0 = d.getPunto().valorConstante();
                double y0 = f.evaluar(v, x0);
                double m = r.getValorNumerico();
                if (Double.isFinite(y0) && Double.isFinite(m)) {
                    // recta tangente: y = f(a) + f'(a)(x − a)
                    tangente = suma(num(y0), mul(num(m), resta(var(v), num(x0))));
                    etiquetaTangente = "Tangente en " + v + " = " + Numeros.decimal(x0);
                }
            }
            grafica.mostrar(v, f, nombreF, r.getResultado(),
                    FormatoHtml.nombreDerivadaTexto(d.getOrden(), v, false), null, null, x0, tangente, etiquetaTangente);
        } else if (op instanceof IntegralIndefinida) {
            grafica.mostrar(v, f, nombreF, r.getResultado(), "F(" + v + ") con C = 0", null, null, null, null, null);
        } else if (op instanceof IntegralDefinida id) {
            grafica.mostrar(v, f, nombreF, null, null, id.getA(), id.getB(), null, null, null);
        }
    }

    private void agregarAlHistorial(Consulta consulta, ResultadoCalculo r) {
        String texto = FormatoHtml.expresion(FormatoHtml.textoPlano(r)).replace("\n", "<br>");
        String html = "<html><div style='padding:3px 2px'><font color='" + Estilo.hex(Estilo.ACENTO) + "'><b>"
                + r.getOperacion().getNombre().toUpperCase() + "</b></font> &nbsp;<font color='gray'>variable "
                + consulta.getVariable() + "</font><br>" + texto + "</div></html>";
        historial.add(0, consulta.conResumen(html));
        while (historial.size() > 60) historial.remove(historial.size() - 1);
    }

    private void cargar(Consulta c) {
        campoFuncion.setText(c.getFuncion());
        comboVariable.setSelectedItem(c.getVariable());
        switch (c.getTipo()) {
            case DERIVADA -> radioDerivada.setSelected(true);
            case INDEFINIDA -> radioIndefinida.setSelected(true);
            case DEFINIDA -> radioDefinida.setSelected(true);
        }
        spinnerOrden.setValue(c.getOrden());
        checkPunto.setSelected(c.getPunto() != null);
        campoPunto.setEnabled(c.getPunto() != null);
        if (c.getPunto() != null) campoPunto.setText(c.getPunto());
        campoA.setText(c.getLimiteA());
        campoB.setText(c.getLimiteB());
        actualizarVariable();
        cambiarOperacion();
    }

    private void insertar(String texto) {
        String t = texto.equals("VARIABLE") ? variable() : texto;
        campoFuncion.replaceSelection(t);
        if (t.endsWith("()") || t.equals("||")) {
            campoFuncion.setCaretPosition(campoFuncion.getCaretPosition() - 1);
        }
        campoFuncion.requestFocusInWindow();
    }

    private void actualizarVariable() {
        String v = variable();
        etiquetaFuncion.setText("f(" + v + ") =");
        checkPunto.setText("Evaluar en " + v + " =");
        if (botonVariable != null) botonVariable.setText(v);
    }

    private void cambiarOperacion() {
        String tarjeta = radioDerivada.isSelected() ? TARJETA_DERIVADA
                : radioIndefinida.isSelected() ? TARJETA_INDEFINIDA : TARJETA_DEFINIDA;
        tarjetas.show(panelOpciones, tarjeta);
    }

    private void limpiar() {
        campoFuncion.setText("");
        textoCopiable = "";
        panelResultado.setText(FormatoHtml.bienvenida());
        grafica.mostrarMensaje("La gráfica aparece aquí cuando calculas algo.");
        campoFuncion.requestFocusInWindow();
    }

    private void copiarResultado() {
        if (textoCopiable.isEmpty()) return;
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(textoCopiable), null);
    }

    private void mostrarError(String mensaje) {
        panelResultado.setText(FormatoHtml.error(mensaje));
        textoCopiable = "";
    }

    private String variable() {
        return (String) comboVariable.getSelectedItem();
    }

    // =====================================================================
    //  AYUDANTES DE DISEÑO
    // =====================================================================

    private static JPanel tarjeta(String titulo, JComponent contenido) {
        JPanel p = new JPanel(new BorderLayout(0, 10));
        p.setBackground(Estilo.TARJETA);
        p.setBorder(new CompoundBorder(new LineBorder(Estilo.BORDE, 1, true), new EmptyBorder(12, 14, 14, 14)));
        JLabel t = new JLabel(titulo.toUpperCase());
        t.setFont(Estilo.ui(Font.BOLD, 11f));
        t.setForeground(Estilo.ACENTO);
        p.add(t, BorderLayout.NORTH);
        p.add(contenido, BorderLayout.CENTER);
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static JPanel fila(Component... componentes) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 4));
        p.setOpaque(false);
        for (Component c : componentes) p.add(c);
        return p;
    }

    private static JLabel etiqueta(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Estilo.ui(Font.PLAIN, 13.5f));
        return l;
    }

    private static JLabel nota(String texto) {
        JLabel l = new JLabel(texto);
        l.setFont(Estilo.ui(Font.PLAIN, 12f));
        l.setForeground(Estilo.TEXTO_SUAVE);
        return l;
    }

    private static JTextField campoCorto(String texto) {
        JTextField c = new JTextField(texto, 6);
        c.setFont(Estilo.mono(Font.PLAIN, 14f));
        return c;
    }

    /** Ícono de la ventana: ∫ blanco sobre un cuadro naranja. */
    private static BufferedImage crearIcono() {
        BufferedImage img = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setPaint(new GradientPaint(0, 0, new Color(249, 140, 50), 64, 64, new Color(220, 90, 10)));
        g.fillRoundRect(0, 0, 64, 64, 18, 18);
        g.setColor(Color.WHITE);
        g.setFont(Estilo.mate(Font.BOLD, 46f));
        java.awt.FontMetrics fm = g.getFontMetrics();
        g.drawString("∫", (64 - fm.stringWidth("∫")) / 2, 64 - (64 - fm.getAscent()) / 2 - 8);
        g.dispose();
        return img;
    }

    /** Muestra el HTML de cada consulta en las listas (historial y ejemplos). */
    private static class RenderizadorHtml extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> lista, Object valor, int indice,
                                                      boolean seleccionado, boolean foco) {
            JLabel l = (JLabel) super.getListCellRendererComponent(lista, valor, indice, seleccionado, foco);
            l.setFont(Estilo.mate(Font.PLAIN, 14f));
            l.setBorder(new CompoundBorder(new javax.swing.border.MatteBorder(0, 0, 1, 0, Estilo.BORDE),
                    new EmptyBorder(6, 8, 6, 8)));
            if (seleccionado) {
                l.setBackground(Estilo.ACENTO_CLARO);
                l.setForeground(Estilo.TINTA);
            }
            return l;
        }
    }
}
