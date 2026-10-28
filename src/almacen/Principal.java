package almacen;

import almacen.conexion.ConexionBD;
import almacen.controlador.ControladorAlmacen;
import almacen.modelo.DetalleVenta;
import almacen.modelo.Producto;
import almacen.modelo.ResumenMetricaDia;
import almacen.modelo.ResumenProductoVenta;
import almacen.modelo.Venta;
import almacen.util.FilaCargaStock;
import almacen.vista.VentanaMenuPrincipal;
import java.io.File;
import java.time.LocalDate;
import java.util.List;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Clase Principal de arranque del Sistema de Gestión de Almacén.
 * Inicializa la base de datos SQL relacional local y lanza la interfaz gráfica Swing.
 * Incluye además el argumento opcional "--verificar" para ejecutar una prueba
 * integral automatizada de todos los requerimientos desde consola.
 */
public class Principal {

    public static void main(String[] args) {
        // 1. Inicializar la Base de Datos Relacional SQL local y sus tablas
        ConexionBD.inicializarBaseDeDatos();
        final ControladorAlmacen controlador = new ControladorAlmacen();

        // 2. Si se invoca con "--verificar", ejecutar prueba integral y mostrar resultados
        if (args != null && args.length > 0 && "--verificar".equals(args[0])) {
            ejecutarVerificacionIntegral(controlador, args.length > 1 ? args[1] : null);
            ConexionBD.cerrarConexion();
            return;
        }

        // 3. Configurar el Look and Feel del sistema e iniciar la interfaz gráfica de escritorio
        try {
            UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (Exception exIgnorado) {
            // Continuar con Look and Feel por defecto
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                VentanaMenuPrincipal menu = new VentanaMenuPrincipal(controlador);
                menu.setVisible(true);
            }
        });
    }

    /**
     * Ejecuta una prueba secuencial e imperativa de todos los casos de uso requeridos:
     * alta de producto, carga manual de stock, carga desde planilla Excel (.xlsx),
     * simulación de venta por escáner de código de barras y cálculo de métricas.
     */
    private static void ejecutarVerificacionIntegral(ControladorAlmacen ctrl, String rutaExcelPrueba) {
        System.out.println("=== INICIANDO VERIFICACIÓN INTEGRAL DEL SISTEMA DE ALMACÉN ===");

        // A. Alta de un nuevo producto con código de barras
        String rtaAlta = ctrl.guardarProducto("7790001009998", "Harina de Trigo 000 1Kg", "Almacén", 1150.00, 25, 8);
        System.out.println("[1] Alta de producto: " + rtaAlta);

        // B. Carga manual de stock
        String rtaManual = ctrl.cargarStockManual("7790001009998", 15, "Ingreso manual de prueba");
        System.out.println("[2] Carga manual de stock: " + rtaManual);

        // C. Interpretación y carga de stock desde planilla Excel (.xlsx)
        if (rutaExcelPrueba != null) {
            try {
                File archivoExcel = new File(rutaExcelPrueba);
                List<FilaCargaStock> filas = ctrl.previsualizarPlanillaExcel(archivoExcel);
                System.out.println("[3] Filas interpretadas desde planilla Excel (" + archivoExcel.getName() + "): " + filas.size());
                int actualizados = ctrl.aplicarCargaDesdeExcel(filas, archivoExcel.getName());
                System.out.println("    -> Productos actualizados desde Excel: " + actualizados);
            } catch (Exception e) {
                System.out.println("    -> Error en prueba de Excel: " + e.getMessage());
            }
        }

        // D. Simulación de Nueva Venta escaneando códigos de barras
        ctrl.iniciarNuevaVenta();
        System.out.println("[4] Escaneo en Punto de Venta:");
        System.out.println("    -> " + ctrl.escanearProductoEnVenta("7790001001015", 2));
        System.out.println("    -> " + ctrl.escanearProductoEnVenta("7790001009998", 3));
        Venta cerrada = ctrl.confirmarYFinalizarVenta("EFECTIVO");
        if (cerrada != null) {
            System.out.println("    -> Venta confirmada Ticket #" + cerrada.getIdVenta()
                    + " | Total cobrado: $" + String.format("%.2f", cerrada.getMontoTotal())
                    + " | Ítems: " + cerrada.getCantidadItems());
            List<DetalleVenta> dets = cerrada.getDetalles();
            for (int i = 0; i < dets.size(); i++) {
                DetalleVenta d = dets.get(i);
                System.out.println("       * " + d.getCodigoBarras() + " | " + d.getNombreProducto()
                        + " x " + d.getCantidad() + " = $" + String.format("%.2f", d.getSubtotal()));
            }
        }

        // E. Métricas de negocio
        System.out.println("[5] Métricas de Negocio:");
        System.out.println("    -> Monto Total Diario (Hoy):   $" + String.format("%.2f", ctrl.obtenerMontoDiarioHoy()));
        System.out.println("    -> Monto Total Semanal:        $" + String.format("%.2f", ctrl.obtenerMontoSemanalActual()));
        System.out.println("    -> Monto Total Mensual:        $" + String.format("%.2f", ctrl.obtenerMontoMensualActual()));

        List<ResumenProductoVenta> menosVendidos = ctrl.obtenerProductosMenosVendidos(5);
        System.out.println("    -> Top 5 Productos Menos Vendidos:");
        for (int i = 0; i < menosVendidos.size(); i++) {
            ResumenProductoVenta r = menosVendidos.get(i);
            System.out.println("       " + (i + 1) + ". [" + r.getCodigoBarras() + "] " + r.getNombre()
                    + " -> Vendidas: " + r.getUnidadesVendidas() + " u. (Stock actual: " + r.getStockActual() + ")");
        }

        List<LocalDate> semanas = ctrl.obtenerSemanasDisponiblesParaAnalisis();
        LocalDate lunesSemanaPasada = semanas.size() > 1 ? semanas.get(1) : semanas.get(0);
        List<ResumenMetricaDia> desglose = ctrl.obtenerDesgloseDeSemana(lunesSemanaPasada);
        ResumenMetricaDia peorDia = ctrl.determinarDiaMenosVendido(desglose);
        if (peorDia != null) {
            System.out.println("    -> Día de la semana menos vendido (Semana del " + lunesSemanaPasada + "): "
                    + peorDia.getNombreDia() + " (" + peorDia.getFecha() + ") con $"
                    + String.format("%.2f", peorDia.getMontoTotal()));
        }

        List<Producto> catalogoFinal = ctrl.obtenerCatalogoProductos();
        System.out.println("[6] Total de productos activos en la BD SQL: " + catalogoFinal.size());
        System.out.println("=== VERIFICACIÓN INTEGRAL FINALIZADA CON ÉXITO ===");
    }
}
