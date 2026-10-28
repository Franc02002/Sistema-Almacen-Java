package almacen.vista;

import almacen.controlador.ControladorAlmacen;
import almacen.modelo.ResumenMetricaDia;
import almacen.modelo.ResumenProductoVenta;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana independiente para "Obtener Métricas de Negocio":
 * 1) Monto total hecho Diario, Semanal y Mensual.
 * 2) Ranking de Productos Menos Vendidos.
 * 3) Día de la semana menos vendido en base al análisis de una semana ya existente.
 */
public class VentanaMetricasNegocio extends JFrame {

    private ControladorAlmacen controlador;
    private JLabel lblMontoDiario;
    private JLabel lblMontoSemanal;
    private JLabel lblMontoMensual;
    private JTable tablaMenosVendidos;
    private DefaultTableModel modeloMenosVendidos;
    private JComboBox<String> cmbSemanasExistentes;
    private List<LocalDate> listaLunesSemanas;
    private JTable tablaSemana;
    private DefaultTableModel modeloSemana;
    private JLabel lblResultadoDiaMenosVendido;

    public VentanaMetricasNegocio(ControladorAlmacen controlador) {
        this.controlador = controlador;
        configurarVentana();
        inicializarComponentes();
        cargarTodasLasMetricas();
    }

