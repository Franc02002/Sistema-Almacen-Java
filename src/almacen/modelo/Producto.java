package almacen.modelo;

/**
 * Clase de dominio que representa un Producto a la venta en el almacén.
 * Se identifica unívocamente por su código de barras para permitir la lectura
 * directa mediante escáner láser/USB en el punto de venta.
 */
public class Producto {

    private String codigoBarras;
    private String nombre;
    private String categoria;
    private double precioUnitario;
    private int stockActual;
    private int stockMinimo;
    private boolean activo;

    /**
     * Constructor vacío requerido para instanciación imperativa paso a paso.
     */
    public Producto() {
        this.codigoBarras = "";
        this.nombre = "";
        this.categoria = "General";
        this.precioUnitario = 0.0;
        this.stockActual = 0;
        this.stockMinimo = 5;
        this.activo = true;
    }

    /**
     * Constructor completo para inicializar un producto con todos sus datos.
     *
     * @param codigoBarras   Código EAN-13 / EAN-8 o interno leído por el escáner.
     * @param nombre         Nombre descriptivo del artículo.
     * @param categoria      Rubro o familia del producto (ej. Almacén, Bebidas, Lácteos).
     * @param precioUnitario Precio de venta al público por unidad.
     * @param stockActual    Cantidad de unidades disponibles actualmente en góndola/depósito.
     * @param stockMinimo    Cantidad mínima sugerida antes de alertar reposición.
     * @param activo         Estado lógico del producto en el catálogo.
     */
    public Producto(String codigoBarras, String nombre, String categoria, double precioUnitario, int stockActual, int stockMinimo, boolean activo) {
        this.codigoBarras = codigoBarras;
        this.nombre = nombre;
        this.categoria = categoria;
        this.precioUnitario = precioUnitario;
        this.stockActual = stockActual;
        this.stockMinimo = stockMinimo;
        this.activo = activo;
    }

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public void setPrecioUnitario(double precioUnitario) {
        this.precioUnitario = precioUnitario;
    }

    public int getStockActual() {
        return stockActual;
    }

    public void setStockActual(int stockActual) {
        this.stockActual = stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    /**
     * Determina de forma imperativa si el producto se encuentra en nivel crítico de stock.
     *
     * @return true si el stock actual es menor o igual al stock mínimo.
     */
    public boolean tieneStockBajo() {
        if (this.stockActual <= this.stockMinimo) {
            return true;
        } else {
            return false;
        }
    }
}
