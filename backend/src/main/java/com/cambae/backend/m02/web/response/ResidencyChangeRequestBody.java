package com.cambae.backend.m02.web.response;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m02.internal.change.domain.ChangeType;
import com.cambae.backend.m02.internal.change.service.ResidencyChangeService;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.SelfHiddenFields;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import tools.jackson.databind.JsonNode;

/**
 * Yêu cầu thay đổi lưu trú (contracts/api-004.yaml, schema {@code ResidencyChangeRequest}; FR-038, FR-043). {@code
 * costImpact} là nội dung tài chính (FR-028): chỉ người xem được phần tài chính của hợp đồng thấy, người khác bị bỏ trường
 * và tên ghi vào {@code hiddenFields} ({@link SelfHiddenFields}). {@code minorChange} tính lúc đọc trên hợp đồng hiện hành,
 * không lưu (research T-06).
 */
@Schema(name = "ResidencyChangeRequest")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ResidencyChangeRequestBody(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) long contractId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<ChangeType> changeTypes,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) JsonNode newValue,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) LocalDate requestedEffectiveDate,
        JsonNode reference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason,
        long approvalRequestId,
        JsonNode previousValue,
        @FieldGroup(InfoGroup.FINANCE) JsonNode costImpact,
        @Schema(accessMode = Schema.AccessMode.READ_ONLY) boolean minorChange,
        String status,
        Long addendumId,
        String requestedBy,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) boolean financeVisible,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget, SelfHiddenFields {

    public static ResidencyChangeRequestBody of(ResidencyChangeService.View view) {
        ApprovalRequestView request = view.details().request();
        JsonNode content = request.content();
        return new ResidencyChangeRequestBody(
                view.content().contractId(),
                List.copyOf(view.content().changeTypes()),
                content.get("newValue"),
                request.requestedEffectiveDate(),
                view.content().reference(),
                request.reason() == null ? null : new Reason(request.reason()),
                request.id(),
                content.get("previousValue"),
                view.financeVisible() ? content.get("costImpact") : null,
                view.minorChange(),
                request.status(),
                view.addendumId(),
                view.requestedBy(),
                List.of(),
                view.financeVisible(),
                view.target());
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }

    /** Ảnh hưởng chi phí chỉ hiện với người xem được phần tài chính của hợp đồng (FR-028); tên ghi vào hiddenFields. */
    @Override
    @JsonIgnore
    public Set<String> selfHiddenFields() {
        return financeVisible ? Set.of() : Set.of("costImpact");
    }
}
