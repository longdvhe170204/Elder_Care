package com.cambae.backend.support;

import java.net.URI;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Lớp cơ sở cho test tích hợp chạy trên MySQL cài trên máy (research R-14).
 *
 * <p>Đọc {@code TEST_DB_URL} (ví dụ {@code jdbc:mysql://localhost:3306/eldercare_test}), {@code TEST_DB_ADMIN_USER},
 * {@code TEST_DB_ADMIN_PASSWORD}. Mỗi lần chạy JVM: xóa và tạo lại schema test, tạo lại hai tài khoản
 * {@code eldercare_app} và {@code eldercare_retention} (R-05) với mật khẩu ngẫu nhiên, rồi để Flyway chạy bằng tài
 * khoản quản trị. Quyền theo bảng của hai tài khoản nằm trong migration của bảng đó; ứng dụng kết nối bằng
 * {@code eldercare_app}.
 */
@SpringBootTest
@ActiveProfiles("test")
public abstract class MySqlIntegrationTestBase {

    public static final String APP_USER = "eldercare_app";
    public static final String RETENTION_USER = "eldercare_retention";
    private static final String DEFAULT_SCHEMA = "eldercare_test";

    private static TestDatabase database;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        TestDatabase db = prepareDatabase();
        registry.add("spring.datasource.url", db::schemaUrl);
        registry.add("spring.datasource.username", () -> APP_USER);
        registry.add("spring.datasource.password", db::appPassword);
        registry.add("spring.flyway.url", db::schemaUrl);
        registry.add("spring.flyway.user", db::adminUser);
        registry.add("spring.flyway.password", db::adminPassword);
        registry.add("eldercare.datasource.retention.url", db::schemaUrl);
        registry.add("eldercare.datasource.retention.username", () -> RETENTION_USER);
        registry.add("eldercare.datasource.retention.password", db::retentionPassword);
    }

    private static synchronized TestDatabase prepareDatabase() {
        if (database == null) {
            database = TestDatabase.fromEnvironment();
            database.recreate();
        }
        return database;
    }

    record TestDatabase(
            String serverUrl,
            String schema,
            String query,
            String adminUser,
            String adminPassword,
            String appPassword,
            String retentionPassword) {

        static TestDatabase fromEnvironment() {
            String url = requireEnv("TEST_DB_URL");
            if (!url.startsWith("jdbc:mysql://")) {
                throw new IllegalStateException("TEST_DB_URL phải có dạng jdbc:mysql://host:port/schema");
            }
            URI uri = URI.create(url.substring("jdbc:".length()));
            String path = uri.getPath() == null ? "" : uri.getPath().replaceFirst("^/", "");
            String schema = path.isBlank() ? DEFAULT_SCHEMA : path;
            if (!schema.matches("[A-Za-z0-9_]+")) {
                throw new IllegalStateException("Tên schema test không hợp lệ: " + schema);
            }
            String serverUrl = "jdbc:mysql://" + uri.getAuthority() + "/";
            return new TestDatabase(
                    serverUrl,
                    schema,
                    uri.getQuery(),
                    requireEnv("TEST_DB_ADMIN_USER"),
                    requireEnv("TEST_DB_ADMIN_PASSWORD"),
                    randomPassword(),
                    randomPassword());
        }

        String schemaUrl() {
            return serverUrl + schema + (query == null ? "" : "?" + query);
        }

        void recreate() {
            try (Connection c = DriverManager.getConnection(serverUrl, adminUser, adminPassword);
                    Statement s = c.createStatement()) {
                s.execute("DROP DATABASE IF EXISTS `" + schema + "`");
                s.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                recreateUser(s, APP_USER, appPassword);
                recreateUser(s, RETENTION_USER, retentionPassword);
            } catch (SQLException e) {
                throw new IllegalStateException("Không dựng được schema test " + schema + ": " + e.getMessage(), e);
            }
        }

        private static void recreateUser(Statement s, String user, String password) throws SQLException {
            s.execute("DROP USER IF EXISTS '" + user + "'@'%'");
            s.execute("CREATE USER '" + user + "'@'%' IDENTIFIED BY '" + password + "'");
        }

        private static String requireEnv(String name) {
            String value = System.getenv(name);
            if (value == null || value.isBlank()) {
                throw new IllegalStateException("Thiếu biến môi trường " + name + " cho test tích hợp (research R-14)");
            }
            return value;
        }

        private static String randomPassword() {
            String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
            SecureRandom random = new SecureRandom();
            StringBuilder sb = new StringBuilder("Aa1-");
            for (int i = 0; i < 24; i++) {
                sb.append(chars.charAt(random.nextInt(chars.length())));
            }
            return sb.toString();
        }
    }
}
