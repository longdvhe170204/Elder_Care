package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.care.api.dto.CarePlanItemView;
import java.util.*;
import org.junit.jupiter.api.Test;

/** T010: ghép theo id gốc,không báo đổi chỉ vì sao chép sang phiên bản mới. */
class FR_008_CarePlanComparisonTest {
    private CarePlanItemView item(long id, String content) {
        return BR_M04_25_CarePlanItemSequenceTest.item(id, content, "ONCE_PER_SHIFT", null, null, List.of(), null);
    }

    private Object compare(Map<Long, CarePlanItemView> before, Map<Long, CarePlanItemView> after) throws Exception {
        return Class.forName("com.eldercare.backend.care.internal.careplan.domain.CarePlanComparison")
                .getMethod("compare", Map.class, Map.class)
                .invoke(null, before, after);
    }

    private Object field(Object record, String name) throws Exception {
        return record.getClass().getMethod(name).invoke(record);
    }

    @Test
    void FR_008_copyWithNewIdButSameContentHasNoChanges() throws Exception {
        var result = compare(Map.of(7L, item(7, "Giữ nguyên")), Map.of(7L, item(90, "Giữ nguyên")));
        assertThat((List<?>) field(result, "changed")).isEmpty();
        assertThat((List<?>) field(result, "added")).isEmpty();
        assertThat((List<?>) field(result, "removed")).isEmpty();
    }

    @Test
    void FR_008_addedRemovedAndChangedFieldsAreSeparatedByLineage() throws Exception {
        var result = compare(
                Map.of(7L, item(7, "Cũ"), 8L, item(8, "Bỏ")), Map.of(7L, item(90, "Mới"), 9L, item(91, "Thêm")));
        assertThat((List<?>) field(result, "added")).hasSize(1);
        assertThat((List<?>) field(result, "removed")).hasSize(1);
        var changes = (List<?>) field(result, "changed");
        assertThat(changes).hasSize(1);
        assertThat(field(changes.getFirst(), "changedFields")).isEqualTo(List.of("content"));
    }
}
