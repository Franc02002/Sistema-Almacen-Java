package almacen.conexion;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * Clase encargada de administrar la conexión hacia la Base de Datos Relacional SQL local.
 * Implementa el patrón Singleton e inicializa de forma automática las tablas relacionales
 * (PRODUCTO, VENTA, DETALLE_VENTA, MOVIMIENTO_STOCK) junto con un lote de datos iniciales
 * para que las métricas diarias, semanales y mensuales puedan probarse de inmediato.
 */
public class ConexionBD {

    private static Connection conexionUnica = null;
    private static String motorActivo = "HSQLDB";

    private ConexionBD() {
    }

    /**
     * Obtiene la instancia activa de conexión SQL local.
     * Intenta primero cargar SQLite si el driver está presente en el classpath;
     * de lo contrario, utiliza el motor relacional embebido HSQLDB incluido en lib/hsqldb.jar.
     *
     * @return Objeto Connection JDBC listo para consultas SQL.
     * @throws SQLException Si ocurre un error al abrir la base de datos.
     */
    public static Connection obtenerConexion() throws SQLException {
        if (conexionUnica != null && !conexionUnica.isClosed()) {
            return conexionUnica;
        }

        File carpetaDatos = new File("datos_almacen");
        if (!carpetaDatos.exists()) {
            carpetaDatos.mkdirs();
        }

        try {
            Class.forName("org.sqlite.JDBC");
            String urlSqlite = "jdbc:sqlite:datos_almacen/almacen_local.db";
            conexionUnica = DriverManager.getConnection(urlSqlite);
            motorActivo = "SQLITE";
            Statement stPragma = conexionUnica.createStatement();
            stPragma.execute("PRAGMA foreign_keys = ON;");
            stPragma.close();
        } catch (ClassNotFoundException exSqlite) {
            try {
                Class.forName("org.hsqldb.jdbcDriver");
                try {
                    String urlHsqldb = "jdbc:hsqldb:file:datos_almacen/almacen_sql;shutdown=true";
                    conexionUnica = DriverManager.getConnection(urlHsqldb, "SA", "");
                    Statement stTest = conexionUnica.createStatement();
                    stTest.execute("CREATE TABLE TEST_SYNC (id INT)");
                    stTest.execute("DROP TABLE TEST_SYNC");
                    stTest.close();
                    motorActivo = "HSQLDB";
                } catch (SQLException exSync) {
                    // Fallback automático a base de datos relacional SQL en memoria si el sistema de archivos restringe fsync
                    conexionUnica = DriverManager.getConnection("jdbc:hsqldb:mem:almacen_sql", "SA", "");
                    motorActivo = "HSQLDB";
                }
            } catch (ClassNotFoundException exHsql) {
                throw new SQLException("No se encontró ningún driver JDBC local (HSQLDB o SQLite) en el classpath.");
            }
        }

        return conexionUnica;
    }

