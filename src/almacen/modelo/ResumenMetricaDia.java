package almacen.modelo;

import java.time.LocalDate;

/**
 * Objeto de transferencia/resumen utilizado para el análisis semanal de ventas.
 * Almacena las métricas acumuladas de un día específico dentro de una semana analizada.
 */
public class ResumenMetricaDia {

    private int numeroDiaSemana; // 1 = Lunes ... 7 = Domingo
    private String nombreDia;
    private LocalDate fecha;
    private int cantidadTickets;
    private int unidadesVendidas;
    private double montoTotal;

    public ResumenMetricaDia(int numeroDiaSemana, String nombreDia, LocalDate fecha,
                             int cantidadTickets, int unidadesVendidas, double montoTotal) {
        this.numeroDiaSemana = numeroDiaSemana;
        this.nombreDia = nombreDia;
        this.fecha = fecha;
        this.cantidadTickets = cantidadTickets;
        this.unidadesVendidas = unidadesVendidas;
        this.montoTotal = montoTotal;
    }

    public int getNumeroDiaSemana() {
        return numeroDiaSemana;
    }

    public void setNumeroDiaSemana(int numeroDiaSemana) {
        this.numeroDiaSemana = numeroDiaSemana;
    }

    public String getNombreDia() {
        return nombreDia;
    }

    public void setNombreDia(String nombreDia) {
        this.nombreDia = nombreDia;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public int getCantidadTickets() {
        return cantidadTickets;
    }

    public void setCantidadTickets(int cantidadTickets) {
        this.cantidadTickets = cantidadTickets;
    }

    public int getUnidadesVendidas() {
        return unidadesVendidas;
    }

    public void setUnidadesVendidas(int unidadesVendidas) {
        this.unidadesVendidas = unidadesVendidas;
    }

    public double getMontoTotal() {
        return montoTotal;
    }

    public void setMontoTotal(double montoTotal) {
        this.montoTotal = montoTotal;
    }
}
