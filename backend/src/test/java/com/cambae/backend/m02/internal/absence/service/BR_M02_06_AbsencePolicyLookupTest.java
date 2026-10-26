package com.cambae.backend.m02.internal.absence.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.Attendance;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Hold;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Outcome;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Source;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 004 T048 (BR-M02-06, FR-050 → FR-053, FR-060; research T-08): hàm thuần tra chính sách phí khi vắng, phủ bảng ví dụ 6.7:
 * dòng của hợp đồng thắng dòng bảng cơ sở, "Theo hợp đồng" lấy từ hợp đồng, không có dòng thì 100% và giữ giường kèm dấu.
 */
class BR_M02_06_AbsencePolicyLookupTest {

    /** Giá trị mặc định của CFG-M02-05 (V000_04). */
    static final JsonNode TABLE = parse("""
            {"rows":[
             {"residencyType":"LONG_TERM","absenceType":"HOME_OR_OUTING","fromDay":1,"toDay":null,"feeRatePercent":100,"holdBed":"YES"},
             {"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":1,"toDay":7,"feeRatePercent":100,"holdBed":"YES"},
             {"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":8,"toDay":30,"feeRatePercent":70,"holdBed":"YES"},
             {"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":31,"toDay":null,"feeRatePercent":"MANAGER_DECIDES","holdBed":"MANAGER_DECIDES"},
             {"residencyType":"SHORT_TERM","absenceType":"ANY","fromDay":1,"toDay":null,"feeRatePercent":"BY_CONTRACT","holdBed":"BY_CONTRACT"},
             {"residencyType":"DAY_CARE","absenceType":"NOTIFIED_IN_ADVANCE","fromDay":null,"toDay":null,"feeRatePercent":0,"holdBed":null},
             {"residencyType":"DAY_CARE","absenceType":"UNANNOUNCED","fromDay":null,"toDay":null,"feeRatePercent":50,"holdBed":null}]}""");

    static JsonNode parse(String json) {
        return JsonMapper.builder().build().readTree(json);
    }

    private static Outcome longTerm(AbsenceType type, int day, BigDecimal previous) {
        return AbsencePolicyResolver.resolve(
                ResidenceType.LONG_TERM_RESIDENTIAL, type, day, List.of(), TABLE, previous);
    }

    private static BigDecimal pct(String value) {
        return new BigDecimal(value);
    }

    @Test
    void homeVisitAndFamilyOuting_areFullFeeAndHoldTheBed_fromDayOne() {
        for (AbsenceType type : List.of(AbsenceType.HOME_VISIT, AbsenceType.FAMILY_OUTING)) {
            Outcome outcome = longTerm(type, 5, null);
            assertThat(outcome.coefficient()).isEqualByComparingTo("100");
            assertThat(outcome.hold()).isEqualTo(Hold.YES);
            assertThat(outcome.source()).isEqualTo(Source.FACILITY);
            assertThat(outcome.pendingDecision()).isFalse();
            assertThat(outcome.noPolicyLine()).isFalse();
        }
    }

    @Test
    void hospital_followsTheThreeRanges_ofTheDefaultTable() {
        assertThat(longTerm(AbsenceType.HOSPITAL, 1, null).coefficient()).isEqualByComparingTo("100");
        assertThat(longTerm(AbsenceType.HOSPITAL, 7, pct("100")).coefficient()).isEqualByComparingTo("100");
        assertThat(longTerm(AbsenceType.HOSPITAL, 8, pct("100")).coefficient()).isEqualByComparingTo("70");
        assertThat(longTerm(AbsenceType.HOSPITAL, 30, pct("70")).coefficient()).isEqualByComparingTo("70");
        Outcome threshold = longTerm(AbsenceType.HOSPITAL, 31, pct("70"));
        assertThat(threshold.hold()).isEqualTo(Hold.MANAGER_DECIDES);
        assertThat(threshold.holdBed())
                .as("giường vẫn giữ tới khi Quản lý viện quyết (FR-056)")
                .isTrue();
    }

    @Test
    void managerDecidesLine_usesThePreviousLineCoefficient_andIsMarkedPending() {
        Outcome outcome = longTerm(AbsenceType.HOSPITAL, 35, pct("70"));
        assertThat(outcome.coefficient()).isEqualByComparingTo("70");
        assertThat(outcome.pendingDecision()).isTrue();
        assertThat(outcome.noPolicyLine()).isFalse();
    }

