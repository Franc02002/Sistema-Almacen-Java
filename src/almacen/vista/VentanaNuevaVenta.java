package almacen.vista;

import almacen.controlador.ControladorAlmacen;
import almacen.modelo.DetalleVenta;
import almacen.modelo.Producto;
import almacen.modelo.Venta;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana independiente para "Empezar una Nueva Venta" (Punto de Venta con Escáner).
 * Permite escanear códigos de barras en forma continua, descuenta internamente el stock,
 * muestra los productos que conforman la venta actual, calcula en tiempo real la sumatoria
 * del total a cobrar y presenta botones grandes con explicación clara de cada acción.
 */
public class VentanaNuevaVenta extends JFrame {

    private ControladorAlmacen controlador;
    private JTextField txtCodigoBarras;
    private JSpinner spnCantidad;
    private JTable tablaVenta;
    private DefaultTableModel modeloTabla;
    private JLabel lblTotalPagar;
    private JLabel lblCantidadUnidades;
    private JLabel lblEstadoEscaneo;
    private JComboBox<String> cmbMetodoPago;

    public VentanaNuevaVenta(ControladorAlmacen controlador) {
        this.controlador = controlador;
        configurarVentana();
        inicializarComponentes();
        actualizarVistaVenta();
    }

    private void configurarVentana() {
        setTitle("Punto de Venta (POS) — Nueva Venta con Escáner de Código de Barras");
        setSize(1080, 720);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        setLayout(new BorderLayout(12, 12));
    }

