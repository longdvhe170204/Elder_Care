package com.eldercare.backend.incident.internal.incident.service;

import static com.eldercare.backend.incident.internal.incident.domain.IncidentActions.FAMILY_NOTICE_RECORDED;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.FamilyNotice;
import com.eldercare.backend.incident.internal.incident.domain.FamilyNoticeStatus;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ghi nhận việc báo người nhà của một sự cố (BF-06): ai báo, lúc nào, cách nào. Người chịu trách nhiệm báo là người có
 * chuyên môn y tế phụ trách người cao tuổi (Bác sĩ, Điều dưỡng, Trưởng tầng hoặc người được giao thay); Nhân viên chăm sóc
 * và Hành chính không ghi (tình trạng sức khỏe do người có chuyên môn báo).
 */
@Service
public class IncidentFamilyNoticeService {
    static final Set<Role> NOTIFIER_ROLES = Set.of(Role.DOCTOR, Role.NURSING_STAFF, Role.FLOOR_MANAGER);

    private final IncidentStore incidents;
    private final PermissionApi permissions;
    private final AuditLogApi audit;
    private final BusinessTime time;
    private final AccountApi accounts;

    public IncidentFamilyNoticeService(
            IncidentStore incidents,
            PermissionApi permissions,
            AuditLogApi audit,
            BusinessTime time,
            AccountApi accounts) {
        this.accounts = accounts;
        this.incidents = incidents;
        this.permissions = permissions;
        this.audit = audit;
        this.time = time;
    }

    /**
     * Hạn báo người nhà theo mức (đề xuất BF-06, nhóm A và viện chỉnh): Khẩn cấp 30 phút, Trung bình 4 giờ, Nhẹ 24 giờ, tính
     * từ lúc ghi nhận.
     */
    static Duration dueAfter(com.eldercare.backend.incident.internal.alert.domain.IncidentLevel level) {
        return switch (level) {
            case CRITICAL -> Duration.ofMinutes(30);
            case MEDIUM -> Duration.ofHours(4);
            case LOW -> Duration.ofHours(24);
        };
    }

    @Transactional(readOnly = true)
    public FamilyNoticeStatus status(long incidentId, CurrentUser user) {
        var row = incidents.familyNoticeRow(incidentId).orElseThrow(NotFoundException::new);
        var view = permissions.hasPermission(
                user,
                new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.VIEW),
                new Target("M05.INCIDENT", Long.toString(incidentId), row.residentId(), null));
        if (!view.allowed()) {
            if (view.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xem sự cố");
        }
        Instant due = row.residentId() == null ? null : row.createdAt().plus(dueAfter(row.level()));
        // Không nhắc khi sự cố còn Mới, Đang xử lý (đang cứu người); chỉ nhắc khi chuyển theo dõi hoặc đã đóng (BF-06)
        boolean remindable = Set.of("MONITORING", "CLOSED").contains(row.status());
        boolean informed = FamilyNotice.INFORMED.equals(row.state());
        boolean overdue = remindable && !informed && due != null && time.now().isAfter(due);
        String by = row.notifiedBy() == null
                ? null
                : accounts.fullName(row.notifiedBy()).orElse(null);
        return new FamilyNoticeStatus(
                row.recordedLater(),
                row.state(),
                row.notifiedAt(),
                row.method(),
                row.note(),
                row.notifiedBy(),
                by,
                due,
                overdue);
    }

    @Transactional
    public void record(long incidentId, FamilyNotice notice, CurrentUser user) {
        var incident = incidents.lock(incidentId).orElseThrow(NotFoundException::new);
        var target =
                new Target("M05.INCIDENT", Long.toString(incidentId), incident.residentId(), incident.floorAreaId());
        var permission = permissions.hasPermission(
                user, new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE), target);
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền ghi nhận việc báo người nhà");
        }
        boolean byDuty = permission
                .basis()
                .map(b -> b.kind() == PermissionBasis.Kind.DUTY)
                .orElse(false);
        if (!byDuty && NOTIFIER_ROLES.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Chỉ Bác sĩ, Điều dưỡng, Trưởng tầng ghi nhận việc báo người nhà");
        }
        if (incident.residentId() == null) {
            throw new InvalidValueException("Sự cố không gắn người cao tuổi nên không có người nhà để báo");
        }
        if (notice == null
                || !FamilyNotice.INFORMED.equals(notice.state())
                || !FamilyNoticeMethods.VALID.contains(notice.method())) {
            throw new InvalidValueException(
                    "Cần ghi cách báo (gọi điện, trực tiếp, nhắn tin) của lần đã báo người nhà");
        }
        if (incidents.familyNoticeState(incidentId).equals(FamilyNotice.INFORMED)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "FAMILY_ALREADY_INFORMED", "Sự cố đã ghi nhận việc báo người nhà");
        }
        Instant now = time.now();
        Instant at = notice.at() == null ? now : notice.at();
        if (at.isAfter(now)) throw new InvalidValueException("Thời điểm báo người nhà không được sau hiện tại");
        incidents.saveFamilyNotice(
                incidentId,
                incidents.isRecordedLater(incidentId),
                FamilyNotice.INFORMED,
                user.accountId(),
                at,
                notice.method(),
                notice.note());
        audit.write(AuditLogEntry.builder()
                .action(FAMILY_NOTICE_RECORDED)
                .entity("M05.INCIDENT", incidentId)
                .resident(incident.residentId())
                .after(Map.of("method", notice.method(), "at", at.toString(), "by", user.accountId()))
                .build());
    }
}
