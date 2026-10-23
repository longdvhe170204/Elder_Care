package com.cambae.backend.m13.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m13.internal.lookup.domain.DeliveryError;
import com.cambae.backend.m13.internal.lookup.service.NotificationLookupService;
import com.cambae.backend.m13.web.response.NotificationHistoryBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lịch sử thông báo, lỗi gửi theo contracts/api-009.yaml (thẻ {@code lookup}; 009 T034). Quyền theo bảng FR-039: lịch sử
 * Quản lý viện toàn viện, Trưởng tầng tầng được giao; lỗi gửi chỉ Quản lý viện.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "lookup")
public class NotificationLookupController {

    /** Loại lỗi gửi của {@code GET /delivery-errors} (FR-038). */
    public enum ErrorType {
        REJECTED,
        MISSING_LEVEL,
        MISSING_INFORMATION_TYPE,
        NO_RECIPIENT,
        DELIVERY_FAILED,
        INVALID_CONTENT
    }

    private final NotificationLookupService lookup;

    public NotificationLookupController(NotificationLookupService lookup) {
        this.lookup = lookup;
    }

    @GetMapping("/notification-history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    List<NotificationHistoryBody> history(
            @RequestParam(required = false) String sourceRecordType,
            @RequestParam(required = false) String sourceRecordId,
            @RequestParam(required = false) Long recipientId,
            @RequestParam(required = false) Long floorAreaId,
            @RequestParam(required = false) OffsetDateTime from,
            @RequestParam(required = false) OffsetDateTime to) {
        return lookup
                .history(
                        sourceRecordType,
                        sourceRecordId,
                        recipientId,
                        floorAreaId,
                        from == null ? null : from.toInstant(),
                        to == null ? null : to.toInstant())
                .stream()
                .map(NotificationHistoryBody::of)
                .toList();
    }

    @GetMapping("/delivery-errors")
    @ApiResponse(responseCode = "200", description = "OK", content = @Content)
    List<DeliveryError> deliveryErrors(@RequestParam(required = false) ErrorType type) {
        return lookup.deliveryErrors(type == null ? null : type.name());
    }
}