    private void configurarVentana() {
        setTitle("Métricas de Negocio — Recaudación Diaria/Semanal/Mensual, Menos Vendidos y Análisis Semanal");
        setSize(1100, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        setLayout(new BorderLayout(12, 12));
    }

    private void inicializarComponentes() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(EstiloUI.COLOR_ENCABEZADO);
        banner.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel lblTit = new JLabel("MÉTRICAS E INDICADORES DE NEGOCIO DEL ALMACÉN");
        lblTit.setFont(EstiloUI.FUENTE_TITULO);
        lblTit.setForeground(Color.WHITE);
        banner.add(lblTit, BorderLayout.WEST);

        JButton btnRefrescar = new JButton("Recalcular Métricas Ahora");
        btnRefrescar.setFont(EstiloUI.FUENTE_SUBTITULO);
        banner.add(btnRefrescar, BorderLayout.EAST);
        add(banner, BorderLayout.NORTH);

        JPanel panelPrincipal = new JPanel(new BorderLayout(14, 14));
        panelPrincipal.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelPrincipal.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

        // 1. TARJETAS DE RECAUDACIÓN: Diario, Semanal y Mensual
        JPanel panelTarjetas = new JPanel(new GridLayout(1, 3, 16, 0));
        panelTarjetas.setBackground(EstiloUI.COLOR_FONDO_VENTANA);

        lblMontoDiario = new JLabel("$ 0.00", SwingConstants.CENTER);
        lblMontoSemanal = new JLabel("$ 0.00", SwingConstants.CENTER);
        lblMontoMensual = new JLabel("$ 0.00", SwingConstants.CENTER);

        panelTarjetas.add(crearTarjetaKPI("MONTO TOTAL HECHO DIARIO (HOY)", lblMontoDiario, new Color(25, 118, 210)));
        panelTarjetas.add(crearTarjetaKPI("MONTO TOTAL HECHO SEMANAL (7 DÍAS)", lblMontoSemanal, new Color(46, 125, 50)));
        panelTarjetas.add(crearTarjetaKPI("MONTO TOTAL HECHO MENSUAL (MES ACTUAL)", lblMontoMensual, new Color(106, 27, 154)));

        panelPrincipal.add(panelTarjetas, BorderLayout.NORTH);

        // 2. PANEL DIVIDIDO: Izquierda = Productos Menos Vendidos | Derecha = Día menos vendido de una semana existente
        JPanel panelTablas = new JPanel(new GridLayout(1, 2, 16, 0));
        panelTablas.setBackground(EstiloUI.COLOR_FONDO_VENTANA);

        // Panel Izquierdo: Productos Menos Vendidos
        JPanel panelIzq = new JPanel(new BorderLayout(8, 8));
        panelIzq.setBackground(Color.WHITE);
        panelIzq.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstiloUI.COLOR_BOTON_ADVERTENCIA, 2),
                " Ranking de Productos Menos Vendidos (De menor a mayor rotación) "));

        String[] colsProd = new String[] {
            "Código", "Producto", "Stock Actual", "Unid. Vendidas", "Total Generado ($)"
        };
        modeloMenosVendidos = new DefaultTableModel(colsProd, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaMenosVendidos = new JTable(modeloMenosVendidos);
        EstiloUI.configurarTabla(tablaMenosVendidos);
        panelIzq.add(new JScrollPane(tablaMenosVendidos), BorderLayout.CENTER);

        // Panel Derecho: Análisis de Semana Existente -> Día de la semana menos vendido
        JPanel panelDer = new JPanel(new BorderLayout(8, 8));
        panelDer.setBackground(Color.WHITE);
        panelDer.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstiloUI.COLOR_BOTON_PRIMARIO, 2),
                " Análisis de Semana Existente: Día de la Semana Menos Vendido "));

        JPanel panelSelectorSemana = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 6));
        panelSelectorSemana.setOpaque(false);
        panelSelectorSemana.add(new JLabel("Semana a analizar (Lunes a Domingo):"));
        cmbSemanasExistentes = new JComboBox<String>();
        cmbSemanasExistentes.setFont(EstiloUI.FUENTE_SUBTITULO);
        JButton btnAnalizarSemana = new JButton("Analizar Semana");
        btnAnalizarSemana.setFont(EstiloUI.FUENTE_SUBTITULO);
        panelSelectorSemana.add(cmbSemanasExistentes);
        panelSelectorSemana.add(btnAnalizarSemana);

        panelDer.add(panelSelectorSemana, BorderLayout.NORTH);

        String[] colsSem = new String[] {
            "Día de la Semana", "Fecha", "Tickets Emitidos", "Unidades Vendidas", "Monto Total del Día ($)"
        };
        modeloSemana = new DefaultTableModel(colsSem, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaSemana = new JTable(modeloSemana);
        EstiloUI.configurarTabla(tablaSemana);
        panelDer.add(new JScrollPane(tablaSemana), BorderLayout.CENTER);

        lblResultadoDiaMenosVendido = new JLabel("Día menos vendido: —", SwingConstants.CENTER);
        lblResultadoDiaMenosVendido.setFont(new Font("SansSerif", Font.BOLD, 15));
        lblResultadoDiaMenosVendido.setOpaque(true);
        lblResultadoDiaMenosVendido.setBackground(new Color(255, 243, 224));
        lblResultadoDiaMenosVendido.setForeground(new Color(198, 40, 40));
        lblResultadoDiaMenosVendido.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        panelDer.add(lblResultadoDiaMenosVendido, BorderLayout.SOUTH);

        panelTablas.add(panelIzq);
        panelTablas.add(panelDer);

        panelPrincipal.add(panelTablas, BorderLayout.CENTER);
        add(panelPrincipal, BorderLayout.CENTER);

        // Eventos
        btnRefrescar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarTodasLasMetricas();
            }
        });

        btnAnalizarSemana.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarTablaSemanaSeleccionada();
            }
        });

        cmbSemanasExistentes.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                actualizarTablaSemanaSeleccionada();
            }
        });
    }

    private JPanel crearTarjetaKPI(String titulo, JLabel lblValor, Color colorBorde) {
        JPanel card = new JPanel(new BorderLayout(6, 6));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(colorBorde, 2),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)
        ));
        JLabel lblTit = new JLabel(titulo, SwingConstants.CENTER);
        lblTit.setFont(new Font("SansSerif", Font.BOLD, 13));
        lblTit.setForeground(colorBorde);

        lblValor.setFont(new Font("SansSerif", Font.BOLD, 26));
        lblValor.setForeground(Color.DARK_GRAY);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    private void cargarTodasLasMetricas() {
        lblMontoDiario.setText(String.format("$ %.2f", controlador.obtenerMontoDiarioHoy()));
        lblMontoSemanal.setText(String.format("$ %.2f", controlador.obtenerMontoSemanalActual()));
        lblMontoMensual.setText(String.format("$ %.2f", controlador.obtenerMontoMensualActual()));

        modeloMenosVendidos.setRowCount(0);
        List<ResumenProductoVenta> menosVendidos = controlador.obtenerProductosMenosVendidos(20);
        for (int i = 0; i < menosVendidos.size(); i++) {
            ResumenProductoVenta r = menosVendidos.get(i);
            modeloMenosVendidos.addRow(new Object[] {
                r.getCodigoBarras(),
                r.getNombre(),
                Integer.valueOf(r.getStockActual()),
                Integer.valueOf(r.getUnidadesVendidas()),
                String.format("$ %.2f", r.getMontoTotalGenerado())
            });
        }

        listaLunesSemanas = controlador.obtenerSemanasDisponiblesParaAnalisis();
        cmbSemanasExistentes.removeAllItems();
        for (int i = 0; i < listaLunesSemanas.size(); i++) {
            LocalDate lunes = listaLunesSemanas.get(i);
            LocalDate domingo = lunes.plusDays(6);
            cmbSemanasExistentes.addItem("Semana del " + lunes.toString() + " al " + domingo.toString());
        }
        // Seleccionar por defecto la semana anterior completa si hay más de una semana disponible
        if (listaLunesSemanas.size() > 1) {
            cmbSemanasExistentes.setSelectedIndex(1);
        } else if (!listaLunesSemanas.isEmpty()) {
            cmbSemanasExistentes.setSelectedIndex(0);
        }
        actualizarTablaSemanaSeleccionada();
    }

    private void actualizarTablaSemanaSeleccionada() {
        int idx = cmbSemanasExistentes.getSelectedIndex();
        if (idx < 0 || listaLunesSemanas == null || idx >= listaLunesSemanas.size()) {
            return;
        }
        LocalDate lunesElegido = listaLunesSemanas.get(idx);
        List<ResumenMetricaDia> desglose = controlador.obtenerDesgloseDeSemana(lunesElegido);

        modeloSemana.setRowCount(0);
        for (int i = 0; i < desglose.size(); i++) {
            ResumenMetricaDia d = desglose.get(i);
            modeloSemana.addRow(new Object[] {
                d.getNombreDia(),
                d.getFecha().toString(),
                Integer.valueOf(d.getCantidadTickets()),
                Integer.valueOf(d.getUnidadesVendidas()),
                String.format("$ %.2f", d.getMontoTotal())
            });
        }

        ResumenMetricaDia peorDia = controlador.determinarDiaMenosVendido(desglose);
        if (peorDia != null) {
            lblResultadoDiaMenosVendido.setText(
                    "DÍA DE LA SEMANA MENOS VENDIDO: " + peorDia.getNombreDia().toUpperCase()
                    + " (" + peorDia.getFecha().toString() + ") — Recaudación: $"
                    + String.format("%.2f", peorDia.getMontoTotal())
                    + " (" + peorDia.getCantidadTickets() + " ventas / "
                    + peorDia.getUnidadesVendidas() + " u.)");
        }
    }
}
