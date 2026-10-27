package com.cambae.backend.m15.correction;

import static com.cambae.backend.m15.sample.CorrectionFixtures.NURSE_A;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.sample.CorrectionFixtures;
import com.cambae.backend.m15.sample.SampleFloorRecordProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import com.cambae.backend.support.TestUsers;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * FR-026, FR-049 (000 contracts/module-interfaces.md mục 1.6): provider cho biết người cao tuổi của bản gốc thì dòng
 * nhật ký {@code CORRECTION_ADDED} mang {@code resident_id} để tra nhật ký theo người cao tuổi; provider không khai
 * báo thì để rỗng như trước.
 */
@WithMockUser
@Import({CorrectionFixtures.class, FR_026_CorrectionResidentIT.ResidentLinked.class})
class FR_026_CorrectionResidentIT extends MySqlIntegrationTestBase {

    static final String RESIDENT_RECORD = "M15.SAMPLE_RESIDENT_RECORD";
    static final long RESIDENT = TestUsers.nextId();

    /** Bản ghi mẫu gắn tầng như {@code M15.SAMPLE_RECORD}, thêm người cao tuổi. */
    @TestConfiguration
    static class ResidentLinked {
        @Bean
        SampleFloorRecordProvider residentLinkedRecords(JdbcTemplate jdbc) {
            return new SampleFloorRecordProvider(jdbc) {
                @Override
                public String recordType() {
                    return RESIDENT_RECORD;
                }

                @Override
                public Optional<Long> residentId(long id) {
                    return Optional.of(RESIDENT);
                }
            };
        }
    }

    @Autowired
    CorrectionApi corrections;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    private long record() {
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note)"
                        + " VALUES (UTC_TIMESTAMP(6), ?, ?, 'HA 160/90')",
                NURSE_A.accountId(),
                CorrectionFixtures.FLOOR);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private Long loggedResident(String type, long id) {
        return jdbc.queryForObject(
                "SELECT resident_id FROM audit_log WHERE action = 'CORRECTION_ADDED' AND entity_type = ? AND entity_id = ?",
                Long.class,
                type,
                String.valueOf(id));
    }

    @Test
    void FR_026_correctionLog_carriesResidentFromProvider() {
        long id = record();
        user.set(NURSE_A);

        corrections.create(
                RESIDENT_RECORD, id, CorrectionType.AMEND_CONTENT, Map.of("note", "HA 130/90"), new Reason("ghi nhầm"));

        assertThat(loggedResident(RESIDENT_RECORD, id)).isEqualTo(RESIDENT);
    }

    @Test
    void FR_026_providerWithoutResident_logsNoResident() {
        long id = record();
        user.set(NURSE_A);

        corrections.create(
                SampleFloorRecordProvider.TYPE,
                id,
                CorrectionType.AMEND_CONTENT,
                Map.of("note", "HA 130/90"),
                new Reason("ghi nhầm"));

        assertThat(loggedResident(SampleFloorRecordProvider.TYPE, id)).isNull();
    }
}
