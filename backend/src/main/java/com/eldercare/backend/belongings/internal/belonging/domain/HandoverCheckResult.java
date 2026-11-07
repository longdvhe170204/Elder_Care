package com.eldercare.backend.belongings.internal.belonging.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.util.List;

/**
 * Kết quả kiểm một lệnh bàn giao: loại thực hiện (Ghi hư hỏng khi người ghi chọn ở FR-015), trạng thái đích, mã lỗi
 * (glossary 19.4; rỗng là được).
 */
public record HandoverCheckResult(HandoverType effectiveType, BelongingStatus target, List<String> violations) {

    public HandoverCheckResult {
        violations = List.copyOf(violations);
    }

    public boolean allowed() {
        return violations.isEmpty();
    }
}
