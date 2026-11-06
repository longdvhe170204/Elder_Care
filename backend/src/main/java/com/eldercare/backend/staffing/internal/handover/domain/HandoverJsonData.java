package com.eldercare.backend.staffing.internal.handover.domain;

import java.time.Instant;

/** Dữ liệu JSON giữ đủ trường bảng con cũ; id mục chỉ có nghĩa trong bàn giao cha. */
public final class HandoverJsonData {
    private HandoverJsonData() {}

    public record Item(
            long id,
            String itemGroup,
            String sourceType,
            String sourceId,
            Long residentId,
            Long roomId,
            String description,
            String note,
            String noteAuthor,
            boolean resolved,
            String section,
            String statusAtFinalization,
            Instant createdAt) {
        public HandoverItem view(long parent) {
            return new HandoverItem(
                    id,
                    parent,
                    itemGroup,
                    sourceType,
                    sourceId,
                    residentId,
                    roomId,
                    description,
                    note,
                    resolved,
                    section,
                    statusAtFinalization,
                    noteAuthor);
        }
    }

    public record Assessment(String assessmentKind, Long residentId, long residentKey, String content) {}
}
