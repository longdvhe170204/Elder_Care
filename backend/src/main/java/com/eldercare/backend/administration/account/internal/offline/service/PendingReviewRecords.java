package com.eldercare.backend.administration.account.internal.offline.service;

import com.eldercare.backend.administration.account.internal.offline.domain.PendingReason;
import com.eldercare.backend.administration.account.internal.offline.domain.PendingReviewContent;
import com.eldercare.backend.administration.account.internal.offline.domain.PendingReviewRecord;
import com.eldercare.backend.administration.account.internal.offline.domain.ReviewStatus;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.error.NotFoundException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * Bản ghi ngoại tuyến chờ xem lại ở {@code simple_record} loại {@code M15.PENDING_REVIEW_RECORD} (002 T055, T098;
 * data-model mục 0.1; FR-041c): chuyển trạng thái một lần ({@code PENDING_REVIEW} → {@code ACCEPTED} /
 * {@code REJECTED}), dòng đã quyết định khóa; quyết định đầu tiên thắng nhờ khóa dòng và version (000 FR-040).
 */
@Component
class PendingReviewRecords {

    private final SimpleRecordApi records;
    private final JsonMapper json;

    PendingReviewRecords(SimpleRecordApi records, JsonMapper json) {
        this.records = records;
        this.json = json;
    }

    long insert(
            String recordType,
            String content,
            String idempotencyKey,
            long recorderId,
            String deviceId,
            Instant deviceTime,
            Instant syncedAt,
            Instant lastConnectedAt,
            PendingReason reason,
            Long floorAreaId) {
        PendingReviewContent c = new PendingReviewContent(
                recordType,
                json.readTree(content),
                idempotencyKey,
                recorderId,
                deviceId,
                deviceTime,
                syncedAt,
                lastConnectedAt,
                reason,
                floorAreaId,
                null,
                null,
                null,
                null);
        return records.create(
                        PendingReviewContent.KIND,
                        new NewSimpleRecord<>(
                                null,
                                null,
                                ReviewStatus.PENDING_REVIEW.name(),
                                c,
                                null,
                                null,
                                PendingReviewContent.uniqueKey(recorderId, idempotencyKey),
                                recorderId,
                                false))
                .id();
    }

    Optional<PendingReviewRecord> find(long id) {
        try {
            return Optional.of(record(records.require(PendingReviewContent.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Khóa dòng cho quyết định: quyết định đầu tiên thắng (000 FR-040). */
    Optional<PendingReviewRecord> lock(long id) {
        try {
            return Optional.of(record(records.lockForUpdate(PendingReviewContent.KIND, id)));
        } catch (NotFoundException e) {
            return Optional.empty();
        }
    }

    /** Bản ghi đang chờ có id lớn hơn {@code afterId}, theo id tăng dần. */
    List<PendingReviewRecord> pendingAfter(long afterId) {
        return records.listByStatus(PendingReviewContent.KIND, Set.of(ReviewStatus.PENDING_REVIEW.name())).stream()
                .filter(r -> r.id() > afterId)
                .map(this::record)
                .toList();
    }

    /** Quyết định trên dòng đã khóa ({@link #lock}); dòng khóa luôn, không đổi được nữa. */
    void decide(long id, ReviewStatus status, long decidedBy, Instant at, String rejectionReason, String reference) {
        SimpleRecord<PendingReviewContent> r = records.require(PendingReviewContent.KIND, id);
        records.change(
                PendingReviewContent.KIND,
                id,
                r.version(),
                new RecordChange<>(
                        status.name(),
                        r.content().decided(decidedBy, at, rejectionReason, reference),
                        null,
                        r.uniqueKey(),
                        true));
    }

    private PendingReviewRecord record(SimpleRecord<PendingReviewContent> r) {
        PendingReviewContent c = r.content();
        return new PendingReviewRecord(
                r.id(),
                c.recordType(),
                json.writeValueAsString(c.content()),
                c.idempotencyKey(),
                c.recorderId(),
                c.deviceId(),
                c.deviceTime(),
                c.syncedAt(),
                c.lastConnectedAt(),
                c.pendingReason(),
                c.floorAreaId(),
                ReviewStatus.valueOf(r.status()),
                c.decidedBy(),
                c.decidedAt(),
                c.rejectionReason(),
                c.resultReference(),
                r.version());
    }
}
