package org.example;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Clase principal para la gestión de la biblioteca usando JDBC.
 * Está todo dockerizado
 */
public class Main {

    // --- Configuración de la Conexión ---
    private static final String DB_URL = "jdbc:mysql://localhost:3306/biblioteca";
    private static final String USER = "root";
    private static final String PASS = "2001";

    /**
     * Método principal  que ejecuta todas las consulta de la tarea.
     */
    public static void main(String[] args) {
        System.out.println("--- Iniciando Gestor de Biblioteca JDBC ---");

        // --- Punto 4: Consultas Las ejecuto llamando a sus métodos---
        System.out.println("\n--- 4a. Libros de Ficción post-1980 ---");
        punto4a_librosFiccion();

        System.out.println("\n--- 4b. Usuarios con Préstamos Activos ---");
        punto4b_usuariosActivos();

        System.out.println("\n--- 4c. Total de Préstamos por Usuario ---");
        punto4c_prestamosPorUsuario();

        System.out.println("\n--- 4d. Libros con Ejemplares Disponibles ---");
        punto4d_librosDisponibles();

        // --- Punto 5: Manipulación (UPDATE) ---
        System.out.println("\n--- 5a. Incrementar Ejemplares (Historia) ---");
        punto5_actualizarEjemplaresHistoria();

        // --- Punto 6: Registro de Nuevo Préstamo (Transacción) solo acaba cuando está todo terminado ---
        System.out.println("\n--- 6. Registrar Nuevo Préstamo (Transacción) ---");
        // Vamos a prestar el libro ID 10 (El alquimista) al usuario ID 1 (María López)
        // El libro 10 tiene 5 ejemplares.
        punto6_registrarPrestamo(1, 10);
        // Intentemos prestar un libro sin stock (ej: libro 3, que tiene 2 y prestamos 2 más)
        // punto6_registrarPrestamo(2, 3);
        // punto6_registrarPrestamo(3, 3); // Este fallará si descomentas los 3

        // --- Punto 7: Devolución de Libro (Transacción) ---
        System.out.println("\n--- 7. Registrar Devolución (Transacción) ---");
        // Vamos a devolver el préstamo ID 2 (Carlos, libro '1984'), que está 'Activo'
        punto7_registrarDevolucion(2);

        // --- Punto 8: Clasificación de Libros por Disponibilidad ---
        System.out.println("\n--- 8. Clasificar Libros por Disponibilidad ---");
        punto8_clasificarDisponibilidad();

        System.out.println("\n--- Demostración completada ---");
    }

    /**
     * Obtiene una conexión a la base de datos.
     */
    private static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    // --- MÉTODOS DE LA TAREA ---

