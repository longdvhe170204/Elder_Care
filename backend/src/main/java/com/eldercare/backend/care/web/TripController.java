package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.trip.service.*;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.InvalidValueException;
import org.springframework.web.bind.annotation.*;

/** T018: phần chuẩn bị và rời viện của Phase 6; các command trở về thuộc Phase 7. */
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {
    private final TripPreparationService preparation;
    private final TripDepartureService departure;

    public TripController(TripPreparationService preparation, TripDepartureService departure) {
        this.preparation = preparation;
        this.departure = departure;
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    public com.eldercare.backend.care.web.response.TripDetailResponse detail(@PathVariable long id) {
        return com.eldercare.backend.care.web.response.TripDetailResponse.of(
                preparation.detail(preparation.target(id), id));
    }

    @PostMapping("/{id}/assignment")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            content =
                    @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/problem+json",
                            schema =
                                    @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation =
                                                    com.eldercare.backend.care.web.response.CareProblemBody.class)))
    public void assign(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody TripAssignmentWrite body) {
        preparation.assign(preparation.target(id), id, body.leaderId(), body.companionIds());
    }

    @PostMapping("/{id}/eligibility-assessments")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            content =
                    @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/problem+json",
                            schema =
                                    @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation =
                                                    com.eldercare.backend.care.web.response.CareProblemBody.class)))
    public void assess(
            @PathVariable long id,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody TripAssessmentWrite body) {
        if (body.residentId() == null) throw new InvalidValueException("Thiếu người cao tuổi");
        preparation.assess(
                preparation.assessmentTarget(id, body.residentId()),
                id,
                body.residentId(),
                body.result(),
                body.reason());
    }

    @PostMapping("/{id}/{command}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "403",
            content =
                    @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/problem+json",
                            schema =
                                    @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation =
                                                    com.eldercare.backend.care.web.response.CareProblemBody.class)))
    @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "409",
            content =
                    @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/problem+json",
                            schema =
                                    @io.swagger.v3.oas.annotations.media.Schema(
                                            implementation =
                                                    com.eldercare.backend.care.web.response.CareProblemBody.class)))
    public void depart(
            @PathVariable long id,
            @PathVariable
                    @io.swagger.v3.oas.annotations.Parameter(
                            schema =
                                    @io.swagger.v3.oas.annotations.media.Schema(
                                            allowableValues = {
                                                "depart",
                                                "return",
                                                "extend",
                                                "transfer-leader",
                                                "report-missing",
                                                "record-missing-incident",
                                                "finish-return"
                                            }))
                    String command,
            @RequestHeader("Idempotency-Key") String key,
            @RequestBody TripCommandWrite body) {
        if (!java.util.Set.of(
                        "depart",
                        "return",
                        "extend",
                        "transfer-leader",
                        "report-missing",
                        "record-missing-incident",
                        "finish-return")
                .contains(command)) throw new InvalidValueException("Lệnh chuyến không hợp lệ");
        if (!"depart".equals(command))
            throw new com.eldercare.backend.common.error.BusinessException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "CARE_SOURCE_UNAVAILABLE",
                    "Lệnh theo dõi, trở về thuộc Phase 7 chưa cài");
        var reason = body.reason() == null || body.reason().isBlank() ? null : new Reason(body.reason());
        departure.depart(preparation.target(id), id, body.residentIds(), reason, body.deviceAt());
    }
}
