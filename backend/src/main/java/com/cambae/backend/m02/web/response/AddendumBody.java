package com.cambae.backend.m02.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDate;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Phụ lục hợp đồng (contracts/api-004.yaml, schema {@code Addendum}; FR-042, FR-047). Phụ lục chỉ do yêu cầu thay đổi lưu
 * trú đã duyệt tạo (user story 5), nên tới lúc đó {@code GET /contracts/{id}/addenda} trả danh sách rỗng.
 */
@Schema(name = "Addendum")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record AddendumBody(
        Long id,
        Long approvalRequestId,
        List<String> changeTypes,
        JsonNode contentBefore,
        JsonNode contentAfter,
        LocalDate effectiveDate,
        Status status) {

    /** Giá trị theo hợp đồng api-004 (glossary 10.3 ghi {@code NOT_APPLIED}, lệch hợp đồng, báo ở plan). */
    public enum Status {
        PENDING_EFFECTIVE,
        APPLIED,
        NOT_APPLICABLE
    }
}
