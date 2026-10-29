package com.eldercare.backend.support;

import java.net.URI;
import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Lớp cơ sở cho test tích hợp chạy trên MySQL cài trên máy (research R-14).
 *
 * <p>Đọc {@code TEST_DB_URL} (ví dụ {@code jdbc:mysql://localhost:3306/eldercare_test}), {@code TEST_DB_ADMIN_USER},
 * {@code TEST_DB_ADMIN_PASSWORD}. Mỗi lần chạy JVM: xóa và tạo lại schema test, tạo hai tài khoản {@code eldercare_app}
 * và {@code eldercare_retention} (R-05) nếu chưa có, gỡ quyền của chúng trên schema test (giữ quyền trên schema khác) và
 * đặt mật khẩu theo {@code DB_PASSWORD}, {@code DB_RETENTION_PASSWORD} nếu có, không có thì ngẫu nhiên, rồi để Flyway chạy bằng tài
 * khoản quản trị (thêm dữ liệu mẫu {@code db/migration/demo} như profile demo, 000 T048). Quyền của hai tài khoản do migration cấp
 * ({@code V000_00} và migration của từng bảng). Ứng dụng kết nối bằng {@code eldercare_app}.
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
        registry.add("spring.flyway.placeholders.appPassword", db::appPassword);
        registry.add("spring.flyway.placeholders.retentionPassword", db::retentionPassword);
        registry.add("spring.flyway.locations", () -> "classpath:db/migration,classpath:db/migration/demo");
        registry.add("eldercare.datasource.retention.url", db::schemaUrl);
        registry.add("eldercare.datasource.retention.username", () -> RETENTION_USER);
        registry.add("eldercare.datasource.retention.password", db::retentionPassword);
    }

    /** Kết nối bằng tài khoản quản trị, để test thao tác mà tài khoản ứng dụng không có quyền. */
    protected static Connection adminConnection() throws SQLException {
        TestDatabase db = prepareDatabase();
        return DriverManager.getConnection(db.schemaUrl(), db.adminUser(), db.adminPassword());
    }

    /** Kết nối bằng tài khoản {@code eldercare_retention} của job loại bỏ dữ liệu (R-05). */
    protected static Connection retentionConnection() throws SQLException {
        TestDatabase db = prepareDatabase();
        return DriverManager.getConnection(db.schemaUrl(), RETENTION_USER, db.retentionPassword());
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
                    passwordFor("DB_PASSWORD"),
                    passwordFor("DB_RETENTION_PASSWORD"));
        }

        /** Mật khẩu của profile demo nếu có (tài khoản dùng chung, đổi mật khẩu thì demo không đăng nhập được). */
        private static String passwordFor(String name) {
            String value = System.getenv(name);
            return value == null || value.isBlank() ? randomPassword() : value;
        }

        String schemaUrl() {
            return serverUrl + schema + (query == null ? "" : "?" + query);
        }

        void recreate() {
            try (Connection c = DriverManager.getConnection(serverUrl, adminUser, adminPassword);
                    Statement s = c.createStatement()) {
                s.execute("DROP DATABASE IF EXISTS `" + schema + "`");
                s.execute("CREATE DATABASE `" + schema + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci");
                prepareUser(s, APP_USER, appPassword);
                prepareUser(s, RETENTION_USER, retentionPassword);
            } catch (SQLException e) {
                throw new IllegalStateException("Không dựng được schema test " + schema + ": " + e.getMessage(), e);
            }
        }

        /**
         * Tài khoản MySQL dùng chung cả máy (schema {@code elder_care} của profile demo cũng dùng): không xóa tài khoản,
         * chỉ gỡ quyền trên schema test để Flyway cấp lại từ đầu; quyền trên schema khác giữ nguyên.
         */
        private void prepareUser(Statement s, String user, String password) throws SQLException {
            String account = "'" + user + "'@'%'";
            String identified = " IDENTIFIED BY '" + password.replace("'", "''") + "'";
            s.execute("CREATE USER IF NOT EXISTS " + account + identified);
            s.execute("ALTER USER " + account + identified);
            Pattern onSchema = Pattern.compile(" ON `" + Pattern.quote(schema) + "`\\.(\\*|`[^`]+`) TO ");
            List<String> targets = new ArrayList<>();
            try (ResultSet grants = s.executeQuery("SHOW GRANTS FOR " + account)) {
                while (grants.next()) {
                    Matcher m = onSchema.matcher(grants.getString(1));
                    if (m.find()) {
                        targets.add(m.group(1));
                    }
                }
            }
            for (String target : targets) {
                s.execute("REVOKE ALL PRIVILEGES ON `" + schema + "`." + target + " FROM " + account);
            }
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
