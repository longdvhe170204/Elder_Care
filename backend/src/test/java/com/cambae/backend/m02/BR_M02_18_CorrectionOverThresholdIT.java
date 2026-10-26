package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m02.api.events.BedHoldDecisionNeeded;
import com.cambae.backend.m02.api.events.ResidencyDaysChanged;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T049 (BR-M02-18, FR-061; research T-08): lượt vắng đã đóng không bị sửa trực tiếp (trigger); thời điểm về, rời đi
 * đính chính qua {@code M02.ABSENCE}; đính chính làm số ngày vượt ngưỡng thì các ngày mới được tra lại, dòng "Quản lý quyết
 * định" thì Quản lý viện được báo để đặt hệ số bằng COEFFICIENT_ONLY (không còn giường để giữ), 010 được báo, nhật ký tính lại.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_18_CorrectionOverThresholdIT extends AbsenceTestBase {

    private static BigDecimal pct(Map<String, Object> day) {
        return (BigDecimal) day.get("coefficient");
    }

    /** Lượt Bệnh viện đóng ở ngày vắng thứ 30 (hệ số 70%, chưa tới ngưỡng). */
    private long closedAtDay30(long residentId) throws Exception {
        clockAt(d0, 9, 0);
        long absenceId = transferOut(residentId);
        clockAt(d0.plusDays(29), 15, 0);
        recordReturn(residentId, null);
        return absenceId;
    }

    private org.springframework.test.web.servlet.ResultActions correct(long absenceId, String returnedAt)
            throws Exception {
        return postAs(office, "/api/v1/absences/" + absenceId + "/correct", """
                {"type":"EDIT_CONTENT","correctedContent":{"returnedAt":"%s"},"reason":"Ghi nhầm ngày về"}""".formatted(returnedAt));
    }

    @Test
    void FR_061_aClosedAbsence_cannotBeEditedDirectly() throws Exception {
        long absenceId = closedAtDay30(inResidence());

        assertThatThrownBy(() -> jdbc.update("UPDATE absence SET reason = 'sửa' WHERE id = ?", absenceId))
                .hasMessageContaining("closed absence");
    }

    @Test
    void BR_M02_18_correctingTheReturnPastTheThreshold_marksTheNewDaysPending_andNotifiesTheManager() throws Exception {
        long residentId = inResidence();
        long absenceId = closedAtDay30(residentId);
        assertThat(days(absenceId)).hasSize(30);
        Instant newReturn = at(d0.plusDays(31), 15, 0);

        correct(absenceId, newReturn.toString()).andExpect(status().isCreated());

        List<Map<String, Object>> days = days(absenceId);
        assertThat(days).hasSize(32);
        for (int i = 30; i < 32; i++) {
            assertThat(pct(days.get(i))).as("ngày " + (i + 1)).isEqualByComparingTo("70");
            assertThat(days.get(i).get("pending_decision_flag")).isEqualTo(true);
        }
        assertThat(pct(days.get(29))).isEqualByComparingTo("70");
        assertThat(decisionNotices(absenceId))
                .extracting(BedHoldDecisionNeeded::days)
                .containsExactly(32);
        assertThat(events.stream(ResidencyDaysChanged.class)
                        .filter(e -> e.residentId() == residentId && "ABSENCE_CORRECTION".equals(e.cause())))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ABSENCE_RECALCULATED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(absenceId)))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT status FROM absence WHERE id = ?", String.class, absenceId))
                .as("lượt vắng vẫn đã đóng")
                .isEqualTo("CLOSED");

        decide(absenceId, """
                {"choice":"COEFFICIENT_ONLY","coefficient":55,"reason":"Chỉ đặt hệ số cho hai ngày"}""").andExpect(status().isCreated());

        days = days(absenceId);
        assertThat(pct(days.get(29))).isEqualByComparingTo("70");
        assertThat(pct(days.get(30))).isEqualByComparingTo("55");
        assertThat(pct(days.get(31))).isEqualByComparingTo("55");
        assertThat(days.get(30).get("pending_decision_flag")).isEqualTo(false);
        assertThat(days.get(31).get("decision_id")).isNotNull();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_hold_decision WHERE absence_id = ?", Integer.class, absenceId))
                .isEqualTo(1);
    }

    @Test
    void BR_M02_18_aClosedAbsence_onlyAcceptsCoefficientOnly_andOnlyWhenDaysArePending() throws Exception {
        long absenceId = closedAtDay30(inResidence());

        decide(absenceId, """
                {"choice":"COEFFICIENT_ONLY","coefficient":55,"reason":"Không có ngày chờ"}""").andExpect(status().isConflict());
        decide(absenceId, """
                {"choice":"RELEASE","coefficient":0,"reason":"Đã đóng"}""").andExpect(status().isConflict());
    }

    @Test
    void FR_061_shorteningTheAbsence_removesTheDaysAfterTheNewReturn() throws Exception {
        long residentId = inResidence();
        long absenceId = closedAtDay30(residentId);

        correct(absenceId, at(d0.plusDays(9), 15, 0).toString()).andExpect(status().isCreated());

        List<Map<String, Object>> days = days(absenceId);
        assertThat(days).hasSize(10);
        assertThat(days.getLast().get("date")).isEqualTo(Date.valueOf(d0.plusDays(9)));
        assertThat(decisionNotices(absenceId)).isEmpty();
    }

    @Test
    void FR_061_correctingTheDepartureToAnEarlierDay_shiftsTheSequenceNumbers() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 14, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(2), 10, 0));
        clockAt(d0.plusDays(2), 11, 0);
        recordReturn(residentId, null);
        assertThat(days(absenceId)).hasSize(3);

        postAs(office, "/api/v1/absences/" + absenceId + "/correct", """
                {"type":"EDIT_CONTENT","correctedContent":{"departedAt":"%s"},"reason":"Ghi nhầm ngày rời"}""".formatted(at(d0.minusDays(1), 20, 0)))
                .andExpect(status().isCreated());

        List<Map<String, Object>> days = days(absenceId);
        assertThat(days).hasSize(4);
        assertThat(days.getFirst().get("date")).isEqualTo(Date.valueOf(d0.minusDays(1)));
        assertThat(days.getFirst().get("sequence_no")).isEqualTo(1);
        assertThat(days.getLast().get("sequence_no")).isEqualTo(4);
    }

    @Test
    void FR_061_aReturnBeforeTheDeparture_isRefused() throws Exception {
        long residentId = inResidence();
        long absenceId = closedAtDay30(residentId);

        correct(absenceId, at(d0.minusDays(3), 8, 0).toString()).andExpect(status().isUnprocessableContent());
        assertThat(days(absenceId)).hasSize(30);
    }
}
