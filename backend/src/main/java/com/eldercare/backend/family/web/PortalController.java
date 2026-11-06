package com.eldercare.backend.family.web;

import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.family.internal.portal.service.PortalService;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Cổng người thân (012 T028; contracts/api-012.yaml, tag {@code family-portal}; US5, FR-046 → FR-048). Hợp đồng không khai
 * nội dung phản hồi 200; hình dạng ở {@link PortalService}.
 */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "family-portal")
class PortalController {

    private static final String PROBLEM = "application/problem+json";

    private final PortalService portal;

    PortalController(PortalService portal) {
        this.portal = portal;
    }

    @GetMapping("/family-portal/residents")
    @ApiResponses({@ApiResponse(responseCode = "200", description = "OK", content = @Content)})
    List<PortalService.PortalResident> residents() {
        return portal.residents();
    }

    @GetMapping("/family-portal/residents/{residentId}/general")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    PortalService.General general(@PathVariable long residentId) {
        return portal.general(residentId);
    }

    @GetMapping("/family-portal/residents/{residentId}/health/{section}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "NO_HEALTH_VIEW_RIGHT hoặc RIGHT_NOT_EFFECTIVE",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    PortalService.HealthPart health(
            @PathVariable long residentId,
            @PathVariable
                    @Schema(
                            allowableValues = {
                                "allergies-conditions",
                                "vital-signs",
                                "weight",
                                "medications",
                                "incidents",
                                "care-results",
                                "eating",
                                "diet",
                                "medical-appointments"
                            })
                    String section,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to) {
        return portal.health(residentId, section, from, to);
    }

    @GetMapping("/family-portal/residents/{residentId}/contract")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    PortalService.Contracts contract(@PathVariable long residentId) {
        return portal.contract(residentId);
    }

    @GetMapping("/health-view-logs")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK", content = @Content),
        @ApiResponse(
                responseCode = "403",
                description = "PERMISSION_DENIED",
                content = @Content(mediaType = PROBLEM, schema = @Schema(implementation = ProblemBody.class)))
    })
    List<PortalService.HealthViewLog> viewLogs(
            @RequestParam(required = false) Long residentId,
            @RequestParam(required = false) Long familyMemberId,
            @RequestParam(required = false) Instant from,
            @RequestParam(required = false) Instant to) {
        return portal.viewLogs(residentId, familyMemberId, from, to);
    }
}
