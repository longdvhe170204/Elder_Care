package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.administration.account.api.FieldGroup;
import com.eldercare.backend.administration.account.api.SelfHiddenFields;
import com.eldercare.backend.administration.account.api.VisibilityTarget;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.admission.internal.change.domain.ChangeType;
import com.eldercare.backend.admission.internal.change.service.ResidencyChangeService;
import com.eldercare.backend.common.Reason;
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
        @Schema(implementation = Reference.class) JsonNode reference,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) Reason reason,
        long approvalRequestId,
        JsonNode previousValue,

        @FieldGroup(InfoGroup.FINANCE) List<CostImpactItem> costImpact,

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
                view.financeVisible() ? CostImpactItem.of(content.get("costImpact")) : null,
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

    /** Căn cứ của yêu cầu (api-004 {@code reference}): lần đánh giá hoặc lệnh chuyển giường. */
    @Schema(name = "ResidencyChangeReference")
    public record Reference(
            @Schema(allowableValues = {"ASSESSMENT", "BED_TRANSFER_ORDER"})
            String type,

            Long id) {}

    /** Một dòng tác động chi phí (api-004 {@code costImpact[]}), chỉ khi xem được phần tài chính (FR-028). */
    @Schema(name = "ResidencyChangeCostImpact")
    public record CostImpactItem(String item, Long previousUnitPrice, Long newUnitPrice) {
        static List<CostImpactItem> of(JsonNode items) {
            if (items == null || !items.isArray()) return null;
            var list = new java.util.ArrayList<CostImpactItem>();
            for (JsonNode i : items) {
                list.add(new CostImpactItem(
                        i.path("item").asString(null),
                        i.hasNonNull("previousUnitPrice")
                                ? i.get("previousUnitPrice").asLong()
                                : null,
                        i.hasNonNull("newUnitPrice") ? i.get("newUnitPrice").asLong() : null));
            }
            return list;
        }
    }
}
