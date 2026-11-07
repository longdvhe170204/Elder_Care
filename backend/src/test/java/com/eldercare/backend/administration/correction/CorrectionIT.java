package com.eldercare.backend.administration.correction;

import static com.eldercare.backend.administration.sample.CorrectionFixtures.NURSE_A;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.correction.api.CorrectionApi;
import com.eldercare.backend.administration.correction.api.CorrectionResult;
import com.eldercare.backend.administration.correction.api.dto.CorrectionType;
import com.eldercare.backend.administration.correction.api.dto.CreationMethod;
import com.eldercare.backend.administration.sample.CorrectionFixtures;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.administration.sample.SampleOfficeRecordProvider;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/** US2 #1–8; FR-023 → FR-030, DBR-23 (000 T050, T059; quickstart K14, K17). */
@AutoConfigureMockMvc
@WithMockUser
@Import(CorrectionFixtures.class)
class CorrectionIT extends MySqlIntegrationTestBase {

    private static final String FLOOR_RECORD = SampleFloorRecordProvider.TYPE;
    private static final String OFFICE_RECORD = SampleOfficeRecordProvider.TYPE;
    private static final Reason REASON = new Reason("ghi nhầm chỉ số");

    @Autowired
    CorrectionApi corrections;

    @Autowired
    SwitchableUser user;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    MockMvc mvc;

    @AfterEach
    void resetUser() {
        user.set(NURSE_A);
    }

    private long floorRecord(long actorId, String note) {
        jdbc.update(
                "INSERT INTO sample_floor_record (occurred_at, actor_id, floor_area_id, note)"
                        + " VALUES (UTC_TIMESTAMP(6), ?, ?, ?)",
                actorId,
                CorrectionFixtures.FLOOR,
                note);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private long officeRecord(long actorId, String note) {
        jdbc.update(
                "INSERT INTO sample_office_record (occurred_at, actor_id, note) VALUES (UTC_TIMESTAMP(6), ?, ?)",
                actorId,
                note);
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    private CorrectionResult amend(CurrentUser as, String type, long id, String note) {
        user.set(as);
        return corrections.create(type, id, CorrectionType.AMEND_CONTENT, Map.of("note", note), REASON);
    }

    private static String errorCode(Throwable e) {
        return ((BusinessException) e).errorCode();
    }

    @Test
    void US2_1_recorderCorrects_originalUnchanged_andLogged() {
        long id = floorRecord(NURSE_A.accountId(), "HA 160/90");

        CorrectionResult result = amend(NURSE_A, FLOOR_RECORD, id, "HA 130/90");

        assertThat(result).isInstanceOfSatisfying(CorrectionResult.Saved.class, saved -> {
            assertThat(saved.correction().creationMethod()).isEqualTo(CreationMethod.A);
            assertThat(saved.correction().sequenceNo()).isEqualTo(1);
        });
        assertThat(jdbc.queryForObject("SELECT note FROM sample_floor_record WHERE id = ?", String.class, id))
                .isEqualTo("HA 160/90");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'CORRECTION_ADDED' AND entity_type = ?"
                                + " AND entity_id = ? AND reason = ?",
                        Integer.class,
                        FLOOR_RECORD,
                        String.valueOf(id),
                        REASON.value()))
                .isEqualTo(1);
    }
}
