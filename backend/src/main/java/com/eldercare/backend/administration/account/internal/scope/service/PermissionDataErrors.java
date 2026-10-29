package com.eldercare.backend.administration.account.internal.scope.service;

import com.eldercare.backend.administration.account.api.events.PermissionDataError;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.common.time.BusinessTime;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lỗi dữ liệu nguồn của lần kiểm quyền (002 T024; research A-10; FR-041b, CHK059), bảng {@code permission_data_error}.
 * Ghi trong transaction riêng: lệnh bị từ chối rollback thì bản ghi lỗi, sự cố báo và nhật ký vẫn còn. Lần đầu của một
 * lỗi đang mở phát {@link PermissionDataError} (009 báo Quản lý viện), các lần sau chỉ tăng bộ đếm. Đây là bản ghi kỹ
 * thuật kèm lần kiểm quyền, không phải dữ liệu nghiệp vụ tự sinh.
 */
@Service
public class PermissionDataErrors {

    static final AuditAction DENIED_SOURCE_DATA = new AuditAction("PERMISSION_DENIED_SOURCE_DATA", false);

    private final JdbcTemplate jdbc;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final AuditLogApi auditLog;

    PermissionDataErrors(JdbcTemplate jdbc, BusinessTime time, ApplicationEventPublisher events, AuditLogApi auditLog) {
        this.jdbc = jdbc;
        this.time = time;
        this.events = events;
        this.auditLog = auditLog;
    }

    /** Ghi một lần từ chối vì dữ liệu nguồn lỗi; người thực hiện trong nhật ký là người dùng hiện tại. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(InvalidPermissionDataException e) {
        LocalDateTime now = LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC);
        List<Long> open = jdbc.queryForList(
                "SELECT id FROM permission_data_error WHERE source = ? AND record_id <=> ? AND error_kind = ?"
                        + " AND fixed_at IS NULL FOR UPDATE",
                Long.class,
                e.source(),
                e.recordId(),
                e.errorKind());
        long id;
        if (open.isEmpty()) {
            jdbc.update(
                    "INSERT INTO permission_data_error (source, record_id, error_kind, first_at, last_at)"
                            + " VALUES (?, ?, ?, ?, ?)",
                    e.source(),
                    e.recordId(),
                    e.errorKind(),
                    now,
                    now);
            id = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
            events.publishEvent(new PermissionDataError(id, e.source(), e.recordId(), e.errorKind(), time.now()));
        } else {
            id = open.getFirst();
            jdbc.update(
                    "UPDATE permission_data_error SET occurrences = occurrences + 1, last_at = ? WHERE id = ?",
                    now,
                    id);
        }
        auditLog.write(AuditLogEntry.builder()
                .action(DENIED_SOURCE_DATA)
                .entity("M15.PERMISSION_DATA_ERROR", id)
                .details(Map.of("source", e.source(), "errorKind", e.errorKind()))
                .build());
    }

    /**
     * Lần kiểm tra đọc được dữ liệu hợp lệ của bản ghi: đánh dấu đã khắc phục các lỗi đang mở của nó. Transaction riêng
     * như {@link #record}: lần kiểm phạm vi có thể chạy trong transaction chỉ đọc của nơi gọi (tra cứu nhật ký, báo cáo).
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFixed(long recordId) {
        jdbc.update(
                "UPDATE permission_data_error SET fixed_at = ? WHERE record_id = ? AND fixed_at IS NULL",
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC),
                recordId);
    }
}
