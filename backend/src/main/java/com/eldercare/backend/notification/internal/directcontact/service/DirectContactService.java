package com.eldercare.backend.notification.internal.directcontact.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.notification.internal.directcontact.domain.DirectContact;
import com.eldercare.backend.notification.internal.directcontact.persistence.DirectContactStore;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách "cần liên hệ trực tiếp" và ghi kết quả liên hệ (FR-021, FR-031). Quyền theo bảng FR-039 (Phụ lục 27 không có
 * dòng thông báo): Hành chính thực hiện, Quản lý viện xem; tự kiểm theo vai trò.
 */
@Service
public class DirectContactService {

    public enum Result {
        CONTACTED,
        UNREACHABLE
    }

    static final AuditAction RECORDED = new AuditAction("DIRECT_CONTACT_RECORDED", false);
    static final String ENTITY = "M13.DIRECT_CONTACT";
    private static final int LIMIT = 500;

    private final DirectContactStore contacts;
    private final ObjectProvider<CurrentUser> currentUser;
    private final AuditLogApi auditLog;
    private final BusinessTime time;

    DirectContactService(
            DirectContactStore contacts,
            ObjectProvider<CurrentUser> currentUser,
            AuditLogApi auditLog,
            BusinessTime time) {
        this.contacts = contacts;
        this.currentUser = currentUser;
        this.auditLog = auditLog;
        this.time = time;
    }

    @Transactional(readOnly = true)
    public List<DirectContact> list(Optional<String> status) {
        CurrentUser user = currentUser.getObject();
        if (!user.roles().contains(Role.OFFICE_STAFF) && !user.roles().contains(Role.NURSING_HOME_MANAGER)) {
            throw new PermissionDeniedException(
                    "Chỉ Hành chính, Quản lý viện xem danh sách liên hệ trực tiếp (FR-039)");
        }
        return contacts.list(status, LIMIT);
    }

    /** Ghi kết quả liên hệ; Không liên hệ được bắt buộc lý do; mục đã có kết quả thì 409 {@code FINAL_STATE}. */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void record(long id, Result result, Instant occurredAt, Optional<String> reason, Optional<String> note) {
        CurrentUser user = currentUser.getObject();
        if (!user.roles().contains(Role.OFFICE_STAFF)) {
            throw new PermissionDeniedException("Chỉ Hành chính ghi kết quả liên hệ trực tiếp (FR-039)");
        }
        Optional<Reason> why = reason.filter(r -> !r.isBlank()).map(Reason::of);
        if (result == Result.UNREACHABLE && why.isEmpty()) {
            throw new ReasonRequiredException();
        }
        DirectContact row = contacts.lock(id).orElseThrow(NotFoundException::new);
        if (!"PENDING".equals(row.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "FINAL_STATE", "Mục liên hệ đã có kết quả");
        }
        contacts.insertResult(
                id,
                user.accountId(),
                occurredAt,
                result.name(),
                why.map(Reason::value).orElse(null),
                note.orElse(null),
                time.now());
        Map<String, Object> after = new HashMap<>();
        after.put("status", result.name());
        after.put("occurredAt", occurredAt.toString());
        note.ifPresent(n -> after.put("note", n));
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY, id)
                .resident(row.residentId())
                .before(Map.of("status", row.status()))
                .after(after);
        why.ifPresent(entry::reason);
        auditLog.write(entry.build());
    }
}
