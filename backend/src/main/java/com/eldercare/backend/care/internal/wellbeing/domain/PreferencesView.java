package com.eldercare.backend.care.internal.wellbeing.domain;

import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/** FR-051: mục hiện hành và lịch sử giữ nguyên nguồn ghi. */
public record PreferencesView(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Item> current,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED) List<Item> history) {
    /** Schema {@code PreferenceView} của api-014; tên riêng vì {@code Item} trùng record của module khác. */
    @Schema(name = "PreferenceView")
    public record Item(
            long id,
            long interestGroupId,
            String description,
            String level,
            OpinionSource source,
            String status,
            long recordedBy,
            Instant recordedAt,
            Long endedBy,
            Instant endedAt,
            String endReason) {
        public static Item of(SimpleRecord<Preference> row) {
            var p = row.content();
            return new Item(
                    row.id(),
                    p.interestGroupId(),
                    p.description(),
                    p.level(),
                    p.source(),
                    row.status(),
                    row.createdBy(),
                    p.recordedAt(),
                    p.endedBy(),
                    p.endedAt(),
                    p.endReason());
        }
    }
}
