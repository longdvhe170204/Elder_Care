package com.eldercare.backend.billing.internal.item.domain;

import com.eldercare.backend.billing.api.dto.CostType;
import com.eldercare.backend.common.error.BusinessException;
import java.util.*;
import org.springframework.http.HttpStatus;

/** FR-015: chọn id hoặc bộ lọc; không có lựa chọn không được hiểu thành chọn toàn viện. */
public record CostItemSelection(List<Long> itemIds, Filter filter) {
    public CostItemSelection {
        itemIds = itemIds == null ? List.of() : itemIds;
        if (itemIds.isEmpty() == (filter == null) || itemIds.stream().anyMatch(id -> id == null || id <= 0))
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "CONDITION_NOT_MET",
                    "Chọn danh sách khoản hoặc bộ lọc của một bảng");
        if (filter != null && (filter.statementId() == null || filter.statementId() <= 0))
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "CONDITION_NOT_MET", "Cần chọn bảng chi phí");
        itemIds = itemIds.stream().distinct().sorted().toList();
    }

    public boolean individual() {
        return filter == null && itemIds.size() == 1;
    }

    public record Filter(Long statementId, CostType costType, String flags, String origin) {}
}
