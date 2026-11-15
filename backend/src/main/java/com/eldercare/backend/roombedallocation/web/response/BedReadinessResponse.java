package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.common.error.UnmetCondition;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/** T060: điều kiện tại thời điểm đọc, không thay thế kiểm tra khi xác nhận. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BedReadinessResponse(
        boolean allowed,
        boolean disinfectionRequired,
        ResidentSummarySchema departedResident,
        List<Unmet> unmetConditions) {

    /** Điều kiện chưa thỏa theo api-003 {@code UnmetCondition}: mã, mô tả, căn cứ (không có feature sở hữu, miễn được). */
    @Schema(name = "BedReadinessUnmetCondition")
    public record Unmet(String code, String description, String basis) {
        public static List<Unmet> of(List<UnmetCondition> conditions) {
            return conditions == null
                    ? null
                    : conditions.stream()
                            .map(c -> new Unmet(c.code(), c.description(), c.basis()))
                            .toList();
        }
    }
}
