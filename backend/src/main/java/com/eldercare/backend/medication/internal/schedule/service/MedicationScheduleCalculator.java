package com.eldercare.backend.medication.internal.schedule.service;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.medication.internal.catalog.domain.Medication;
import com.eldercare.backend.medication.internal.dose.domain.DoseCancellationReason;
import com.eldercare.backend.medication.internal.dose.domain.DoseStatus;
import com.eldercare.backend.medication.internal.prescription.domain.FrequencyType;
import com.eldercare.backend.medication.internal.prescription.domain.Prescription;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionStatus;
import com.eldercare.backend.medication.internal.prescription.domain.PrescriptionType;
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
import java.util.function.Predicate;
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
            if (!withinPrescriptionPeriod(prescription, scheduledTime)) {
                continue;
            }

            ZonedDateTime zdt = scheduledTime.atZone(ZONE_VN);
            String doseKey = String.format("%d-%s", prescription.getId(), DOSE_KEY_FORMATTER.format(zdt));

            Instant windowFrom = scheduledTime.minus(windowBefore, ChronoUnit.MINUTES);
            Instant windowTo = scheduledTime.plus(windowAfter, ChronoUnit.MINUTES);
            ProjectedStatus projected =
                    projectedStatus(prescription, scheduledTime, windowFrom, windowTo, now, context);

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
                    projected.status(),
                    projected.cancellationReason(),
                    false,
                    null));
        }

        result.sort(Comparator.comparing(CalculatedDoseSlot::scheduledTime));
        return result;
    }

    /** Trạng thái dự kiến của một liều và lý do hủy nếu có. */
    private record ProjectedStatus(DoseStatus status, DoseCancellationReason cancellationReason) {}

    /** Liều nằm từ lúc đơn bắt đầu tới hết ngày kết thúc của đơn (nếu có). */
    private static boolean withinPrescriptionPeriod(Prescription prescription, Instant scheduledTime) {
        if (scheduledTime.isBefore(prescription.getStartedAt())) {
            return false;
        }
        if (prescription.getEndedAt() == null) {
            return true;
        }
        Instant endOfDay =
                prescription.getEndedAt().atTime(LocalTime.MAX).atZone(ZONE_VN).toInstant();
        return !scheduledTime.isAfter(endOfDay);
    }

    /**
     * Trạng thái dự kiến theo thứ tự: đơn tạm ngừng từ thời điểm, đơn đã ngừng, hết hạn, tạm ngừng; người cao tuổi vắng
     * (tạm về, nằm viện: tạm ngừng; chuyến có mang thuốc: thuốc mang theo); còn lại theo cửa sổ liều so với hiện tại.
     */
    private ProjectedStatus projectedStatus(
            Prescription prescription,
            Instant scheduledTime,
            Instant windowFrom,
            Instant windowTo,
            Instant now,
            ScheduleContext context) {
        if (prescription.getSuspendedFrom() != null && !scheduledTime.isBefore(prescription.getSuspendedFrom())) {
            return new ProjectedStatus(DoseStatus.CANCELLED, DoseCancellationReason.DISCONTINUED);
        }
        if (prescription.getStatus() == PrescriptionStatus.DISCONTINUED) {
            return new ProjectedStatus(DoseStatus.CANCELLED, DoseCancellationReason.DISCONTINUED);
        }
        if (prescription.getStatus() == PrescriptionStatus.EXPIRED) {
            return new ProjectedStatus(DoseStatus.CANCELLED, DoseCancellationReason.EXPIRED);
        }
        if (prescription.getStatus() == PrescriptionStatus.SUSPENDED) {
            return new ProjectedStatus(DoseStatus.SUSPENDED, null);
        }
        AbsenceType absence = resolveAbsence(scheduledTime, context);
        if (absence == AbsenceType.TEMPORARY_LEAVE || absence == AbsenceType.HOSPITALIZATION) {
            return new ProjectedStatus(DoseStatus.SUSPENDED, null);
        }
        if (absence == AbsenceType.TRIP_WITH_MEDICATION) {
            return new ProjectedStatus(DoseStatus.LEAVE_MEDICATION, null);
        }
        if (now.isBefore(windowFrom)) {
            return new ProjectedStatus(DoseStatus.UPCOMING, null);
        }
        if (!now.isAfter(windowTo)) {
            return new ProjectedStatus(DoseStatus.DUE, null);
        }
        return new ProjectedStatus(DoseStatus.LATE, null);
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
        FrequencyType freqType = prescription.getFrequencyType();
        if (freqType == null) {
            return new ArrayList<>();
        }
        return switch (freqType) {
            case TIMED_SLOTS -> {
                List<LocalTime> slots = parseTimedSlots(prescription.getTimedSlots());
                yield slots.isEmpty() ? new ArrayList<>() : slotsOnDays(slots, fromTime, toTime, day -> true);
            }
            case EVERY_N_HOURS -> everyNHours(prescription, fromTime, toTime);
            case DAYS_OF_WEEK -> {
                List<LocalTime> slots = parseTimedSlots(prescription.getTimedSlots());
                if (slots.isEmpty()) {
                    slots = List.of(LocalTime.of(8, 0));
                }
                Set<Integer> targetDays = parseDaysOfWeek(prescription.getDaysOfWeek());
                yield slotsOnDays(
                        slots, fromTime, toTime, day -> targetDays.contains(mapDayOfWeekToDbValue(day.getDayOfWeek())));
            }
        };
    }

    /** Các giờ trong ngày của mọi ngày được chọn trong khoảng, chỉ giữ thời điểm nằm trong [fromTime, toTime]. */
    private static List<Instant> slotsOnDays(
            List<LocalTime> slots, Instant fromTime, Instant toTime, Predicate<LocalDate> includeDay) {
        List<Instant> candidateTimes = new ArrayList<>();
        LocalDate curDate = fromTime.atZone(ZONE_VN).toLocalDate();
        LocalDate endDate = toTime.atZone(ZONE_VN).toLocalDate();
        while (!curDate.isAfter(endDate)) {
            if (includeDay.test(curDate)) {
                for (LocalTime slot : slots) {
                    Instant inst = curDate.atTime(slot).atZone(ZONE_VN).toInstant();
                    if (!inst.isBefore(fromTime) && !inst.isAfter(toTime)) {
                        candidateTimes.add(inst);
                    }
                }
            }
            curDate = curDate.plusDays(1);
        }
        return candidateTimes;
    }

    /** Mỗi N giờ (mặc định 8) tính từ liều đầu, hoặc từ lúc đơn bắt đầu khi không có liều đầu. */
    private static List<Instant> everyNHours(Prescription prescription, Instant fromTime, Instant toTime) {
        List<Instant> candidateTimes = new ArrayList<>();
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
     * Parses days of week string ("1,2,3,4,5,6,7" ISO: 1=thứ hai … 7=chủ nhật).
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
     * Mã ngày theo ISO-8601 (chủ dự án chốt 2026-11-08): thứ hai = 1 … chủ nhật = 7.
     */
    public int mapDayOfWeekToDbValue(DayOfWeek dow) {
        return dow.getValue();
    }
}
