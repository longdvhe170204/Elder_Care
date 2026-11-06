package com.eldercare.backend.family.internal.visit.domain;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Kiểm tra đăng ký thăm ({@code KiemTraDangKyTham}; research F-08; FR-029, FR-030 (a) → (e), BR-M10-02): hàm thuần, trả
 * mọi lý do theo thứ tự (a), (b), (c), (d), số người, (e). Rỗng là đạt. Dữ liệu (khoanh vùng, có mặt, số người đã giữ
 * chỗ) do lớp dịch vụ đọc từ nguồn.
 */
public final class VisitRegistrationCheck {

    public static final ZoneId ZONE = BusinessTime.ZONE;

    public enum Reason {
        OUTSIDE_VISIT_SLOT("Ngoài khung giờ thăm"),
        VISIT_SLOT_FULL("Khung giờ đã đủ sức chứa"),
        ISOLATION_ACTIVE("Khu đang khoanh vùng"),
        RESIDENT_NOT_PRESENT("Người cao tuổi không có mặt tại viện"),
        TOO_MANY_VISITORS("Vượt số người tối đa mỗi lượt thăm"),
        OUTSIDE_REGISTRATION_WINDOW("Ngoài thời hạn đăng ký");

        private final String description;

        Reason(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }
    }

    /**
     * @param visitorCount người đăng ký cộng người đi cùng
     * @param counter đăng ký tại quầy của Hành chính
     */
    public record Request(LocalDate date, LocalTime start, LocalTime end, int visitorCount, boolean counter) {}

    /**
     * @param slots khung của CFG-M10-04
     * @param capacity sức chứa mỗi khung (CFG-M10-04)
     * @param reserved số người của các lượt Đã duyệt, Đã vào trong khung
     * @param maxVisitors CFG-M10-06
     * @param minLead, maxLead CFG-M10-05
     * @param isolated người cao tuổi đang chịu khoanh vùng (FR-030 (c))
     * @param present đạt FR-030 (d)
     */
    public record Facts(
            List<VisitSlot> slots,
            int capacity,
            int reserved,
            int maxVisitors,
            Duration minLead,
            Duration maxLead,
            boolean isolated,
            boolean present,
            Instant now) {}

    private VisitRegistrationCheck() {}

    public static List<Reason> check(Request request, Facts facts) {
        List<Reason> reasons = new ArrayList<>();
        VisitSlot slot = new VisitSlot(request.start(), request.end());
        boolean inSlots = facts.slots().contains(slot);
        if (!inSlots) {
            reasons.add(Reason.OUTSIDE_VISIT_SLOT);
        } else if (facts.reserved() + request.visitorCount() > facts.capacity()) {
            reasons.add(Reason.VISIT_SLOT_FULL);
        }
        if (facts.isolated()) {
            reasons.add(Reason.ISOLATION_ACTIVE);
        }
        if (!facts.present()) {
            reasons.add(Reason.RESIDENT_NOT_PRESENT);
        }
        if (request.visitorCount() > facts.maxVisitors()) {
            reasons.add(Reason.TOO_MANY_VISITORS);
        }
        Instant start = slot.startOn(request.date());
        boolean currentAtCounter =
                request.counter() && !facts.now().isBefore(start) && facts.now().isBefore(slot.endOn(request.date()));
        if (!currentAtCounter) {
            Duration lead = Duration.between(facts.now(), start);
            if (lead.compareTo(facts.minLead()) < 0 || lead.compareTo(facts.maxLead()) > 0) {
                reasons.add(Reason.OUTSIDE_REGISTRATION_WINDOW);
            }
        }
        return List.copyOf(reasons);
    }
}
