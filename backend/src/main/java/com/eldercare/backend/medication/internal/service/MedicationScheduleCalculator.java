package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.medication.internal.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.domain.DoseStatus;
import com.eldercare.backend.medication.internal.domain.FrequencyType;
import com.eldercare.backend.medication.internal.domain.Medication;
import com.eldercare.backend.medication.internal.domain.Prescription;
import com.eldercare.backend.medication.internal.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.domain.PrescriptionType;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Pure medication schedule calculator (research D-06; FR-018 -> FR-023; BR-M07-01).
 * Computes projected dose slots without pre-generating database records.
 * doseKey format: {prescriptionId}-{yyyyMMddHHmm} (Asia/Ho_Chi_Minh).
 */
@Component
public class MedicationScheduleCalculator {

    public static final ZoneId ZONE_VN = ZoneId.of("Asia/Ho_Chi_Minh");
    public static final DateTimeFormatter DOSE_KEY_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    private final ParameterApi parameters;

    public MedicationScheduleCalculator(ParameterApi parameters) {
        this.parameters = parameters;
    }

    /** doseKey của liều có thời điểm dự kiến đã biết (liều đã vật hóa). */
    public static String doseKey(long prescriptionId, Instant scheduledTime) {
        return prescriptionId + "-" + DOSE_KEY_FORMATTER.format(scheduledTime.atZone(ZONE_VN));
    }

    /** Cửa sổ mặc định của liều khi thuốc không đặt cửa sổ riêng (CFG-M07-01). */
    public int defaultWindowMinutes() {
        return (int) parameters.read(ParameterCode.CFG_M07_01).asDuration().toMinutes();
    }

    /**
     * Projects dose slots for a single prescription within the given time range.
     */
    public List<CalculatedDoseSlot> calculate(
            Prescription prescription,
            Medication medication,
            Instant fromTime,
            Instant toTime,
            Instant now,
            ScheduleContext context) {

        if (prescription == null || prescription.getPrescriptionType() == PrescriptionType.PRN) {
            return Collections.emptyList();
        }

        List<Instant> candidateTimes = generateCandidateTimes(prescription, fromTime, toTime);
        if (candidateTimes.isEmpty()) {
            return Collections.emptyList();
        }

        int defaultWindow = defaultWindowMinutes();
        int windowBefore = medication != null && medication.getBeforeWindowMinutes() != null
                ? medication.getBeforeWindowMinutes()
                : defaultWindow;
        int windowAfter = medication != null && medication.getAfterWindowMinutes() != null
                ? medication.getAfterWindowMinutes()
                : defaultWindow;

        String medName = medication != null ? medication.getName() : "Medication";
        boolean specialControl = medication != null && medication.isSpecialControl();

        List<CalculatedDoseSlot> result = new ArrayList<>();
        for (Instant scheduledTime : candidateTimes) {
            // Check boundary with startedAt
            if (scheduledTime.isBefore(prescription.getStartedAt())) {
                continue;
            }

            // Check boundary with endedAt (end of day)
            if (prescription.getEndedAt() != null) {
                Instant endOfDay = prescription
                        .getEndedAt()
                        .atTime(LocalTime.MAX)
                        .atZone(ZONE_VN)
                        .toInstant();
                if (scheduledTime.isAfter(endOfDay)) {
                    continue;
                }
            }

            ZonedDateTime zdt = scheduledTime.atZone(ZONE_VN);
            String doseKey = String.format("%d-%s", prescription.getId(), DOSE_KEY_FORMATTER.format(zdt));

            Instant windowFrom = scheduledTime.minus(windowBefore, ChronoUnit.MINUTES);
            Instant windowTo = scheduledTime.plus(windowAfter, ChronoUnit.MINUTES);

            DoseStatus status;
            DoseCancellationReason cancellationReason = null;

            // Check if prescription is cancelled/terminated or suspendedFrom applies
            if (prescription.getSuspendedFrom() != null && !scheduledTime.isBefore(prescription.getSuspendedFrom())) {
                status = DoseStatus.CANCELLED;
                cancellationReason = DoseCancellationReason.DISCONTINUED;
            } else if (prescription.getStatus() == PrescriptionStatus.DISCONTINUED) {
                status = DoseStatus.CANCELLED;
                cancellationReason = DoseCancellationReason.DISCONTINUED;
            } else if (prescription.getStatus() == PrescriptionStatus.EXPIRED) {
                status = DoseStatus.CANCELLED;
                cancellationReason = DoseCancellationReason.EXPIRED;
            } else if (prescription.getStatus() == PrescriptionStatus.SUSPENDED) {
                status = DoseStatus.SUSPENDED;
            } else {
                // Check resident absence intervals
                AbsenceType absence = resolveAbsence(scheduledTime, context);
                if (absence == AbsenceType.TEMPORARY_LEAVE || absence == AbsenceType.HOSPITALIZATION) {
                    status = DoseStatus.SUSPENDED;
                } else if (absence == AbsenceType.TRIP_WITH_MEDICATION) {
                    status = DoseStatus.LEAVE_MEDICATION;
                } else {
                    // Time window relative to 'now'
                    if (now.isBefore(windowFrom)) {
                        status = DoseStatus.UPCOMING;
                    } else if (!now.isAfter(windowTo)) {
                        status = DoseStatus.DUE;
                    } else {
                        status = DoseStatus.LATE;
                    }
                }
            }

            result.add(new CalculatedDoseSlot(
                    doseKey,
                    prescription.getId(),
                    prescription.getResidentId(),
                    prescription.getMedicationId(),
                    medName,
                    prescription.getDosage(),
                    prescription.getDosageUnit(),
                    prescription.getRoute(),
                    prescription.getMedicationSource(),
                    specialControl,
                    scheduledTime,
                    windowFrom,
                    windowTo,
                    status,
                    cancellationReason,
                    false,
                    null));
        }

        result.sort(Comparator.comparing(CalculatedDoseSlot::scheduledTime));
        return result;
    }

