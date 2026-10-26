package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.m02.api.events.BedHoldDecisionNeeded;
import com.cambae.backend.m02.api.events.BedHoldReviewDue;
import com.cambae.backend.m02.api.events.ResidencyDaysChanged;
import com.cambae.backend.support.TestUsers.TestUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T049 (BR-M02-07, BR-M02-17, FR-055, FR-056; Q-307, Q-345): tới ngưỡng giữ giường thì chỉ báo Quản lý viện một lần,
 * không tạo yêu cầu; lệnh Quyết định giữ giường chỉ Quản lý viện (Trưởng tầng, Hành chính bị 403); Giữ tiếp, Giải phóng
 * tính lại các ngày "chờ quyết định" và báo 010; ngày 1 là "Quản lý quyết định" thì báo ngay khi mở lượt.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_07_BedHoldDecisionIT extends AbsenceTestBase {

    @AfterEach
    void restore() throws Exception {
        restoreAbsenceTable();
    }

    /** Lượt Bệnh viện đã tới ngày 33: ngưỡng ở ngày 31. */
    private long hospitalPastThreshold(long residentId) throws Exception {
        clockAt(d0, 9, 0);
        long absenceId = transferOut(residentId);
        runJobOn(d0.plusDays(32));
        return absenceId;
    }

    private static BigDecimal pct(Map<String, Object> day) {
        return (BigDecimal) day.get("coefficient");
    }

    @Test
    void BR_M02_07_reachingTheThreshold_notifiesTheManagerOnce_andCreatesNoRequest() throws Exception {
        long residentId = inResidence();
        int requestsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class);

        long absenceId = hospitalPastThreshold(residentId);
        runJobOn(d0.plusDays(33));

        List<BedHoldDecisionNeeded> notices = decisionNotices(absenceId);
        assertThat(notices).hasSize(1);
        assertThat(notices.getFirst().days()).isEqualTo(31);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM approval_request", Integer.class))
                .as("không tạo yêu cầu phê duyệt (Q-307)")
                .isEqualTo(requestsBefore);
        assertThat(jdbc.queryForObject(
                        "SELECT threshold_notified_at IS NOT NULL FROM absence WHERE id = ?", Boolean.class, absenceId))
                .isTrue();
    }

    @Test
    void Q_345_onlyTheManagerMayDecide_floorManagerAndOfficeAre403() throws Exception {
        long residentId = inResidence();
        long absenceId = hospitalPastThreshold(residentId);
        String body = """
                {"choice":"HOLD_CONTINUE","coefficient":60,"reviewDate":"%s","reason":"Gia đình xin giữ"}""".formatted(d0.plusDays(40));
        TestUser floor = users.staff(Role.FLOOR_MANAGER);

        // Hành chính có T ở dòng "Tạm vắng, trở về" nên qua được lớp quyền và bị chặn bởi kiểm vai trò của service
        // (403).
        // Trưởng tầng chưa được phân công tầng của người này nên bị ẩn bởi phạm vi (FR-044a); Trưởng tầng được phân
        // công
        // cũng chỉ tới được kiểm vai trò đó.
        postAs(floor, "/api/v1/absences/" + absenceId + "/bed-hold-decision", body)
                .andExpect(status().is4xxClientError());
        postAs(office, "/api/v1/absences/" + absenceId + "/bed-hold-decision", body)
                .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM bed_hold_decision WHERE absence_id = ?", Integer.class, absenceId))
                .isZero();
    }

    @Test
    void BR_M02_07_holdContinue_recalculatesThePendingDays_andSetsTheReviewDate() throws Exception {
        long residentId = inResidence();
        long absenceId = hospitalPastThreshold(residentId);
        LocalDate review = d0.plusDays(40);

        decide(absenceId, """
                {"choice":"HOLD_CONTINUE","coefficient":60,"reviewDate":"%s","reason":"Gia đình xin giữ"}""".formatted(review))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdBed").value(true))
                .andExpect(jsonPath("$.decisions[0].choice").value("HOLD_CONTINUE"));

        List<Map<String, Object>> days = days(absenceId);
        assertThat(pct(days.get(29))).as("ngày 30 không đổi").isEqualByComparingTo("70");
        for (int i = 30; i < 33; i++) {
            assertThat(pct(days.get(i))).as("ngày " + (i + 1)).isEqualByComparingTo("60");
            assertThat(days.get(i).get("pending_decision_flag")).isEqualTo(false);
            assertThat(days.get(i).get("decision_id")).isNotNull();
            assertThat(days.get(i).get("hold_bed")).isEqualTo(true);
        }
        assertThat(events.stream(ResidencyDaysChanged.class)
                        .filter(e -> e.residentId() == residentId && "BED_HOLD_DECISION".equals(e.cause())))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ABSENCE_DECISION' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(absenceId)))
                .isEqualTo(1);

        runJobOn(review);
        assertThat(events.stream(BedHoldReviewDue.class).filter(e -> e.absenceId() == absenceId))
                .as("tới ngày xem xét lại thì báo lại")
                .hasSize(1);
        assertThat(decisionNotices(absenceId)).as("không báo lại ngưỡng").hasSize(1);
        assertThat(pct(days(absenceId).getLast()))
                .as("chưa có quyết định mới: giữ hệ số đã quyết")
                .isEqualByComparingTo("60");
    }

    @Test
    void BR_M02_07_releaseWith0Percent_stopsHoldingTheBed() throws Exception {
        long residentId = inResidence();
        long absenceId = hospitalPastThreshold(residentId);

        decide(absenceId, """
                {"choice":"RELEASE","coefficient":0,"reason":"Gia đình không giữ nữa"}""")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.holdBed").value(false));

        List<Map<String, Object>> days = days(absenceId);
        for (int i = 30; i < 33; i++) {
            assertThat(pct(days.get(i))).isEqualByComparingTo("0");
            assertThat(days.get(i).get("hold_bed")).isEqualTo(false);
        }
        assertThat(pct(days.get(29))).isEqualByComparingTo("70");

        decide(absenceId, """
                {"choice":"RELEASE","coefficient":10,"reason":"Quyết định lần hai"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("BED_NOT_HELD"));
    }

    @Test
    void FR_055_holdContinueNeedsAReviewDate_andABeforeThresholdDecisionIsRefused() throws Exception {
        long residentId = inResidence();
        long hospital = hospitalPastThreshold(residentId);
        decide(hospital, """
                {"choice":"HOLD_CONTINUE","coefficient":60,"reason":"Thiếu ngày xem xét"}""").andExpect(status().isUnprocessableContent());

        long other = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(other, "HOME_VISIT", at(d0.plusDays(3), 10, 0));
        runJobOn(d0.plusDays(2));
        decide(absenceId, """
                {"choice":"RELEASE","coefficient":0,"reason":"Quá sớm"}""")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("THRESHOLD_NOT_REACHED"));
    }

    @Test
    void BR_M02_17_managerDecidesOnDayOne_notifiesImmediately_with100PercentPendingDays() throws Exception {
        long residentId = inResidence();
        setAbsenceTable("""
                [{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":1,"toDay":null,"feeRatePercent":"MANAGER_DECIDES","holdBed":"MANAGER_DECIDES"}]""");
        clockAt(d0, 9, 0);

        long absenceId = transferOut(residentId);

        assertThat(decisionNotices(absenceId)).hasSize(1);
        assertThat(decisionNotices(absenceId).getFirst().days()).isEqualTo(1);
        Map<String, Object> day = days(absenceId).getFirst();
        assertThat(pct(day)).isEqualByComparingTo("100");
        assertThat(day.get("pending_decision_flag")).isEqualTo(true);
        assertThat(day.get("hold_bed")).as("giường giữ tạm").isEqualTo(true);

        runJobOn(d0.plusDays(2));
        assertThat(decisionNotices(absenceId)).as("không báo lặp").hasSize(1);

        decide(absenceId, """
                {"choice":"HOLD_CONTINUE","coefficient":40,"reviewDate":"%s","reason":"Quyết định ngày 1"}""".formatted(d0.plusDays(10))).andExpect(status().isCreated());
        assertThat(days(absenceId)).allSatisfy(d -> assertThat(pct(d)).isEqualByComparingTo("40"));
    }
}
