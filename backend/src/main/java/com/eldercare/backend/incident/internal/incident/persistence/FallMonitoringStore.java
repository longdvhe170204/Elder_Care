package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.incident.internal.incident.domain.FallMonitoringContent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Repository;

/**
 * Khoảng theo dõi sau ngã trong hồ sơ chung: mỗi người tối đa một khoảng Đang theo dõi, giữ bằng khóa duy nhất
 * (FR-052). Lần ngã mới thay khoảng cũ ({@code REPLACED}) và nhả khóa.
 */
@Repository
public class FallMonitoringStore {
    public static final String ACTIVE = "ACTIVE";
    public static final String REPLACED = "REPLACED";
    public static final String ENDED_EARLY = "ENDED_EARLY";
    public static final String EXPIRED = "EXPIRED";
    static final SharedKind<FallMonitoringContent> KIND =
            SharedKind.of("M05.POST_FALL_MONITORING", FallMonitoringContent.class);

    private final SimpleRecordApi records;

    public FallMonitoringStore(SimpleRecordApi records) {
        this.records = records;
    }

    /** Mở khoảng mới cho lần ngã; gọi sau khi khóa theo dõi người cao tuổi. */
    public long start(long residentId, long incidentId, Instant anchor, Instant endsAt, Duration interval, long actor) {
        active(residentId)
                .ifPresent(current -> records.change(
                        KIND,
                        current.id(),
                        current.version(),
                        new RecordChange<>(REPLACED, current.content(), null, null, true)));
        return records.create(
                        KIND,
                        new NewSimpleRecord<>(
                                residentId,
                                new EntityRef("M05.INCIDENT", incidentId),
                                ACTIVE,
                                new FallMonitoringContent(incidentId, anchor, endsAt, interval.toMinutes(), null, null),
                                null,
                                null,
                                key(residentId),
                                actor,
                                false))
                .id();
    }

    public Optional<SimpleRecord<FallMonitoringContent>> active(long residentId) {
        return records.findByUniqueKey(KIND, key(residentId));
    }

    /** Khóa dòng khoảng đang theo dõi tới hết transaction. */
    public Optional<SimpleRecord<FallMonitoringContent>> lockActive(long residentId) {
        return active(residentId).map(current -> records.lockForUpdate(KIND, current.id()));
    }

    /** Đóng khoảng ở trạng thái cuối và nhả khóa duy nhất của người. */
    public void finish(SimpleRecord<FallMonitoringContent> current, String status, FallMonitoringContent content) {
        records.change(KIND, current.id(), current.version(), new RecordChange<>(status, content, null, null, true));
    }

    /** Các khoảng đang theo dõi đã quá {@code endsAt}. */
    public List<SimpleRecord<FallMonitoringContent>> dueForExpiry(Instant now, int limit) {
        return records.listByStatus(KIND, Set.of(ACTIVE)).stream()
                .filter(window -> !window.content().endsAt().isAfter(now))
                .limit(limit)
                .toList();
    }

    /** Mọi khoảng đang theo dõi sau ngã, cho thống kê của 016 (FR-081). */
    public List<SimpleRecord<FallMonitoringContent>> activeWindows() {
        return records.listByStatus(KIND, Set.of(ACTIVE));
    }

    private static String key(long residentId) {
        return "POST_FALL:" + residentId;
    }
}
