package almacen.util;

/**
 * Representa un registro individual interpretado desde una planilla Excel (.xlsx o .csv)
 * para realizar la carga masiva de stock sobre productos existentes (o alta opcional).
 */
public class FilaCargaStock {

    private int numeroFila;
    private String codigoBarras;
    private String nombreProducto;
    private String categoria;
    private int cantidadASumar;
    private double nuevoPrecioOpcional;
    private String estadoValidacion;

    public FilaCargaStock(int numeroFila, String codigoBarras, String nombreProducto,
                          String categoria, int cantidadASumar, double nuevoPrecioOpcional,
                          String estadoValidacion) {
        this.numeroFila = numeroFila;
        this.codigoBarras = codigoBarras;
        this.nombreProducto = nombreProducto;
        this.categoria = categoria;
        this.cantidadASumar = cantidadASumar;
        this.nuevoPrecioOpcional = nuevoPrecioOpcional;
        this.estadoValidacion = estadoValidacion;
    }

    public int getNumeroFila() {
        return numeroFila;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getCategoria() {
        return categoria;
    }

    public int getCantidadASumar() {
        return cantidadASumar;
    }

    public double getNuevoPrecioOpcional() {
        return nuevoPrecioOpcional;
    }

    public String getEstadoValidacion() {
        return estadoValidacion;
    }

    public void setEstadoValidacion(String estadoValidacion) {
        this.estadoValidacion = estadoValidacion;
    }
}
