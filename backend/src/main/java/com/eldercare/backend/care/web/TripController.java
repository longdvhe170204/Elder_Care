package com.eldercare.backend.care.web;

import com.eldercare.backend.care.internal.trip.service.*;
import com.eldercare.backend.care.web.request.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.error.InvalidValueException;
import org.springframework.web.bind.annotation.*;

/** T018 (Phase 6), T020 (Phase 7): chuẩn bị, rời viện, trở về và báo thiếu người qua port 007. */
@RestController
@RequestMapping("/api/v1/trips")
public class TripController {
    private final TripPreparationService preparation;
    private final TripDepartureService departure;
    private final TripReturnService returns;
    private final TripMissingService missing;
    private final com.eldercare.backend.care.internal.trip.service.TripParticipantInfo participantInfo;

    public TripController(
            TripPreparationService preparation,
            TripDepartureService departure,
            TripReturnService returns,
            TripMissingService missing,
            com.eldercare.backend.care.internal.trip.service.TripParticipantInfo participantInfo) {
        this.participantInfo = participantInfo;
        this.preparation = preparation;
        this.departure = departure;
        this.returns = returns;
        this.missing = missing;
    }

    @GetMapping("/{id}")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK")
    public com.eldercare.backend.care.web.response.TripDetailResponse detail(@PathVariable long id) {
        var detail = preparation.detail(preparation.target(id), id);
        return com.eldercare.backend.care.web.response.TripDetailResponse.of(detail, participantInfo.of(detail));
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
        var reason = body.reason() == null || body.reason().isBlank() ? null : new Reason(body.reason());
        var target = preparation.target(id);
        switch (command) {
            case "depart" -> departure.depart(target, id, body.residentIds(), reason, body.deviceAt());
            case "return" -> returns.returnResidents(target, id, body.residentIds(), body.deviceAt());
            case "extend" -> returns.extend(target, id, body.newPlannedReturnAt(), reason);
            case "transfer-leader" -> returns.transferLeader(target, id, body.newLeaderId(), reason);
            case "finish-return" -> returns.finishReturn(target, id, body.deviceAt());
            case "report-missing", "record-missing-incident" ->
                missing.report(
                        target,
                        id,
                        body.residentIds(),
                        command.equals("record-missing-incident"),
                        body.lastSeenPlace(),
                        body.lastSeenAt(),
                        body.description(),
                        body.deviceAt());
            default -> throw new InvalidValueException("Lệnh chuyến không hợp lệ");
        }
    }
}
