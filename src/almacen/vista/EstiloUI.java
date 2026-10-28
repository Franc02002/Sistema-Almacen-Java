package almacen.vista;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.table.JTableHeader;

/**
 * Clase utilitaria de diseño visual para garantizar que todas las ventanas de la aplicación
 * sean simples, eficaces, legibles y con botones grandes de alto contraste pensados
 * para el trabajo diario en un almacén.
 */
public class EstiloUI {

    public static final Color COLOR_FONDO_VENTANA = new Color(244, 246, 249);
    public static final Color COLOR_ENCABEZADO = new Color(27, 54, 93);
    public static final Color COLOR_BOTON_PRIMARIO = new Color(25, 118, 210);
    public static final Color COLOR_BOTON_EXITO = new Color(46, 125, 50);
    public static final Color COLOR_BOTON_PELIGRO = new Color(198, 40, 40);
    public static final Color COLOR_BOTON_ADVERTENCIA = new Color(230, 81, 0);
    public static final Color COLOR_BOTON_NEUTRO = new Color(69, 90, 100);

    public static final Font FUENTE_TITULO = new Font("SansSerif", Font.BOLD, 22);
    public static final Font FUENTE_SUBTITULO = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FUENTE_BOTON_GRANDE = new Font("SansSerif", Font.BOLD, 15);
    public static final Font FUENTE_CAMPO = new Font("SansSerif", Font.PLAIN, 15);
    public static final Font FUENTE_TABLA = new Font("SansSerif", Font.PLAIN, 14);
    public static final Font FUENTE_TOTAL_GIGANTE = new Font("SansSerif", Font.BOLD, 34);

    /**
     * Crea un botón grande con texto principal y subtítulo descriptivo para que el cajero
     * sepa inmediatamente qué hace cada opción.
     */
    public static JButton crearBotonGrande(String tituloPrincipal, String descripcionAccion, Color colorFondo) {
        String html = "<html><center><b style='font-size:12pt;'>" + tituloPrincipal + "</b><br>"
                    + "<span style='font-size:9pt; font-weight:normal;'>" + descripcionAccion + "</span></center></html>";
        JButton boton = new JButton(html);
        boton.setBackground(colorFondo);
        boton.setForeground(Color.WHITE);
        boton.setFocusPainted(false);
        boton.setOpaque(true);
        boton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorFondo.darker(), 2),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)
        ));
        boton.setPreferredSize(new Dimension(220, 68));
        boton.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return boton;
    }

    /**
     * Configura una tabla Swing con filas amplias y encabezados claros.
     */
    public static void configurarTabla(JTable tabla) {
        tabla.setFont(FUENTE_TABLA);
        tabla.setRowHeight(28);
        tabla.setGridColor(new Color(210, 215, 220));
        tabla.setSelectionBackground(new Color(bbHex(207), bbHex(226), bbHex(243)));
        tabla.setSelectionForeground(Color.BLACK);

        JTableHeader header = tabla.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 14));
        header.setBackground(new Color(225, 232, 240));
        header.setForeground(COLOR_ENCABEZADO);
        header.setPreferredSize(new Dimension(header.getWidth(), 34));
    }

    private static int bbHex(int v) {
        return v;
    }
}