    /**
     * Crea las tablas relacionales si no existen y carga datos históricos de ejemplo
     * cuando la base de datos se inicia por primera vez.
     */
    public static void inicializarBaseDeDatos() {
        try {
            Connection conn = obtenerConexion();
            Statement stmt = conn.createStatement();

            if ("SQLITE".equals(motorActivo)) {
                stmt.execute("CREATE TABLE IF NOT EXISTS PRODUCTO ("
                        + "codigo_barras VARCHAR(50) NOT NULL PRIMARY KEY, "
                        + "nombre VARCHAR(120) NOT NULL, "
                        + "categoria VARCHAR(60) NOT NULL, "
                        + "precio_unitario DECIMAL(12,2) NOT NULL, "
                        + "stock_actual INTEGER NOT NULL, "
                        + "stock_minimo INTEGER DEFAULT 5 NOT NULL, "
                        + "activo BOOLEAN DEFAULT 1 NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS VENTA ("
                        + "id_venta INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "fecha_hora TIMESTAMP NOT NULL, "
                        + "monto_total DECIMAL(12,2) NOT NULL, "
                        + "cantidad_items INTEGER NOT NULL, "
                        + "metodo_pago VARCHAR(40) DEFAULT 'EFECTIVO' NOT NULL)");

                stmt.execute("CREATE TABLE IF NOT EXISTS DETALLE_VENTA ("
                        + "id_detalle INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "id_venta INTEGER NOT NULL, "
                        + "codigo_barras VARCHAR(50) NOT NULL, "
                        + "cantidad INTEGER NOT NULL, "
                        + "precio_unitario_historico DECIMAL(12,2) NOT NULL, "
                        + "subtotal DECIMAL(12,2) NOT NULL, "
                        + "FOREIGN KEY (id_venta) REFERENCES VENTA(id_venta) ON DELETE CASCADE, "
                        + "FOREIGN KEY (codigo_barras) REFERENCES PRODUCTO(codigo_barras))");

                stmt.execute("CREATE TABLE IF NOT EXISTS MOVIMIENTO_STOCK ("
                        + "id_movimiento INTEGER PRIMARY KEY AUTOINCREMENT, "
                        + "codigo_barras VARCHAR(50) NOT NULL, "
                        + "fecha_hora TIMESTAMP NOT NULL, "
                        + "cantidad_agregada INTEGER NOT NULL, "
                        + "origen VARCHAR(30) NOT NULL, "
                        + "observacion VARCHAR(200), "
                        + "FOREIGN KEY (codigo_barras) REFERENCES PRODUCTO(codigo_barras))");
            } else {
                ejecutarDdlSeguro(stmt, "CREATE TABLE PRODUCTO ("
                        + "codigo_barras VARCHAR(50) NOT NULL PRIMARY KEY, "
                        + "nombre VARCHAR(120) NOT NULL, "
                        + "categoria VARCHAR(60) NOT NULL, "
                        + "precio_unitario DECIMAL(12,2) NOT NULL, "
                        + "stock_actual INTEGER NOT NULL, "
                        + "stock_minimo INTEGER DEFAULT 5 NOT NULL, "
                        + "activo BOOLEAN DEFAULT TRUE NOT NULL)");

                ejecutarDdlSeguro(stmt, "CREATE TABLE VENTA ("
                        + "id_venta INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1) PRIMARY KEY, "
                        + "fecha_hora TIMESTAMP NOT NULL, "
                        + "monto_total DECIMAL(12,2) NOT NULL, "
                        + "cantidad_items INTEGER NOT NULL, "
                        + "metodo_pago VARCHAR(40) DEFAULT 'EFECTIVO' NOT NULL)");

                ejecutarDdlSeguro(stmt, "CREATE TABLE DETALLE_VENTA ("
                        + "id_detalle INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1) PRIMARY KEY, "
                        + "id_venta INTEGER NOT NULL, "
                        + "codigo_barras VARCHAR(50) NOT NULL, "
                        + "cantidad INTEGER NOT NULL, "
                        + "precio_unitario_historico DECIMAL(12,2) NOT NULL, "
                        + "subtotal DECIMAL(12,2) NOT NULL, "
                        + "CONSTRAINT fk_det_venta FOREIGN KEY (id_venta) REFERENCES VENTA(id_venta) ON DELETE CASCADE, "
                        + "CONSTRAINT fk_det_prod FOREIGN KEY (codigo_barras) REFERENCES PRODUCTO(codigo_barras))");

                ejecutarDdlSeguro(stmt, "CREATE TABLE MOVIMIENTO_STOCK ("
                        + "id_movimiento INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1) PRIMARY KEY, "
                        + "codigo_barras VARCHAR(50) NOT NULL, "
                        + "fecha_hora TIMESTAMP NOT NULL, "
                        + "cantidad_agregada INTEGER NOT NULL, "
                        + "origen VARCHAR(30) NOT NULL, "
                        + "observacion VARCHAR(200), "
                        + "CONSTRAINT fk_mov_prod FOREIGN KEY (codigo_barras) REFERENCES PRODUCTO(codigo_barras))");
            }

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM PRODUCTO");
            int cantidadProductos = 0;
            if (rs.next()) {
                cantidadProductos = rs.getInt(1);
            }
            rs.close();
            stmt.close();

            if (cantidadProductos == 0) {
                cargarDatosSemilla(conn);
            }
        } catch (SQLException e) {
            System.err.println("Error al inicializar la base de datos SQL local: " + e.getMessage());
        }
    }