    /**
     * 4a. Lista todos los libros de la categoría “Ficción” publicados después del año 1980.
     */
    public static void punto4a_librosFiccion() {
        String sql = "SELECT titulo, autor, anio_publicacion FROM libros WHERE categoria = 'Ficción' AND anio_publicacion > 1980";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("  - Título: %s, Autor: %s, Año: %d%n",
                        rs.getString("titulo"),
                        rs.getString("autor"),
                        rs.getInt("anio_publicacion"));
            }

        } catch (SQLException e) {
            System.err.println("Error en 4a: " + e.getMessage());
        }
    }

    /**
     * 4b. Muestra los usuarios que tienen préstamos activos junto con los títulos de los libros.
     */
    public static void punto4b_usuariosActivos() {
        String sql = "SELECT u.nombre_completo, l.titulo " +
                "FROM prestamos p " +
                "JOIN usuarios u ON p.usuario_id = u.id " +
                "JOIN libros l ON p.libro_id = l.id " +
                "WHERE p.estado = 'Activo'";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("  - Usuario: %s, Libro: %s%n",
                        rs.getString("nombre_completo"),
                        rs.getString("titulo"));
            }

        } catch (SQLException e) {
            System.err.println("Error en 4b: " + e.getMessage());
        }
    }

    /**
     * 4c. Calcula el número total de préstamos realizados por cada usuario.
     */
    public static void punto4c_prestamosPorUsuario() {
        String sql = "SELECT u.nombre_completo, COUNT(p.id) AS total_prestamos " +
                "FROM usuarios u " +
                "LEFT JOIN prestamos p ON u.id = p.usuario_id " +
                "GROUP BY u.id, u.nombre_completo"; // Agrupar por ID y Nombre

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("  - Usuario: %s, Total Préstamos: %d%n",
                        rs.getString("nombre_completo"),
                        rs.getInt("total_prestamos"));
            }

        } catch (SQLException e) {
            System.err.println("Error en 4c: " + e.getMessage());
        }
    }

    /**
     * 4d. Encuentra qué libros tienen ejemplares disponibles para préstamo.
     */
    public static void punto4d_librosDisponibles() {
        String sql = "SELECT titulo, autor, ejemplares_disponibles FROM libros WHERE ejemplares_disponibles > 0";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                System.out.printf("  - Título: %s, Ejemplares: %d%n",
                        rs.getString("titulo"),
                        rs.getInt("ejemplares_disponibles"));
            }

        } catch (SQLException e) {
            System.err.println("Error en 4d: " + e.getMessage());
        }
    }

    /**
     * 5a. Incremente en dos el número de ejemplares disponibles para "Historia".
     */
    public static void punto5_actualizarEjemplaresHistoria() {
        String sql = "UPDATE libros SET ejemplares_disponibles = ejemplares_disponibles + 2 WHERE categoria = 'Historia'";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            int filasAfectadas = stmt.executeUpdate(sql);
            System.out.printf("  Se incrementaron 2 ejemplares a %d libro(s) de 'Historia'.%n", filasAfectadas);

            // Verificación
            try (ResultSet rs = stmt.executeQuery("SELECT titulo, ejemplares_disponibles FROM libros WHERE categoria = 'Historia'")) {
                while(rs.next()) {
                    System.out.printf("    -> (Verificación) %s ahora tiene %d ejemplares.%n", rs.getString("titulo"), rs.getInt("ejemplares_disponibles"));
                }
            }

        } catch (SQLException e) {
            System.err.println("Error en 5a: " + e.getMessage());
        }
    }

    /**
     * 6. Registra un nuevo préstamo. (Transaccional)
     *
     * @param usuarioId ID del usuario que toma el libro.
     * @param libroId   ID del libro a prestar.
     */
    public static void punto6_registrarPrestamo(int usuarioId, int libroId) {
        String sqlCheckStock = "SELECT ejemplares_disponibles FROM libros WHERE id = ? FOR UPDATE";
        String sqlUpdateStock = "UPDATE libros SET ejemplares_disponibles = ejemplares_disponibles - 1 WHERE id = ?";
        String sqlInsertPrestamo = "INSERT INTO prestamos (id, usuario_id, libro_id, fecha_prestamo, estado) VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = getConnection();
            // --- INICIO DE LA TRANSACCIÓN ---
            conn.setAutoCommit(false);

            int ejemplares = 0;

            // 1. Comprobar stock (y bloquear la fila con "FOR UPDATE")
            try (PreparedStatement stmtCheck = conn.prepareStatement(sqlCheckStock)) {
                stmtCheck.setInt(1, libroId);
                try (ResultSet rs = stmtCheck.executeQuery()) {
                    if (rs.next()) {
                        ejemplares = rs.getInt("ejemplares_disponibles");
                    } else {
                        throw new SQLException("El libro (ID: " + libroId + ") no existe.");
                    }
                }
            }

            // 2. Validar si hay stock
            if (ejemplares > 0) {
                System.out.printf("  Stock verificado (Libro ID %d). Quedan %d. Procediendo...%n", libroId, ejemplares);

                // 3. Actualizar el stock
                try (PreparedStatement stmtUpdate = conn.prepareStatement(sqlUpdateStock)) {
                    stmtUpdate.setInt(1, libroId);
                    stmtUpdate.executeUpdate();
                }

                // 4. Insertar el nuevo préstamo
                int nuevoPrestamoId = 0;
                try(Statement stmtId = conn.createStatement();
                    ResultSet rsId = stmtId.executeQuery("SELECT MAX(id) FROM prestamos")) {
                    if(rsId.next()) {
                        nuevoPrestamoId = rsId.getInt(1) + 1;
                    }
                }

                try (PreparedStatement stmtInsert = conn.prepareStatement(sqlInsertPrestamo)) {
                    stmtInsert.setInt(1, nuevoPrestamoId);
                    stmtInsert.setInt(2, usuarioId);
                    stmtInsert.setInt(3, libroId);
                    stmtInsert.setDate(4, Date.valueOf(LocalDate.now())); // Fecha actual
                    stmtInsert.setString(5, "Activo");
                    stmtInsert.executeUpdate();
                }

                // --- COMMIT ---
                // Si todo fue bien, confirmamos los cambios
                conn.commit();
                System.out.printf("  ¡Éxito! Préstamo (ID %d) registrado. Libro %d -> Usuario %d.%n", nuevoPrestamoId, libroId, usuarioId);

            } else {
                // No hay stock, revertimos (aunque no sea necesario, es buena práctica)
                System.out.printf("  Fallo: No hay ejemplares disponibles del libro ID %d.%n", libroId);
                conn.rollback();
            }

        } catch (SQLException e) {
            System.err.println("Error en transacción 6: " + e.getMessage());
            try {
                if (conn != null) {
                    System.err.println("  Realizando Rollback...");
                    conn.rollback(); // --- ROLLBACK ---
                }
            } catch (SQLException e2) {
                System.err.println("Error en rollback: " + e2.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Devolver al estado normal
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
    }

    /**
     * 7. Registra la devolución de un libro. (Transaccional)
     *
     * @param prestamoId ID del préstamo a finalizar.
     */
    public static void punto7_registrarDevolucion(int prestamoId) {
        String sqlGetPrestamo = "SELECT libro_id, estado FROM prestamos WHERE id = ? FOR UPDATE";
        String sqlUpdatePrestamo = "UPDATE prestamos SET estado = 'Devuelto', fecha_devolucion = ? WHERE id = ?";
        String sqlUpdateStock = "UPDATE libros SET ejemplares_disponibles = ejemplares_disponibles + 1 WHERE id = ?";

        Connection conn = null;
        try {
            conn = getConnection();
            // --- INICIO DE LA TRANSACCIÓN ---
            conn.setAutoCommit(false);

            int libroId = -1;
            String estadoActual = "";

            // 1. Verificar el préstamo
            try (PreparedStatement stmtGet = conn.prepareStatement(sqlGetPrestamo)) {
                stmtGet.setInt(1, prestamoId);
                try (ResultSet rs = stmtGet.executeQuery()) {
                    if (rs.next()) {
                        libroId = rs.getInt("libro_id");
                        estadoActual = rs.getString("estado");
                    } else {
                        throw new SQLException("El préstamo (ID: " + prestamoId + ") no existe.");
                    }
                }
            }

            // 2. Validar
            if (estadoActual.equals("Devuelto")) {
                System.out.printf("  Aviso: El préstamo ID %d ya estaba devuelto. No se hace nada.%n", prestamoId);
                conn.rollback();
            } else if (libroId != -1) {
                // 3. Actualizar el préstamo a 'Devuelto'
                try (PreparedStatement stmtUpdateP = conn.prepareStatement(sqlUpdatePrestamo)) {
                    stmtUpdateP.setDate(1, Date.valueOf(LocalDate.now())); // Fecha actual
                    stmtUpdateP.setInt(2, prestamoId);
                    stmtUpdateP.executeUpdate();
                }

                // 4. Incrementar el stock del libro
                try (PreparedStatement stmtUpdateL = conn.prepareStatement(sqlUpdateStock)) {
                    stmtUpdateL.setInt(1, libroId);
                    stmtUpdateL.executeUpdate();
                }

                // --- COMMIT ---
                conn.commit();
                System.out.printf("  ¡Éxito! Devolución registrada (Préstamo ID %d). Stock del libro ID %d actualizado.%n", prestamoId, libroId);

            } else {
                throw new SQLException("Error al procesar la devolución.");
            }

        } catch (SQLException e) {
            System.err.println("Error en transacción 7: " + e.getMessage());
            try {
                if (conn != null) {
                    System.err.println("  Realizando Rollback...");
                    conn.rollback(); // --- ROLLBACK ---
                }
            } catch (SQLException e2) {
                System.err.println("Error en rollback: " + e2.getMessage());
            }
        } finally {
            try {
                if (conn != null) {
                    conn.setAutoCommit(true); // Devolver al estado normal
                    conn.close();
                }
            } catch (SQLException e) {
                System.err.println("Error cerrando conexión: " + e.getMessage());
            }
        }
    }

    /**
     * 8. Clasificar libros por su disponibilidad.
     */
    public static void punto8_clasificarDisponibilidad() {
        String sql = "SELECT titulo, ejemplares_disponibles FROM libros";

        // Usamos Mapas para guardar las listas de libros
        Map<String, List<String>> clasificacion = new HashMap<>();
        clasificacion.put("Alta disponibilidad", new ArrayList<>());
        clasificacion.put("Moderada disponibilidad", new ArrayList<>());
        clasificacion.put("Sin disponibilidad", new ArrayList<>());

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                String titulo = rs.getString("titulo");
                int ejemplares = rs.getInt("ejemplares_disponibles");

                // Lógica de clasificación en Java
                if (ejemplares > 5) {
                    clasificacion.get("Alta disponibilidad").add(titulo + " (" + ejemplares + ")");
                } else if (ejemplares >= 1 && ejemplares <= 5) {
                    clasificacion.get("Moderada disponibilidad").add(titulo + " (" + ejemplares + ")");
                } else {
                    clasificacion.get("Sin disponibilidad").add(titulo + " (" + ejemplares + ")");
                }
            }

            // Mostramos los resultados
            System.out.println("  --- Clasificación de Disponibilidad ---");

            System.out.println("  [Alta Disponibilidad (> 5)]");
            clasificacion.get("Alta disponibilidad").forEach(t -> System.out.println("    - " + t));

            System.out.println("  [Moderada Disponibilidad (1-5)]");
            clasificacion.get("Moderada disponibilidad").forEach(t -> System.out.println("    - " + t));

            System.out.println("  [Sin Disponibilidad (0)]");
            if (clasificacion.get("Sin disponibilidad").isEmpty()) {
                System.out.println("    - (Ninguno)");
            } else {
                clasificacion.get("Sin disponibilidad").forEach(t -> System.out.println("    - " + t));
            }


        } catch (SQLException e) {
            System.err.println("Error en 8: " + e.getMessage());
        }
    }
}