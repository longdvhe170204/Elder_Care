package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.session.service.FamilyActivityReadService;
import com.eldercare.backend.care.web.response.CareProblemBody;
import com.eldercare.backend.care.web.response.FamilyActivitiesResponse;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import java.time.LocalDate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 014 FR-068 (T026): hoạt động của người cao tuổi trên cổng người thân (api-014, schema duyệt 2026-11-10). */
@RestController
@RequestMapping("/api/v1")
public class FamilyPortalActivityController {
    private final FamilyActivityReadService activities;

    public FamilyPortalActivityController(FamilyActivityReadService activities) {
        this.activities = activities;
    }

    @GetMapping("/family-portal/residents/{residentId}/activities")
    @ApiResponse(responseCode = "200", description = "OK")
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = CareProblemBody.class)))
    public FamilyActivitiesResponse activities(
            @PathVariable long residentId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return FamilyActivitiesResponse.of(activities.of(residentId, from, to));
    }
}
