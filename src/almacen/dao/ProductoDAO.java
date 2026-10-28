package almacen.dao;

import almacen.conexion.ConexionBD;
import almacen.modelo.Producto;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class ProductoDAO {

    /**
     * Busca un producto en el catálogo a partir de su código de barras exacto.
     *
     * @param codigoBarras Código leído por el escáner o ingresado manualmente.
     * @return Instancia de Producto si existe, o null si no se encuentra registrado.
     */
    public Producto buscarPorCodigoBarras(String codigoBarras) {
        Producto encontrado = null;
        String sql = "SELECT codigo_barras, nombre, categoria, precio_unitario, stock_actual, stock_minimo, activo "
                   + "FROM PRODUCTO WHERE codigo_barras = ?";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, codigoBarras.trim());
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                encontrado = new Producto(
                        rs.getString("codigo_barras"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getDouble("precio_unitario"),
                        rs.getInt("stock_actual"),
                        rs.getInt("stock_minimo"),
                        rs.getBoolean("activo")
                );
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al buscar producto por código de barras: " + e.getMessage());
        }
        return encontrado;
    }

    /**
     * Recupera de forma imperativa la lista completa de productos activos del almacén.
     *
     * @return Lista de objetos Producto ordenada alfabéticamente por nombre.
     */
    public List<Producto> listarTodos() {
        List<Producto> lista = new ArrayList<Producto>();
        String sql = "SELECT codigo_barras, nombre, categoria, precio_unitario, stock_actual, stock_minimo, activo "
                   + "FROM PRODUCTO WHERE activo = TRUE ORDER BY nombre ASC";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Producto p = new Producto(
                        rs.getString("codigo_barras"),
                        rs.getString("nombre"),
                        rs.getString("categoria"),
                        rs.getDouble("precio_unitario"),
                        rs.getInt("stock_actual"),
                        rs.getInt("stock_minimo"),
                        rs.getBoolean("activo")
                );
                lista.add(p);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al listar catálogo de productos: " + e.getMessage());
        }
        return lista;
    }

    /**
     * Inserta un nuevo producto en la base de datos SQL.
     *
     * @param producto Objeto Producto con sus datos validados.
     * @return true si la inserción fue exitosa, false en caso contrario.
     */
    public boolean insertarProducto(Producto producto) {
        String sql = "INSERT INTO PRODUCTO (codigo_barras, nombre, categoria, precio_unitario, stock_actual, stock_minimo, activo) "
                   + "VALUES (?, ?, ?, ?, ?, ?, TRUE)";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, producto.getCodigoBarras().trim());
            ps.setString(2, producto.getNombre().trim());
            ps.setString(3, producto.getCategoria().trim());
            ps.setDouble(4, producto.getPrecioUnitario());
            ps.setInt(5, producto.getStockActual());
            ps.setInt(6, producto.getStockMinimo());

            int filas = ps.executeUpdate();
            ps.close();
            return filas > 0;
        } catch (SQLException e) {
            System.err.println("Error al insertar producto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Actualiza los datos descriptivos, el precio unitario y el stock de un producto existente.
     *
     * @param producto Objeto Producto con los datos actualizados.
     * @return true si se modificó el registro correctamente.
     */
    public boolean actualizarProducto(Producto producto) {
        String sql = "UPDATE PRODUCTO SET nombre = ?, categoria = ?, precio_unitario = ?, "
                   + "stock_actual = ?, stock_minimo = ? WHERE codigo_barras = ?";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setString(1, producto.getNombre().trim());
            ps.setString(2, producto.getCategoria().trim());
            ps.setDouble(3, producto.getPrecioUnitario());
            ps.setInt(4, producto.getStockActual());
            ps.setInt(5, producto.getStockMinimo());
            ps.setString(6, producto.getCodigoBarras().trim());

            int filas = ps.executeUpdate();
            ps.close();
            return filas > 0;
        } catch (SQLException e) {
            System.err.println("Error al actualizar producto: " + e.getMessage());
            return false;
        }
    }

    /**
     * Incrementa el stock de un producto existente y deja constancia en la tabla MOVIMIENTO_STOCK.
     *
     * @param codigoBarras     Código de barras del producto a recargar.
     * @param cantidadASumar   Cantidad de unidades que ingresan al almacén (mayor a 0).
     * @param origen           Origen de la carga ("MANUAL" o "PLANILLA_EXCEL").
     * @param observacion      Detalle adicional o nombre del archivo Excel importado.
     * @return true si el stock fue incrementado y auditado con éxito.
     */
    public boolean sumarStock(String codigoBarras, int cantidadASumar, String origen, String observacion) {
        String sqlUpdate = "UPDATE PRODUCTO SET stock_actual = stock_actual + ? WHERE codigo_barras = ?";
        String sqlMov = "INSERT INTO MOVIMIENTO_STOCK (codigo_barras, fecha_hora, cantidad_agregada, origen, observacion) "
                      + "VALUES (?, ?, ?, ?, ?)";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement psUpd = conn.prepareStatement(sqlUpdate);
            psUpd.setInt(1, cantidadASumar);
            psUpd.setString(2, codigoBarras.trim());
            int filas = psUpd.executeUpdate();
            psUpd.close();

            if (filas > 0) {
                PreparedStatement psMov = conn.prepareStatement(sqlMov);
                psMov.setString(1, codigoBarras.trim());
                psMov.setTimestamp(2, new Timestamp(System.currentTimeMillis()));
                psMov.setInt(3, cantidadASumar);
                psMov.setString(4, origen);
                psMov.setString(5, observacion);
                psMov.executeUpdate();
                psMov.close();
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("Error al sumar stock: " + e.getMessage());
            return false;
        }
    }

    /**
     * Descuenta unidades del stock de un producto dentro de una transacción de venta activa.
     * Verifica que el stock resultante nunca sea negativo.
     *
     * @param connTransaccional Conexión activa de la transacción de venta (ACLARCION ADICIONAL MAS ABAJO)
     * @param codigoBarras      Código de barras del producto vendido.
     * @param cantidadADescontar Unidades vendidas a descontar del inventario.
     * @return true si había stock suficiente y se realizó el quite.
     * @throws SQLException Si ocurre un fallo SQL durante la transacción.
     */

    /**El objeto connection es necesario para que todas las operaciones que anteceden y proceden de esta se realicen bajo la misma conexion garantizando que todo ocurra bajo una misma transaccion*/
    /** Al ocurrir todas las operaciones en la misma transaccion si en una parte falla el sistema hace un rollback y deja la BD en el mismo estado en el cual empezo la transaccion*/

    public boolean descontarStockTransaccional(Connection connTransaccional, String codigoBarras, int cantidadADescontar) throws SQLException {
        String sql = "UPDATE PRODUCTO SET stock_actual = stock_actual - ? "
                   + "WHERE codigo_barras = ? AND stock_actual >= ?";
        PreparedStatement ps = connTransaccional.prepareStatement(sql);
        ps.setInt(1, cantidadADescontar);
        ps.setString(2, codigoBarras.trim());
        ps.setInt(3, cantidadADescontar);
        int filas = ps.executeUpdate();
        ps.close();
        return filas > 0;
    }
}
