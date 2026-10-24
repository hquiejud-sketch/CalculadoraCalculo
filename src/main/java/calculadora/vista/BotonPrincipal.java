package calculadora.vista;

import javax.swing.JButton;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

/** Botón naranja redondeado (se dibuja a mano para que se vea igual en cualquier sistema). */
public class BotonPrincipal extends JButton {

    public BotonPrincipal(String texto) {
        super(texto);
        setFont(Estilo.ui(Font.BOLD, 15f));
        setForeground(Color.WHITE);
        setContentAreaFilled(false);
        setBorderPainted(false);
        setFocusPainted(false);
        setOpaque(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Color fondo = Estilo.ACENTO;
        if (getModel().isPressed()) fondo = fondo.darker();
        else if (getModel().isRollover()) fondo = new Color(214, 92, 10);
        g2.setColor(fondo);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(getForeground());
        g2.setFont(getFont());
        FontMetrics fm = g2.getFontMetrics();
        String texto = getText();
        g2.drawString(texto, (getWidth() - fm.stringWidth(texto)) / 2,
                (getHeight() - fm.getHeight()) / 2 + fm.getAscent());
        g2.dispose();
    }
}
