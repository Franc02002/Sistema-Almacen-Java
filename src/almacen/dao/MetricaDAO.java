package almacen.dao;

import almacen.conexion.ConexionBD;
import almacen.modelo.ResumenMetricaDia;
import almacen.modelo.ResumenProductoVenta;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * DAO especializado en consultas analíticas y métricas de negocio:
 * - Monto total hecho diario, semanal y mensual.
 * - Ranking de productos menos vendidos (incluyendo aquellos con 0 ventas).
 * - Desglose diario de una semana existente para determinar el día de la semana menos vendido.
 *
 * Teoria
 *   DAO (Data Access Object) encargado de todas las operaciones SQL sobre la tabla PRODUCTO  y el registro de auditoría en MOVIMIENTO_STOCK.
 *
 *   PreparedStatement es una interfaz de java del parquete java.sql, representa una instruccion SQL precompilada, se usa como canal intermedio para envitar consultas
 *   parametrizadas a la BD de forma segura y eficiente.
 *   Usar los datos parametrizados hace que la consulta se envie solo con los marcadores de posicion, el motor SQL general el plan de ejecucion antes de saber los valores reales.
 *   Luego se envian los parametros y la BD incrusta esos valores directamente en los espacios reservados sin volver a analizar la consulta.
 *
 *      Parametros parametrizados(?), son marcadores de posicion que indican a la base de datos que ahi existira un dato pero sera pasado separado de la instruccion SQL,
 *      estos datos seran pasados luego con el PreparedStatement para evitar vulnearbilidades(SQL Injection) y ademas mejora la optimizacion al solo cargar una vez el plan de ejecucion
 *
 *
 */
public class MetricaDAO {

    private static final String[] NOMBRES_DIAS = new String[] {
        "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
    };

