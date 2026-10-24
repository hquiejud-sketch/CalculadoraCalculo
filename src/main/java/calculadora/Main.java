package calculadora;

import calculadora.vista.Estilo;
import calculadora.vista.VentanaPrincipal;

import javax.swing.SwingUtilities;

/**
 * Punto de entrada de la aplicación.
 *
 * <p>Calculadora de Cálculo — Proyecto #3, Programación II (Ingeniería en Sistemas).
 * Resuelve derivadas e integrales simples y definidas respecto a x o y.</p>
 */
public class Main {

    public static void main(String[] args) {
        // Swing debe crearse en el hilo de eventos (Event Dispatch Thread)
        SwingUtilities.invokeLater(() -> {
            Estilo.aplicar();
            new VentanaPrincipal().setVisible(true);
        });
    }
}