    private static void ejecutarDdlSeguro(Statement stmt, String sql) {
        try {
            stmt.execute(sql);
        } catch (SQLException exIgnorado) {
            // La tabla ya existe en la base de datos
        }
    }

    /**
     * Inserta de forma imperativa productos típicos de un almacén barrial y ventas
     * distribuidas en una semana completa anterior y en el día actual.
     */
    private static void cargarDatosSemilla(Connection conn) throws SQLException {
        String[][] productosIniciales = new String[][] {
            {"7790001001015", "Yerba Mate Tradicional 1Kg", "Almacén", "4200.00", "45", "10"},
            {"7790001001022", "Aceite de Girasol Puro 900ml", "Almacén", "2350.00", "32", "8"},
            {"7790001001039", "Fideos Tallarines Trigo Candial 500g", "Pastas", "1450.00", "60", "12"},
            {"7790001001046", "Arroz Largo Fino 00000 1Kg", "Almacén", "1890.00", "50", "10"},
            {"7790001001053", "Leche Entera Larga Vida 1L", "Lácteos", "1650.00", "38", "15"},
            {"7790001001060", "Galletitas de Agua Clásicas Paq x3", "Galletitas", "1780.00", "40", "10"},
            {"7790001001077", "Gaseosa Cola 2.25L", "Bebidas", "3100.00", "28", "8"},
            {"7790001001084", "Agua Mineral sin Gas 2L", "Bebidas", "1300.00", "35", "10"},
            {"7790001001091", "Detergente Lavavajillas Limón 750ml", "Limpieza", "2100.00", "18", "5"},
            {"7790001001107", "Jabón en Pan Blanco x2u", "Limpieza", "1520.00", "14", "5"},
            {"7790001001114", "Mermelada de Durazno Frasco 454g", "Almacén", "2680.00", "12", "4"},
            {"7790001001121", "Atún al Natural en Lata 170g", "Conservas", "3450.00", "15", "5"}
        };

        PreparedStatement psProd = conn.prepareStatement(
                "INSERT INTO PRODUCTO (codigo_barras, nombre, categoria, precio_unitario, stock_actual, stock_minimo, activo) "
                + "VALUES (?, ?, ?, ?, ?, ?, TRUE)");

        for (int i = 0; i < productosIniciales.length; i++) {
            psProd.setString(1, productosIniciales[i][0]);
            psProd.setString(2, productosIniciales[i][1]);
            psProd.setString(3, productosIniciales[i][2]);
            psProd.setDouble(4, Double.parseDouble(productosIniciales[i][3]));
            psProd.setInt(5, Integer.parseInt(productosIniciales[i][4]));
            psProd.setInt(6, Integer.parseInt(productosIniciales[i][5]));
            psProd.executeUpdate();
        }
        psProd.close();

        LocalDate hoy = LocalDate.now();
        int diaActualIso = hoy.getDayOfWeek().getValue();
        LocalDate lunesSemanaActual = hoy.minusDays(diaActualIso - 1);
        LocalDate lunesSemanaAnterior = lunesSemanaActual.minusDays(7);

        // Ventas de los 7 días de la semana pasada (Martes es el día menos vendido con $4.050)
        registrarVentaSemilla(conn, lunesSemanaAnterior.plusDays(0), "7790001001015", 3, 4200.00, "7790001001053", 4, 1650.00);
        registrarVentaSemilla(conn, lunesSemanaAnterior.plusDays(1), "7790001001084", 2, 1300.00, "7790001001039", 1, 1450.00);
        registrarVentaSemilla(conn, lunesSemanaAnterior.plusDays(2), "7790001001022", 3, 2350.00, "7790001001046", 4, 1890.00);
        registrarVentaSemilla(conn, lunesSemanaAnterior.plusDays(3), "7790001001077", 4, 3100.00, "7790001001060", 3, 1780.00);
        registrarVentaSemilla(conn, lunesSemanaAnterior.plusDays(4), "7790001001015", 4, 4200.00, "7790001001077", 3, 3100.00);
        registrarVentaSemilla(conn, lunesSemanaAnterior.plusDays(5), "7790001001015", 5, 4200.00, "7790001001053", 6, 1650.00);
        registrarVentaSemilla(conn, lunesSemanaAnterior.plusDays(6), "7790001001077", 3, 3100.00, "7790001001039", 4, 1450.00);

        // Ventas del día de hoy
        registrarVentaSemilla(conn, hoy, "7790001001015", 2, 4200.00, "7790001001053", 3, 1650.00);
        registrarVentaSemilla(conn, hoy, "7790001001022", 1, 2350.00, "7790001001060", 2, 1780.00);
    }

