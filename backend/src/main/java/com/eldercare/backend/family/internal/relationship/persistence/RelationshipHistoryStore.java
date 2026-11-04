package com.eldercare.backend.family.internal.relationship.persistence;

import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.HistoryRow;
import com.eldercare.backend.administration.shared.api.dto.HistoryTrack;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.family.internal.relationship.domain.RelationshipAttribute;
import java.time.Instant;
import org.springframework.stereotype.Repository;

/**
 * Lịch sử quyền, vai trò theo quan hệ ({@code lich_su_quyen} → {@code status_history}, R-15): trả lời "tại thời điểm T"
 * (FR-068). Không có dòng nào trước T thì giá trị là tắt (FR-008: quyền mặc định tắt khi lập quan hệ).
 */
@Repository
public class RelationshipHistoryStore {

    private final StatusHistoryApi history;

    RelationshipHistoryStore(StatusHistoryApi history) {
        this.history = history;
    }

    public boolean valueAt(long relationshipId, RelationshipAttribute attribute, Instant at) {
        return history.at(track(attribute), relationshipId, at)
                .map(HistoryRow::value)
                .map(Boolean::parseBoolean)
                .orElse(false);
    }

    /** Ghi giá trị mới từ {@code at}; lệnh đổi quyền, vai trò gọi trong transaction của lệnh. */
    public void record(
            long relationshipId,
            RelationshipAttribute attribute,
            boolean value,
            Instant at,
            String command,
            Long actorId,
            Reason reason) {
        history.add(
                track(attribute),
                relationshipId,
                new HistoryEntry<>(
                        HistoryKind.POINT,
                        null,
                        Boolean.toString(value),
                        null,
                        at,
                        command,
                        actorId,
                        actorId == null,
                        reason,
                        null,
                        null,
                        null,
                        null,
                        null));
    }

    private static HistoryTrack<Void> track(RelationshipAttribute attribute) {
        return new HistoryTrack<>(RelationshipAttribute.ENTITY_TYPE, attribute.name(), Void.class);
    }
}
