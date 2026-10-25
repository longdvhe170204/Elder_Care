package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 004 T007 (data-model mục 6; FR-034, FR-035, DBR-08, Q-297): danh mục dịch vụ và phiên bản đơn giá. Cơ sở dữ liệu giữ
 * các bảo đảm: mỗi mức một dịch vụ chăm sóc Hiệu lực, tên Hiệu lực không trùng, phiên bản không chồng khoảng, phiên bản
 * đã được tham chiếu không sửa giá và ngày bắt đầu.
 */
class ServiceCatalogSchemaIT extends MySqlIntegrationTestBase {

    @Autowired
    JdbcTemplate jdbc;

    /** Các test dùng chung schema nên bỏ dịch vụ chăm sóc Hiệu lực còn lại từ test khác (Q-297). */
    @BeforeEach
    void deactivateCareServices() {
        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE type = 'CARE'");
    }

    private long service(String type, String name, String careLevel) {
        jdbc.update(
                "INSERT INTO service (type, name, unit, billing_period, care_level_code) VALUES (?, ?, 'tháng', 'MONTH', ?)",
                type,
                name,
                careLevel);
        return jdbc.queryForObject("SELECT id FROM service WHERE name = ? ORDER BY id DESC LIMIT 1", Long.class, name);
    }

    private long otherService() {
        return service("MEAL", "Suất ăn " + UUID.randomUUID(), null);
    }

    private void version(long serviceId, int price, String from, String to) {
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from, effective_to) VALUES (?, ?, ?, ?)",
                serviceId,
                price,
                from,
                to);
    }

    @Test
    void Q_297_careService_needsLevel_andOnlyOneActivePerLevel() {
        assertThatThrownBy(() -> service("CARE", "Chăm sóc không mức " + UUID.randomUUID(), null))
                .isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> service("MEAL", "Suất ăn có mức " + UUID.randomUUID(), "BASIC"))
                .isInstanceOf(DataAccessException.class);

        long first = service("CARE", "Chăm sóc cơ bản " + UUID.randomUUID(), "BASIC");
        assertThatThrownBy(() -> service("CARE", "Chăm sóc cơ bản khác " + UUID.randomUUID(), "BASIC"))
                .isInstanceOf(DuplicateKeyException.class);

        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE id = ?", first);
        service("CARE", "Chăm sóc cơ bản mới " + UUID.randomUUID(), "BASIC");
    }

    @Test
    void FR_034_activeNamesAreUnique_inactiveNameCanBeReused() {
        String name = "Phục hồi " + UUID.randomUUID();
        long first = service("REHABILITATION", name, null);

        assertThatThrownBy(() -> service("ACTIVITY", name, null)).isInstanceOf(DuplicateKeyException.class);

        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE id = ?", first);
        service("ACTIVITY", name, null);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM service WHERE name = ?", Long.class, name))
                .isEqualTo(2);
    }

    @Test
    void FR_035_priceVersion_rejectsNegativePriceAndReversedRange() {
        long id = otherService();

        assertThatThrownBy(() -> version(id, -1, "2030-01-01", null)).isInstanceOf(DataAccessException.class);
        assertThatThrownBy(() -> version(id, 100, "2030-02-01", "2030-01-31")).isInstanceOf(DataAccessException.class);
    }

    @Test
    void DBR_08_priceVersions_ofOneService_neverOverlap() {
        long id = otherService();
        version(id, 100, "2030-01-01", "2030-06-30");

        assertThatThrownBy(() -> version(id, 120, "2030-06-30", null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("DBR-08");
        assertThatThrownBy(() -> version(id, 120, "2029-12-01", "2030-01-01"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("DBR-08");
        version(id, 120, "2030-07-01", null);
        assertThatThrownBy(() -> version(id, 130, "2031-01-01", null))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("DBR-08");
        // Dịch vụ khác có cùng khoảng thì không chồng.
        version(otherService(), 90, "2030-01-01", null);
    }

    @Test
    void DBR_08_closingOpenVersion_isNotAnOverlapWithItself() {
        long id = otherService();
        version(id, 100, "2030-01-01", null);

        jdbc.update("UPDATE price_version SET effective_to = '2030-06-30' WHERE service_id = ?", id);
        version(id, 120, "2030-07-01", null);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM price_version WHERE service_id = ?", Long.class, id))
                .isEqualTo(2);
    }

    @Test
    void DBR_08_referencedVersion_keepsPriceAndStart_butMayBeClosedOnce() throws SQLException {
        long id = otherService();
        version(id, 100, "2030-01-01", null);
        jdbc.update("UPDATE price_version SET referenced = TRUE WHERE service_id = ?", id);

        try (Connection admin = adminConnection();
                Statement statement = admin.createStatement()) {
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE price_version SET unit_price = 999 WHERE service_id = " + id))
                    .hasMessageContaining("DBR-08");
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE price_version SET effective_from = '2030-01-02' WHERE service_id = " + id))
                    .hasMessageContaining("DBR-08");
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE price_version SET referenced = FALSE WHERE service_id = " + id))
                    .hasMessageContaining("DBR-08");

            assertThat(statement.executeUpdate(
                            "UPDATE price_version SET effective_to = '2030-06-30' WHERE service_id = " + id))
                    .isEqualTo(1);
            assertThatThrownBy(() -> statement.executeUpdate(
                            "UPDATE price_version SET effective_to = '2030-07-31' WHERE service_id = " + id))
                    .hasMessageContaining("DBR-08");
        }
    }

    @Test
    void FR_035_appAccount_canOnlyCloseOrMarkReferenced_notEditPrice() {
        long id = otherService();
        version(id, 100, "2030-01-01", null);

        assertThatThrownBy(() -> jdbc.update("UPDATE price_version SET unit_price = 1 WHERE service_id = ?", id))
                .isInstanceOf(DataAccessException.class);
        jdbc.update("UPDATE price_version SET referenced = TRUE WHERE service_id = ?", id);
        jdbc.update("UPDATE price_version SET effective_to = '2030-12-31' WHERE service_id = ?", id);
    }
}
