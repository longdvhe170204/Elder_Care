package com.eldercare.backend.administration.account.internal.scope.service;

import com.eldercare.backend.administration.account.api.events.PermissionDataError;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.account.internal.scope.domain.PermissionDataErrorContent;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lỗi dữ liệu nguồn của lần kiểm quyền (002 T024, T098; research A-10; FR-041b, CHK059) ở {@code simple_record} loại
 * {@code M15.PERMISSION_DATA_ERROR}. Ghi trong transaction riêng: lệnh bị từ chối rollback thì bản ghi lỗi, sự cố báo và
 * nhật ký vẫn còn. Lần đầu của một lỗi đang mở phát {@link PermissionDataError} (009 báo Quản lý viện), các lần sau chỉ
 * tăng bộ đếm. Đây là bản ghi kỹ thuật kèm lần kiểm quyền, không phải dữ liệu nghiệp vụ tự sinh.
 */
@Service
public class PermissionDataErrors {

    static final AuditAction DENIED_SOURCE_DATA = new AuditAction("PERMISSION_DENIED_SOURCE_DATA", false);

    private final SimpleRecordApi records;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final AuditLogApi auditLog;

    PermissionDataErrors(
            SimpleRecordApi records, BusinessTime time, ApplicationEventPublisher events, AuditLogApi auditLog) {
        this.records = records;
        this.time = time;
        this.events = events;
        this.auditLog = auditLog;
    }

    /**
     * Ghi một lần từ chối vì dữ liệu nguồn lỗi; người thực hiện trong nhật ký và người lập dòng lỗi là người dùng hiện
     * tại ({@code accountId}).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(InvalidPermissionDataException e, long accountId) {
        Instant now = time.now();
        String key = PermissionDataErrorContent.openKey(e.source(), e.recordId(), e.errorKind());
        Optional<SimpleRecord<PermissionDataErrorContent>> open =
                records.findByUniqueKey(PermissionDataErrorContent.KIND, key);
        long id;
        if (open.isEmpty()) {
            id = records.create(
                            PermissionDataErrorContent.KIND,
                            new NewSimpleRecord<>(
                                    null,
                                    null,
                                    PermissionDataErrorContent.OPEN,
                                    new PermissionDataErrorContent(
                                            e.source(), e.recordId(), e.errorKind(), now, now, 1, null),
                                    null,
                                    null,
                                    key,
                                    accountId,
                                    false))
                    .id();
            events.publishEvent(new PermissionDataError(id, e.source(), e.recordId(), e.errorKind(), now));
        } else {
            SimpleRecord<PermissionDataErrorContent> r = records.lockForUpdate(
                    PermissionDataErrorContent.KIND, open.get().id());
            id = r.id();
            records.change(
                    PermissionDataErrorContent.KIND,
                    id,
                    r.version(),
                    new RecordChange<>(r.status(), r.content().occurredAgain(now), null, key, false));
        }
        auditLog.write(AuditLogEntry.builder()
                .action(DENIED_SOURCE_DATA)
                .entity("M15.PERMISSION_DATA_ERROR", id)
                .details(Map.of("source", e.source(), "errorKind", e.errorKind()))
                .build());
    }

    /**
     * Lần kiểm tra đọc được dữ liệu hợp lệ của bản ghi: đánh dấu đã khắc phục các lỗi đang mở của nó và nhả khóa lỗi
     * đang mở. Transaction riêng như {@link #record}: lần kiểm phạm vi có thể chạy trong transaction chỉ đọc của nơi gọi
     * (tra cứu nhật ký, báo cáo).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFixed(long recordId) {
        Instant now = time.now();
        for (SimpleRecord<PermissionDataErrorContent> open :
                records.listByStatus(PermissionDataErrorContent.KIND, Set.of(PermissionDataErrorContent.OPEN))) {
            if (!Objects.equals(open.content().recordId(), recordId)) {
                continue;
            }
            SimpleRecord<PermissionDataErrorContent> r =
                    records.lockForUpdate(PermissionDataErrorContent.KIND, open.id());
            if (PermissionDataErrorContent.OPEN.equals(r.status())) {
                records.change(
                        PermissionDataErrorContent.KIND,
                        r.id(),
                        r.version(),
                        new RecordChange<>(
                                PermissionDataErrorContent.FIXED, r.content().fixed(now), null, null, false));
            }
        }
    }
}
