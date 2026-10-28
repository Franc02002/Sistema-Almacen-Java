package almacen.modelo;

/**
 * Representa un renglón (ítem) dentro de una Venta.
 * Conserva el precio unitario histórico vigente al momento de la venta
 * y calcula el subtotal correspondiente (cantidad * precioUnitarioHistorico).
 */
public class DetalleVenta {

    private int idDetalle;
    private int idVenta;
    private String codigoBarras;
    private String nombreProducto;
    private int cantidad;
    private double precioUnitarioHistorico;
    private double subtotal;

    public DetalleVenta() {
        this.idDetalle = 0;
        this.idVenta = 0;
        this.codigoBarras = "";
        this.nombreProducto = "";
        this.cantidad = 0;
        this.precioUnitarioHistorico = 0.0;
        this.subtotal = 0.0;
    }

    public DetalleVenta(String codigoBarras, String nombreProducto, int cantidad, double precioUnitarioHistorico) {
        this.idDetalle = 0;
        this.idVenta = 0;
        this.codigoBarras = codigoBarras;
        this.nombreProducto = nombreProducto;
        this.cantidad = cantidad;
        this.precioUnitarioHistorico = precioUnitarioHistorico;
        recalcularSubtotal();
    }

    /**
     * Calcula y actualiza de forma imperativa el subtotal del renglón.
     */
    public void recalcularSubtotal() {
        this.subtotal = Math.round((this.cantidad * this.precioUnitarioHistorico) * 100.0) / 100.0;
    }

    public int getIdDetalle() {
        return idDetalle;
    }

    public void setIdDetalle(int idDetalle) {
        this.idDetalle = idDetalle;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
        recalcularSubtotal();
    }

    public double getPrecioUnitarioHistorico() {
        return precioUnitarioHistorico;
    }

    public void setPrecioUnitarioHistorico(double precioUnitarioHistorico) {
        this.precioUnitarioHistorico = precioUnitarioHistorico;
        recalcularSubtotal();
    }

    public double getSubtotal() {
        return subtotal;
    }

    public void setSubtotal(double subtotal) {
        this.subtotal = subtotal;
    }
}
