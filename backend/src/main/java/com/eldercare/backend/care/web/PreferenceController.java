package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.wellbeing.service.PreferenceService;
import com.eldercare.backend.care.web.request.ActivityReasonWrite;
import com.eldercare.backend.care.web.request.PreferenceWrite;
import com.eldercare.backend.care.web.response.CareProblemBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** 014 T022 (FR-051): ghi, bỏ sở thích. GET sở thích, hồ sơ tinh thần chờ schema phản hồi được duyệt (plan Phase 8). */
@RestController
@RequestMapping("/api/v1")
public class PreferenceController {
    private final PreferenceService preferences;

    public PreferenceController(PreferenceService preferences) {
        this.preferences = preferences;
    }

    @PostMapping("/residents/{residentId}/preferences")
    @ApiResponse(responseCode = "201", description = "OK")
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public ResponseEntity<Void> record(
            @PathVariable long residentId,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody PreferenceWrite body) {
        long id = preferences.record(
                PreferenceService.target(residentId),
                residentId,
                body.interestGroupId(),
                body.description(),
                body.level(),
                body.source(),
                Boolean.TRUE.equals(body.confirmReplacement()));
        return ResponseEntity.created(URI.create("/api/v1/preferences/" + id)).build();
    }

    @PostMapping("/preferences/{id}/remove")
    @ApiResponse(responseCode = "200", description = "OK")
    public void remove(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody ActivityReasonWrite body) {
        preferences.remove(preferences.targetOf(id), id, body.reason());
    }
}
