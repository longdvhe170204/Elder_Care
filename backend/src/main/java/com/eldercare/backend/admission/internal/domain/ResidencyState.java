package com.eldercare.backend.admission.internal.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

/**
 * Trạng thái lưu trú theo người ở {@code resident_module_state} (loại {@code M02.RESIDENCY}; data-model 004 mục 0, 2, 10; R-15):
 * ngày Hoàn tất tiếp nhận (đặt một lần), dấu hồ sơ lưu trú đã đóng (FR-072, đặt một lần, không gỡ) và kết quả tính của danh sách
 * việc sau qua đời.
 */
public record ResidencyState(LocalDate admittedOn, Instant closedAt, Map<String, PostDeathItem> postDeathItems) {

    public static final SharedKind<ResidencyState> KIND = SharedKind.of("M02.RESIDENCY", ResidencyState.class);

    /** Dòng do khóa theo người tạo lần đầu có dữ liệu rỗng. */
    public ResidencyState {
        postDeathItems = postDeathItems == null ? Map.of() : postDeathItems;
    }

    /** Kết quả tính của một mục việc sau qua đời (FR-071; research T-13). */
    public record PostDeathItem(String status, String basis, Instant updatedAt) {}

    public static ResidencyState empty() {
        return new ResidencyState(null, null, Map.of());
    }

    public ResidencyState withAdmittedOn(LocalDate on) {
        return new ResidencyState(on, closedAt, postDeathItems);
    }

    public ResidencyState withClosedAt(Instant at) {
        return new ResidencyState(admittedOn, at, postDeathItems);
    }

    public ResidencyState withPostDeathItems(Map<String, PostDeathItem> items) {
        return new ResidencyState(admittedOn, closedAt, items);
    }
}
