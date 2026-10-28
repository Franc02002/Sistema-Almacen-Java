package almacen.vista;

import almacen.controlador.ControladorAlmacen;
import almacen.modelo.Producto;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana independiente para "Subir y Gestionar Productos del Almacén".
 * Permite registrar productos nuevos con su código de barras, nombre, categoría,
 * precio unitario, stock actual y stock mínimo, o editar productos existentes.
 */
public class VentanaGestionProductos extends JFrame {

    private ControladorAlmacen controlador;
    private JTextField txtCodigoBarras;
    private JTextField txtNombre;
    private JComboBox<String> cmbCategoria;
    private JTextField txtPrecio;
    private JTextField txtStockInicial;
    private JTextField txtStockMinimo;
    private JTable tablaProductos;
    private DefaultTableModel modeloTabla;

    public VentanaGestionProductos(ControladorAlmacen controlador) {
        this.controlador = controlador;
        configurarVentana();
        inicializarComponentes();
        cargarTablaCatalogo();
    }

    private void configurarVentana() {
        setTitle("Catálogo de Productos — Subir Productos, Precios y Códigos de Barras");
        setSize(1020, 660);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        setLayout(new BorderLayout(12, 12));
    }

    private void inicializarComponentes() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(EstiloUI.COLOR_ENCABEZADO);
        banner.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel lblTit = new JLabel("ALTA Y GESTIÓN DE PRODUCTOS DEL ALMACÉN (CÓDIGO DE BARRAS, PRECIO Y STOCK)");
        lblTit.setFont(EstiloUI.FUENTE_TITULO);
        lblTit.setForeground(Color.WHITE);
        banner.add(lblTit, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        // Panel Izquierdo: Formulario de Alta / Modificación
        JPanel panelFormulario = new JPanel(new GridLayout(7, 2, 8, 14));
        panelFormulario.setBackground(Color.WHITE);
        panelFormulario.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(8, 16, 16, 8),
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(EstiloUI.COLOR_BOTON_PRIMARIO, 2),
                        " Datos del Producto a Guardar ")
        ));

        txtCodigoBarras = new JTextField(16);
        txtCodigoBarras.setFont(EstiloUI.FUENTE_CAMPO);
        txtNombre = new JTextField(16);
        txtNombre.setFont(EstiloUI.FUENTE_CAMPO);
        cmbCategoria = new JComboBox<String>(new String[] {
            "Almacén", "Bebidas", "Lácteos", "Pastas", "Galletitas", "Limpieza", "Conservas", "Fiambres", "Otros"
        });
        cmbCategoria.setEditable(true);
        cmbCategoria.setFont(EstiloUI.FUENTE_CAMPO);
        txtPrecio = new JTextField("0.00");
        txtPrecio.setFont(EstiloUI.FUENTE_CAMPO);
        txtStockInicial = new JTextField("0");
        txtStockInicial.setFont(EstiloUI.FUENTE_CAMPO);
        txtStockMinimo = new JTextField("5");
        txtStockMinimo.setFont(EstiloUI.FUENTE_CAMPO);

        panelFormulario.add(new JLabel("Código de Barras (Escáner):"));
        panelFormulario.add(txtCodigoBarras);
        panelFormulario.add(new JLabel("Nombre del Producto:"));
        panelFormulario.add(txtNombre);
        panelFormulario.add(new JLabel("Rubro / Categoría:"));
        panelFormulario.add(cmbCategoria);
        panelFormulario.add(new JLabel("Precio de Venta ($):"));
        panelFormulario.add(txtPrecio);
        panelFormulario.add(new JLabel("Stock Actual (Unidades):"));
        panelFormulario.add(txtStockInicial);
        panelFormulario.add(new JLabel("Stock Mínimo Alerta:"));
        panelFormulario.add(txtStockMinimo);

        JButton btnGuardar = EstiloUI.crearBotonGrande(
                "GUARDAR PRODUCTO",
                "Registra o actualiza en la BD SQL",
                EstiloUI.COLOR_BOTON_EXITO);
        JButton btnLimpiar = EstiloUI.crearBotonGrande(
                "LIMPIAR CAMPOS",
                "Prepara el formulario para un alta nueva",
                EstiloUI.COLOR_BOTON_NEUTRO);

        panelFormulario.add(btnGuardar);
        panelFormulario.add(btnLimpiar);

        add(panelFormulario, BorderLayout.WEST);

        // Panel Central: Tabla con todos los productos y su stock
        JPanel panelTabla = new JPanel(new BorderLayout(8, 8));
        panelTabla.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelTabla.setBorder(BorderFactory.createEmptyBorder(8, 8, 16, 16));

        String[] cols = new String[] {
            "Código de Barras", "Nombre del Producto", "Categoría",
            "Precio Unitario ($)", "Stock Actual", "Stock Mín.", "Estado Stock"
        };
        modeloTabla = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tablaProductos = new JTable(modeloTabla);
        EstiloUI.configurarTabla(tablaProductos);
        JScrollPane scroll = new JScrollPane(tablaProductos);
        scroll.setBorder(BorderFactory.createTitledBorder(" Catálogo Actual del Almacén (Haga clic en una fila para editarla) "));
        panelTabla.add(scroll, BorderLayout.CENTER);

        JPanel panelPie = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelPie.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        JButton btnRefrescar = new JButton("Actualizar Listado SQL");
        btnRefrescar.setFont(EstiloUI.FUENTE_SUBTITULO);
        panelPie.add(btnRefrescar);
        panelTabla.add(panelPie, BorderLayout.SOUTH);

        add(panelTabla, BorderLayout.CENTER);

        // Eventos imperativos
        tablaProductos.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int fila = tablaProductos.getSelectedRow();
                if (fila >= 0) {
                    String cod = modeloTabla.getValueAt(fila, 0).toString();
                    Producto p = controlador.buscarProductoPorCodigo(cod);
                    if (p != null) {
                        txtCodigoBarras.setText(p.getCodigoBarras());
                        txtNombre.setText(p.getNombre());
                        cmbCategoria.setSelectedItem(p.getCategoria());
                        txtPrecio.setText(String.valueOf(p.getPrecioUnitario()));
                        txtStockInicial.setText(String.valueOf(p.getStockActual()));
                        txtStockMinimo.setText(String.valueOf(p.getStockMinimo()));
                    }
                }
            }
        });

        btnGuardar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                guardarProductoActual();
            }
        });

        btnLimpiar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                limpiarFormulario();
            }
        });

        btnRefrescar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cargarTablaCatalogo();
            }
        });
    }

    private void guardarProductoActual() {
        try {
            String codigo = txtCodigoBarras.getText().trim();
            String nombre = txtNombre.getText().trim();
            String categoria = cmbCategoria.getSelectedItem() != null ? cmbCategoria.getSelectedItem().toString() : "General";
            double precio = Double.parseDouble(txtPrecio.getText().trim().replace(",", "."));
            int stock = Integer.parseInt(txtStockInicial.getText().trim());
            int minimo = Integer.parseInt(txtStockMinimo.getText().trim());

            String rta = controlador.guardarProducto(codigo, nombre, categoria, precio, stock, minimo);
            if (rta.startsWith("OK:")) {
                JOptionPane.showMessageDialog(this, rta, "Producto Guardado", JOptionPane.INFORMATION_MESSAGE);
                limpiarFormulario();
                cargarTablaCatalogo();
            } else {
                JOptionPane.showMessageDialog(this, rta, "Validación", JOptionPane.WARNING_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this,
                    "Verifique que el Precio sea un número decimal válido y el Stock un número entero.",
                    "Formato Numérico Inválido", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void limpiarFormulario() {
        txtCodigoBarras.setText("");
        txtNombre.setText("");
        txtPrecio.setText("0.00");
        txtStockInicial.setText("0");
        txtStockMinimo.setText("5");
        txtCodigoBarras.requestFocusInWindow();
    }

    private void cargarTablaCatalogo() {
        modeloTabla.setRowCount(0);
        List<Producto> lista = controlador.obtenerCatalogoProductos();
        for (int i = 0; i < lista.size(); i++) {
            Producto p = lista.get(i);
            String estado = p.tieneStockBajo() ? "REPONER STOCK" : "OK";
            modeloTabla.addRow(new Object[] {
                p.getCodigoBarras(),
                p.getNombre(),
                p.getCategoria(),
                String.format("$ %.2f", p.getPrecioUnitario()),
                Integer.valueOf(p.getStockActual()),
                Integer.valueOf(p.getStockMinimo()),
                estado
            });
        }
    }
}
