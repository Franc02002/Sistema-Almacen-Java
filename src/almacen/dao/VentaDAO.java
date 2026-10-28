package almacen.dao;

import almacen.conexion.ConexionBD;
import almacen.modelo.DetalleVenta;
import almacen.modelo.Venta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO encargado de persistir las transacciones de Venta y sus renglones (DETALLE_VENTA),
 * asegurando la atomicidad (ACID) entre el registro de la venta y el quite de stock.
 */
public class VentaDAO {

    private ProductoDAO productoDAO;

    public VentaDAO() {
        this.productoDAO = new ProductoDAO();
    }

    /**
     * Registra una venta completa de forma transaccional:
     * 1) Inserta la cabecera en VENTA.
     * 2) Recorre imperativamente cada DetalleVenta, lo inserta en DETALLE_VENTA.
     * 3) Ejecuta el quite de stock en PRODUCTO.
     * Si algún ítem no tiene stock suficiente o falla, ejecuta ROLLBACK completo.
     *
     * @param venta Objeto Venta con sus detalles ya calculados.
     * @return El ID de la venta generada (> 0), o -1 si la transacción fue revertida.
     */
    public int registrarVentaTransaccional(Venta venta) {
        Connection conn = null;
        try {
            conn = ConexionBD.obtenerConexion();
            conn.setAutoCommit(false);

            // 1. Insertar cabecera de la venta
            String sqlVenta = "INSERT INTO VENTA (fecha_hora, monto_total, cantidad_items, metodo_pago) VALUES (?, ?, ?, ?)";
            PreparedStatement psVenta = conn.prepareStatement(sqlVenta);
            psVenta.setTimestamp(1, venta.getFechaHora());
            psVenta.setDouble(2, venta.getMontoTotal());
            psVenta.setInt(3, venta.getCantidadItems());
            psVenta.setString(4, venta.getMetodoPago());
            psVenta.executeUpdate();
            psVenta.close();

            int idVentaGenerado = -1;
            Statement stMax = conn.createStatement();
            ResultSet rsMax = stMax.executeQuery("SELECT MAX(id_venta) FROM VENTA");
            if (rsMax.next()) {
                idVentaGenerado = rsMax.getInt(1);
            }
            rsMax.close();
            stMax.close();

            if (idVentaGenerado <= 0) {
                conn.rollback();
                conn.setAutoCommit(true);
                return -1;
            }

            // 2. Insertar cada renglón de detalle y descontar stock de forma imperativa
            String sqlDet = "INSERT INTO DETALLE_VENTA (id_venta, codigo_barras, cantidad, precio_unitario_historico, subtotal) "
                          + "VALUES (?, ?, ?, ?, ?)";
            PreparedStatement psDet = conn.prepareStatement(sqlDet);

            List<DetalleVenta> detalles = venta.getDetalles();
            for (int i = 0; i < detalles.size(); i++) {
                DetalleVenta d = detalles.get(i);

                boolean stockDescontado = productoDAO.descontarStockTransaccional(
                        conn, d.getCodigoBarras(), d.getCantidad());
                if (!stockDescontado) {
                    psDet.close();
                    conn.rollback();
                    conn.setAutoCommit(true);
                    return -1;
                }

                psDet.setInt(1, idVentaGenerado);
                psDet.setString(2, d.getCodigoBarras());
                psDet.setInt(3, d.getCantidad());
                psDet.setDouble(4, d.getPrecioUnitarioHistorico());
                psDet.setDouble(5, d.getSubtotal());
                psDet.executeUpdate();
            }
            psDet.close();

            // 3. Confirmar transacción
            conn.commit();
            conn.setAutoCommit(true);
            venta.setIdVenta(idVentaGenerado);
            return idVentaGenerado;

        } catch (SQLException e) {
            System.err.println("Error en transacción de venta: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    conn.setAutoCommit(true);
                } catch (SQLException exRoll) {
                    System.err.println("Error en rollback: " + exRoll.getMessage());
                }
            }
            return -1;
        }
    }

    /**
     * Recupera los productos que conformaron una venta específica para su lectura en pantalla.
     *
     * @param idVenta Identificador de la venta a consultar.
     * @return Lista de objetos DetalleVenta con el nombre del producto asociado.
     */
    public List<DetalleVenta> obtenerDetallesDeVenta(int idVenta) {
        List<DetalleVenta> detalles = new ArrayList<DetalleVenta>();
        String sql = "SELECT d.id_detalle, d.id_venta, d.codigo_barras, p.nombre, "
                   + "d.cantidad, d.precio_unitario_historico, d.subtotal "
                   + "FROM DETALLE_VENTA d "
                   + "INNER JOIN PRODUCTO p ON d.codigo_barras = p.codigo_barras "
                   + "WHERE d.id_venta = ? ORDER BY d.id_detalle ASC";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1, idVenta);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                DetalleVenta d = new DetalleVenta();
                d.setIdDetalle(rs.getInt("id_detalle"));
                d.setIdVenta(rs.getInt("id_venta"));
                d.setCodigoBarras(rs.getString("codigo_barras"));
                d.setNombreProducto(rs.getString("nombre"));
                d.setCantidad(rs.getInt("cantidad"));
                d.setPrecioUnitarioHistorico(rs.getDouble("precio_unitario_historico"));
                d.setSubtotal(rs.getDouble("subtotal"));
                detalles.add(d);
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al obtener detalles de venta: " + e.getMessage());
        }
        return detalles;
    }

    /**
     * Lista las últimas ventas registradas en el almacén para consulta de tickets emitidos.
     *
     * @param limite Cantidad máxima de ventas recientes a devolver.
     * @return Lista de cabeceras de Venta con sus detalles cargados.
     */
    public List<Venta> listarUltimasVentas(int limite) {
        List<Venta> ventas = new ArrayList<Venta>();
        String sql = "SELECT id_venta, fecha_hora, monto_total, cantidad_items, metodo_pago "
                   + "FROM VENTA ORDER BY fecha_hora DESC";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            int contador = 0;
            while (rs.next() && contador < limite) {
                Venta v = new Venta();
                v.setIdVenta(rs.getInt("id_venta"));
                v.setFechaHora(rs.getTimestamp("fecha_hora"));
                v.setMontoTotal(rs.getDouble("monto_total"));
                v.setCantidadItems(rs.getInt("cantidad_items"));
                v.setMetodoPago(rs.getString("metodo_pago"));
                v.setDetalles(obtenerDetallesDeVenta(v.getIdVenta()));
                ventas.add(v);
                contador = contador + 1;
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al listar últimas ventas: " + e.getMessage());
        }
        return ventas;
    }
}
