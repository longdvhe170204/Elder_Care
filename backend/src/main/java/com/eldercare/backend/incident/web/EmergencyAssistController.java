package com.eldercare.backend.incident.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.incident.internal.alert.service.EmergencyAssistService;
import com.eldercare.backend.incident.web.request.AssistDetailsRequest;
import com.eldercare.backend.incident.web.response.M05ProblemBody;
import com.eldercare.backend.incident.web.response.SosProblemBody;
import com.eldercare.backend.incident.web.response.SosStatusResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Nút hỗ trợ khẩn cấp của nhân viên (US13; FR-047i, FR-047j; research S-24). Đi qua phiên như mọi lệnh nhân viên. */
@RestController
@RequestMapping("/api/v1/emergency-assist")
public class EmergencyAssistController {
    private static final String PROBLEM = "application/problem+json";

    private final EmergencyAssistService assist;
    private final CurrentUser currentUser;

    public EmergencyAssistController(EmergencyAssistService assist, CurrentUser currentUser) {
        this.assist = assist;
        this.currentUser = currentUser;
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Đã gửi"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = SosProblemBody.class)))
    })
    public ResponseEntity<SosStatusResponse> press(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        var status = assist.press(currentUser, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(SosStatusResponse.of(status));
    }

    @GetMapping("/current")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "204", description = "Không có cảnh báo đang mở", content = @Content)
    })
    public ResponseEntity<SosStatusResponse> current() {
        return assist.current(currentUser)
                .map(status -> ResponseEntity.ok(SosStatusResponse.of(status)))
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PostMapping("/{id}/details")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Đã ghi"),
        @ApiResponse(
                responseCode = "409",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M05ProblemBody.class)))
    })
    public ResponseEntity<Void> details(@PathVariable long id, @RequestBody AssistDetailsRequest request) {
        assist.details(id, request == null ? null : request.toDetails(), currentUser);
        return ResponseEntity.noContent().build();
    }
}
