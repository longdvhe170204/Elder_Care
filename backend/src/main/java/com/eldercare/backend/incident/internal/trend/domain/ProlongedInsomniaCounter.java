package com.eldercare.backend.incident.internal.trend.domain;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BinaryOperator;
import java.util.stream.Collectors;

/**
 * Hàm thuần "DemDemMatNgu" (FR-039c, CHK045, research S-10): đếm đêm thuộc nhóm mất ngủ liên tiếp, lùi từ đêm của kết
 * quả vừa ghi. Mỗi ngày lấy kết quả hiện hành mới nhất; đêm không có ghi nhận hoặc khác nhóm làm đứt chuỗi; đêm vắng mặt
 * không tính và không làm đứt chuỗi.
 */
public final class ProlongedInsomniaCounter {
    /** Nhóm quy tắc "mất ngủ" của danh mục chất lượng ngủ 005 ({@code MAT_NGU}). */
    public static final String INSOMNIA = "INSOMNIA";

    private ProlongedInsomniaCounter() {}

    /**
     * Một kết quả ngủ hiện hành: {@code night} là ngày dương lịch của thời điểm dự kiến của công việc ghi nhận ngủ;
     * {@code group} là nhóm quy tắc của giá trị, rỗng nếu không thuộc nhóm nào.
     */
    public record SleepResult(LocalDate night, Instant recordedAt, String group) {}

    public static int consecutiveNights(LocalDate night, List<SleepResult> results, Set<LocalDate> absentNights) {
        BinaryOperator<SleepResult> latest =
                (a, b) -> Comparator.comparing(SleepResult::recordedAt).compare(a, b) >= 0 ? a : b;
        Map<LocalDate, SleepResult> byNight =
                results.stream().collect(Collectors.toMap(SleepResult::night, r -> r, latest));
        int count = 0;
        for (LocalDate day = night; ; day = day.minusDays(1)) {
            if (absentNights.contains(day)) continue;
            var group = Optional.ofNullable(byNight.get(day)).map(SleepResult::group);
            if (group.isEmpty() || !INSOMNIA.equals(group.get())) return count;
            count++;
        }
    }
}
