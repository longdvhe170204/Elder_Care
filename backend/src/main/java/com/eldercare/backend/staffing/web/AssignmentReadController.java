package com.eldercare.backend.staffing.web;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.staffing.internal.assignment.service.AssignmentReadService;
import com.eldercare.backend.staffing.web.response.AssignmentReadResponses;
import com.eldercare.backend.staffing.web.response.M09ProblemBody;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import java.time.Instant;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/shifts/{id}")
@ApiResponses({
    @ApiResponse(
            responseCode = "400",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = M09ProblemBody.class))),
    @ApiResponse(
            responseCode = "401",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = M09ProblemBody.class))),
    @ApiResponse(
            responseCode = "403",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = M09ProblemBody.class))),
    @ApiResponse(
            responseCode = "404",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = M09ProblemBody.class))),
    @ApiResponse(
            responseCode = "409",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = M09ProblemBody.class))),
    @ApiResponse(
            responseCode = "422",
            content =
                    @Content(
                            mediaType = "application/problem+json",
                            schema = @Schema(implementation = M09ProblemBody.class)))
})
class AssignmentReadController {
    private final AssignmentReadService reads;

    AssignmentReadController(AssignmentReadService reads) {
        this.reads = reads;
    }

    @GetMapping("/assignment-resolution")
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AssignmentReadResponses.Resolution.class)))
    AssignmentReadResponses.Resolution resolution(
            @PathVariable long id,
            @RequestParam long residentId,
            @RequestParam Instant at,
            @Parameter(schema = @Schema(type = "string")) @RequestParam String role,
            @RequestParam(required = false) Long taskTypeId) {
        Role parsed;
        try {
            parsed = Role.valueOf(role);
        } catch (IllegalArgumentException e) {
            throw new com.eldercare.backend.common.error.BusinessException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Vai trò không hợp lệ");
        }
        return AssignmentReadResponses.Resolution.of(reads.resolution(id, residentId, at, parsed, taskTypeId));
    }

    @GetMapping("/unassigned-residents")
    @ApiResponse(
            responseCode = "200",
            content =
                    @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = AssignmentReadResponses.Unassigned.class)))
    AssignmentReadResponses.Unassigned unassigned(@PathVariable long id) {
        return AssignmentReadResponses.Unassigned.of(reads.unassigned(id));
    }
}
