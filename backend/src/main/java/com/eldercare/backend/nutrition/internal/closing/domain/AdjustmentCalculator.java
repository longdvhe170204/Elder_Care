package com.eldercare.backend.nutrition.internal.closing.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Hàm thuần phát sinh (FR-039; research M-08; tên {@code TinhPhatSinh} trong tasks): so trạng thái suất hiện hành với trạng
 * thái mới của cùng các người, trả "+1", "−1" theo (tầng/khu, chế độ ăn) và đổi suất đặc biệt (số suất 0).
 */
public final class AdjustmentCalculator {

    public enum Change {
        ADDED,
        REMOVED,
        MOVED_OUT,
        MOVED_IN,
        PORTION_CHANGED
    }

    /** Một dòng phát sinh: {@code before} rỗng khi thêm, {@code after} rỗng khi bớt. */
    public record Delta(
            long floorAreaId,
            long dietId,
            int portionCount,
            Change change,
            CountedPerson before,
            CountedPerson after) {}

    private AdjustmentCalculator() {}

    /** Khóa của hai bản đồ là {@link CountedPerson#key()}; người vắng ở cả hai thì không xét. */
    public static List<Delta> diff(Map<String, CountedPerson> current, Map<String, CountedPerson> fresh) {
        List<Delta> out = new ArrayList<>();
        current.forEach((key, before) -> {
            CountedPerson after = fresh.get(key);
            if (after == null) {
                out.add(new Delta(before.floorAreaId(), before.dietId(), -1, Change.REMOVED, before, null));
            } else if (after.floorAreaId() != before.floorAreaId() || after.dietId() != before.dietId()) {
                out.add(new Delta(before.floorAreaId(), before.dietId(), -1, Change.MOVED_OUT, before, after));
                out.add(new Delta(after.floorAreaId(), after.dietId(), 1, Change.MOVED_IN, before, after));
            } else if (!before.sameServing(after)) {
                out.add(new Delta(after.floorAreaId(), after.dietId(), 0, Change.PORTION_CHANGED, before, after));
            }
        });
        fresh.forEach((key, after) -> {
            if (!current.containsKey(key)) {
                out.add(new Delta(after.floorAreaId(), after.dietId(), 1, Change.ADDED, null, after));
            }
        });
        return out;
    }
}
