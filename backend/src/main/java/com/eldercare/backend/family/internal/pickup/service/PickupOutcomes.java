package com.eldercare.backend.family.internal.pickup.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.common.error.AlreadyProcessedException;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.family.internal.pickup.domain.PickupOutcome;
import com.eldercare.backend.family.internal.pickup.domain.PickupRecord;
import com.eldercare.backend.family.internal.pickup.persistence.PickupRecordStore;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Kết quả của bản ghi đón (FR-017, FR-026, FR-027; BR-M10-03): Đã dùng trong transaction của lệnh nguồn, Không dùng khi
 * bỏ người đón, tắt dấu tự về, quá CFG-M10-09. Mỗi bản ghi một kết quả (khóa {@link PickupOutcome#key}); dùng lần hai,
 * dùng bản ghi Không dùng hoặc quá hạn trả 409 {@code PICKUP_RECORD_ALREADY_USED}. Không khóa người cao tuổi của M10 ở
 * đây: lệnh nguồn của 004, 005 đã giữ khóa của module đó, khóa thêm theo thứ tự ngược với lệnh Viện đưa về sẽ tắc.
 */
@Component
class PickupOutcomes {

    private final PickupRecordStore store;
    private final RecordEventApi recordEvents;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    PickupOutcomes(
            PickupRecordStore store,
            RecordEventApi recordEvents,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.recordEvents = recordEvents;
        this.time = time;
        this.currentUser = currentUser;
    }

    PickupRecord.Status status(long recordId) {
        return recordEvents
                .findByUniqueKey(PickupOutcome.KIND, PickupOutcome.key(recordId))
                .map(e -> e.content().status())
                .orElse(PickupRecord.Status.UNUSED);
    }

    void markUsed(long recordId, String sourceCommand, long sourceId) {
        PickupRecord record = store.find(recordId).orElseThrow(NotFoundException::new);
        Instant now = time.now();
        if (!record.usableAt(now, status(recordId))) {
            throw alreadyUsed();
        }
        try {
            add(record, new PickupOutcome(PickupOutcome.Kind.USED, sourceCommand, sourceId, null), actor(), now);
        } catch (AlreadyProcessedException e) {
            throw alreadyUsed();
        }
    }

    /** Ghi Không dùng cho các bản ghi chưa có kết quả; bỏ qua bản ghi đã có (đã kiểm trong cùng khóa). */
    int notUsed(List<PickupRecord> pending, String reason, Instant at) {
        int done = 0;
        for (PickupRecord record : pending) {
            if (status(record.id()) != PickupRecord.Status.UNUSED) {
                continue;
            }
            add(record, new PickupOutcome(PickupOutcome.Kind.NOT_USED, null, null, reason), null, at);
            done++;
        }
        return done;
    }

    /** Người của lệnh nguồn; không có phiên (job của module nguồn) thì ghi là hệ thống. */
    private Long actor() {
        CurrentUser user = currentUser.getIfAvailable();
        if (user == null) {
            return null;
        }
        try {
            return user.accountId();
        } catch (IllegalStateException e) {
            return null;
        }
    }

    private void add(PickupRecord record, PickupOutcome outcome, Long actorId, Instant at) {
        recordEvents.add(
                PickupOutcome.KIND,
                new EntityRef(PickupRecord.ENTITY_TYPE, record.id()),
                new NewRecordEvent<>(
                        outcome, null, actorId, actorId == null, at, null, at, null, PickupOutcome.key(record.id())));
    }

    private static BusinessException alreadyUsed() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "PICKUP_RECORD_ALREADY_USED",
                "Bản ghi đón đã dùng, không dùng hoặc đã hết hiệu lực");
    }
}
