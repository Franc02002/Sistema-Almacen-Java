package almacen.vista;

import almacen.controlador.ControladorAlmacen;
import almacen.modelo.Producto;
import almacen.util.FilaCargaStock;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana independiente para "Cargar Stock de Productos Existentes".
 * Ofrece las dos modalidades solicitadas:
 * 1) Carga Manual: escaneando o ingresando el código de barras y la cantidad a sumar.
 * 2) Carga por Planilla Excel (.xlsx o .csv): selecciona el archivo, el programa lo interpreta,
 *    muestra la vista previa de cantidades e impacta el incremento de stock en la BD SQL.
 */
public class VentanaCargaStock extends JFrame {

    private ControladorAlmacen controlador;
    private JTextField txtCodigoManual;
    private JTextField txtCantidadManual;
    private JTextField txtObservacionManual;
    private JLabel lblProductoDetectado;
    private JTable tablaPreviewExcel;
    private DefaultTableModel modeloTablaExcel;
    private List<FilaCargaStock> filasPendientesExcel;
    private String nombreArchivoActual;

    public VentanaCargaStock(ControladorAlmacen controlador) {
        this.controlador = controlador;
        this.filasPendientesExcel = new ArrayList<FilaCargaStock>();
        this.nombreArchivoActual = "";
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Reposición e Ingreso de Mercadería — Carga Manual y por Planilla Excel (.xlsx)");
        setSize(1040, 680);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        getContentPane().setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        setLayout(new BorderLayout(12, 12));
    }

