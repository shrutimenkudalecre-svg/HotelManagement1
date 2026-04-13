import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class TestDB {
    public static void main(String[] args) {
        String password = System.getenv("AIVEN_PASSWORD");
        if (password == null || password.isEmpty()) {
            System.out.println("Set AIVEN_PASSWORD env var");
            return;
        }

        String host = "mysql-3b81e08-hotelmanagement.i.aivencloud.com";
        String port = "24200";
        String db = "defaultdb";
        String user = "avnadmin";

        String[] testUrls = {
            "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=true&serverTimezone=UTC",
            "jdbc:mysql://" + host + ":" + port + "/" + db + "?sslMode=REQUIRED&serverTimezone=UTC",
            "jdbc:mysql://" + host + ":" + port + "/" + db + "?useSSL=true&requireSSL=true&serverTimezone=UTC"
        };

        for (String url : testUrls) {
            System.out.println("Testing: " + url);
            try {
                Connection conn = DriverManager.getConnection(url, user, password);
                System.out.println("✅ SUCCESS");
                conn.close();
            } catch (Exception e) {
                System.out.println("❌ FAILED: " + e.getMessage());
            }
        }
    }
}
