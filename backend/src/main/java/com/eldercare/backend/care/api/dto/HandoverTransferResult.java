package com.eldercare.backend.care.api.dto;

import java.util.List;

/** Kết quả lệnh FR-047/047a thuộc 005. */
public record HandoverTransferResult(List<Long> closedTaskIds, List<Long> transferredTaskIds) {
    public HandoverTransferResult {
        closedTaskIds = List.copyOf(closedTaskIds);
        transferredTaskIds = List.copyOf(transferredTaskIds);
    }
}
