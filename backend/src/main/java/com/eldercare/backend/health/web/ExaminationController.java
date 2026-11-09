package com.eldercare.backend.health.web;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.health.internal.examination.service.ExaminationService;
import com.eldercare.backend.health.web.request.ExaminationRecordBody;
import com.eldercare.backend.health.web.response.M06ProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Bản ghi khám và điều trị (FR-072, FR-073; contracts/api-007 mục J). */
@RestController
@RequestMapping("/api/v1/residents/{residentId}/examinations")
public class ExaminationController {
    private static final String PROBLEM = "application/problem+json";

    private final ExaminationService service;

    public ExaminationController(ExaminationService service) {
        this.service = service;
    }

    @GetMapping
    public List<ExaminationRecordBody> list(@PathVariable long residentId) {
        return service.list(target(residentId), residentId).stream()
                .map(ExaminationRecordBody::from)
                .toList();
    }

    @PostMapping
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "OK"),
        @ApiResponse(
                responseCode = "403",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class))),
        @ApiResponse(
                responseCode = "422",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = M06ProblemBody.class)))
    })
    public ResponseEntity<Void> record(
            @PathVariable long residentId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody ExaminationRecordBody body) {
        service.record(target(residentId), residentId, body.toInput());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static Target target(long residentId) {
        return new Target("M06.EXAMINATION_RECORD", null, residentId, null);
    }
}
