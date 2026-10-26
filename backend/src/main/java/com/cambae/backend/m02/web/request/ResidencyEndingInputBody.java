package com.cambae.backend.m02.web.request;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m02.api.dto.EndCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Thân Lập hồ sơ kết thúc lưu trú (contracts/api-004.yaml, POST {@code /residency-endings}; FR-062): trường hợp, ngày kết
 * thúc dự kiến, người yêu cầu, lý do, nơi chuyển đến (bắt buộc với Chuyển cơ sở), có phải lý do y tế (Bác sĩ lập).
 */
public record ResidencyEndingInputBody(
        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Long residentId,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        EndCaseInput endCase,

        Boolean medicalReason,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        LocalDate expectedEndDate,

        @Size(max = 255) String destination,

        @NotNull @Size(max = 255) @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        String requestedBy,

        @NotNull @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
        Reason reason) {

    /** Bốn trường hợp kết thúc lưu trú (không có qua đời, FR-062). */
    public enum EndCaseInput {
        CONTRACT_EXPIRED(EndCase.CONTRACT_EXPIRED),
        DISCHARGE(EndCase.DISCHARGE),
        TRANSFER_TO_ANOTHER_FACILITY(EndCase.TRANSFER_TO_ANOTHER_FACILITY),
        CONTRACT_TERMINATION(EndCase.CONTRACT_TERMINATION);

        private final EndCase endCase;

        EndCaseInput(EndCase endCase) {
            this.endCase = endCase;
        }

        public EndCase endCase() {
            return endCase;
        }
    }
}
