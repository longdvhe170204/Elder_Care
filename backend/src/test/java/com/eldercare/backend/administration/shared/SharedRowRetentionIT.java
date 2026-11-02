package com.eldercare.backend.administration.shared;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * {@link SharedRowRetention} xóa dòng bảng dùng chung của bản ghi, của người cao tuổi bằng tài khoản
 * {@code eldercare_retention} (research R-15 mục 2, R-11; 000 T108 phần 2).
 */
class SharedRowRetentionIT extends MySqlIntegrationTestBase {

    @Autowired
    SharedRowRetention retention;

    @Autowired
    RetentionDatabase database;

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void deleteForResidents_removesSimpleRecordsAndModuleState_ofThoseResidentsOnly() {
        long gone = TestUsers.nextId();
        long kept = TestUsers.nextId();
        for (long resident : List.of(gone, kept)) {
            jdbc.update(
                    "INSERT INTO simple_record (kind, module, resident_id, status, content, created_at, created_by,"
                            + " version, locked_at) VALUES ('M15.TEST_RECORD', 'M15', ?, 'DONE', '{}', UTC_TIMESTAMP(6), 1, 0,"
                            + " UTC_TIMESTAMP(6))",
                    resident);
            jdbc.update(
                    "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                            + " VALUES (?, 'M15', '{}', 0, UTC_TIMESTAMP(6))",
                    resident);
        }

        database.transaction().executeWithoutResult(s -> retention.deleteForResidents(List.of(gone)));

        assertThat(rowsOf(gone)).isZero();
        assertThat(rowsOf(kept)).isEqualTo(2);
    }

    @Test
    void deleteFor_residentEntity_alsoRemovesRowsByResident() {
        long resident = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO resident_module_state (resident_id, module, data, version, updated_at)"
                        + " VALUES (?, 'M15', '{}', 0, UTC_TIMESTAMP(6))",
                resident);

        database.transaction()
                .executeWithoutResult(
                        s -> retention.deleteFor(List.of(new EntityRef(SharedRowRetention.RESIDENT, resident))));

        assertThat(rowsOf(resident)).isZero();
    }

    private int rowsOf(long resident) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM simple_record WHERE resident_id = ?", Integer.class, resident)
                + jdbc.queryForObject(
                        "SELECT COUNT(*) FROM resident_module_state WHERE resident_id = ?", Integer.class, resident);
    }
}
