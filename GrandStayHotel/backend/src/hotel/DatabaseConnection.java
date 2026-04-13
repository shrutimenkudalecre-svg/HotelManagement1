package hotel;

import java.sql.*;

public class DatabaseConnection {

    // Reads from environment variables; falls back to local defaults for development
    private static final String DB_HOST     = getEnv("DB_HOST",     "localhost");
    private static final String DB_PORT     = getEnv("DB_PORT",     "3306");
    private static final String DB_NAME     = getEnv("DB_NAME",     "grand_stay_hotel");
    private static final String DB_USER     = getEnv("DB_USER",     "root");
    private static final String DB_PASSWORD = getEnv("DB_PASSWORD", "pass123");

    private static final String URL =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
        "?sslMode=REQUIRED&serverTimezone=UTC";

    private static Connection connection = null;

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(URL, DB_USER, DB_PASSWORD);
                System.out.println("Database connected: " + DB_HOST + ":" + DB_PORT + "/" + DB_NAME);
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found: " + e.getMessage());
            }
        }
        return connection;
    }

    public static void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing connection: " + e.getMessage());
        }
    }
}
