package com.eldercare.backend.care.internal.session.domain;

import com.eldercare.backend.common.error.UnmetCondition;
import java.time.LocalTime;
import java.util.*;

/**
 * 014 T010 (research L-05 {@code DieuKienDangKy}; FR-015 → FR-018, BR-M04-15): hàm thuần trả điều kiện chặn và cảnh
 * báo không chặn. Đối tượng của hoạt động (mức chăm sóc, loại hình, tầng) chưa xét vì {@code targetGroup} chưa có cấu
 * trúc trong hợp đồng (plan Phase 4); sở thích "không thích" (FR-052) thêm ở US6.
 */
public final class RegistrationConditions {
    private RegistrationConditions() {}

    /**
     * @param groupOrTrip hoạt động nhóm hoặc chuyến đi (FR-015 (g))
     * @param dayCare khác {@code null} khi người cao tuổi là người bán trú vào ngày của buổi
     */
    public record Facts(
            String sessionStatus,
            boolean started,
            boolean groupOrTrip,
            int activeRegistrations,
            int maxParticipants,
            boolean residentStatusInvalid,
            boolean residentIsolated,
            boolean locationIsolated,
            boolean suspectedInfection,
            boolean openMediumOrHigher,
            boolean overlap,
            DayCare dayCare,
            Optional<String> careNote) {}

    /** Giờ buổi trong ngày và lịch đến của ngày đó ({@code arrival}, {@code departure} rỗng khi không có lịch). */
    public record DayCare(
            LocalTime sessionStart,
            LocalTime sessionEnd,
            LocalTime arrival,
            LocalTime departure,
            boolean lateReturnConsent) {}

    public record Result(List<UnmetCondition> blockers, List<UnmetCondition> warnings) {}

    public static Result check(Facts f) {
        var blockers = new ArrayList<UnmetCondition>();
        var warnings = new ArrayList<UnmetCondition>();
        if (!Session.SCHEDULED.equals(f.sessionStatus()))
            blockers.add(new UnmetCondition("STATE_CHANGED", "Buổi không ở Đã lên lịch"));
        else if (f.started()) blockers.add(new UnmetCondition("SESSION_ALREADY_STARTED", "Buổi đã bắt đầu"));
        if (f.activeRegistrations() >= f.maxParticipants())
            blockers.add(new UnmetCondition("CAPACITY_FULL", "Buổi đã đủ số lượng tối đa"));
        if (f.residentIsolated() || f.locationIsolated())
            blockers.add(
                    new UnmetCondition("ISOLATION_ACTIVE", "Người cao tuổi hoặc địa điểm thuộc vùng đang khoanh vùng"));
        if (f.residentStatusInvalid())
            blockers.add(new UnmetCondition(
                    "RESIDENT_STATUS_INVALID", "Người cao tuổi đang tiếp nhận hoặc ở trạng thái cuối"));
        if (f.overlap()) blockers.add(new UnmetCondition("TIME_OVERLAP", "Đã có đăng ký ở buổi khác chồng thời gian"));
        if (f.dayCare() != null) dayCare(f.dayCare(), blockers);
        if (f.suspectedInfection()) {
            if (f.groupOrTrip())
                blockers.add(new UnmetCondition(
                        "INFECTION_SUSPECTED",
                        "Người cao tuổi mang dấu nghi nhiễm, không vào hoạt động nhóm, chuyến đi"));
            else warnings.add(new UnmetCondition("INFECTION_SUSPECTED", "Người cao tuổi mang dấu nghi nhiễm"));
        }
        f.careNote()
                .filter(note -> !note.isBlank())
                .ifPresent(note -> warnings.add(new UnmetCondition("CARE_NOTE", "Lưu ý chăm sóc: " + note)));
        if (f.openMediumOrHigher())
            warnings.add(new UnmetCondition(
                    "OPEN_ALERT_OR_INCIDENT", "Có cảnh báo hoặc sự cố mức Trung bình trở lên đang mở"));
        return new Result(List.copyOf(blockers), List.copyOf(warnings));
    }

    private static void dayCare(DayCare d, List<UnmetCondition> blockers) {
        if (d.arrival() == null || d.departure() == null || d.sessionStart().isBefore(d.arrival()))
            blockers.add(new UnmetCondition(
                    "OUTSIDE_DAY_CARE_SCHEDULE", "Buổi ngoài lịch đến của người bán trú trong ngày"));
        else if (d.sessionEnd().isAfter(d.departure()) && !d.lateReturnConsent())
            blockers.add(new UnmetCondition(
                    "LATE_RETURN_CONSENT_REQUIRED", "Buổi kết thúc sau giờ về theo lịch, cần đồng ý về muộn"));
    }
}
