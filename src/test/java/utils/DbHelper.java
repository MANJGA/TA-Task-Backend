package utils;

import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DbHelper {

    private final String dbPath;
    private final String url;

    public DbHelper() {
        this.dbPath = System.getProperty("sqlite.path",
                System.getProperty("user.dir") + "/target/test-results.db");
        this.url = "jdbc:sqlite:" + dbPath;
        init();
    }

    private void init() {
        try (Connection con = DriverManager.getConnection(url);
             Statement st = con.createStatement()) {

            st.execute("""
                CREATE TABLE IF NOT EXISTS test_results (
                  id INTEGER PRIMARY KEY AUTOINCREMENT,
                  test_name TEXT UNIQUE,
                  status TEXT,
                  execution_time DATETIME
                )
            """);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to init SQLite DB at " + dbPath, e);
        }
    }

    public void upsertResult(String testName, String status) {
        String now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        String sql = """
            INSERT INTO test_results (test_name, status, execution_time)
            VALUES (?, ?, ?)
            ON CONFLICT(test_name)
            DO UPDATE SET status = excluded.status, execution_time = excluded.execution_time
        """;

        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, testName);
            ps.setString(2, status);
            ps.setString(3, now);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to upsert test result for " + testName, e);
        }
    }
}
