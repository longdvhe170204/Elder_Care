package com.cambae.backend.m13.internal.intake.service;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.common.notification.NotificationRequest;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m13.internal.intake.domain.RequestValidation;
import com.cambae.backend.m13.internal.intake.domain.ValidatedRequest;
import com.cambae.backend.m13.internal.intake.persistence.NotificationRequestStore;
import com.cambae.backend.m13.internal.recipient.service.NotificationCreation;
import com.cambae.backend.m13.internal.recipient.service.RecipientResolver.Resolution;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tiếp nhận yêu cầu thông báo (FR-001 → FR-006; research B-02, B-03): kiểm tra, chống trùng theo (bản ghi nguồn, khóa
 * sự kiện), lưu kèm thời điểm tiếp nhận. Chạy sau commit của lệnh nguồn nên lỗi ở đây không làm hỏng lệnh nguồn.
 */
@Service
public class NotificationIntake {

    private static final Logger log = LoggerFactory.getLogger(NotificationIntake.class);

    private final NotificationRequestStore store;
    private final BusinessTime time;
    private final NotificationCreation creation;

    NotificationIntake(NotificationRequestStore store, BusinessTime time, NotificationCreation creation) {
        this.store = store;
        this.time = time;
        this.creation = creation;
    }

    /** Tiếp nhận mọi yêu cầu của một sự kiện nguồn; trả id các yêu cầu mới (yêu cầu trùng không có trong kết quả). */
    @Transactional
    public List<Long> receive(HasNotificationRequests event) {
        Instant now = time.now();
        List<NotificationRequest> requests;
        try {
            requests = event.notificationRequests();
        } catch (RuntimeException e) {
            // Module nguồn dựng yêu cầu lỗi: không thử lại được, ghi vào danh sách lỗi gửi (FR-006)
            log.warn(
                    "Sự kiện {} không dựng được yêu cầu thông báo",
                    event.getClass().getName(),
                    e);
            store.insertBuildFailure(event.getClass().getName(), now);
            return List.of();
        }
        if (requests == null) {
            return List.of();
        }
        return requests.stream()
                .map(r -> receive(r, now))
                .flatMap(Optional::stream)
                .toList();
    }

    private Optional<Long> receive(NotificationRequest r, Instant now) {
        ValidatedRequest v = RequestValidation.validate(r);
        if (v.rejectionReason().isPresent()) {
            return Optional.of(store.insert(v, now));
        }
        Optional<Long> existing =
                store.findId(r.sourceRecord().type(), r.sourceRecord().id(), r.eventKey());
        if (existing.isPresent()) {
            store.insertDuplicate(existing.get(), now);
            return Optional.empty();
        }
        Resolution resolution = creation.resolve(v, now);
        long id;
        try {
            id = store.insert(NotificationCreation.withFlags(v, resolution), resolution.groupOutcomes(), now);
        } catch (DuplicateKeyException e) {
            // Hai lần nhận đồng thời: lần kia đã commit trước (FR-005)
            long first = store.findIdLatest(
                            r.sourceRecord().type(), r.sourceRecord().id(), r.eventKey())
                    .orElseThrow(() -> e);
            store.insertDuplicate(first, now);
            return Optional.empty();
        }
        creation.create(id, resolution, v, now);
        return Optional.of(id);
    }
}
