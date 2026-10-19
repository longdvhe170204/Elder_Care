package com.cambae.backend.m15.correction.web;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionChainView;
import com.cambae.backend.m15.correction.api.CorrectionResult;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CorrectionView;
import com.cambae.backend.m15.correction.api.CreationMethod;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** {@code /api/v1/corrections/{recordType}/{originalRecordId}} theo contracts/api-000.yaml (000 T059). */
@RestController
@RequestMapping("/api/v1/corrections/{recordType}/{originalRecordId}")
public class CorrectionController {

    private final CorrectionApi corrections;

    public CorrectionController(CorrectionApi corrections) {
        this.corrections = corrections;
    }

    @GetMapping
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ChainBody chain(@PathVariable String recordType, @PathVariable long originalRecordId) {
        CorrectionChainView chain = corrections.chain(recordType, originalRecordId);
        return new ChainBody(
                chain.recordType(),
                chain.originalRecordId(),
                chain.corrected(),
                chain.voided(),
                chain.originalContent(),
                chain.currentContent(),
                chain.corrections().stream().map(CorrectionBody::of).toList());
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(
                responseCode = "201",
                content =
                        @Content(
                                mediaType = "application/json",
                                schema = @Schema(implementation = CorrectionBody.class))),
        // 202 kèm ApprovalRequest (cách C) khai khi có yêu cầu phê duyệt (US5); ApiContractIT ghi chờ
        @ApiResponse(
                responseCode = "403",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "409",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public ResponseEntity<Object> create(
            @PathVariable String recordType,
            @PathVariable long originalRecordId,
            @RequestHeader(name = "Idempotency-Key", required = false) UUID idempotencyKey,
            @RequestBody CreateBody body) {
        CorrectionResult result =
                corrections.create(recordType, originalRecordId, body.type(), body.correctedContent(), body.reason());
        return switch (result) {
            case CorrectionResult.Saved saved ->
                ResponseEntity.status(HttpStatus.CREATED).body(CorrectionBody.of(saved.correction()));
            case CorrectionResult.PendingApproval pending ->
                ResponseEntity.status(HttpStatus.ACCEPTED).body(pending);
        };
    }

    public record CreateBody(
            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            CorrectionType type,

            Object correctedContent,

            @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
            Reason reason) {}

    /** Người thực hiện theo schema {@code Actor}; họ tên có khi 002 cung cấp hồ sơ tài khoản. */
    public record ActorBody(ActorType type, Long id, String fullName, String role) {}

    /** Người thực hiện là người dùng hay Bộ lập lịch (schema {@code Actor}). */
    public enum ActorType {
        USER,
        SCHEDULER
    }

    public record CorrectionBody(
            long id,
            int sequenceNo,
            CorrectionType type,
            Object correctedContent,
            String reason,
            ActorBody actor,
            CreationMethod creationMethod,
            Long approvalRequestId,
            Instant recordedAt) {

        static CorrectionBody of(CorrectionView c) {
            return new CorrectionBody(
                    c.id(),
                    c.sequenceNo(),
                    c.type(),
                    c.correctedContent(),
                    c.reason(),
                    new ActorBody(ActorType.USER, c.actorId(), null, null),
                    c.creationMethod(),
                    c.approvalRequestId(),
                    c.recordedAt());
        }
    }

    public record ChainBody(
            String recordType,
            long originalRecordId,
            boolean corrected,
            boolean voided,
            Object originalContent,
            Object currentContent,
            List<CorrectionBody> corrections) {}
}
