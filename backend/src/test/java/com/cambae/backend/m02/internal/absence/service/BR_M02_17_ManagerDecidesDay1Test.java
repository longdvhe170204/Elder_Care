package com.cambae.backend.m02.internal.absence.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.absence.domain.AbsenceType;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Hold;
import com.cambae.backend.m02.internal.absence.service.AbsencePolicyResolver.Outcome;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import java.util.List;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 004 T048 (BR-M02-17, FR-056 CHK013): dòng chính sách của ngày vắng thứ nhất đã là "Quản lý quyết định" nên không có dòng
 * liền trước: giường giữ tạm, hệ số 100% kèm dấu "chờ quyết định".
 */
class BR_M02_17_ManagerDecidesDay1Test {

    private static final JsonNode TABLE = JsonMapper.builder().build().readTree("""
            {"rows":[{"residencyType":"LONG_TERM","absenceType":"HOSPITAL","fromDay":1,"toDay":null,
                      "feeRatePercent":"MANAGER_DECIDES","holdBed":"MANAGER_DECIDES"}]}""");

    @Test
    void facilityLineManagerDecidesOnDayOne_is100PercentPending_andHoldsTheBedTemporarily() {
        Outcome outcome = AbsencePolicyResolver.resolve(
                ResidenceType.LONG_TERM_RESIDENTIAL, AbsenceType.HOSPITAL, 1, List.of(), TABLE, null);

        assertThat(outcome.coefficient()).isEqualByComparingTo("100");
        assertThat(outcome.pendingDecision()).isTrue();
        assertThat(outcome.hold()).isEqualTo(Hold.MANAGER_DECIDES);
        assertThat(outcome.holdBed()).isTrue();
    }

    @Test
    void contractLineManagerDecidesOnDayOne_behavesTheSame() {
        AbsencePolicyLine line = new AbsencePolicyLine(
                ResidenceType.LONG_TERM_RESIDENTIAL, "HOSPITAL", 1, null, "MANAGER_DECIDES", "MANAGER_DECIDES");

        Outcome outcome = AbsencePolicyResolver.resolve(
                ResidenceType.LONG_TERM_RESIDENTIAL, AbsenceType.HOSPITAL, 1, List.of(line), TABLE, null);

        assertThat(outcome.coefficient()).isEqualByComparingTo("100");
        assertThat(outcome.pendingDecision()).isTrue();
    }
}
