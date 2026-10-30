package com.eldercare.backend.medication.web.response;

import com.eldercare.backend.medication.internal.service.response.PrnAdministrationResponse;
import com.eldercare.backend.medication.internal.service.response.PrnHistoryResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.function.Function;

/** Phản hồi PRN theo api-006 ({@code PrnHistory}, {@code PrnAdministration}; FR-024, FR-025). */
public final class PrnBodies {

    private PrnBodies() {}

    public record PrnAdministrationBody(
            Long id,
            Long prescriptionId,
            Long residentId,
            Instant administeredAt,
            BigDecimal dosage,
            String dosageUnit,
            String reason,
            String adverseReaction,
            String note,

            @Schema(description = "Họ tên người ghi") String administeredBy,

            @Schema(description = "Họ tên người báo lại (FR-027)")
            String reportedBy,

            boolean exceededLimit,
            boolean recordedAfterDisconnect) {

        static PrnAdministrationBody of(PrnAdministrationResponse a, Function<Long, String> names) {
            return new PrnAdministrationBody(
                    a.getId(),
                    a.getPrescriptionId(),
                    a.getResidentId(),
                    a.getAdministeredAt(),
                    a.getDosage(),
                    a.getDosageUnit(),
                    a.getReason(),
                    a.getAdverseReaction(),
                    a.getNote(),
                    names.apply(a.getAdministeredById()),
                    a.getReportedBy(),
                    a.isExceededLimit(),
                    a.isRecordedAfterDisconnect());
        }
    }

    public record PrnHistoryBody(
            Long prescriptionId,
            List<PrnAdministrationBody> administrations,
            Instant earliestNextAllowedTime,
            boolean canAdministerNow) {

        /** {@code names}: họ tên theo id tài khoản người ghi. */
        public static PrnHistoryBody of(PrnHistoryResponse h, Function<Long, String> names) {
            return new PrnHistoryBody(
                    h.getPrescriptionId(),
                    h.getAdministrations().stream()
                            .map(a -> PrnAdministrationBody.of(a, names))
                            .toList(),
                    h.getEarliestNextAllowedTime(),
                    h.canAdministerNow());
        }
    }
}