    private void inicializarComponentes() {
        // 1. PANEL SUPERIOR: Encabezado + Lector de Código de Barras
        JPanel panelNorte = new JPanel(new BorderLayout(10, 10));
        panelNorte.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelNorte.setBorder(BorderFactory.createEmptyBorder(12, 16, 4, 16));

        JPanel bannerTitulo = new JPanel(new BorderLayout());
        bannerTitulo.setBackground(EstiloUI.COLOR_ENCABEZADO);
        bannerTitulo.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel lblTitulo = new JLabel("CAJA / NUEVA VENTA — ESCANEE EL CÓDIGO DE BARRAS DEL PRODUCTO");
        lblTitulo.setFont(EstiloUI.FUENTE_TITULO);
        lblTitulo.setForeground(Color.WHITE);
        bannerTitulo.add(lblTitulo, BorderLayout.WEST);
        panelNorte.add(bannerTitulo, BorderLayout.NORTH);

        JPanel panelEscaner = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 10));
        panelEscaner.setBackground(Color.WHITE);
        panelEscaner.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(EstiloUI.COLOR_BOTON_PRIMARIO, 2),
                " Entrada de Escáner Láser / Código de Barras (Presione ENTER tras escanear) "));

        JLabel lblCod = new JLabel("Código de Barras:");
        lblCod.setFont(EstiloUI.FUENTE_SUBTITULO);
        txtCodigoBarras = new JTextField(20);
        txtCodigoBarras.setFont(new Font("Monospaced", Font.BOLD, 18));
        txtCodigoBarras.setToolTipText("Pase el producto por el lector de código de barras o digite el código y presione Enter");

        JLabel lblCant = new JLabel("Cantidad:");
        lblCant.setFont(EstiloUI.FUENTE_SUBTITULO);
        spnCantidad = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spnCantidad.setFont(new Font("SansSerif", Font.BOLD, 18));
        spnCantidad.setPreferredSize(new Dimension(80, 36));

        JButton btnAgregarManual = EstiloUI.crearBotonGrande(
                "AGREGAR AL TICKET [ENTER]",
                "Lee el código y suma el producto a la venta",
                EstiloUI.COLOR_BOTON_PRIMARIO);
        btnAgregarManual.setPreferredSize(new Dimension(250, 54));

        JButton btnBuscarCatalogo = EstiloUI.crearBotonGrande(
                "BUSCAR EN LISTA",
                "Elegir producto si el código está borroso",
                EstiloUI.COLOR_BOTON_NEUTRO);
        btnBuscarCatalogo.setPreferredSize(new Dimension(220, 54));

        panelEscaner.add(lblCod);
        panelEscaner.add(txtCodigoBarras);
        panelEscaner.add(lblCant);
        panelEscaner.add(spnCantidad);
        panelEscaner.add(btnAgregarManual);
        panelEscaner.add(btnBuscarCatalogo);

        panelNorte.add(panelEscaner, BorderLayout.CENTER);

        lblEstadoEscaneo = new JLabel("Listo para escanear productos. El cursor está ubicado en el lector de código de barras.");
        lblEstadoEscaneo.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblEstadoEscaneo.setForeground(new Color(25, 118, 210));
        panelNorte.add(lblEstadoEscaneo, BorderLayout.SOUTH);

        add(panelNorte, BorderLayout.NORTH);

        // 2. PANEL CENTRAL: Tabla de Productos que conforman la venta actual
        JPanel panelCentro = new JPanel(new BorderLayout(8, 8));
        panelCentro.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelCentro.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));

        String[] columnas = new String[] {
            "N° Renglón", "Código de Barras", "Descripción del Producto",
            "Cantidad", "Precio Unitario ($)", "Subtotal ($)"
        };
        modeloTabla = new DefaultTableModel(columnas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaVenta = new JTable(modeloTabla);
        EstiloUI.configurarTabla(tablaVenta);
        JScrollPane scrollTabla = new JScrollPane(tablaVenta);
        scrollTabla.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 190, 205), 1),
                " Productos que conforman la Venta Actual "));
        panelCentro.add(scrollTabla, BorderLayout.CENTER);

        add(panelCentro, BorderLayout.CENTER);

        // 3. PANEL INFERIOR: Visor Gigante de Total a Cobrar + Botonera Grande de Acciones
        JPanel panelSur = new JPanel(new BorderLayout(12, 12));
        panelSur.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelSur.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

        // Panel de Totales y Medio de Pago
        JPanel panelTotales = new JPanel(new BorderLayout(16, 8));
        panelTotales.setBackground(new Color(232, 245, 233));
        panelTotales.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(EstiloUI.COLOR_BOTON_EXITO, 2),
                BorderFactory.createEmptyBorder(10, 20, 10, 20)
        ));

        JPanel panelIzqTotales = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 6));
        panelIzqTotales.setOpaque(false);
        lblCantidadUnidades = new JLabel("Unidades en la venta: 0 u.");
        lblCantidadUnidades.setFont(new Font("SansSerif", Font.BOLD, 17));
        JLabel lblPago = new JLabel("   Medio de Pago:");
        lblPago.setFont(new Font("SansSerif", Font.BOLD, 16));
        cmbMetodoPago = new JComboBox<String>(new String[] {"EFECTIVO", "TARJETA DE DÉBITO", "TRANSFERENCIA / QR"});
        cmbMetodoPago.setFont(new Font("SansSerif", Font.BOLD, 15));
        panelIzqTotales.add(lblCantidadUnidades);
        panelIzqTotales.add(lblPago);
        panelIzqTotales.add(cmbMetodoPago);

        lblTotalPagar = new JLabel("TOTAL A COBRAR: $ 0,00");
        lblTotalPagar.setFont(EstiloUI.FUENTE_TOTAL_GIGANTE);
        lblTotalPagar.setForeground(new Color(27, 94, 32));
        lblTotalPagar.setHorizontalAlignment(SwingConstants.RIGHT);

        panelTotales.add(panelIzqTotales, BorderLayout.WEST);
        panelTotales.add(lblTotalPagar, BorderLayout.EAST);
        panelSur.add(panelTotales, BorderLayout.NORTH);

        // Botonera Grande de Opciones Claras
        JPanel panelBotonesGrandes = new JPanel(new GridLayout(1, 5, 12, 0));
        panelBotonesGrandes.setBackground(EstiloUI.COLOR_FONDO_VENTANA);

        JButton btnCambiarCant = EstiloUI.crearBotonGrande(
                "CAMBIAR CANTIDAD",
                "Modifica las unidades del ítem marcado",
                EstiloUI.COLOR_BOTON_PRIMARIO);

        JButton btnQuitarItem = EstiloUI.crearBotonGrande(
                "QUITAR PRODUCTO",
                "Elimina el renglón marcado de la venta",
                EstiloUI.COLOR_BOTON_ADVERTENCIA);

        JButton btnCancelarVenta = EstiloUI.crearBotonGrande(
                "VACIAR / REINICIAR",
                "Cancela el ticket actual y empieza de cero",
                EstiloUI.COLOR_BOTON_PELIGRO);

        JButton btnVerUltimosTickets = EstiloUI.crearBotonGrande(
                "LEER VENTAS HECHAS",
                "Muestra el detalle de productos vendidos",
                EstiloUI.COLOR_BOTON_NEUTRO);

        JButton btnCobrarVenta = EstiloUI.crearBotonGrande(
                "COBRAR Y CERRAR VENTA",
                "Descuenta el stock en SQL y emite el ticket",
                EstiloUI.COLOR_BOTON_EXITO);

        panelBotonesGrandes.add(btnCambiarCant);
        panelBotonesGrandes.add(btnQuitarItem);
        panelBotonesGrandes.add(btnCancelarVenta);
        panelBotonesGrandes.add(btnVerUltimosTickets);
        panelBotonesGrandes.add(btnCobrarVenta);

        panelSur.add(panelBotonesGrandes, BorderLayout.CENTER);
        add(panelSur, BorderLayout.SOUTH);

        // =====================================================================
        // EVENTOS IMPERATIVOS
        // =====================================================================

        // Escaneo directo con tecla ENTER en el campo de código de barras
        txtCodigoBarras.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    procesarEscaneoActual();
                }
            }
        });

        btnAgregarManual.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                procesarEscaneoActual();
            }
        });

        btnBuscarCatalogo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                seleccionarProductoDesdeLista();
            }
        });

        btnCambiarCant.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cambiarCantidadSeleccionada();
            }
        });

        btnQuitarItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                quitarRenglonSeleccionado();
            }
        });

        btnCancelarVenta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelarVentaActual();
            }
        });

        btnVerUltimosTickets.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mostrarHistorialTicketsEmitidos();
            }
        });

        btnCobrarVenta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cobrarYFinalizar();
            }
        });
    }

    private void procesarEscaneoActual() {
        String codigo = txtCodigoBarras.getText().trim();
        int cantidad = ((Integer) spnCantidad.getValue()).intValue();

        String respuesta = controlador.escanearProductoEnVenta(codigo, cantidad);
        if (respuesta.startsWith("OK:")) {
            lblEstadoEscaneo.setForeground(new Color(46, 125, 50));
            lblEstadoEscaneo.setText(respuesta);
            txtCodigoBarras.setText("");
            spnCantidad.setValue(Integer.valueOf(1));
            actualizarVistaVenta();
        } else {
            lblEstadoEscaneo.setForeground(EstiloUI.COLOR_BOTON_PELIGRO);
            lblEstadoEscaneo.setText(respuesta);
            JOptionPane.showMessageDialog(this, respuesta, "Atención al escanear", JOptionPane.WARNING_MESSAGE);
            txtCodigoBarras.selectAll();
        }
        txtCodigoBarras.requestFocusInWindow();
    }

    private void seleccionarProductoDesdeLista() {
        List<Producto> catalogo = controlador.obtenerCatalogoProductos();
        if (catalogo.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No hay productos cargados en el almacén.");
            return;
        }
        String[] opciones = new String[catalogo.size()];
        for (int i = 0; i < catalogo.size(); i++) {
            Producto p = catalogo.get(i);
            opciones[i] = p.getCodigoBarras() + " — " + p.getNombre()
                    + " ($" + String.format("%.2f", p.getPrecioUnitario()) + " | Stock: " + p.getStockActual() + ")";
        }
        String elegido = (String) JOptionPane.showInputDialog(
                this,
                "Seleccione el producto a agregar a la venta:",
                "Búsqueda Rápida de Producto",
                JOptionPane.PLAIN_MESSAGE,
                null,
                opciones,
                opciones[0]);

        if (elegido != null) {
            String codigoExtraido = elegido.split(" — ")[0].trim();
            txtCodigoBarras.setText(codigoExtraido);
            procesarEscaneoActual();
        }
    }

    private void cambiarCantidadSeleccionada() {
        int fila = tablaVenta.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione primero un producto en la tabla de la venta actual.");
            return;
        }
        String entrada = JOptionPane.showInputDialog(this, "Ingrese la nueva cantidad de unidades:", "1");
        if (entrada != null && !entrada.trim().isEmpty()) {
            try {
                int nuevaCant = Integer.parseInt(entrada.trim());
                String rta = controlador.cambiarCantidadRenglonVenta(fila, nuevaCant);
                lblEstadoEscaneo.setText(rta);
                actualizarVistaVenta();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Ingrese un número entero válido.");
            }
        }
        txtCodigoBarras.requestFocusInWindow();
    }

    private void quitarRenglonSeleccionado() {
        int fila = tablaVenta.getSelectedRow();
        if (fila < 0) {
            JOptionPane.showMessageDialog(this, "Seleccione primero en la tabla el producto que desea quitar.");
            return;
        }
        controlador.quitarRenglonDeVenta(fila);
        lblEstadoEscaneo.setText("Producto quitado de la venta actual.");
        actualizarVistaVenta();
        txtCodigoBarras.requestFocusInWindow();
    }

    private void cancelarVentaActual() {
        if (controlador.getVentaEnCurso().getDetalles().isEmpty()) {
            return;
        }
        int confirmar = JOptionPane.showConfirmDialog(
                this,
                "¿Está seguro de vaciar todos los productos escaneados en esta venta?",
                "Confirmar Cancelación",
                JOptionPane.YES_NO_OPTION);
        if (confirmar == JOptionPane.YES_OPTION) {
            controlador.iniciarNuevaVenta();
            lblEstadoEscaneo.setText("Venta reiniciada. Puede comenzar a escanear para un nuevo cliente.");
            actualizarVistaVenta();
        }
        txtCodigoBarras.requestFocusInWindow();
    }

    private void cobrarYFinalizar() {
        Venta actual = controlador.getVentaEnCurso();
        if (actual.getDetalles().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Escanee al menos un producto antes de cobrar la venta.");
            txtCodigoBarras.requestFocusInWindow();
            return;
        }

        String medioPago = (String) cmbMetodoPago.getSelectedItem();
        Venta cerrada = controlador.confirmarYFinalizarVenta(medioPago);
        if (cerrada != null) {
            // Construir comprobante detallado leyendo todos los productos que conformaron la venta
            StringBuilder ticket = new StringBuilder();
            ticket.append("========================================================\n");
            ticket.append("          ALMACÉN BARRIAL — TICKET DE VENTA N° ").append(cerrada.getIdVenta()).append("\n");
            ticket.append("========================================================\n");
            ticket.append("Fecha y Hora : ").append(cerrada.getFechaHora().toString()).append("\n");
            ticket.append("Medio de Pago: ").append(cerrada.getMetodoPago()).append("\n");
            ticket.append("--------------------------------------------------------\n");
            ticket.append(String.format("%-15s %-22s %5s %10s\n", "CÓDIGO", "PRODUCTO", "CANT.", "SUBTOTAL"));
            ticket.append("--------------------------------------------------------\n");

            List<DetalleVenta> itemsVendidos = cerrada.getDetalles();
            for (int i = 0; i < itemsVendidos.size(); i++) {
                DetalleVenta d = itemsVendidos.get(i);
                String nomCorto = d.getNombreProducto();
                if (nomCorto.length() > 22) {
                    nomCorto = nomCorto.substring(0, 22);
                }
                ticket.append(String.format("%-15s %-22s %5d $%9.2f\n",
                        d.getCodigoBarras(), nomCorto, d.getCantidad(), d.getSubtotal()));
            }
            ticket.append("--------------------------------------------------------\n");
            ticket.append("TOTAL DE UNIDADES : ").append(cerrada.getCantidadItems()).append(" u.\n");
            ticket.append(String.format("TOTAL COBRADO     : $ %.2f\n", cerrada.getMontoTotal()));
            ticket.append("========================================================\n");
            ticket.append("(El stock de cada producto fue descontado automáticamente)");

            JTextArea areaTicket = new JTextArea(ticket.toString(), 18, 56);
            areaTicket.setEditable(false);
            areaTicket.setFont(new Font("Monospaced", Font.PLAIN, 13));
            JOptionPane.showMessageDialog(this, new JScrollPane(areaTicket),
                    "Venta Registrada con Éxito — Lectura de Productos de la Venta",
                    JOptionPane.INFORMATION_MESSAGE);

            lblEstadoEscaneo.setForeground(new Color(46, 125, 50));
            lblEstadoEscaneo.setText("Última venta N° " + cerrada.getIdVenta() + " cobrada por $"
                    + String.format("%.2f", cerrada.getMontoTotal()) + ". Lista para nuevo cliente.");
            actualizarVistaVenta();
        } else {
            JOptionPane.showMessageDialog(this,
                    "No se pudo completar la venta. Verifique el stock disponible.",
                    "Error Transaccional", JOptionPane.ERROR_MESSAGE);
        }
        txtCodigoBarras.requestFocusInWindow();
    }

    private void mostrarHistorialTicketsEmitidos() {
        List<Venta> ultimas = controlador.obtenerUltimasVentas(15);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ultimas.size(); i++) {
            Venta v = ultimas.get(i);
            sb.append("TICKET #").append(v.getIdVenta())
              .append(" | Fecha: ").append(v.getFechaHora())
              .append(" | Medio: ").append(v.getMetodoPago())
              .append(" | Total: $").append(String.format("%.2f", v.getMontoTotal())).append("\n");
            List<DetalleVenta> dets = v.getDetalles();
            for (int j = 0; j < dets.size(); j++) {
                DetalleVenta d = dets.get(j);
                sb.append("   -> [").append(d.getCodigoBarras()).append("] ")
                  .append(d.getNombreProducto()).append(" x ").append(d.getCantidad())
                  .append(" u. ($").append(String.format("%.2f", d.getPrecioUnitarioHistorico()))
                  .append(" c/u) = $").append(String.format("%.2f", d.getSubtotal())).append("\n");
            }
            sb.append("--------------------------------------------------------------------------\n");
        }
        JTextArea area = new JTextArea(sb.toString(), 22, 68);
        area.setEditable(false);
        area.setFont(new Font("Monospaced", Font.PLAIN, 13));
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Lectura de Productos que Conforman las Ventas Realizadas",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void actualizarVistaVenta() {
        modeloTabla.setRowCount(0);
        Venta actual = controlador.getVentaEnCurso();
        List<DetalleVenta> items = actual.getDetalles();

        for (int i = 0; i < items.size(); i++) {
            DetalleVenta d = items.get(i);
            Object[] fila = new Object[] {
                Integer.valueOf(i + 1),
                d.getCodigoBarras(),
                d.getNombreProducto(),
                Integer.valueOf(d.getCantidad()),
                String.format("$ %.2f", d.getPrecioUnitarioHistorico()),
                String.format("$ %.2f", d.getSubtotal())
            };
            modeloTabla.addRow(fila);
        }

        lblCantidadUnidades.setText("Unidades en la venta: " + actual.getCantidadItems() + " u.");
        lblTotalPagar.setText(String.format("TOTAL A COBRAR: $ %.2f", actual.getMontoTotal()));
    }
}