    private void inicializarComponentes() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(EstiloUI.COLOR_ENCABEZADO);
        banner.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));
        JLabel lblTit = new JLabel("CARGA DE STOCK A PRODUCTOS EXISTENTES (MANUAL O PLANILLA EXCEL)");
        lblTit.setFont(EstiloUI.FUENTE_TITULO);
        lblTit.setForeground(Color.WHITE);
        banner.add(lblTit, BorderLayout.WEST);
        add(banner, BorderLayout.NORTH);

        JPanel panelCuerpo = new JPanel(new BorderLayout(12, 12));
        panelCuerpo.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelCuerpo.setBorder(BorderFactory.createEmptyBorder(8, 16, 16, 16));

        // 1. SECCIÓN SUPERIOR: Carga Manual Rápida por Código de Barras
        JPanel panelManual = new JPanel(new BorderLayout(8, 8));
        panelManual.setBackground(Color.WHITE);
        panelManual.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(EstiloUI.COLOR_BOTON_PRIMARIO, 2),
                        " OPCIÓN 1: Carga Manual de Stock por Código de Barras "),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        JPanel filaCamposManual = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 6));
        filaCamposManual.setOpaque(false);

        txtCodigoManual = new JTextField(15);
        txtCodigoManual.setFont(EstiloUI.FUENTE_CAMPO);
        txtCantidadManual = new JTextField("10", 6);
        txtCantidadManual.setFont(EstiloUI.FUENTE_CAMPO);
        txtObservacionManual = new JTextField("Reposición proveedor", 18);
        txtObservacionManual.setFont(EstiloUI.FUENTE_CAMPO);

        JButton btnVerificarCodigo = new JButton("Verificar Código");
        btnVerificarCodigo.setFont(EstiloUI.FUENTE_SUBTITULO);

        JButton btnImpactarManual = EstiloUI.crearBotonGrande(
                "SUMAR STOCK MANUAL",
                "Incrementa la cantidad en la BD SQL",
                EstiloUI.COLOR_BOTON_EXITO);

        filaCamposManual.add(new JLabel("Código de Barras:"));
        filaCamposManual.add(txtCodigoManual);
        filaCamposManual.add(btnVerificarCodigo);
        filaCamposManual.add(new JLabel("Cantidad a Sumar:"));
        filaCamposManual.add(txtCantidadManual);
        filaCamposManual.add(new JLabel("Nota:"));
        filaCamposManual.add(txtObservacionManual);
        filaCamposManual.add(btnImpactarManual);

        lblProductoDetectado = new JLabel("Ingrese o escanee un código de barras para sumar unidades a su stock actual.");
        lblProductoDetectado.setFont(EstiloUI.FUENTE_SUBTITULO);
        lblProductoDetectado.setForeground(EstiloUI.COLOR_ENCABEZADO);

        panelManual.add(filaCamposManual, BorderLayout.CENTER);
        panelManual.add(lblProductoDetectado, BorderLayout.SOUTH);

        panelCuerpo.add(panelManual, BorderLayout.NORTH);

        // 2. SECCIÓN CENTRAL E INFERIOR: Carga Masiva interpretando Planilla Excel (.xlsx / .csv)
        JPanel panelExcel = new JPanel(new BorderLayout(10, 10));
        panelExcel.setBackground(Color.WHITE);
        panelExcel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createTitledBorder(
                        BorderFactory.createLineBorder(EstiloUI.COLOR_BOTON_EXITO, 2),
                        " OPCIÓN 2: Carga Masiva de Stock Interpretando Planilla Excel (.xlsx / .csv) "),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));

        JPanel barraBotonesExcel = new JPanel(new FlowLayout(FlowLayout.LEFT, 14, 6));
        barraBotonesExcel.setOpaque(false);

        JButton btnSeleccionarExcel = EstiloUI.crearBotonGrande(
                "1. SELECCIONAR PLANILLA EXCEL",
                "Abre un archivo .xlsx o .csv y lee sus filas",
                EstiloUI.COLOR_BOTON_PRIMARIO);
        btnSeleccionarExcel.setPreferredSize(new Dimension(280, 62));

        JButton btnConfirmarCargaExcel = EstiloUI.crearBotonGrande(
                "2. CONFIRMAR Y CARGAR STOCK",
                "Suma todas las cantidades leídas al inventario",
                EstiloUI.COLOR_BOTON_EXITO);
        btnConfirmarCargaExcel.setPreferredSize(new Dimension(280, 62));

        JLabel lblFormato = new JLabel(
                "<html><b>Formato esperado de la planilla Excel:</b><br>"
                + "Columna A: <code>Código de Barras</code> | Columna B: <code>Nombre</code> (opcional) | "
                + "Columna C (o B): <code>Cantidad a Sumar</code> | Columna D: <code>Precio</code> (opcional)</html>");
        barraBotonesExcel.add(btnSeleccionarExcel);
        barraBotonesExcel.add(btnConfirmarCargaExcel);
        barraBotonesExcel.add(lblFormato);

        panelExcel.add(barraBotonesExcel, BorderLayout.NORTH);

        String[] colsExcel = new String[] {
            "Fila Excel", "Código de Barras", "Producto en Almacén",
            "Cantidad a Sumar", "Precio Opcional ($)", "Estado de Interpretación"
        };
        modeloTablaExcel = new DefaultTableModel(colsExcel, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tablaPreviewExcel = new JTable(modeloTablaExcel);
        EstiloUI.configurarTabla(tablaPreviewExcel);
        panelExcel.add(new JScrollPane(tablaPreviewExcel), BorderLayout.CENTER);

        panelCuerpo.add(panelExcel, BorderLayout.CENTER);
        add(panelCuerpo, BorderLayout.CENTER);

        // Eventos imperativos
        btnVerificarCodigo.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                Producto p = controlador.buscarProductoPorCodigo(txtCodigoManual.getText());
                if (p != null) {
                    lblProductoDetectado.setForeground(EstiloUI.COLOR_BOTON_EXITO);
                    lblProductoDetectado.setText("Producto encontrado: " + p.getNombre()
                            + " | Stock actual: " + p.getStockActual() + " u. | Precio: $"
                            + String.format("%.2f", p.getPrecioUnitario()));
                } else {
                    lblProductoDetectado.setForeground(EstiloUI.COLOR_BOTON_PELIGRO);
                    lblProductoDetectado.setText("No existe un producto registrado con ese código de barras.");
                }
            }
        });

        btnImpactarManual.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    int cant = Integer.parseInt(txtCantidadManual.getText().trim());
                    String rta = controlador.cargarStockManual(
                            txtCodigoManual.getText(), cant, txtObservacionManual.getText());
                    if (rta.startsWith("OK:")) {
                        lblProductoDetectado.setForeground(EstiloUI.COLOR_BOTON_EXITO);
                        lblProductoDetectado.setText(rta);
                        JOptionPane.showMessageDialog(VentanaCargaStock.this, rta,
                                "Stock Actualizado", JOptionPane.INFORMATION_MESSAGE);
                        txtCodigoManual.setText("");
                    } else {
                        JOptionPane.showMessageDialog(VentanaCargaStock.this, rta,
                                "Atención", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(VentanaCargaStock.this,
                            "Ingrese una cantidad entera válida mayor a 0.");
                }
            }
        });

        btnSeleccionarExcel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                seleccionarYPrevisualizarExcel();
            }
        });

        btnConfirmarCargaExcel.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                confirmarCargaMasivaExcel();
            }
        });
    }

    private void seleccionarYPrevisualizarExcel() {
        JFileChooser selector = new JFileChooser(new File("."));
        selector.setDialogTitle("Seleccione la Planilla Excel (.xlsx o .csv) con el Stock a Cargar");
        selector.setFileFilter(new FileNameExtensionFilter("Planillas Excel (.xlsx, .csv)", "xlsx", "csv"));

        int opcion = selector.showOpenDialog(this);
        if (opcion == JFileChooser.APPROVE_OPTION) {
            File archivo = selector.getSelectedFile();
            try {
                filasPendientesExcel = controlador.previsualizarPlanillaExcel(archivo);
                nombreArchivoActual = archivo.getName();
                refrescarTablaExcel();
                JOptionPane.showMessageDialog(this,
                        "Se interpretaron " + filasPendientesExcel.size() + " filas desde '" + nombreArchivoActual
                        + "'.\nVerifique la tabla y presione '2. CONFIRMAR Y CARGAR STOCK'.",
                        "Planilla Excel Interpretada", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this,
                        "No se pudo leer la planilla: " + ex.getMessage(),
                        "Error de Lectura Excel", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void confirmarCargaMasivaExcel() {
        if (filasPendientesExcel == null || filasPendientesExcel.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Primero seleccione una planilla Excel con el botón '1. SELECCIONAR PLANILLA EXCEL'.");
            return;
        }
        int actualizados = controlador.aplicarCargaDesdeExcel(filasPendientesExcel, nombreArchivoActual);
        refrescarTablaExcel();
        JOptionPane.showMessageDialog(this,
                "Proceso finalizado: Se actualizaron/cargaron " + actualizados + " productos en la base de datos SQL.",
                "Carga Masiva Completada", JOptionPane.INFORMATION_MESSAGE);
    }

    private void refrescarTablaExcel() {
        modeloTablaExcel.setRowCount(0);
        for (int i = 0; i < filasPendientesExcel.size(); i++) {
            FilaCargaStock f = filasPendientesExcel.get(i);
            modeloTablaExcel.addRow(new Object[] {
                Integer.valueOf(f.getNumeroFila()),
                f.getCodigoBarras(),
                f.getNombreProducto(),
                Integer.valueOf(f.getCantidadASumar()),
                f.getNuevoPrecioOpcional() > 0 ? String.format("$ %.2f", f.getNuevoPrecioOpcional()) : "(Sin cambio)",
                f.getEstadoValidacion()
            });
        }
    }
}
