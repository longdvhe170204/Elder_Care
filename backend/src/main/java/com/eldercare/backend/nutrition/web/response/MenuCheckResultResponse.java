package com.eldercare.backend.nutrition.web.response;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả kiểm tra thực đơn (api-011 {@code MenuCheckResult}). Lệnh trên món trả kết quả kiểm tra lại thực đơn chưa hết
 * tuần sau khi xác nhận bản sửa (FR-004); trước khi có thực đơn (phase thực đơn) kết quả rỗng.
 */
@Schema(name = "MenuCheckResult")
public record MenuCheckResultResponse(
        List<CoverageGap> coverageGaps,
        List<Object> dishRepetitions,

        @Schema(description = "Bếp: residentCount; Bác sĩ: kèm tên người")
        List<Object> allergies,

        boolean professionalReasonRequired,
        List<String> warningCodes) {

    @Schema(name = "MenuCoverageGap")
    public record CoverageGap(Long dietId, LocalDate date, Long mealId) {}

    public static MenuCheckResultResponse empty() {
        return new MenuCheckResultResponse(List.of(), List.of(), List.of(), false, List.of());
    }
}
