package com.cambae.backend.m07.web;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m07.api.dto.DoseDetailView;
import com.cambae.backend.m07.internal.service.CalculatedDoseSlot;
import com.cambae.backend.m07.internal.service.DoseAdministrationService;
import com.cambae.backend.m07.internal.service.MedicationScheduleCalculator;
import com.cambae.backend.m07.internal.service.MedicationScheduleService;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller for medication schedule and rounds (FR-018 -> FR-023; research D-06, D-09).
 */
@RestController
public class MedicationScheduleController {

    private final MedicationScheduleService scheduleService;
    private final DoseAdministrationService doseAdministrationService;
    private final BusinessTime businessTime;

    public MedicationScheduleController(
            MedicationScheduleService scheduleService,
            DoseAdministrationService doseAdministrationService,
            BusinessTime businessTime) {
        this.scheduleService = scheduleService;
        this.doseAdministrationService = doseAdministrationService;
        this.businessTime = businessTime;
    }

    /**
     * Retrieve resident medication schedule by date range (pure function + materialized; research D-06).
     */
    @GetMapping({"/api/v1/nguoi-cao-tuoi/{nctId}/lich-thuoc", "/api/v1/residents/{nctId}/medication-schedule"})
    public List<CalculatedDoseSlot> getResidentSchedule(
            @PathVariable("nctId") Long residentId,
            @RequestParam(name = "tuNgay", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate fromDate,
            @RequestParam(name = "denNgay", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate toDate) {

        Instant now = businessTime.now();
        LocalDate today = now.atZone(MedicationScheduleCalculator.ZONE_VN).toLocalDate();
        LocalDate effectiveFrom = fromDate != null ? fromDate : today;
        LocalDate effectiveTo = toDate != null ? toDate : effectiveFrom.plusDays(6);

        return scheduleService.getResidentSchedule(residentId, effectiveFrom, effectiveTo, now);
    }

    /**
     * Retrieve medication doses for medication round by floor and shift (FR-026, FR-026a).
     */
    @GetMapping({"/api/v1/lieu-thuoc/vong-phat-thuoc", "/api/v1/medication-doses/rounds"})
    public List<DoseDetailView> getMedicationRound(
            @RequestParam(name = "tangKhuVucId", required = false) Long floorAreaId,
            @RequestParam(name = "tuGio", required = false) Instant fromTime,
            @RequestParam(name = "denGio", required = false) Instant toTime) {

        Instant now = businessTime.now();
        Instant effectiveFrom = fromTime != null ? fromTime : now.minus(1, ChronoUnit.HOURS);
        Instant effectiveTo = toTime != null ? toTime : now.plus(3, ChronoUnit.HOURS);

        return scheduleService.getMedicationRound(floorAreaId, effectiveFrom, effectiveTo, now);
    }

    /**
     * Retrieve detail of a medication dose by dose key ({prescriptionId}-{yyyyMMddHHmm}).
     */
    @GetMapping({"/api/v1/lieu-thuoc/{doseKey}", "/api/v1/medication-doses/{doseKey}"})
    public DoseDetailView getDoseDetail(@PathVariable("doseKey") String doseKey) {
        return doseAdministrationService.getDoseDetail(doseKey, businessTime.now());
    }
}
