package com.eldercare.backend.belongings.internal.ending.domain;

import com.eldercare.backend.belongings.api.dto.BelongingEndingConditionView;
import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Điều kiện (b) kết thúc lưu trú ({@code DieuKienDoGui}; 013 T024, FR-025, BR-M12-03, research K-08, Q-148): Đạt khi
 * người cao tuổi không còn đồ Đang giữ, Đang được sử dụng hoặc Hư hỏng. Đồ Thất lạc không làm Chưa đạt; đồ Thất lạc có sự
 * cố còn mở liệt kê riêng vì chặn qua điều kiện (d) của 004. Hàm thuần, tính lúc hỏi.
 */
public final class EndingConditionCalculator {

    private static final Set<BelongingStatus> BLOCKING =
            Set.of(BelongingStatus.HELD, BelongingStatus.IN_USE, BelongingStatus.DAMAGED);

    private EndingConditionCalculator() {}

    /** Một đồ chưa bị loại: trạng thái và, với đồ Thất lạc, sự cố của lần Báo thất lạc còn mở hay không. */
    public record Item(long id, BelongingStatus status, boolean lostIncidentOpen) {}

    public static BelongingEndingConditionView evaluate(List<Item> items) {
        List<Long> blocking = new ArrayList<>();
        List<Long> lostWithOpenIncident = new ArrayList<>();
        for (Item item : items) {
            if (BLOCKING.contains(item.status())) {
                blocking.add(item.id());
            } else if (item.status() == BelongingStatus.LOST && item.lostIncidentOpen()) {
                lostWithOpenIncident.add(item.id());
            }
        }
        return new BelongingEndingConditionView(blocking.isEmpty(), blocking, lostWithOpenIncident);
    }
}
