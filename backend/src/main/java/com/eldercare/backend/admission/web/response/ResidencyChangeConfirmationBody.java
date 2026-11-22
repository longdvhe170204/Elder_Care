package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.admission.internal.change.service.RepresentativeChangeConfirmationService.PortalView;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Đề nghị xác nhận thay đổi lưu trú đang chờ người đại diện (FR-043c; api-004 schema {@code ResidencyChangeConfirmation}):
 * nội dung thay đổi, giá trị trước, ảnh hưởng chi phí và ngày hiệu lực mong muốn.
 */
@Schema(name = "ResidencyChangeConfirmation")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidencyChangeConfirmationBody(
        long id,
        long approvalRequestId,
        long residentId,
        String status,
        Instant sentAt,
        List<String> changeTypes,
        JsonNode previousValue,
        JsonNode newValue,
        List<ResidencyChangeRequestBody.CostImpactItem> costImpact,
        LocalDate requestedEffectiveDate,
        String reason) {

    public static ResidencyChangeConfirmationBody of(PortalView view) {
        List<String> types = new java.util.ArrayList<>();
        if (view.changeTypes() != null) {
            view.changeTypes().forEach(t -> types.add(t.asString()));
        }
        return new ResidencyChangeConfirmationBody(
                view.id(),
                view.approvalRequestId(),
                view.residentId(),
                view.status(),
                view.sentAt(),
                types,
                view.previousValue(),
                view.newValue(),
                ResidencyChangeRequestBody.CostImpactItem.of(view.costImpact()),
                view.requestedEffectiveDate(),
                view.reason());
    }
}
