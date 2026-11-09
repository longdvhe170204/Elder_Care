package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.api.events.ContractStatusChanged;
import com.eldercare.backend.admission.api.events.ResidencyEndingCompleted;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;

/**
 * 004 T057 (US7; FR-063, FR-065, FR-027, FR-072; research T-09): danh sách điều kiện (a) → (h) khớp lệnh Kết thúc lưu trú,
 * lệnh chặn và liệt kê đủ điều kiện chưa đạt, và các tác động khi thành công (hợp đồng Kết thúc hoặc Chấm dứt, hồ sơ
 * Hoàn tất, lượt vắng đóng, hồ sơ lưu trú đã đóng).
 */
@AutoConfigureMockMvc
@WithMockUser
class EndingConditionsIT extends EndingTestBase {

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
    void FR_065_success_completesTheEnding_endsTheContract_andClosesTheResidencyRecord() throws Exception {
        ReadyEnding ready = readyToEnd(d0.plusDays(1));
        long contractId = jdbc.queryForObject(
                "SELECT id FROM contract WHERE resident_id = ? AND status = 'ACTIVE'", Long.class, ready.residentId());
        Instant effectiveAt = at(d0.plusDays(1), 12, 0);

        endResidency(ready.residentId(), ready.endingId(), effectiveAt);

        Map<String, Object> row =
                jdbc.queryForMap("SELECT * FROM residency_ending WHERE kind = 'ENDING' AND id = ?", ready.endingId());
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
        assertThat(jdbc.queryForObject(M02Sql.residencyField("closedAt"), String.class, ready.residentId()))
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
