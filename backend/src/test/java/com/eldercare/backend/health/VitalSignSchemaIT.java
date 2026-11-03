package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

/** Chứng minh khóa theo dòng vẫn giữ ràng buộc sau khi gộp thành phần vào JSON. */
class VitalSignSchemaIT extends MySqlIntegrationTestBase {
    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    TransactionTemplate transactions;

    @Test
    void FR_009_draftConfirmsOnceAndOnlyRetentionDeletes() throws Exception {
        long id = draft();
        try (Connection admin = adminConnection()) {
            assertThatThrownBy(() ->
                            execute(admin, "UPDATE vital_sign_record SET components=JSON_OBJECT() WHERE id=" + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("immutable");
            assertThatThrownBy(() -> execute(
                            admin,
                            "UPDATE vital_sign_record SET confirmation_status='CONFIRMED',confirmed_at=created_at,locked_at=created_at,labels='IMMEDIATE_ACTION' WHERE id="
                                    + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("immutable");
            jdbc.update(
                    "UPDATE vital_sign_record SET confirmation_status='CONFIRMED',confirmed_at=created_at,locked_at=created_at,labels='NOT_REMEASURED' WHERE id=?",
                    id);
            assertThatThrownBy(() -> execute(admin, "UPDATE vital_sign_record SET note='đổi' WHERE id=" + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("immutable");
            assertThatThrownBy(() -> execute(admin, "DELETE FROM vital_sign_record WHERE id=" + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("retention");
            assertThat(jdbc.queryForObject(
                            "SELECT TABLE_COMMENT FROM information_schema.TABLES WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='vital_sign_record'",
                            String.class))
                    .isEqualTo("DataGroup=MULTIPLE");
        } finally {
            try (Connection retention = retentionConnection()) {
                execute(retention, "DELETE FROM vital_sign_record WHERE id=" + id);
            }
        }
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM vital_sign_record WHERE id=?", Long.class, id))
                .isZero();
    }

    @Test
    void FR_012_futureMeasurementAndIncompleteLockAreRejected() throws Exception {
        long id = draft();
        try (Connection admin = adminConnection()) {
            assertThatThrownBy(() -> execute(
                            admin,
                            "UPDATE vital_sign_record SET confirmation_status='CONFIRMED',confirmed_at=created_at WHERE id="
                                    + id))
                    .isInstanceOf(SQLException.class);
            assertThatThrownBy(() -> execute(
                            admin,
                            "INSERT INTO vital_sign_record SELECT " + (id + 1)
                                    + ",resident_id,indicator_id,DATE_ADD(created_at,INTERVAL 1 SECOND),measured_by,task_id,device_measured_at,offline,threshold_version_id,level,confirmation_status,remeasurement_deadline,remeasurement_for,components,labels,note,confirmed_at,locked_at,created_at,created_by,synced_after_permission_loss FROM vital_sign_record WHERE id="
                                    + id))
                    .isInstanceOf(SQLException.class)
                    .hasMessageContaining("chk_vital_sign_measured_at");
        } finally {
            try (Connection retention = retentionConnection()) {
                execute(retention, "DELETE FROM vital_sign_record WHERE id=" + id);
            }
        }
    }

    @Test
    void DBR_23_jsonRetainsDecimalPrecisionAndComponentLevelConstraints() throws Exception {
        long id = draft();
        try (Connection admin = adminConnection()) {
            for (String components : new String[] {
                "{}",
                "{\"VALUE\":{\"value\":1.001,\"level\":\"NORMAL\"}}",
                "{\"VALUE\":{\"value\":100000,\"level\":\"NORMAL\"}}",
                "{\"VALUE\":{\"value\":1,\"level\":\"INVALID\"}}",
                "{\"VALUE\":{\"value\":1}}"
            }) {
                long rejectedId = TestUsers.nextId();
                assertThatThrownBy(() -> execute(
                                admin,
                                "INSERT INTO vital_sign_record SELECT " + rejectedId
                                        + ",resident_id,indicator_id,measured_at,measured_by,task_id,device_measured_at,offline,threshold_version_id,level,confirmation_status,remeasurement_deadline,remeasurement_for,CAST('"
                                        + components
                                        + "' AS JSON),labels,note,confirmed_at,locked_at,created_at,created_by,synced_after_permission_loss FROM vital_sign_record WHERE id="
                                        + id))
                        .isInstanceOf(SQLException.class)
                        .hasMessageContaining("chk_vital_sign_components");
            }
        } finally {
            try (Connection retention = retentionConnection()) {
                execute(retention, "DELETE FROM vital_sign_record WHERE id=" + id);
            }
        }
    }

    private long draft() {
        long id = TestUsers.nextId();
        long indicator = transactions.execute(status -> catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SCHEMA_" + id,
                                "Chỉ số kiểm thử lược đồ " + id,
                                null,
                                0,
                                Map.of("unit", "kiểm thử", "vitalSign", true)))
                .id());
        jdbc.update("""
                INSERT INTO vital_sign_record(id,resident_id,indicator_id,measured_at,measured_by,level,
                  confirmation_status,components,labels,created_at,created_by)
                VALUES(?,?,?,'2026-10-30 08:00:00',1,'DANGEROUS','AWAITING_REMEASUREMENT',
                  '{"VALUE":{"value":80.25,"level":"DANGEROUS"}}','','2026-10-30 08:01:00',1)
                """, id, TestUsers.nextId(), indicator);
        return id;
    }

    private void execute(Connection connection, String sql) throws SQLException {
        try (var statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }
}
