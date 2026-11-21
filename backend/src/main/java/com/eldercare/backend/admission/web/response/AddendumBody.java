package com.eldercare.backend.admission.web.response;

import com.eldercare.backend.administration.account.api.FieldGroup;
import com.eldercare.backend.administration.account.api.VisibilityTarget;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.admission.internal.change.domain.Addendum;
import com.eldercare.backend.admission.internal.contract.service.ContractService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Phụ lục hợp đồng (contracts/api-004.yaml, schema {@code Addendum}; FR-042, FR-043d, FR-047). Phụ lục chỉ do yêu cầu thay
 * đổi lưu trú đã duyệt tạo và không sửa được nội dung. {@code contentBefore}, {@code contentAfter} chỉ gồm các thuộc tính
 * bị đổi và là nội dung tài chính (FR-028, nhóm {@code FINANCE}): 002 bỏ khỏi phản hồi khi người xem không được xem.
 */
@Schema(name = "Addendum")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddendumBody(
        Long id,
        Long approvalRequestId,
        Integer addendumNo,
        List<String> changeTypes,
        @FieldGroup(InfoGroup.FINANCE) JsonNode contentBefore,
        @FieldGroup(InfoGroup.FINANCE) JsonNode contentAfter,
        LocalDate effectiveDate,
        Status status,
        Boolean signatureRequired,
        LocalDate signedDate,
        Long signedByFamilyMemberId,
        Long facilitySignatoryStaffId,
        Long signedFileId,
        List<String> hiddenFields,
        @JsonIgnore @Schema(hidden = true) Target target)
        implements VisibilityTarget {

    /**
     * Giá trị theo hợp đồng api-004 (glossary 10.3 ghi {@code NOT_APPLIED}, lệch hợp đồng, báo ở plan); Chờ ký, Đã hủy
     * thêm 2026-11-20 (Q-419, Q-420), cùng tên ở cơ sở dữ liệu và API.
     */
    public enum Status {
        PENDING_SIGNATURE,
        PENDING_EFFECTIVE,
        APPLIED,
        NOT_APPLICABLE,
        CANCELLED
    }

    public static AddendumBody of(Addendum addendum, long residentId) {
        return new AddendumBody(
                addendum.id(),
                addendum.approvalRequestId(),
                addendum.addendumNo(),
                addendum.changeTypes().stream().map(Enum::name).toList(),
                addendum.contentBefore(),
                addendum.contentAfter(),
                addendum.effectiveDate(),
                status(addendum.status()),
                addendum.signatureRequired(),
                addendum.signedDate(),
                addendum.signedByFamilyMemberId(),
                addendum.facilitySignatoryStaffId(),
                addendum.signedFileId(),
                List.of(),
                ContractService.target(addendum.contractId(), residentId));
    }

    public static Status status(Addendum.Status status) {
        return switch (status) {
            case PENDING_SIGNATURE -> Status.PENDING_SIGNATURE;
            case PENDING_EFFECTIVE -> Status.PENDING_EFFECTIVE;
            case APPLIED -> Status.APPLIED;
            case NOT_APPLIED -> Status.NOT_APPLICABLE;
            case CANCELLED -> Status.CANCELLED;
        };
    }

    @Override
    @JsonIgnore
    public Target visibilityTarget() {
        return target;
    }
}
