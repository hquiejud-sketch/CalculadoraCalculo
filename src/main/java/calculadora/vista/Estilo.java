package calculadora.vista;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.util.Arrays;
import java.util.List;

/** Colores y fuentes de la aplicación (un solo lugar para cambiar el diseño). */
public final class Estilo {

    private Estilo() {
    }

    public static final Color FONDO = new Color(244, 246, 250);
    public static final Color TARJETA = Color.WHITE;
    public static final Color BORDE = new Color(222, 227, 235);
    public static final Color TINTA = new Color(17, 24, 39);
    public static final Color TEXTO_SUAVE = new Color(100, 112, 130);
    public static final Color OSCURO = new Color(24, 26, 35);
    public static final Color ACENTO = new Color(234, 108, 20);
    public static final Color ACENTO_CLARO = new Color(255, 241, 228);
    public static final Color AZUL = new Color(37, 99, 235);
    public static final Color VERDE = new Color(22, 150, 80);
    public static final Color ROJO = new Color(200, 40, 40);
    public static final Color AMBAR = new Color(180, 100, 0);

    public static final String FAMILIA_UI = elegir("Aa", "Segoe UI", "Inter", "Helvetica Neue", "DejaVu Sans", "SansSerif");
    public static final String FAMILIA_MATE = elegir("∫√π·∂≈−", "Cambria", "Segoe UI", "DejaVu Serif", "Serif");
    public static final String FAMILIA_MONO = elegir("x^2", "Consolas", "JetBrains Mono", "DejaVu Sans Mono", "Monospaced");

    public static Font ui(int estilo, float tamano) {
        return new Font(FAMILIA_UI, estilo, 12).deriveFont(tamano);
    }

    public static Font mate(int estilo, float tamano) {
        return new Font(FAMILIA_MATE, estilo, 12).deriveFont(tamano);
    }

    public static Font mono(int estilo, float tamano) {
        return new Font(FAMILIA_MONO, estilo, 12).deriveFont(tamano);
    }

    /** Usa la primera fuente instalada que pueda mostrar los símbolos indicados. */
    private static String elegir(String simbolos, String... familias) {
        List<String> instaladas;
        try {
            instaladas = Arrays.asList(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        } catch (Exception e) {
            return familias[familias.length - 1];
        }
        for (String familia : familias) {
            if (instaladas.contains(familia) && new Font(familia, Font.PLAIN, 12).canDisplayUpTo(simbolos) == -1) {
                return familia;
            }
        }
        return familias[familias.length - 1];
    }

    /** Look & Feel Nimbus con los colores de la aplicación. */
    public static void aplicar() {
        UIManager.put("control", FONDO);
        UIManager.put("nimbusFocus", new Color(251, 160, 90));
        UIManager.put("nimbusSelectionBackground", ACENTO);
        UIManager.put("nimbusSelection", ACENTO);
        UIManager.put("defaultFont", ui(Font.PLAIN, 13f));
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ignorada) {
            // si Nimbus no está disponible se queda el aspecto por defecto
        }
        UIManager.getLookAndFeelDefaults().put("defaultFont", ui(Font.PLAIN, 13f));
        UIManager.getLookAndFeelDefaults().put("Button.contentMargins", new java.awt.Insets(6, 6, 6, 6));
    }

    public static String hex(Color c) {
        return String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
    }
}
