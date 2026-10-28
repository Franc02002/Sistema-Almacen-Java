package almacen.modelo;

/**
 * Representa el resumen estadístico de rotación de un producto para el reporte
 * de "Productos Menos Vendidos" en el módulo de métricas de negocio.
 */
public class ResumenProductoVenta {

    private String codigoBarras;
    private String nombre;
    private String categoria;
    private double precioUnitario;
    private int stockActual;
    private int unidadesVendidas;
    private double montoTotalGenerado;

    public ResumenProductoVenta(String codigoBarras, String nombre, String categoria, double precioUnitario, int stockActual, int unidadesVendidas, double montoTotalGenerado) {
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioUnitario = precioUnitario;
        this.stockActual = stockActual;
        this.unidadesVendidas = unidadesVendidas;
        this.montoTotalGenerado = montoTotalGenerado;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public int getStockActual() {
        return stockActual;
    }

    public int getUnidadesVendidas() {
        return unidadesVendidas;
    }

    public double getMontoTotalGenerado() {
        return montoTotalGenerado;
    }
}
