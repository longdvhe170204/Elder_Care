package com.eldercare.backend.medication.web;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.error.web.ProblemBody;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.medication.internal.dose.domain.DoseDetailView;
import com.eldercare.backend.medication.internal.dose.service.DoseAdministrationService;
import com.eldercare.backend.medication.internal.dose.service.DoseViewService;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleCalculator;
import com.eldercare.backend.medication.internal.schedule.service.MedicationScheduleService;
import com.eldercare.backend.medication.web.response.DoseBodies.DoseBody;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final com.eldercare.backend.medication.internal.dose.service.DoseExtrasService doseExtras;
    private final BusinessTime businessTime;
    private final MedicationScheduleCalculator scheduleCalculator;
    private final ObjectProvider<CurrentUser> currentUsers;
    private final ObjectProvider<PermissionApi> permissionApis;

    public MedicationScheduleController(
            MedicationScheduleService scheduleService,
            DoseAdministrationService doseAdministrationService,
            DoseViewService doseViews,
            com.eldercare.backend.medication.internal.dose.service.DoseExtrasService doseExtras,
            BusinessTime businessTime,
            MedicationScheduleCalculator scheduleCalculator) {
        this(
                scheduleService,
                doseAdministrationService,
                doseViews,
                doseExtras,
                businessTime,
                scheduleCalculator,
                null,
                null);
    }

    @Autowired
    public MedicationScheduleController(
            MedicationScheduleService scheduleService,
            DoseAdministrationService doseAdministrationService,
            DoseViewService doseViews,
            com.eldercare.backend.medication.internal.dose.service.DoseExtrasService doseExtras,
            BusinessTime businessTime,
            MedicationScheduleCalculator scheduleCalculator,
            ObjectProvider<CurrentUser> currentUsers,
            ObjectProvider<PermissionApi> permissionApis) {
        this.scheduleService = scheduleService;
        this.doseAdministrationService = doseAdministrationService;
        this.doseViews = doseViews;
        this.doseExtras = doseExtras;
        this.businessTime = businessTime;
        this.scheduleCalculator = scheduleCalculator;
        this.currentUsers = currentUsers;
        this.permissionApis = permissionApis;
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

    /** Lịch sử trạng thái các liều thuốc của người cao tuổi theo khoảng ngày (M07). */
    @GetMapping("/api/v1/medication-doses/history")
    @ApiResponse(responseCode = "200", description = "OK")
    public List<DoseBody> getDoseHistory(
            @RequestParam(name = "residentId") Long residentId,
            @RequestParam(name = "fromDate") LocalDate fromDate,
            @RequestParam(name = "toDate") LocalDate toDate,
            @RequestParam(name = "status", required = false) String status) {
        Instant now = businessTime.now();
        return scheduleService.getResidentSchedule(residentId, fromDate, toDate, now).stream()
                .map(slot -> {
                    if (Boolean.TRUE.equals(slot.materialized())) {
                        var dv = doseViews.view(doseAdministrationService.getDoseDetail(slot.doseKey(), now));
                        return DoseBody.of(dv, doseExtras.of(dv));
                    }
                    return DoseBody.of(slot);
                })
                .filter(b -> status == null
                        || status.isBlank()
                        || status.equalsIgnoreCase(b.currentStatus())
                        || status.equalsIgnoreCase(b.status()))
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
        int defaultWindowMinutes = scheduleCalculator.defaultWindowMinutes();
        Instant effectiveFrom = fromTime != null ? fromTime : now.minusSeconds(defaultWindowMinutes * 60L);
        Instant effectiveTo = toTime != null ? toTime : now.plusSeconds(defaultWindowMinutes * 60L);

        List<DoseDetailView> round =
                filterForCurrentUser(scheduleService.getMedicationRound(floorAreaId, effectiveFrom, effectiveTo, now));
        return doseViews.views(round).stream()
                .map(v -> DoseBody.of(v, doseExtras.of(v)))
                .toList();
    }

    private List<DoseDetailView> filterForCurrentUser(List<DoseDetailView> round) {
        CurrentUser user = currentUsers != null ? currentUsers.getIfAvailable() : null;
        PermissionApi perms = permissionApis != null ? permissionApis.getIfAvailable() : null;
        if (user == null || perms == null) {
            return round;
        }
        Set<PermissionKey> granted = perms.permissions(user);
        boolean isDoctor = granted.contains(new PermissionKey(Function.PRESCRIPTION, Operation.PROFESSIONAL));
        boolean isNurse =
                granted.contains(new PermissionKey(Function.MEDICATION_ADMINISTRATION, Operation.PROFESSIONAL));
        if (isDoctor || !isNurse || user.staffId().isEmpty()) {
            return round;
        }
        long staffId = user.staffId().get();
        long accountId = user.accountId();
        return round.stream()
                .filter(d -> d.assignedNurseId() == null
                        || Objects.equals(d.assignedNurseId(), staffId)
                        || Objects.equals(d.recorderId(), accountId))
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
        var view = doseViews.view(doseAdministrationService.getDoseDetail(doseKey, businessTime.now()));
        return DoseBody.of(view, doseExtras.of(view));
    }
}
