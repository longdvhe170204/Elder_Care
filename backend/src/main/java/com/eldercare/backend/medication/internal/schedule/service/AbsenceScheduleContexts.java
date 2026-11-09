package com.eldercare.backend.medication.internal.schedule.service;

import com.eldercare.backend.medication.internal.handover.domain.MedicationAbsenceInterval;
import com.eldercare.backend.medication.internal.handover.persistence.MedicationAbsenceIntervalRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Ngữ cảnh lịch thuốc từ khoảng vắng đã lưu (research D-06 {@code boiCanh}, D-15; BR-M07-03, FR-021, FR-048b): liều có
 * thời điểm trong khoảng tính Tạm dừng hoặc Mang theo. Khoảng do bean tham gia lệnh trạng thái ghi
 * ({@code MedicationStatusParticipant}).
 */
@Component
public class AbsenceScheduleContexts {

    private final MedicationAbsenceIntervalRepository absences;

    public AbsenceScheduleContexts(MedicationAbsenceIntervalRepository absences) {
        this.absences = absences;
    }

    public ScheduleContext of(Long residentId, Long floorAreaId) {
        return new ScheduleContext(residentId, toIntervals(absences.findByResidentId(residentId)), floorAreaId);
    }

    public ScheduleContext of(Long residentId) {
        return of(residentId, null);
    }

    /**
     * Tạm vắng không mang thuốc, bệnh viện, chuyến đi có lệnh "Không mang thuốc" → Tạm dừng; Tạm vắng, chuyến đi có
     * mang thuốc → Mang theo. Đã về thì khoảng dừng ở giờ về thực tế, chưa về thì ở giờ dự kiến (rỗng là còn mở).
     */
    public static List<ResidentAbsenceInterval> toIntervals(List<MedicationAbsenceInterval> intervals) {
        return intervals.stream()
                .map(i -> new ResidentAbsenceInterval(
                        i.getResidentId(),
                        i.getFromTime(),
                        i.isOpen() ? i.getExpectedToTime() : i.getActualToTime(),
                        switch (i.getAbsenceType()) {
                            case TEMPORARY_LEAVE_NO_MED, TRIP_NO_MED -> AbsenceType.TEMPORARY_LEAVE;
                            case HOSPITAL -> AbsenceType.HOSPITALIZATION;
                            case TEMPORARY_LEAVE_WITH_MED, TRIP_WITH_MED -> AbsenceType.TRIP_WITH_MEDICATION;
                        }))
                .toList();
    }
}
