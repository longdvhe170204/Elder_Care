package com.cambae.backend.m15.correction;

import static com.cambae.backend.m15.sample.CorrectionFixtures.FLOOR_MANAGER;
import static com.cambae.backend.m15.sample.CorrectionFixtures.NURSE_A;
import static com.cambae.backend.m15.sample.CorrectionFixtures.NURSE_B;
import static com.cambae.backend.m15.sample.CorrectionFixtures.OFFICE_A;
import static com.cambae.backend.m15.sample.CorrectionFixtures.SHIFT_LEAD;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.ReasonRequiredException;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionChainView;
import com.cambae.backend.m15.correction.api.CorrectionResult;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CreationMethod;
import com.cambae.backend.m15.sample.CorrectionFixtures;
import com.cambae.backend.m15.sample.SampleFloorRecordProvider;
import com.cambae.backend.m15.sample.SampleOfficeRecordProvider;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import com.cambae.backend.support.SwitchableUser;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
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

    @Test
    void US2_2_missingReason_isRejected() {
        long id = floorRecord(NURSE_A.accountId(), "x");

        assertThatThrownBy(() ->
                        corrections.create(FLOOR_RECORD, id, CorrectionType.AMEND_CONTENT, Map.of("note", "y"), null))
                .isInstanceOf(ReasonRequiredException.class);
    }

    @Test
    void US2_3_sameRoleColleague_notLead_isRefused_butShiftLeadUsesB() {
        long id = floorRecord(NURSE_A.accountId(), "x");

        assertThatThrownBy(() -> amend(NURSE_B, FLOOR_RECORD, id, "y"))
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("CORRECTION_NOT_ALLOWED"));
        assertThat(amend(SHIFT_LEAD, FLOOR_RECORD, id, "y"))
                .isInstanceOfSatisfying(
                        CorrectionResult.Saved.class,
                        saved -> assertThat(saved.correction().creationMethod()).isEqualTo(CreationMethod.B));
    }

    @Test
    void US2_4_floorManager_cannotCorrectUnflooredRecord() {
        long id = officeRecord(OFFICE_A.accountId(), "giao đồ gửi");

        assertThatThrownBy(() -> amend(FLOOR_MANAGER, OFFICE_RECORD, id, "sửa"))
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("CORRECTION_NOT_ALLOWED"));
    }

    @Test
    void US2_5_6_secondCorrection_pointsToOriginal_andBecomesCurrent() {
        long id = floorRecord(NURSE_A.accountId(), "gốc");
        amend(NURSE_A, FLOOR_RECORD, id, "lần 1");
        amend(NURSE_A, FLOOR_RECORD, id, "lần 2");

        CorrectionChainView chain = corrections.chain(FLOOR_RECORD, id);

        assertThat(chain.corrected()).isTrue();
        assertThat(chain.originalContent()).isEqualTo(Map.of("note", "gốc"));
        assertThat(chain.currentContent()).isEqualTo(Map.of("note", "lần 2"));
        assertThat(chain.corrections()).extracting(c -> c.sequenceNo()).containsExactly(1, 2);
        assertThat(jdbc.queryForList(
                        "SELECT DISTINCT original_record_id FROM correction WHERE record_type = ? AND original_record_id = ?",
                        Long.class,
                        FLOOR_RECORD,
                        id))
                .containsExactly(id);
        assertThat(corrections.current(FLOOR_RECORD, List.of(id)).get(id).content())
                .isEqualTo(Map.of("note", "lần 2"));
    }

    @Test
    void US2_7_void_thenAmend_isRejected() {
        long id = floorRecord(NURSE_A.accountId(), "ghi nhầm người");
        user.set(NURSE_A);
        corrections.create(FLOOR_RECORD, id, CorrectionType.VOID, null, REASON);

        assertThat(corrections.current(FLOOR_RECORD, List.of(id)).get(id).voided())
                .isTrue();
        assertThatThrownBy(() -> amend(NURSE_A, FLOOR_RECORD, id, "sửa"))
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("RECORD_VOIDED"));
    }

    @Test
    void US2_8_missingOriginal_or_unsupportedType_isRejected() {
        assertThatThrownBy(() -> amend(NURSE_A, FLOOR_RECORD, 999_999_999L, "x"))
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("RECORD_NOT_CONFIRMED"));
        assertThatThrownBy(() -> amend(NURSE_A, "M11.CLOSED_COST", 1, "x"))
                .satisfies(e -> assertThat(errorCode(e)).isEqualTo("CORRECTION_NOT_SUPPORTED"));
    }

    @Test
    void FR_024_invalidContent_isRejected() {
        long id = floorRecord(NURSE_A.accountId(), "x");
        user.set(NURSE_A);

        assertThatThrownBy(() ->
                        corrections.create(FLOOR_RECORD, id, CorrectionType.AMEND_CONTENT, Map.of("other", 1), REASON))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void T059_httpCreateThenReadChain() throws Exception {
        long id = floorRecord(NURSE_A.accountId(), "gốc");
        user.set(NURSE_A);

        mvc.perform(
                        post("/api/v1/corrections/" + FLOOR_RECORD + "/" + id)
                                .with(csrf())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"note\":\"đúng\"},\"reason\":\"ghi nhầm\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sequenceNo").value(1))
                .andExpect(jsonPath("$.creationMethod").value("A"))
                .andExpect(jsonPath("$.actor.id").value(NURSE_A.accountId()));
        mvc.perform(get("/api/v1/corrections/" + FLOOR_RECORD + "/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.corrected").value(true))
                .andExpect(jsonPath("$.currentContent.note").value("đúng"))
                .andExpect(jsonPath("$.corrections[0].reason").value("ghi nhầm"));
        mvc.perform(post("/api/v1/corrections/" + FLOOR_RECORD + "/" + id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"AMEND_CONTENT\",\"correctedContent\":{\"note\":\"x\"},\"reason\":\" \"}"))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.errorCode").value("REASON_REQUIRED"));
    }
}
