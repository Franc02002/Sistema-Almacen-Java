package almacen.modelo;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa una transacción de Venta completa en el almacén.
 * Contiene la cabecera de la operación y la lista de renglones (DetalleVenta) que conforman el ticket.
 * EJ: 5 gaseosas(detalletaVenta) , 3 galletitas (detalleVenta) , estos dos items representan el cuerpo de una Venta.
 */
public class Venta {

    private int idVenta;
    private Timestamp fechaHora;
    private double montoTotal;
    private int cantidadItems;
    private String metodoPago;
    private List<DetalleVenta> detalles;

    public Venta() {
        this.idVenta = 0;
        this.fechaHora = new Timestamp(System.currentTimeMillis());
        this.montoTotal = 0.0;
        this.cantidadItems = 0;
        this.metodoPago = "EFECTIVO";
        this.detalles = new ArrayList<DetalleVenta>();
    }

    /**
     * Recorre de forma imperativa todos los renglones de la venta para
     * actualizar la sumatoria del monto total a cobrar y el total de unidades.
     */
    public void recalcularTotales() {
        double sumaMonto = 0.0;
        int sumaUnidades = 0;

        for (int i = 0; i < this.detalles.size(); i++) {
            DetalleVenta item = this.detalles.get(i);
            item.recalcularSubtotal();
            sumaMonto = sumaMonto + item.getSubtotal();
            sumaUnidades = sumaUnidades + item.getCantidad();
        }

        this.montoTotal = Math.round(sumaMonto * 100.0) / 100.0;
        this.cantidadItems = sumaUnidades;
    }

    public int getIdVenta() {
        return idVenta;
    }

    public void setIdVenta(int idVenta) {
        this.idVenta = idVenta;
    }

    public Timestamp getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Timestamp fechaHora) {
        this.fechaHora = fechaHora;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }

    public int getCantidadItems() {
        return cantidadItems;
    }

    public void setCantidadItems(int cantidadItems) {
        this.cantidadItems = cantidadItems;
    }

    public String getMetodoPago() {
        return metodoPago;
    }

    public void setMetodoPago(String metodoPago) {
        this.metodoPago = metodoPago;
    }

    public List<DetalleVenta> getDetalles() {
        return detalles;
    }

    public void setDetalles(List<DetalleVenta> detalles) {
        this.detalles = detalles;
        recalcularTotales();
    }
}