    /**
     * Calcula el monto total recaudado entre dos instantes de tiempo (inclusive).
     *
     * @param inicio Timestamp de inicio del período.
     * @param fin    Timestamp de cierre del período.
     * @return Suma total de ventas en pesos ($).
     */
    public double obtenerMontoEnRango(Timestamp inicio, Timestamp fin) {
        double montoTotal = 0.0;
        /** Coalese atrapa un posible null y lo convierte en 0 */
        //El alias 'total' de la query luego sera usado para guardar en una variable el resultado de la misma
        String sql = "SELECT COALESCE(SUM(monto_total), 0) AS total FROM VENTA WHERE fecha_hora >= ? AND fecha_hora <= ?";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setTimestamp(1, inicio);
            ps.setTimestamp(2, fin);
            //Realizo la query
            ResultSet rs = ps.executeQuery();

            //Como la query es una suma solo devolvera una fila, por eso el uso de if
            if (rs.next()) {
                //Guardo el resultado de la query en una variable, total es el alias que se le puso a la query
                montoTotal = rs.getDouble("total");
            }
            //Cierro los recursos usados
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al consultar monto en rango: " + e.getMessage());
        }
        return Math.round(montoTotal * 100.0) / 100.0;
    }

    /**
     * Obtiene el monto total hecho en un día específico (de 00:00:00 a 23:59:59).
     */
    public double obtenerMontoTotalDiario(LocalDate fecha) {
        Timestamp inicio = Timestamp.valueOf(LocalDateTime.of(fecha, LocalTime.of(0, 0, 0)));
        Timestamp fin = Timestamp.valueOf(LocalDateTime.of(fecha, LocalTime.of(23, 59, 59)));
        return obtenerMontoEnRango(inicio, fin);
    }

    /**
     * Obtiene el monto total hecho en los últimos 7 días hasta la fecha de referencia inclusive.
     */
    public double obtenerMontoTotalSemanal(LocalDate fechaReferencia) {
        LocalDate haceSieteDias = fechaReferencia.minusDays(6);
        Timestamp inicio = Timestamp.valueOf(LocalDateTime.of(haceSieteDias, LocalTime.of(0, 0, 0)));
        Timestamp fin = Timestamp.valueOf(LocalDateTime.of(fechaReferencia, LocalTime.of(23, 59, 59)));
        return obtenerMontoEnRango(inicio, fin);
    }

    /**
     * Obtiene el monto total hecho durante todo el mes calendario de la fecha de referencia.
     */
    public double obtenerMontoTotalMensual(LocalDate fechaReferencia) {
        LocalDate primerDiaMes = LocalDate.of(fechaReferencia.getYear(), fechaReferencia.getMonthValue(), 1);
        LocalDate ultimoDiaMes = primerDiaMes.plusMonths(1).minusDays(1);
        Timestamp inicio = Timestamp.valueOf(LocalDateTime.of(primerDiaMes, LocalTime.of(0, 0, 0)));
        Timestamp fin = Timestamp.valueOf(LocalDateTime.of(ultimoDiaMes, LocalTime.of(23, 59, 59)));
        return obtenerMontoEnRango(inicio, fin);
    }

    /**
     * Obtiene el listado de productos ordenados de MENOR a MAYOR cantidad de unidades vendidas.
     * Utiliza LEFT JOIN entre PRODUCTO y DETALLE_VENTA para incluir también los productos
     * que nunca fueron vendidos (0 unidades), fundamentales para detectar mercadería estancada.
     *
     * @param limite Cantidad máxima de productos a retornar en el ranking.
     * @return Lista ordenada desde el producto menos vendido.
     */
    public List<ResumenProductoVenta> obtenerProductosMenosVendidos(int limite) {
        List<ResumenProductoVenta> resultado = new ArrayList<ResumenProductoVenta>();
        String sql = "SELECT p.codigo_barras, p.nombre, p.categoria, p.precio_unitario, p.stock_actual, "
                   + "COALESCE(SUM(d.cantidad), 0) AS unidades_vendidas, "
                   + "COALESCE(SUM(d.subtotal), 0) AS monto_generado "
                   + "FROM PRODUCTO p "
                   + "LEFT JOIN DETALLE_VENTA d ON p.codigo_barras = d.codigo_barras "
                   + "WHERE p.activo = TRUE "
                   + "GROUP BY p.codigo_barras, p.nombre, p.categoria, p.precio_unitario, p.stock_actual "
                   + "ORDER BY unidades_vendidas ASC, monto_generado ASC, p.nombre ASC"
                   + "LIMIT ?";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ps.setInt(1,limite);

            try(ResultSet rs = ps.executeQuery()){
                while (rs.next() ) {
                    ResumenProductoVenta item = new ResumenProductoVenta(
                            rs.getString("codigo_barras"),
                            rs.getString("nombre"),
                            rs.getString("categoria"),
                            rs.getDouble("precio_unitario"),
                            rs.getInt("stock_actual"),
                            rs.getInt("unidades_vendidas"),
                            rs.getDouble("monto_generado")
                    );

                    resultado.add(item);
                }
                rs.close();
            }
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al obtener productos menos vendidos: " + e.getMessage());
        }
        return resultado;
    }

    /**
     * Analiza de forma imperativa los 7 días (Lunes a Domingo) de una semana existente
     * a partir de la fecha del lunes de dicha semana.
     *
     * @param lunesDeLaSemana Fecha correspondiente al Lunes de la semana a analizar.
     * @return Lista de 7 objetos ResumenMetricaDia (uno por cada día de Lunes a Domingo).
     */
    public List<ResumenMetricaDia> obtenerDesgloseSemanal(LocalDate lunesDeLaSemana) {
        // Ajustar por seguridad al lunes de esa semana si el usuario ingresó otro día
        int valorDia = lunesDeLaSemana.getDayOfWeek().getValue();
        LocalDate lunesReal = lunesDeLaSemana.minusDays(valorDia - 1);

        List<ResumenMetricaDia> desglose = new ArrayList<ResumenMetricaDia>();
        String sqlDia = "SELECT COUNT(*) AS tickets, COALESCE(SUM(cantidad_items), 0) AS unidades, "
                      + "COALESCE(SUM(monto_total), 0) AS monto "
                      + "FROM VENTA WHERE fecha_hora >= ? AND fecha_hora <= ?";

        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sqlDia);

            for (int i = 0; i < 7; i++) {
                LocalDate fechaDia = lunesReal.plusDays(i);
                Timestamp inicioDia = Timestamp.valueOf(LocalDateTime.of(fechaDia, LocalTime.of(0, 0, 0)));
                Timestamp finDia = Timestamp.valueOf(LocalDateTime.of(fechaDia, LocalTime.of(23, 59, 59)));

                ps.setTimestamp(1, inicioDia);
                ps.setTimestamp(2, finDia);
                ResultSet rs = ps.executeQuery();

                int tickets = 0;
                int unidades = 0;
                double monto = 0.0;
                if (rs.next()) {
                    tickets = rs.getInt("tickets");
                    unidades = rs.getInt("unidades");
                    monto = rs.getDouble("monto");
                }
                rs.close();

                ResumenMetricaDia resumenDia = new ResumenMetricaDia(
                        i + 1,
                        NOMBRES_DIAS[i],
                        fechaDia,
                        tickets,
                        unidades,
                        Math.round(monto * 100.0) / 100.0
                );
                desglose.add(resumenDia);
            }
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al analizar desglose semanal: " + e.getMessage());
        }
        return desglose;
    }

    /**
     * Obtiene la lista de Lunes correspondientes a las semanas en las que existen ventas registradas,
     * para facilitar al usuario la selección de una semana ya existente en el análisis.
     */
    public List<LocalDate> listarLunesDeSemanasExistentes() {
        List<LocalDate> lunesSemanas = new ArrayList<LocalDate>();
        String sql = "SELECT fecha_hora FROM VENTA ORDER BY fecha_hora DESC";
        try {
            Connection conn = ConexionBD.obtenerConexion();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Timestamp ts = rs.getTimestamp("fecha_hora");
                if (ts != null) {
                    LocalDate fecha = ts.toLocalDateTime().toLocalDate();
                    DayOfWeek dow = fecha.getDayOfWeek();
                    LocalDate lunes = fecha.minusDays(dow.getValue() - 1);

                    boolean yaExiste = false;
                    for (int i = 0; i < lunesSemanas.size(); i++) {
                        if (lunesSemanas.get(i).equals(lunes)) {
                            yaExiste = true;
                            break;
                        }
                    }
                    if (!yaExiste) {
                        lunesSemanas.add(lunes);
                    }
                }
            }
            rs.close();
            ps.close();
        } catch (SQLException e) {
            System.err.println("Error al listar semanas existentes: " + e.getMessage());
        }

        // Si por algún motivo no hubiera ventas, agregar la semana pasada por defecto
        if (lunesSemanas.isEmpty()) {
            LocalDate hoy = LocalDate.now();
            LocalDate lunesActual = hoy.minusDays(hoy.getDayOfWeek().getValue() - 1);
            lunesSemanas.add(lunesActual.minusDays(7));
        }
        return lunesSemanas;
    }
}
