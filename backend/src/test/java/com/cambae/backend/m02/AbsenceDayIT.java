package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m02.api.events.AbsenceClosed;
import com.cambae.backend.m02.api.events.AbsenceOpened;
import com.cambae.backend.m02.api.events.NoAbsencePolicyLine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.node.ObjectNode;

/**
 * 004 T049 (BR-M02-06, FR-048, FR-052, FR-053, FR-058, FR-059; research T-08): mở lượt qua lệnh Cho tạm vắng của 001, job
 * {@code ABSENCE_DAYS} ghi mỗi ngày dương lịch một dòng với hệ số tra từ chính sách (cả ngày rời và ngày về), dòng của hợp
 * đồng thắng bảng cơ sở, bảng đổi không làm đổi ngày đã tính, thiếu dòng thì 100% kèm dấu và cảnh báo Quản lý viện.
 */
@AutoConfigureMockMvc
@WithMockUser
class AbsenceDayIT extends AbsenceTestBase {

    @AfterEach
    void restore() throws Exception {
        restoreAbsenceTable();
    }

    private static BigDecimal pct(Map<String, Object> day) {
        return (BigDecimal) day.get("coefficient");
    }

    @Test
    void FR_048_grantTemporaryAbsence_opensAnAbsence_withThePickupRecordUsed() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);

        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(4), 17, 0));

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM absence WHERE id = ?", absenceId);
        assertThat(row.get("status")).isEqualTo("OPEN");
        assertThat(row.get("absence_type")).isEqualTo("HOME_VISIT");
        assertThat(row.get("initial_bed_hold")).isEqualTo(true);
        assertThat(row.get("takes_medication")).isEqualTo(false);
        assertThat(((Number) row.get("picked_up_by_id")).longValue()).isEqualTo(PICKER);
        assertThat(pickups.isUsed(((Number) row.get("pickup_record_id")).longValue()))
                .isTrue();
        assertThat(events.stream(AbsenceOpened.class).filter(e -> e.absenceId() == absenceId))
                .hasSize(1);
        assertThat(days(absenceId))
                .as("ngày rời viện là ngày vắng thứ nhất (FR-058)")
                .hasSize(1);
        assertThat(days(absenceId).getFirst().get("sequence_no")).isEqualTo(1);
    }

    @Test
    void FR_048_withoutAValidPickupRecord_theCommandIsRefused() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        ObjectNode details = grantDetails(residentId, "HOME_VISIT", at(d0.plusDays(1), 10, 0));
        pickups.reset();

        var failure = failure(() -> statusService.grantTemporaryAbsence(
                com.cambae.backend.m01.internal.status.service.ResidentStatusService.target(residentId),
                new com.cambae.backend.common.Reason("Thử"),
                details));

        assertThat(failure).isNotNull();
        assertThat(unmetCodes(failure)).containsExactly("ABS_PICKUP_RECORD");
        assertThat(currentStatus(residentId).name()).isEqualTo("IN_RESIDENCE");
    }

    @Test
    void BR_M02_06_homeForFiveDays_hasFiveDays_at100Percent_includingTheReturnDay() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(4), 17, 0));

        runJobOn(d0.plusDays(2));
        assertThat(days(absenceId)).hasSize(3);

        clockAt(d0.plusDays(4), 16, 0);
        recordReturn(residentId, null);

        List<Map<String, Object>> days = days(absenceId);
        assertThat(days).hasSize(5);
        for (int i = 0; i < 5; i++) {
            assertThat(days.get(i).get("sequence_no")).isEqualTo(i + 1);
            assertThat(pct(days.get(i))).isEqualByComparingTo("100");
            assertThat(days.get(i).get("hold_bed")).isEqualTo(true);
            assertThat(days.get(i).get("policy_line_source")).isEqualTo("FACILITY");
        }
        assertThat(days.getLast().get("date"))
                .as("ngày về vẫn là ngày vắng (FR-058)")
                .isEqualTo(java.sql.Date.valueOf(d0.plusDays(4)));
        assertThat(jdbc.queryForObject("SELECT status FROM absence WHERE id = ?", String.class, absenceId))
                .isEqualTo("CLOSED");
        assertThat(events.stream(AbsenceClosed.class).filter(e -> e.absenceId() == absenceId))
                .hasSize(1);
    }

    @Test
    void BR_M02_06_hospitalForThirtyFiveDays_followsTheThreeRanges() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 9, 0);
        long absenceId = transferOut(residentId);

        runJobOn(d0.plusDays(34));

        List<Map<String, Object>> days = days(absenceId);
        assertThat(days).hasSize(35);
        assertThat(pct(days.get(0))).isEqualByComparingTo("100");
        assertThat(pct(days.get(6))).as("ngày 7").isEqualByComparingTo("100");
        assertThat(pct(days.get(7))).as("ngày 8").isEqualByComparingTo("70");
        assertThat(pct(days.get(29))).as("ngày 30").isEqualByComparingTo("70");
        for (int i = 30; i < 35; i++) {
            assertThat(pct(days.get(i)))
                    .as("ngày " + (i + 1) + " dùng hệ số dòng liền trước")
                    .isEqualByComparingTo("70");
            assertThat(days.get(i).get("pending_decision_flag")).isEqualTo(true);
            assertThat(days.get(i).get("hold_bed"))
                    .as("chưa có quyết định thì giường vẫn giữ")
                    .isEqualTo(true);
        }
        assertThat(days.get(29).get("pending_decision_flag")).isEqualTo(false);
    }

    @Test
    void BR_M02_06_aDifferentTypeWithoutALine_gets100Percent_withTheFlag_andTheManagerIsWarned() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);

        long absenceId = grant(residentId, "SAME_DAY_MEDICAL_VISIT", at(d0, 12, 0));

        Map<String, Object> day = days(absenceId).getFirst();
        assertThat(pct(day)).isEqualByComparingTo("100");
        assertThat(day.get("hold_bed")).isEqualTo(true);
        assertThat(day.get("no_policy_line_flag")).isEqualTo(true);
        assertThat(day.get("policy_line_source")).isEqualTo("DEFAULT");
        assertThat(events.stream(NoAbsencePolicyLine.class).filter(e -> e.absenceId() == absenceId))
                .hasSize(1);
    }

    @Test
    void FR_051_aShortTermContractOverride_replacesTheFacilityTable() throws Exception {
        long residentId = residentWithRepresentative();
        ObjectNode body = body(residentId);
        body.put("residenceType", "SHORT_TERM_RESIDENTIAL");
        body.put("endDate", d0.plusDays(60).toString());
        body.putArray("absencePolicyOverrides")
                .addObject()
                .put("residenceType", "SHORT_TERM_RESIDENTIAL")
                .put("absenceType", "ANY")
                .put("fromDay", 1)
                .put("coefficient", "50")
                .put("holdBed", "NO");
        activeContractWithApprovedTerms(residentId, body);
        setStatus(residentId, "IN_RESIDENCE");
        clockAt(d0, 10, 0);

        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(2), 10, 0));

        Map<String, Object> row = jdbc.queryForMap("SELECT initial_bed_hold FROM absence WHERE id = ?", absenceId);
        assertThat(row.get("initial_bed_hold"))
                .as("dòng ghi đè không giữ giường (FR-054)")
                .isEqualTo(false);
        Map<String, Object> day = days(absenceId).getFirst();
        assertThat(pct(day)).isEqualByComparingTo("50");
        assertThat(day.get("policy_line_source")).isEqualTo("CONTRACT");
        assertThat(day.get("hold_bed")).isEqualTo(false);
    }

    @Test
    void FR_052_changingTheTable_doesNotChangeDaysAlreadyCalculated() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(5), 10, 0));
        runJobOn(d0.plusDays(1));
        assertThat(days(absenceId)).hasSize(2);

        setAbsenceTable("""
                [{"residencyType":"LONG_TERM","absenceType":"HOME_OR_OUTING","fromDay":1,"toDay":null,"feeRatePercent":80,"holdBed":"YES"}]""");
        runJobOn(d0.plusDays(2));

        List<Map<String, Object>> days = days(absenceId);
        assertThat(days).hasSize(3);
        assertThat(pct(days.get(0))).as("ngày đã tính giữ nguyên").isEqualByComparingTo("100");
        assertThat(pct(days.get(1))).isEqualByComparingTo("100");
        assertThat(pct(days.get(2))).as("ngày mới dùng bảng hiện hành").isEqualByComparingTo("80");
    }

    @Test
    void FR_053_aTableWithoutAnyRowForTheType_appliesTheNoPolicyRule() throws Exception {
        long residentId = inResidence();
        setAbsenceTable("""
                [{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":1,"toDay":null,"feeRatePercent":100,"holdBed":"YES"}]""");
        clockAt(d0, 10, 0);

        long absenceId = grant(residentId, "FAMILY_OUTING", at(d0.plusDays(1), 10, 0));
        runJobOn(d0.plusDays(1));

        List<Map<String, Object>> days = days(absenceId);
        assertThat(days).hasSize(2);
        assertThat(days).allSatisfy(day -> {
            assertThat(pct(day)).isEqualByComparingTo("100");
            assertThat(day.get("no_policy_line_flag")).isEqualTo(true);
        });
        assertThat(events.stream(NoAbsencePolicyLine.class).filter(e -> e.absenceId() == absenceId))
                .extracting(NoAbsencePolicyLine::date)
                .containsExactlyInAnyOrder(d0, d0.plusDays(1));
    }

    @Test
    void FR_058_aReturnTheSameDay_isOneAbsenceDay() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 8, 0);
        long absenceId = grant(residentId, "SAME_DAY_MEDICAL_VISIT", at(d0, 11, 0));

        clockAt(d0, 11, 0);
        recordReturn(residentId, null);

        assertThat(days(absenceId)).hasSize(1);
        LocalDate date = ((java.sql.Date) days(absenceId).getFirst().get("date")).toLocalDate();
        assertThat(date).isEqualTo(d0);
    }
}
