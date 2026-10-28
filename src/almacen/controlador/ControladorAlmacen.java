package almacen.controlador;

import almacen.dao.MetricaDAO;
import almacen.dao.ProductoDAO;
import almacen.dao.VentaDAO;
import almacen.modelo.DetalleVenta;
import almacen.modelo.Producto;
import almacen.modelo.ResumenMetricaDia;
import almacen.modelo.ResumenProductoVenta;
import almacen.modelo.Venta;
import almacen.util.FilaCargaStock;
import almacen.util.LectorPlanillaExcel;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Controlador central de la aplicación (Capa de Negocio / Servicio).
 * Coordina de manera imperativa la comunicación entre las ventanas gráficas (Swing)
 * y los objetos de acceso a datos (DAO), manteniendo el estado de la venta en curso
 * y ejecutando los algoritmos de validación y métricas.
 */
public class ControladorAlmacen {

    private ProductoDAO productoDAO;
    private VentaDAO ventaDAO;
    private MetricaDAO metricaDAO;
    private LectorPlanillaExcel lectorExcel;
    private Venta ventaEnCurso;

    public ControladorAlmacen() {
        this.productoDAO = new ProductoDAO();
        this.ventaDAO = new VentaDAO();
        this.metricaDAO = new MetricaDAO();
        this.lectorExcel = new LectorPlanillaExcel();
        this.ventaEnCurso = new Venta();
    }

    // =========================================================================
    // SECCIÓN 1: GESTIÓN DE PRODUCTOS Y CATÁLOGO
    // =========================================================================

    public List<Producto> obtenerCatalogoProductos() {
        return productoDAO.listarTodos();
    }

    public Producto buscarProductoPorCodigo(String codigoBarras) {
        if (codigoBarras == null || codigoBarras.trim().isEmpty()) {
            return null;
        }
        return productoDAO.buscarPorCodigoBarras(codigoBarras.trim());
    }

    /**
     * Registra o actualiza un producto validando de forma imperativa sus campos.
     *
     * @return Mensaje indicando el resultado de la operación.
     */
    public String guardarProducto(String codigoBarras, String nombre, String categoria,
                                  double precioUnitario, int stockInicial, int stockMinimo) {
        if (codigoBarras == null || codigoBarras.trim().isEmpty()) {
            return "ERROR: El código de barras es obligatorio.";
        }
        if (nombre == null || nombre.trim().isEmpty()) {
            return "ERROR: El nombre del producto es obligatorio.";
        }
        if (precioUnitario <= 0) {
            return "ERROR: El precio unitario debe ser mayor a $0.00.";
        }
        if (stockInicial < 0) {
            return "ERROR: El stock no puede ser negativo.";
        }

        String codigoLimpio = codigoBarras.trim();
        Producto existente = productoDAO.buscarPorCodigoBarras(codigoLimpio);

        if (existente == null) {
            Producto nuevo = new Producto(
                    codigoLimpio,
                    nombre.trim(),
                    (categoria == null || categoria.trim().isEmpty()) ? "General" : categoria.trim(),
                    precioUnitario,
                    stockInicial,
                    stockMinimo < 0 ? 0 : stockMinimo,
                    true
            );
            boolean ok = productoDAO.insertarProducto(nuevo);
            if (ok) {
                return "OK: Producto '" + nuevo.getNombre() + "' registrado con éxito.";
            } else {
                return "ERROR: No se pudo guardar el producto en la base de datos.";
            }
        } else {
            existente.setNombre(nombre.trim());
            existente.setCategoria((categoria == null || categoria.trim().isEmpty()) ? "General" : categoria.trim());
            existente.setPrecioUnitario(precioUnitario);
            existente.setStockActual(stockInicial);
            existente.setStockMinimo(stockMinimo < 0 ? 0 : stockMinimo);
            boolean ok = productoDAO.actualizarProducto(existente);
            if (ok) {
                return "OK: Datos del producto '" + existente.getNombre() + "' actualizados correctamente.";
            } else {
                return "ERROR: No se pudo actualizar el producto existente.";
            }
        }
    }

    // =========================================================================
    // SECCIÓN 2: CARGA DE STOCK MANUAL Y POR PLANILLA EXCEL
    // =========================================================================

