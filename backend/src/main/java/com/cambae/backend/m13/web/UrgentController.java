package com.cambae.backend.m13.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m13.internal.urgent.service.UrgentAcknowledgement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Thông báo Khẩn cấp theo contracts/api-009.yaml (thẻ {@code urgent}; 009 T021): xác nhận đã nhận. "Xác nhận và tiếp
 * nhận" (lệnh tiếp nhận của 007) và lượt nhắc gọi làm sau.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "urgent")
public class UrgentController {

    private final UrgentAcknowledgement acknowledgement;

    public UrgentController(UrgentAcknowledgement acknowledgement) {
        this.acknowledgement = acknowledgement;
    }

    @PostMapping("/notifications/{id}/acknowledge")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    ResponseEntity<Void> acknowledge(
            @PathVariable long id, @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey) {
        acknowledgement.acknowledge(id);
        return ResponseEntity.ok().build();
    }
}