    /**
     * Resolves absence for the given scheduled time from context.
     */
    private AbsenceType resolveAbsence(Instant scheduledTime, ScheduleContext context) {
        if (context == null || context.absences() == null || context.absences().isEmpty()) {
            return null;
        }
        for (ResidentAbsenceInterval interval : context.absences()) {
            if (interval.covers(scheduledTime)) {
                return interval.absenceType();
            }
        }
        return null;
    }

    /**
     * Generates all candidate execution instants for the prescription inside [fromTime, toTime].
     */
    public List<Instant> generateCandidateTimes(Prescription prescription, Instant fromTime, Instant toTime) {
        List<Instant> candidateTimes = new ArrayList<>();
        FrequencyType freqType = prescription.getFrequencyType();
        if (freqType == null) {
            return candidateTimes;
        }

        ZonedDateTime fromZdt = fromTime.atZone(ZONE_VN);
        ZonedDateTime toZdt = toTime.atZone(ZONE_VN);

        switch (freqType) {
            case TIMED_SLOTS -> {
                List<LocalTime> slots = parseTimedSlots(prescription.getTimedSlots());
                if (slots.isEmpty()) {
                    break;
                }
                LocalDate curDate = fromZdt.toLocalDate();
                LocalDate endDate = toZdt.toLocalDate();
                while (!curDate.isAfter(endDate)) {
                    for (LocalTime slot : slots) {
                        Instant inst = curDate.atTime(slot).atZone(ZONE_VN).toInstant();
                        if (!inst.isBefore(fromTime) && !inst.isAfter(toTime)) {
                            candidateTimes.add(inst);
                        }
                    }
                    curDate = curDate.plusDays(1);
                }
            }
            case EVERY_N_HOURS -> {
                Integer intervalHours = prescription.getEveryNHours();
                if (intervalHours == null || intervalHours <= 0) {
                    intervalHours = 8;
                }
                Instant current =
                        prescription.getFirstSlot() != null ? prescription.getFirstSlot() : prescription.getStartedAt();

                // If starting before fromTime, jump forward
                if (current.isBefore(fromTime)) {
                    long hoursDiff = ChronoUnit.HOURS.between(current, fromTime);
                    long steps = hoursDiff / intervalHours;
                    current = current.plus(steps * intervalHours, ChronoUnit.HOURS);
                }

                while (current.isBefore(fromTime)) {
                    current = current.plus(intervalHours, ChronoUnit.HOURS);
                }

                while (!current.isAfter(toTime)) {
                    candidateTimes.add(current);
                    current = current.plus(intervalHours, ChronoUnit.HOURS);
                }
            }
            case DAYS_OF_WEEK -> {
                List<LocalTime> slots = parseTimedSlots(prescription.getTimedSlots());
                if (slots.isEmpty()) {
                    slots = List.of(LocalTime.of(8, 0));
                }
                Set<Integer> targetDays = parseDaysOfWeek(prescription.getDaysOfWeek());
                LocalDate curDate = fromZdt.toLocalDate();
                LocalDate endDate = toZdt.toLocalDate();
                while (!curDate.isAfter(endDate)) {
                    int dayValue = mapDayOfWeekToDbValue(curDate.getDayOfWeek());
                    if (targetDays.contains(dayValue)) {
                        for (LocalTime slot : slots) {
                            Instant inst = curDate.atTime(slot).atZone(ZONE_VN).toInstant();
                            if (!inst.isBefore(fromTime) && !inst.isAfter(toTime)) {
                                candidateTimes.add(inst);
                            }
                        }
                    }
                    curDate = curDate.plusDays(1);
                }
            }
        }

        return candidateTimes;
    }

