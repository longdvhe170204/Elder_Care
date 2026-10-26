package com.cambae.backend.m02.web.response;

import com.cambae.backend.m02.internal.change.domain.Addendum;
import com.cambae.backend.m02.internal.contract.service.ContractService;
import com.cambae.backend.m15.account.api.FieldGroup;
import com.cambae.backend.m15.account.api.VisibilityTarget;
import com.cambae.backend.m15.account.api.dto.InfoGroup;
import com.cambae.backend.m15.account.api.dto.Target;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Phụ lục hợp đồng (contracts/api-004.yaml, schema {@code Addendum}; FR-042, FR-047). Phụ lục chỉ do yêu cầu thay đổi lưu
 * trú đã duyệt tạo và không sửa được. {@code contentBefore}, {@code contentAfter} chỉ gồm các thuộc tính bị đổi và là nội
 * dung tài chính (FR-028, nhóm {@code FINANCE}): 002 bỏ khỏi phản hồi khi người xem không được xem.
 */
@Schema(name = "Addendum")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddendumBody(
        Long id,
        Long approvalRequestId,
        List<String> changeTypes,
        @FieldGroup(InfoGroup.FINANCE) JsonNode contentBefore,
        @FieldGroup(InfoGroup.FINANCE) JsonNode contentAfter,
        LocalDate effectiveDate,
        Status status,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    /** Giá trị theo hợp đồng api-004 (glossary 10.3 ghi {@code NOT_APPLIED}, lệch hợp đồng, báo ở plan). */
    public enum Status {
        PENDING_EFFECTIVE,
        APPLIED,
        NOT_APPLICABLE
    }

    public static AddendumBody of(Addendum addendum, long residentId) {
        return new AddendumBody(
                addendum.id(),
                addendum.approvalRequestId(),
                addendum.changeTypes().stream().map(Enum::name).toList(),
                addendum.contentBefore(),
                addendum.contentAfter(),
                addendum.effectiveDate(),
                switch (addendum.status()) {
                    case PENDING_EFFECTIVE -> Status.PENDING_EFFECTIVE;
                    case APPLIED -> Status.APPLIED;
                    case NOT_APPLIED -> Status.NOT_APPLICABLE;
                },
                List.of(),
                ContractService.target(addendum.contractId(), residentId));
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
