package com.cambae.backend.m07.web;

import com.cambae.backend.common.error.web.ProblemBody;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.internal.service.DoseAdministrationService;
import com.cambae.backend.m07.internal.service.DoseViewService;
import com.cambae.backend.m07.internal.service.MedicationScheduleCalculator;
import com.cambae.backend.m07.internal.service.MedicationScheduleService;
import com.cambae.backend.m07.web.response.DoseBodies.DoseBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for medication schedule and rounds (FR-018 -> FR-023; research D-06, D-09).
 */
@RestController
@Tag(name = "dose")
public class MedicationScheduleController {

    private final MedicationScheduleService scheduleService;
    private final DoseAdministrationService doseAdministrationService;
    private final DoseViewService doseViews;
    private final BusinessTime businessTime;

    public MedicationScheduleController(
            MedicationScheduleService scheduleService,
            DoseAdministrationService doseAdministrationService,
            DoseViewService doseViews,
            BusinessTime businessTime) {
        this.scheduleService = scheduleService;
        this.doseAdministrationService = doseAdministrationService;
        this.doseViews = doseViews;
        this.businessTime = businessTime;
    }

    /**
     * Retrieve resident medication schedule by date range (pure function + materialized; research D-06).
     */
    @GetMapping("/api/v1/residents/{residentId}/medication-schedule")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<DoseBody> getResidentSchedule(
            @PathVariable("residentId") Long residentId,
            @RequestParam(name = "from") Instant from,
            @RequestParam(name = "to") Instant to) {

        // Lịch tính theo ngày (research D-06); trả các liều có thời điểm dự kiến trong [from, to)
        LocalDate fromDate = from.atZone(MedicationScheduleCalculator.ZONE_VN).toLocalDate();
        LocalDate toDate = to.atZone(MedicationScheduleCalculator.ZONE_VN).toLocalDate();
        return scheduleService.getResidentSchedule(residentId, fromDate, toDate, businessTime.now()).stream()
                .filter(slot -> !slot.scheduledTime().isBefore(from)
                        && slot.scheduledTime().isBefore(to))
                .map(DoseBody::of)
                .toList();
    }

    /**
     * Retrieve medication doses for medication round by floor and shift (FR-026, FR-026a).
     */
    @GetMapping("/api/v1/medication-doses/rounds")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<DoseBody> getMedicationRound(
            @RequestParam(name = "floorAreaId", required = false) Long floorAreaId,
            @RequestParam(name = "from", required = false) Instant fromTime,
            @RequestParam(name = "to", required = false) Instant toTime) {

        Instant now = businessTime.now();
        Instant effectiveFrom = fromTime != null ? fromTime : now.minus(1, ChronoUnit.HOURS);
        Instant effectiveTo = toTime != null ? toTime : now.plus(3, ChronoUnit.HOURS);

        return doseViews
                .views(scheduleService.getMedicationRound(floorAreaId, effectiveFrom, effectiveTo, now))
                .stream()
                .map(DoseBody::of)
                .toList();
    }

    /**
     * Retrieve detail of a medication dose by dose key ({prescriptionId}-{yyyyMMddHHmm}).
     */
    @GetMapping("/api/v1/medication-doses/{doseKey}")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(
                responseCode = "404",
                content =
                        @Content(
                                mediaType = "application/problem+json",
                                schema = @Schema(implementation = ProblemBody.class)))
    })
    public DoseBody getDoseDetail(@PathVariable("doseKey") String doseKey) {
        return DoseBody.of(doseViews.view(doseAdministrationService.getDoseDetail(doseKey, businessTime.now())));
    }
}
