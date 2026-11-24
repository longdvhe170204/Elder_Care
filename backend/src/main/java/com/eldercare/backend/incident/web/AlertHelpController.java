package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.alert.service.EmergencyAssistService;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * "Tôi đến giúp" ở cảnh báo từ nút SOS, nút hỗ trợ (đề xuất 2026-11-23): người nhận báo mình đang tới phụ, người lo chính
 * vẫn là người tiếp nhận; khi chuyển sự cố thì thành người tham gia xử lý.
 */
@RestController
@RequestMapping("/api/v1/alerts")
public class AlertHelpController {
    private static final String PROBLEM = "application/problem+json";

    private final EmergencyAssistService assist;
    private final CurrentUser currentUser;

    public AlertHelpController(EmergencyAssistService assist, CurrentUser currentUser) {
        this.assist = assist;
        this.currentUser = currentUser;
    }

    @PostMapping("/{id}/join-help")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> joinHelp(
            @PathVariable long id, @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        assist.joinHelp(id, currentUser);
        return ResponseEntity.ok().build();
    }
}
