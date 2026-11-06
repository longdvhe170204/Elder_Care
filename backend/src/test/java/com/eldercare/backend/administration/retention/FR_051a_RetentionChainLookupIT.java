package com.eldercare.backend.administration.retention;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.administration.retention.scheduling.ExpiredDataPurgeJob;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.administration.sample.SampleOfficeRecordProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * FR-051a, FR-051 (000 T112): job tìm bản ghi trỏ tới một bản ghi bằng cả {@code referencesTo} của loại bị trỏ và
 * {@code referencing} của mọi provider (bản ghi module khác trỏ tới hồ sơ); ứng viên lấy theo trang sau mốc, nên ứng viên
 * đầu danh sách bị giữ không chặn ứng viên sau (cỡ lô đặt 2 cho test).
 */
@Import(RetentionFixtures.class)
@TestPropertySource(properties = "eldercare.retention.batch-size=2")
class FR_051a_RetentionChainLookupIT extends MySqlIntegrationTestBase {

    private static final String FLOOR = SampleFloorRecordProvider.TYPE;
    private static final String OFFICE = SampleOfficeRecordProvider.TYPE;
    private static final CurrentUser NURSE = FakePermissions.user(10, Role.NURSING_STAFF);
    private static final CurrentUser SCHEDULER = FakePermissions.user(0, Role.SCHEDULER);

    @Autowired
    ExpiredDataPurgeJob job;

    @Autowired
    RetentionFixtures.Control control;

    @Autowired
    SwitchableUser user;

    @Autowired
    BusinessTime time;

    @Autowired
    JdbcTemplate jdbc;

    private Instant expired;
    private Instant recent;

    @BeforeEach
    void setUp() {
        control.clear();
        expired = time.now().atZone(BusinessTime.ZONE).minusYears(11).toInstant();
        recent = time.now().minusSeconds(86_400);
        // dọn hết dữ liệu hết hạn còn lại (mỗi đợt chỉ còn giới hạn số trang)
        for (int i = 0; i < 5; i++) {
            runJob();
        }
    }

    @AfterEach
    void tearDown() {
        control.clear();
        user.set(NURSE);
    }

    private void runJob() {
        user.set(SCHEDULER);
        try {
            job.run();
        } finally {
            user.set(NURSE);
        }
    }

    private long record(String table, Instant at) {
        String floor = table.equals("sample_floor_record") ? ", floor_area_id" : "";
        String floorValue = table.equals("sample_floor_record") ? ", 2" : "";
        jdbc.update(
                "INSERT INTO " + table + " (occurred_at, actor_id, note" + floor + ") VALUES (?, ?, 'ghi nhận'"
                        + floorValue + ")",
                LocalDateTime.ofInstant(at, ZoneOffset.UTC),
                NURSE.accountId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private boolean exists(String table, long id) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE id = ?", Integer.class, id) > 0;
    }

    @Test
    void FR_051a_recordOfAnotherTypeReferencingIt_isFoundByReferencing_keepsItWhileUnexpired() {
        long target = record("sample_floor_record", expired);
        long referrer = record("sample_office_record", recent);
        control.crossReference(new DataRef(OFFICE, referrer), new DataRef(FLOOR, target));

        runJob();

        assertThat(exists("sample_floor_record", target)).isTrue();
        assertThat(exists("sample_office_record", referrer)).isTrue();
    }

    @Test
    void FR_051a_bothExpired_viaReferencing_arePurgedTogether() {
        long target = record("sample_floor_record", expired);
        long referrer = record("sample_office_record", expired);
        control.crossReference(new DataRef(OFFICE, referrer), new DataRef(FLOOR, target));

        runJob();

        assertThat(exists("sample_floor_record", target)).isFalse();
        assertThat(exists("sample_office_record", referrer)).isFalse();
    }

    @Test
    void FR_051_keptCandidatesAtTheHead_doNotStarveLaterCandidates() {
        Instant older = expired.minusSeconds(3600);
        long keptA = record("sample_floor_record", older);
        long keptB = record("sample_floor_record", older.plusSeconds(1));
        long recentReferrer = record("sample_office_record", recent);
        control.reference(new DataRef(OFFICE, recentReferrer), new DataRef(FLOOR, keptA));
        control.reference(new DataRef(OFFICE, recentReferrer), new DataRef(FLOOR, keptB));
        long later = record("sample_floor_record", expired);

        runJob();

        assertThat(exists("sample_floor_record", keptA)).isTrue();
        assertThat(exists("sample_floor_record", keptB)).isTrue();
        assertThat(exists("sample_floor_record", later)).isFalse();
    }
}