    @Test
    void aTypeWithoutALine_sameDayVisitAndOther_gets100PercentHoldAndTheNoPolicyFlag() {
        for (AbsenceType type : List.of(AbsenceType.SAME_DAY_MEDICAL_VISIT, AbsenceType.OTHER)) {
            Outcome outcome = longTerm(type, 1, null);
            assertThat(outcome.coefficient()).isEqualByComparingTo("100");
            assertThat(outcome.holdBed()).isTrue();
            assertThat(outcome.source()).isEqualTo(Source.DEFAULT);
            assertThat(outcome.noPolicyLine()).isTrue();
            assertThat(outcome.pendingDecision()).isFalse();
        }
    }

    @Test
    void aContractLine_winsOverTheFacilityTable() {
        AbsencePolicyLine line =
                new AbsencePolicyLine(ResidenceType.SHORT_TERM_RESIDENTIAL, "ANY", 1, null, "50", "NO");

        Outcome outcome = AbsencePolicyResolver.resolve(
                ResidenceType.SHORT_TERM_RESIDENTIAL, AbsenceType.HOME_VISIT, 1, List.of(line), TABLE, null);

        assertThat(outcome.coefficient()).isEqualByComparingTo("50");
        assertThat(outcome.hold()).isEqualTo(Hold.NO);
        assertThat(outcome.holdBed()).isFalse();
        assertThat(outcome.source()).isEqualTo(Source.CONTRACT);
    }

    @Test
    void aContractLineOverridesALongTermLine_onlyWithinItsDayRange() {
        AbsencePolicyLine fromDay3 =
                new AbsencePolicyLine(ResidenceType.LONG_TERM_RESIDENTIAL, "HOME_VISIT", 3, null, "80", "YES");

        Outcome day2 = AbsencePolicyResolver.resolve(
                ResidenceType.LONG_TERM_RESIDENTIAL, AbsenceType.HOME_VISIT, 2, List.of(fromDay3), TABLE, null);
        Outcome day3 = AbsencePolicyResolver.resolve(
                ResidenceType.LONG_TERM_RESIDENTIAL, AbsenceType.HOME_VISIT, 3, List.of(fromDay3), TABLE, null);

        assertThat(day2.source()).isEqualTo(Source.FACILITY);
        assertThat(day2.coefficient()).isEqualByComparingTo("100");
        assertThat(day3.source()).isEqualTo(Source.CONTRACT);
        assertThat(day3.coefficient()).isEqualByComparingTo("80");
    }

    @Test
    void byContractLine_withoutAContractLine_fallsBackToTheNoPolicyRule() {
        Outcome outcome = AbsencePolicyResolver.resolve(
                ResidenceType.SHORT_TERM_RESIDENTIAL, AbsenceType.OTHER, 1, List.of(), TABLE, null);

        assertThat(outcome.coefficient()).isEqualByComparingTo("100");
        assertThat(outcome.holdBed()).isTrue();
        assertThat(outcome.noPolicyLine()).isTrue();
        assertThat(outcome.source()).isEqualTo(Source.DEFAULT);
    }

    @Test
    void dayCare_coefficientFollowsTheAttendanceStatus() {
        assertThat(AbsencePolicyResolver.dayCare(Attendance.PRESENT, List.of(), TABLE)
                        .coefficient())
                .isEqualByComparingTo("100");
        assertThat(AbsencePolicyResolver.dayCare(Attendance.ABSENT_NOTIFIED, List.of(), TABLE)
                        .coefficient())
                .isEqualByComparingTo("0");
        assertThat(AbsencePolicyResolver.dayCare(Attendance.ABSENT_UNNOTIFIED, List.of(), TABLE)
                        .coefficient())
                .isEqualByComparingTo("50");
    }

    @Test
    void dayCare_usesTheTableGivenAtCalculationTime_andAContractLineWins() {
        JsonNode changed = parse("""
                {"rows":[{"residencyType":"DAY_CARE","absenceType":"UNANNOUNCED","fromDay":null,"toDay":null,"feeRatePercent":80,"holdBed":null}]}""");
        AbsencePolicyLine override =
                new AbsencePolicyLine(ResidenceType.DAY_CARE, "NOTIFIED_IN_ADVANCE", null, null, "10", null);

        assertThat(AbsencePolicyResolver.dayCare(Attendance.ABSENT_UNNOTIFIED, List.of(), changed)
                        .coefficient())
                .isEqualByComparingTo("80");
        assertThat(AbsencePolicyResolver.dayCare(Attendance.ABSENT_NOTIFIED, List.of(override), TABLE)
                        .coefficient())
                .isEqualByComparingTo("10");
        assertThat(AbsencePolicyResolver.dayCare(Attendance.ABSENT_NOTIFIED, List.of(), changed)
                        .noPolicyLine())
                .as("bảng mới không có dòng Vắng có báo: áp 100% kèm dấu")
                .isTrue();
    }
}
