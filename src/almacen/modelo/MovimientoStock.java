package almacen.modelo;

import java.sql.Timestamp;

/**
 * Representa un registro histórico de ingreso de mercadería al stock,
 * ya sea realizado de manera manual por el operador o importando una planilla Excel.
 */
public class MovimientoStock {

    private int idMovimiento;
    private String codigoBarras;
    private Timestamp fechaHora;
    private int cantidadAgregada;
    private String origen;
    private String observacion;

    public MovimientoStock() {
        this.idMovimiento = 0;
        this.codigoBarras = "";
        this.fechaHora = new Timestamp(System.currentTimeMillis());
        this.cantidadAgregada = 0;
        this.origen = "MANUAL";
        this.observacion = "";
    }

    public MovimientoStock(String codigoBarras, int cantidadAgregada, String origen, String observacion) {
        this.idMovimiento = 0;
        this.codigoBarras = codigoBarras;
        this.fechaHora = new Timestamp(System.currentTimeMillis());
        this.cantidadAgregada = cantidadAgregada;
        this.origen = origen;
        this.observacion = observacion;
    }

    public int getIdMovimiento() {
        return idMovimiento;
    }

    public void setIdMovimiento(int idMovimiento) {
        this.idMovimiento = idMovimiento;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public Timestamp getFechaHora() {
        return fechaHora;
    }

    public void setFechaHora(Timestamp fechaHora) {
        this.fechaHora = fechaHora;
    }

    public int getCantidadAgregada() {
        return cantidadAgregada;
    }

    public void setCantidadAgregada(int cantidadAgregada) {
        this.cantidadAgregada = cantidadAgregada;
    }

    public String getOrigen() {
        return origen;
    }

    public void setOrigen(String origen) {
        this.origen = origen;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }
}
