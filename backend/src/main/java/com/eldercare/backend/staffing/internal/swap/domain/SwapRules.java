package com.eldercare.backend.staffing.internal.swap.domain;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.shift.domain.ContinuousShiftChain.Interval;
import java.time.Duration;
import java.time.Instant;
import java.time.YearMonth;
import java.util.Collection;

/** Hàm thuần của đổi ca (FR-019, FR-021 (a), (g)); không đọc dữ liệu. */
public final class SwapRules {

    /** Hai khoảng giờ chồng nhau; chạm đầu cuối không tính (BR-M09-03). */
    public static boolean overlaps(Interval a, Interval b) {
        return a.from().isBefore(b.to()) && b.from().isBefore(a.to());
    }

    /** Có khoảng nào trong {@code existing} chồng {@code candidate}. */
    public static boolean anyOverlap(Collection<Interval> existing, Interval candidate) {
        return existing.stream().anyMatch(e -> overlaps(e, candidate));
    }

    /** Số giờ của các ca nằm trong tháng theo múi giờ cơ sở (DBR-25), cắt phần ngoài tháng. */
    public static Duration hoursInMonth(Collection<Interval> shifts, YearMonth month) {
        Instant from = month.atDay(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        Instant to =
                month.plusMonths(1).atDay(1).atStartOfDay(BusinessTime.ZONE).toInstant();
        Duration total = Duration.ZERO;
        for (Interval s : shifts) {
            Instant start = s.from().isAfter(from) ? s.from() : from;
            Instant end = s.to().isBefore(to) ? s.to() : to;
            if (end.isAfter(start)) {
                total = total.plus(Duration.between(start, end));
            }
        }
        return total;
    }

    private SwapRules() {}
}
