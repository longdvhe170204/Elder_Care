package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import com.cambae.backend.m02.api.events.ResidencyEndingCompleted;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T057 (US7; FR-063, FR-065, FR-027, FR-072; research T-09): danh sách điều kiện (a) → (h) khớp lệnh Kết thúc lưu trú,
 * lệnh chặn và liệt kê đủ điều kiện chưa đạt, và các tác động khi thành công (hợp đồng Kết thúc hoặc Chấm dứt, hồ sơ
 * Hoàn tất, lượt vắng đóng, hồ sơ lưu trú đã đóng).
 */
@AutoConfigureMockMvc
@WithMockUser
class EndingConditionsIT extends EndingTestBase {

    @Test
    void FR_063_theList_hasEightLines_withAutomaticOnesForContractAndBed() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));

        JsonNode list = conditions(id);

        List<String> codes = list.get("conditions")
                .valueStream()
                .map(line -> line.get("code").asString())
                .toList();
        assertThat(codes)
                .containsExactly(
                        "END_A_COSTS",
                        "END_B_BELONGINGS",
                        "END_C_STORED_MEDICATION",
                        "END_D_ALERTS_INCIDENTS",
                        "END_E_HANDOVER",
                        "END_F_CONTRACT",
                        "END_G_BED",
                        "END_H_SETTLEMENT");
        assertThat(line(list, "END_A_COSTS").get("status").asString()).isEqualTo("MET");
        assertThat(line(list, "END_F_CONTRACT").get("status").asString()).isEqualTo("AUTOMATIC");
        assertThat(line(list, "END_G_BED").get("status").asString()).isEqualTo("AUTOMATIC");
        assertThat(line(list, "END_E_HANDOVER").get("status").asString()).isEqualTo("NOT_MET");
        assertThat(line(list, "END_E_HANDOVER").get("module").asString()).isEqualTo("M02");
    }

    @Test
    void FR_063_unmetConditionsOfOtherModules_showAsNotMet_andHandoverMakesEMet() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));
        otherUnmet(B_BELONGINGS, true);
        otherUnmet(D_ALERTS, true);

        JsonNode before = conditions(id);
        handover(id);
        JsonNode after = conditions(id);

        assertThat(line(before, B_BELONGINGS).get("status").asString()).isEqualTo("NOT_MET");
        assertThat(line(before, D_ALERTS).get("status").asString()).isEqualTo("NOT_MET");
        assertThat(line(before, "END_C_STORED_MEDICATION").get("status").asString())
                .isEqualTo("MET");
        assertThat(line(before, "END_E_HANDOVER").get("status").asString()).isEqualTo("NOT_MET");
        assertThat(line(after, "END_E_HANDOVER").get("status").asString()).isEqualTo("MET");
        assertThat(line(after, B_BELONGINGS).get("status").asString()).isEqualTo("NOT_MET");
    }

    @Test
    void FR_063_lineH_mergesTheTwoSettlementCodes_andShowsBothReasons() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0.plusDays(5));
        otherUnmet(H_SETTLEMENT, true);
        otherUnmet(H_CONFIRMATION, false);

        JsonNode list = conditions(id);

        long linesOfH = list.get("conditions")
                .valueStream()
                .filter(line -> line.get("code").asString().startsWith("END_H"))
                .count();
        assertThat(linesOfH).isEqualTo(1);
        assertThat(line(list, H_SETTLEMENT).get("status").asString()).isEqualTo("NOT_MET");
        assertThat(line(list, H_SETTLEMENT).get("basis").asString())
                .contains(H_SETTLEMENT)
                .contains(H_CONFIRMATION);
    }

    @Test
    void FR_065_theCommand_isBlocked_andListsEveryUnmetCondition() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        long noHandover = inResidence();
        long noHandoverEnding = createEnding(noHandover, d0.plusDays(1));
        otherUnmet(B_BELONGINGS, true);
        otherUnmet(D_ALERTS, true);

        BusinessException blocked = endResidencyFailure(noHandover, noHandoverEnding, at(d0.plusDays(1), 12, 0));

        assertThat(blocked).isNotNull();
        assertThat(unmetCodes(blocked)).containsExactlyInAnyOrder(B_BELONGINGS, D_ALERTS, "END_E_HANDOVER");
        assertThat(endingStatus(noHandoverEnding)).isEqualTo("PREPARING");
        assertThat(residentStatus(noHandover)).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(unmetCodes(endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0))))
                .containsExactlyInAnyOrder(B_BELONGINGS, D_ALERTS);
    }

    @Test
    void FR_004_theListOfNotMetLines_matchesTheCommandsUnmetConditions() throws Exception {
        long residentId = inResidence();
        long id = createEnding(residentId, d0);
        otherUnmet(D_ALERTS, true);
        otherUnmet(H_SETTLEMENT, true);
        noonOf(d0);

        BusinessException blocked = endResidencyFailure(residentId, id, at(d0, 12, 0));

        List<String> notMet = conditions(id)
                .get("conditions")
                .valueStream()
                .filter(line -> "NOT_MET".equals(line.get("status").asString()))
                .map(line -> line.get("code").asString())
                .toList();
        assertThat(notMet).containsExactlyInAnyOrderElementsOf(unmetCodes(blocked));
    }

    @Test
    void FR_066_theEffectiveTime_mustBeWithinTheExpectedEndDay() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(2));

        BusinessException early = endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0));
        BusinessException late = endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(3), 0, 5));

        assertThat(unmetCodes(early)).containsExactly("END_EFFECTIVE_DATE_OUT_OF_RANGE");
        assertThat(unmetCodes(late)).containsExactly("END_EFFECTIVE_DATE_OUT_OF_RANGE");
        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.IN_RESIDENCE);
    }

    @Test
    void FR_065_theCommand_needsAPreparingEnding() throws Exception {
        long residentId = inResidence();
        noonOf(d0);

        BusinessException none = endResidencyFailure(residentId, 999_999_999L, at(d0, 12, 0));
        long cancelled = createEnding(residentId, d0);
        cancelRaw(office, cancelled, "Đổi ý").andExpect(status().isOk());
        BusinessException afterCancel = endResidencyFailure(residentId, cancelled, at(d0, 12, 0));

        assertThat(unmetCodes(none)).contains("END_NO_PREPARING_ENDING");
        assertThat(unmetCodes(afterCancel)).contains("END_NO_PREPARING_ENDING");
    }

    @Test
    void FR_065_success_completesTheEnding_endsTheContract_andClosesTheResidencyRecord() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        long contractId = jdbc.queryForObject(
                "SELECT id FROM contract WHERE resident_id = ? AND status = 'ACTIVE'", Long.class, ready.residentId());
        Instant effectiveAt = at(d0.plusDays(1), 12, 0);

        endResidency(ready.residentId(), ready.endingId(), effectiveAt);

        Map<String, Object> row = jdbc.queryForMap("SELECT * FROM residency_ending WHERE id = ?", ready.endingId());
        assertThat(row.get("status")).isEqualTo("COMPLETED");
        assertThat(((java.time.LocalDateTime) row.get("actual_ended_at")).toInstant(java.time.ZoneOffset.UTC))
                .isEqualTo(effectiveAt);
        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
        assertThat(contractStatus(contractId))
                .as("hợp đồng không kỳ hạn mà lưu trú kết thúc thì Chấm dứt (FR-027)")
                .isEqualTo("TERMINATED");
        assertThat(jdbc.queryForObject(
                        "SELECT termination_reason FROM contract WHERE id = ?", String.class, contractId))
                .isNotBlank();
        assertThat(jdbc.queryForObject(
                        "SELECT closed_at FROM resident_residency WHERE resident_id = ?",
                        java.sql.Timestamp.class,
                        ready.residentId()))
                .as("hồ sơ lưu trú đã đóng (FR-072)")
                .isNotNull();
        assertThat(events.stream(ResidencyEndingCompleted.class).filter(e -> e.residencyEndingId() == ready.endingId()))
                .singleElement()
                .satisfies(e -> assertThat(e.endDate()).isEqualTo(d0.plusDays(1)));
        assertThat(events.stream(ContractStatusChanged.class)
                        .filter(e -> e.contractId() == contractId && e.to() == ContractStatus.TERMINATED))
                .hasSize(1);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE action = 'M02_ENDING_COMPLETED' AND entity_id = ?",
                        Integer.class,
                        String.valueOf(ready.endingId())))
                .isEqualTo(1);
    }

    @Test
    void FR_027_contractEndsOnItsEndDate_andIsTerminatedWhenTheStayEndsEarlier() throws Exception {
        LocalDate end = d0.plusDays(3);
        long onTime = shortTermResident(end);
        long onTimeEnding = createEnding(onTime, end);
        handover(onTimeEnding);
        long early = shortTermResident(end.plusDays(10));
        long earlyEnding = createEnding(early, end);
        handover(earlyEnding);
        noonOf(end);

        endResidency(onTime, onTimeEnding, at(end, 12, 0));
        endResidency(early, earlyEnding, at(end, 12, 0));

        assertThat(contractOf(onTime))
                .as("CHK039: không sớm hơn ngày kết thúc hợp đồng")
                .isEqualTo("ENDED");
        assertThat(contractOf(early)).isEqualTo("TERMINATED");
        assertThat(jdbc.queryForObject(
                        "SELECT overdue_since FROM contract WHERE resident_id = ?", LocalDate.class, onTime))
                .isNull();
    }

    @Test
    void FR_065_anOpenAbsence_isClosedAtTheEffectiveTime() throws Exception {
        long residentId = inResidence();
        clockAt(d0, 10, 0);
        long absenceId = grant(residentId, "HOME_VISIT", at(d0.plusDays(4), 10, 0));
        long endingId = createEnding(residentId, d0.plusDays(2));
        handover(endingId);
        noonOf(d0.plusDays(2));

        endResidency(residentId, endingId, at(d0.plusDays(2), 12, 0));

        Map<String, Object> absence = jdbc.queryForMap("SELECT * FROM absence WHERE id = ?", absenceId);
        assertThat(absence.get("status")).isEqualTo("CLOSED");
        assertThat(((java.time.LocalDateTime) absence.get("returned_at")).toInstant(java.time.ZoneOffset.UTC))
                .isEqualTo(at(d0.plusDays(2), 12, 0));
        assertThat(days(absenceId)).hasSize(3);
        assertThat(residentStatus(residentId)).isEqualTo(ResidentStatus.RESIDENCY_ENDED);
    }

    @Test
    void FR_065_aFailedEffect_rollsBackTheWholeCommand() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        otherModules.failOnApply(new IllegalStateException("module khác lỗi"));

        org.junit.jupiter.api.Assertions.assertThrows(
                IllegalStateException.class,
                () -> endResidency(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0)));

        assertThat(endingStatus(ready.endingId())).isEqualTo("PREPARING");
        assertThat(residentStatus(ready.residentId())).isEqualTo(ResidentStatus.IN_RESIDENCE);
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM contract WHERE resident_id = ?", String.class, ready.residentId()))
                .isEqualTo("ACTIVE");
    }

    @Test
    void FR_065_aRaceBetweenCancelAndEnd_leavesOnlyOneOutcome() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        cancelRaw(office, ready.endingId(), "Gia đình đổi ý").andExpect(status().isOk());

        BusinessException blocked =
                endResidencyFailure(ready.residentId(), ready.endingId(), at(d0.plusDays(1), 12, 0));

        assertThat(blocked).isNotNull();
        assertThat(endingStatus(ready.endingId())).isEqualTo("CANCELLED");
    }

    // ---- Dữ liệu ----------------------------------------------------------------------------------------------------

    /** Nội trú ngắn ngày Đang lưu trú, hợp đồng Hiệu lực kết thúc ngày {@code end}. */
    private long shortTermResident(LocalDate end) {
        long residentId = admissionResident();
        setStatus(residentId, ResidentStatus.IN_RESIDENCE.name());
        long contractId = contracts.contract(
                residentId,
                ContractStatus.ACTIVE,
                ResidenceType.SHORT_TERM_RESIDENTIAL,
                CareLevel.BASIC,
                d0.minusDays(20));
        jdbc.update("UPDATE contract SET current_end_date = ? WHERE id = ?", end, contractId);
        user.set(office.user());
        return residentId;
    }

    private String contractOf(long residentId) {
        return jdbc.queryForObject("SELECT status FROM contract WHERE resident_id = ?", String.class, residentId);
    }
}
