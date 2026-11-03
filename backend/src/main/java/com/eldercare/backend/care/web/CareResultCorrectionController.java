package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.result.service.CareResultCorrectionService;
import com.eldercare.backend.care.web.request.CareResultCorrectionWrite;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.common.error.*;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/care-results")
public class CareResultCorrectionController {
    private final CareResultCorrectionService commands;

    public CareResultCorrectionController(CareResultCorrectionService commands) {
        this.commands = commands;
    }

    @PostMapping("/{id}/correct")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    @ApiResponse(responseCode = "201", description = "Đã lưu đính chính")
    public ResponseEntity<Void> correct(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody CareResultCorrectionWrite body) {
        if (body.type() == null) throw new InvalidValueException("Cần chọn kiểu đính chính");
        if (body.reason() == null) throw new ReasonRequiredException();
        commands.correct(id, body.type(), body.correctedContent(), body.reason());
        return ResponseEntity.status(201).build();
    }
}