    /**
     * Carga stock de manera manual para un producto ya existente identificado por código de barras.
     */
    public String cargarStockManual(String codigoBarras, int cantidadASumar, String observacion) {
        if (codigoBarras == null || codigoBarras.trim().isEmpty()) {
            return "ERROR: Debe escanear o escribir un código de barras.";
        }
        if (cantidadASumar <= 0) {
            return "ERROR: La cantidad de stock a sumar debe ser mayor a cero.";
        }

        Producto p = productoDAO.buscarPorCodigoBarras(codigoBarras.trim());
        if (p == null) {
            return "ERROR: No existe ningún producto con el código de barras '" + codigoBarras.trim() + "'.";
        }

        String nota = (observacion == null || observacion.trim().isEmpty())
                ? "Ingreso manual por mostrador"
                : observacion.trim();
        boolean exito = productoDAO.sumarStock(p.getCodigoBarras(), cantidadASumar, "MANUAL", nota);
        if (exito) {
            int nuevoStock = p.getStockActual() + cantidadASumar;
            return "OK: Se sumaron " + cantidadASumar + " unidades a '" + p.getNombre()
                    + "'. Stock resultante: " + nuevoStock + " u.";
        } else {
            return "ERROR: Falló la actualización del stock en la base de datos.";
        }
    }

    /**
     * Interpreta una planilla Excel (.xlsx o .csv) y verifica cada fila contra el catálogo SQL
     * antes de que el usuario confirme la aplicación de los cambios.
     */
    public List<FilaCargaStock> previsualizarPlanillaExcel(File archivoExcel) throws IOException {
        List<FilaCargaStock> filas = lectorExcel.interpretarArchivo(archivoExcel);

        for (int i = 0; i < filas.size(); i++) {
            FilaCargaStock fila = filas.get(i);
            Producto p = productoDAO.buscarPorCodigoBarras(fila.getCodigoBarras());
            if (p != null) {
                fila.setNombreProducto(p.getNombre());
                fila.setEstadoValidacion("LISTO (Stock actual: " + p.getStockActual()
                        + " -> Nuevo: " + (p.getStockActual() + fila.getCantidadASumar()) + ")");
            } else {
                if (fila.getNombreProducto() != null && !fila.getNombreProducto().isEmpty()
                        && fila.getNuevoPrecioOpcional() > 0) {
                    fila.setEstadoValidacion("NUEVO PRODUCTO A CREAR");
                } else {
                    fila.setEstadoValidacion("CÓDIGO NO REGISTRADO EN ALMACÉN");
                }
            }
        }
        return filas;
    }

    /**
     * Aplica de manera imperativa la carga de stock de todas las filas válidas leídas desde el Excel.
     *
     * @param filas         Lista de filas previsualizadas.
     * @param nombreArchivo Nombre del archivo Excel para trazabilidad en MOVIMIENTO_STOCK.
     * @return Cantidad de productos cuyo stock fue actualizado exitosamente.
     */
    public int aplicarCargaDesdeExcel(List<FilaCargaStock> filas, String nombreArchivo) {
        int actualizados = 0;
        for (int i = 0; i < filas.size(); i++) {
            FilaCargaStock fila = filas.get(i);
            Producto existente = productoDAO.buscarPorCodigoBarras(fila.getCodigoBarras());

            if (existente != null) {
                // Si la planilla trae un precio nuevo válido mayor a 0, actualizar también el precio
                if (fila.getNuevoPrecioOpcional() > 0) {
                    existente.setPrecioUnitario(fila.getNuevoPrecioOpcional());
                    productoDAO.actualizarProducto(existente);
                }
                boolean ok = productoDAO.sumarStock(
                        existente.getCodigoBarras(),
                        fila.getCantidadASumar(),
                        "PLANILLA_EXCEL",
                        "Importado desde " + nombreArchivo + " (Fila " + fila.getNumeroFila() + ")"
                );
                if (ok) {
                    fila.setEstadoValidacion("CARGADO (+ " + fila.getCantidadASumar() + " u.)");
                    actualizados = actualizados + 1;
                }
            } else if (fila.getNombreProducto() != null && !fila.getNombreProducto().isEmpty()
                    && fila.getNuevoPrecioOpcional() > 0) {
                // Si el producto no existía pero la planilla especifica nombre y precio, lo da de alta
                Producto nuevo = new Producto(
                        fila.getCodigoBarras(),
                        fila.getNombreProducto(),
                        fila.getCategoria(),
                        fila.getNuevoPrecioOpcional(),
                        fila.getCantidadASumar(),
                        5,
                        true
                );
                if (productoDAO.insertarProducto(nuevo)) {
                    fila.setEstadoValidacion("CREADO Y CARGADO (" + fila.getCantidadASumar() + " u.)");
                    actualizados = actualizados + 1;
                }
            }
        }
        return actualizados;
    }