    private static void registrarVentaSemilla(Connection conn, LocalDate fecha,
                                              String cod1, int cant1, double precio1,
                                              String cod2, int cant2, double precio2) throws SQLException {
        double sub1 = cant1 * precio1;
        double sub2 = cant2 * precio2;
        double total = sub1 + sub2;
        int totalItems = cant1 + cant2;
        Timestamp ts = Timestamp.valueOf(LocalDateTime.of(fecha, LocalTime.of(11, 30, 0)));

        PreparedStatement psVenta = conn.prepareStatement(
                "INSERT INTO VENTA (fecha_hora, monto_total, cantidad_items, metodo_pago) VALUES (?, ?, ?, 'EFECTIVO')");
        psVenta.setTimestamp(1, ts);
        psVenta.setDouble(2, total);
        psVenta.setInt(3, totalItems);
        psVenta.executeUpdate();
        psVenta.close();

        int idVentaGenerado = 1;
        Statement stMax = conn.createStatement();
        ResultSet rsMax = stMax.executeQuery("SELECT MAX(id_venta) FROM VENTA");
        if (rsMax.next()) {
            idVentaGenerado = rsMax.getInt(1);
        }
        rsMax.close();
        stMax.close();

        PreparedStatement psDet = conn.prepareStatement(
                "INSERT INTO DETALLE_VENTA (id_venta, codigo_barras, cantidad, precio_unitario_historico, subtotal) "
                + "VALUES (?, ?, ?, ?, ?)");
        psDet.setInt(1, idVentaGenerado);
        psDet.setString(2, cod1);
        psDet.setInt(3, cant1);
        psDet.setDouble(4, precio1);
        psDet.setDouble(5, sub1);
        psDet.executeUpdate();

        psDet.setInt(1, idVentaGenerado);
        psDet.setString(2, cod2);
        psDet.setInt(3, cant2);
        psDet.setDouble(4, precio2);
        psDet.setDouble(5, sub2);
        psDet.executeUpdate();
        psDet.close();
    }

    public static void cerrarConexion() {
        try {
            if (conexionUnica != null && !conexionUnica.isClosed()) {
                if ("HSQLDB".equals(motorActivo)) {
                    Statement st = conexionUnica.createStatement();
                    st.execute("SHUTDOWN");
                    st.close();
                }
                conexionUnica.close();
            }
        } catch (SQLException e) {
            System.err.println("Aviso al cerrar la base de datos: " + e.getMessage());
        }
    }
}
