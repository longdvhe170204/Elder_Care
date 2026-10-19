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
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/** FR-029, FR-030 (000 T052): view giá trị hiện hành và view bản ghi đã hủy cho báo cáo của module JOIN. */
@Import(CorrectionFixtures.class)
class CorrectionViewIT extends MySqlIntegrationTestBase {

    private static final String TYPE = SampleFloorRecordProvider.TYPE;

    @Autowired
    CorrectionApi corrections;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    private long record() {
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note) VALUES (UTC_TIMESTAMP(6), ?, 2, 'gốc')",
                NURSE_A.accountId());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    @Test
    void FR_030_latestCorrectionView_returnsHighestSequenceAmend() {
        user.set(NURSE_A);
        long id = record();
        Reason reason = new Reason("sửa");
        corrections.create(TYPE, id, CorrectionType.AMEND_CONTENT, Map.of("note", "một"), reason);
        corrections.create(TYPE, id, CorrectionType.AMEND_CONTENT, Map.of("note", "hai"), reason);

        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(corrected_content, '$.note')) FROM v_latest_correction"
                                + " WHERE record_type = ? AND original_record_id = ?",
                        String.class,
                        TYPE,
                        id))
                .isEqualTo("hai");
    }

    @Test
    void FR_029_voidedRecordView_hasVoidTime() {
        user.set(NURSE_A);
        long id = record();
        corrections.create(TYPE, id, CorrectionType.VOID, null, new Reason("ghi cho sai người"));

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM v_voided_record WHERE record_type = ? AND original_record_id = ? AND voided_at IS NOT NULL",
                        Integer.class,
                        TYPE,
                        id))
                .isEqualTo(1);
    }
}