    // =========================================================================
    // SECCIÓN 3: PUNTO DE VENTA (ESCÁNER DE CÓDIGO DE BARRAS Y COBRO)
    // =========================================================================

    public Venta getVentaEnCurso() {
        return ventaEnCurso;
    }

    public void iniciarNuevaVenta() {
        this.ventaEnCurso = new Venta();
    }

    /**
     * Procesa el escaneo de un código de barras en la ventana de Nueva Venta.
     * Verifica que el producto exista y que la cantidad acumulada en el carrito no supere
     * el stock disponible en el almacén, actualizando la sumatoria del total a cobrar.
     *
     * @param codigoBarras Código enviado por el lector láser/USB.
     * @param cantidad     Unidades a agregar (por defecto 1 al escanear).
     * @return Mensaje de estado ("OK: ..." o "ERROR: ...").
     */
    public String escanearProductoEnVenta(String codigoBarras, int cantidad) {
        if (codigoBarras == null || codigoBarras.trim().isEmpty()) {
            return "ERROR: Escanee o ingrese un código de barras válido.";
        }
        if (cantidad <= 0) {
            return "ERROR: La cantidad a vender debe ser al menos 1 unidad.";
        }

        String codigoLimpio = codigoBarras.trim();
        Producto prod = productoDAO.buscarPorCodigoBarras(codigoLimpio);
        if (prod == null) {
            return "ERROR: Producto no encontrado para el código [" + codigoLimpio + "].";
        }

        // Buscar de forma imperativa si el producto ya está en el listado de la venta actual
        List<DetalleVenta> items = ventaEnCurso.getDetalles();
        DetalleVenta detalleExistente = null;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getCodigoBarras().equals(codigoLimpio)) {
                detalleExistente = items.get(i);
                break;
            }
        }

        int cantidadYaEnCarrito = (detalleExistente != null) ? detalleExistente.getCantidad() : 0;
        int cantidadTotalSolicitada = cantidadYaEnCarrito + cantidad;

        if (cantidadTotalSolicitada > prod.getStockActual()) {
            return "ERROR: Stock insuficiente de '" + prod.getNombre()
                    + "'. Disponible en almacén: " + prod.getStockActual()
                    + " u. (en ticket actual: " + cantidadYaEnCarrito + " u.).";
        }

        if (detalleExistente != null) {
            detalleExistente.setCantidad(cantidadTotalSolicitada);
        } else {
            DetalleVenta nuevoRenglon = new DetalleVenta(
                    prod.getCodigoBarras(),
                    prod.getNombre(),
                    cantidad,
                    prod.getPrecioUnitario()
            );
            items.add(nuevoRenglon);
        }

        ventaEnCurso.recalcularTotales();
        int stockRestanteVirtual = prod.getStockActual() - cantidadTotalSolicitada;
        return "OK: Agregado " + cantidad + " x " + prod.getNombre()
                + " ($" + String.format("%.2f", prod.getPrecioUnitario())
                + ") | Stock restante luego de venta: " + stockRestanteVirtual + " u.";
    }

    /**
     * Modifica la cantidad de un renglón ya escaneado en la venta actual, validando el stock.
     */
    public String cambiarCantidadRenglonVenta(int indiceFila, int nuevaCantidad) {
        List<DetalleVenta> items = ventaEnCurso.getDetalles();
        if (indiceFila < 0 || indiceFila >= items.size()) {
            return "ERROR: Seleccione una fila válida de la venta.";
        }
        if (nuevaCantidad <= 0) {
            items.remove(indiceFila);
            ventaEnCurso.recalcularTotales();
            return "OK: Producto retirado de la venta actual.";
        }

        DetalleVenta d = items.get(indiceFila);
        Producto prod = productoDAO.buscarPorCodigoBarras(d.getCodigoBarras());
        if (prod != null && nuevaCantidad > prod.getStockActual()) {
            return "ERROR: Stock máximo disponible para '" + prod.getNombre() + "': " + prod.getStockActual() + " u.";
        }

        d.setCantidad(nuevaCantidad);
        ventaEnCurso.recalcularTotales();
        return "OK: Cantidad actualizada a " + nuevaCantidad + " unidades.";
    }

    /**
     * Quita un renglón seleccionado de la venta en curso.
     */
    public void quitarRenglonDeVenta(int indiceFila) {
        List<DetalleVenta> items = ventaEnCurso.getDetalles();
        if (indiceFila >= 0 && indiceFila < items.size()) {
            items.remove(indiceFila);
            ventaEnCurso.recalcularTotales();
        }
    }

    /**
     * Confirma la venta actual, impacta el quite definitivo de stock en la base de datos SQL
     * y reinicia el carrito para el próximo cliente.
     *
     * @param metodoPago Medio de pago elegido ("EFECTIVO", "DEBITO", "TRANSFERENCIA").
     * @return El objeto Venta cerrado con su ID asignado, o null si ocurrió un error.
     */
    public Venta confirmarYFinalizarVenta(String metodoPago) {
        if (ventaEnCurso.getDetalles().isEmpty()) {
            return null;
        }
        ventaEnCurso.setMetodoPago(metodoPago != null ? metodoPago : "EFECTIVO");
        ventaEnCurso.recalcularTotales();

        int idGenerado = ventaDAO.registrarVentaTransaccional(ventaEnCurso);
        if (idGenerado > 0) {
            Venta ventaCerrada = ventaEnCurso;
            this.ventaEnCurso = new Venta();
            return ventaCerrada;
        }
        return null;
    }

    public List<Venta> obtenerUltimasVentas(int limite) {
        return ventaDAO.listarUltimasVentas(limite);
    }

    // =========================================================================
    // SECCIÓN 4: MÉTRICAS DE NEGOCIO Y ANÁLISIS SEMANAL IMPERATIVO
    // =========================================================================

    public double obtenerMontoDiarioHoy() {
        return metricaDAO.obtenerMontoTotalDiario(LocalDate.now());
    }

    public double obtenerMontoSemanalActual() {
        return metricaDAO.obtenerMontoTotalSemanal(LocalDate.now());
    }

    public double obtenerMontoMensualActual() {
        return metricaDAO.obtenerMontoTotalMensual(LocalDate.now());
    }

    public List<ResumenProductoVenta> obtenerProductosMenosVendidos(int limite) {
        return metricaDAO.obtenerProductosMenosVendidos(limite);
    }

    public List<LocalDate> obtenerSemanasDisponiblesParaAnalisis() {
        return metricaDAO.listarLunesDeSemanasExistentes();
    }

    public List<ResumenMetricaDia> obtenerDesgloseDeSemana(LocalDate lunesSemana) {
        return metricaDAO.obtenerDesgloseSemanal(lunesSemana);
    }

    /**
     * Algoritmo imperativo que recorre los 7 días de una semana existente y determina
     * cuál fue el día de la semana menos vendido (menor monto total recaudado y menor rotación).
     *
     * @param desgloseSieteDias Lista con las métricas de Lunes a Domingo de la semana elegida.
     * @return El objeto ResumenMetricaDia correspondiente al día de menor venta.
     */
    public ResumenMetricaDia determinarDiaMenosVendido(List<ResumenMetricaDia> desgloseSieteDias) {
        if (desgloseSieteDias == null || desgloseSieteDias.isEmpty()) {
            return null;
        }

        ResumenMetricaDia peorDia = desgloseSieteDias.get(0);
        for (int i = 1; i < desgloseSieteDias.size(); i++) {
            ResumenMetricaDia candidato = desgloseSieteDias.get(i);
            if (candidato.getMontoTotal() < peorDia.getMontoTotal()) {
                peorDia = candidato;
            } else if (candidato.getMontoTotal() == peorDia.getMontoTotal()) {
                if (candidato.getUnidadesVendidas() < peorDia.getUnidadesVendidas()) {
                    peorDia = candidato;
                }
            }
        }
        return peorDia;
    }
}