    /**
     * Parses JSON array or comma-separated list of HH:mm (e.g. ["08:00", "20:00"]).
     */
    public List<LocalTime> parseTimedSlots(String jsonOrCsv) {
        if (jsonOrCsv == null || jsonOrCsv.isBlank()) {
            return Collections.emptyList();
        }
        String cleaned =
                jsonOrCsv.replace("[", "").replace("]", "").replace("\"", "").trim();
        if (cleaned.isEmpty()) {
            return Collections.emptyList();
        }
        String[] parts = cleaned.split(",");
        List<LocalTime> times = new ArrayList<>();
        for (String p : parts) {
            String trimmed = p.trim();
            if (!trimmed.isEmpty()) {
                try {
                    times.add(LocalTime.parse(trimmed));
                } catch (Exception ignored) {
                }
            }
        }
        times.sort(Comparator.naturalOrder());
        return times;
    }

    /**
     * Parses days of week string ("1,2,3,4,5,6,7" where 1=Sunday, 2=Monday..).
     */
    public Set<Integer> parseDaysOfWeek(String days) {
        Set<Integer> result = new HashSet<>();
        if (days == null || days.isBlank()) {
            return result;
        }
        String[] parts = days.split(",");
        for (String p : parts) {
            try {
                result.add(Integer.parseInt(p.trim()));
            } catch (Exception ignored) {
            }
        }
        return result;
    }

    /**
     * Maps java.time.DayOfWeek to MySQL SET('1','2','3','4','5','6','7') value:
     * Sunday = 1, Monday = 2, Tuesday = 3, Wednesday = 4, Thursday = 5, Friday = 6, Saturday = 7.
     */
    public int mapDayOfWeekToDbValue(DayOfWeek dow) {
        return switch (dow) {
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 3;
            case WEDNESDAY -> 4;
            case THURSDAY -> 5;
            case FRIDAY -> 6;
            case SATURDAY -> 7;
        };
    }
}
