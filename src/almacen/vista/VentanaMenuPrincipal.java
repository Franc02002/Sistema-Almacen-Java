package almacen.vista;

import almacen.conexion.ConexionBD;
import almacen.controlador.ControladorAlmacen;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

/**
 * Ventana Principal del Sistema de Gestión de Almacén.
 * Presenta un tablero limpio con botones grandes de acceso directo para abrir
 * cada acción en su ventana propia independiente.
 */
public class VentanaMenuPrincipal extends JFrame {

    private ControladorAlmacen controlador;

    public VentanaMenuPrincipal(ControladorAlmacen controlador) {
        this.controlador = controlador;
        configurarVentana();
        inicializarComponentes();
    }

    private void configurarVentana() {
        setTitle("Sistema de Gestión de Almacén — Menú Principal (Arquitectura MVC + DAO + SQL)");
        setSize(900, 560);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        getContentPane().setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        setLayout(new BorderLayout(16, 16));

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                ConexionBD.cerrarConexion();
            }
        });
    }

    private void inicializarComponentes() {
        // Encabezado
        JPanel panelEncabezado = new JPanel(new BorderLayout(6, 6));
        panelEncabezado.setBackground(EstiloUI.COLOR_ENCABEZADO);
        panelEncabezado.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        JLabel lblTitulo = new JLabel("SISTEMA DE GESTIÓN Y PUNTO DE VENTA — ALMACÉN", SwingConstants.CENTER);
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 24));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSubtitulo = new JLabel(
                "Seleccione un módulo para abrir su ventana dedicada (Base de Datos SQL Relacional Local Activa)",
                SwingConstants.CENTER);
        lblSubtitulo.setFont(new Font("SansSerif", Font.PLAIN, 14));
        lblSubtitulo.setForeground(new Color(207, 226, 243));

        panelEncabezado.add(lblTitulo, BorderLayout.NORTH);
        panelEncabezado.add(lblSubtitulo, BorderLayout.SOUTH);
        add(panelEncabezado, BorderLayout.NORTH);

        // Cuadrícula de Botones Grandes hacia cada Ventana Propia
        JPanel panelCentral = new JPanel(new GridLayout(2, 2, 22, 22));
        panelCentral.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelCentral.setBorder(BorderFactory.createEmptyBorder(16, 32, 16, 32));

        JButton btnNuevaVenta = EstiloUI.crearBotonGrande(
                "1. EMPEZAR NUEVA VENTA (CAJA / ESCÁNER)",
                "Escanear códigos de barras, descontar stock automáticamente y sumar el total a cobrar",
                EstiloUI.COLOR_BOTON_EXITO);

        JButton btnGestionProductos = EstiloUI.crearBotonGrande(
                "2. SUBIR Y GESTIONAR PRODUCTOS",
                "Registrar nuevos productos con código de barras, precio unitario y stock inicial",
                EstiloUI.COLOR_BOTON_PRIMARIO);

        JButton btnCargaStock = EstiloUI.crearBotonGrande(
                "3. CARGAR STOCK (MANUAL O EXCEL)",
                "Sumar unidades manualmente o interpretando una planilla Microsoft Excel (.xlsx / .csv)",
                EstiloUI.COLOR_BOTON_ADVERTENCIA);

        JButton btnMetricas = EstiloUI.crearBotonGrande(
                "4. MÉTRICAS Y REPORTES DE NEGOCIO",
                "Ver monto diario, semanal y mensual, productos menos vendidos y día menos vendido de la semana",
                EstiloUI.COLOR_BOTON_NEUTRO);

        panelCentral.add(btnNuevaVenta);
        panelCentral.add(btnGestionProductos);
        panelCentral.add(btnCargaStock);
        panelCentral.add(btnMetricas);

        add(panelCentral, BorderLayout.CENTER);

        // Pie de ventana
        JPanel panelPie = new JPanel(new BorderLayout());
        panelPie.setBackground(EstiloUI.COLOR_FONDO_VENTANA);
        panelPie.setBorder(BorderFactory.createEmptyBorder(4, 32, 16, 32));
        JLabel lblPie = new JLabel(
                "Arquitectura en Capas (Modelo - DAO - Controlador - Vista Swing) | Paradigma Imperativo | Motor SQL Embebido");
        lblPie.setFont(new Font("SansSerif", Font.ITALIC, 13));
        lblPie.setForeground(Color.DARK_GRAY);
        panelPie.add(lblPie, BorderLayout.WEST);
        add(panelPie, BorderLayout.SOUTH);

        // Eventos para abrir cada ventana independiente
        btnNuevaVenta.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VentanaNuevaVenta v = new VentanaNuevaVenta(controlador);
                v.setVisible(true);
            }
        });

        btnGestionProductos.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VentanaGestionProductos v = new VentanaGestionProductos(controlador);
                v.setVisible(true);
            }
        });

        btnCargaStock.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VentanaCargaStock v = new VentanaCargaStock(controlador);
                v.setVisible(true);
            }
        });

        btnMetricas.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                VentanaMetricasNegocio v = new VentanaMetricasNegocio(controlador);
                v.setVisible(true);
            }
        });
    }
}
