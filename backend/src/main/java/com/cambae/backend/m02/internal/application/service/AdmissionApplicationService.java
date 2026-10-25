package com.cambae.backend.m02.internal.application.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.VersionMismatchException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.ResidentStatus;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.AdmissionOpinion;
import com.cambae.backend.m02.internal.application.domain.ApplicationStatus;
import com.cambae.backend.m02.internal.application.persistence.AdmissionApplicationStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lượt đăng ký tiếp nhận (004 US1; FR-001, FR-002, FR-006, BR-M02-15): Hành chính lập khi người cao tuổi Đang tiếp
 * nhận, mỗi người một lượt mở; chỉ sửa bằng lệnh Cập nhật kèm lý do và {@code If-Match}. Mọi lệnh khóa {@code
 * resident_residency} (research T-03). Chuyển Đang chờ, Đã tiếp nhận, Đã hủy do hồ sơ chờ và lệnh trạng thái của 001
 * thực hiện (không ở lớp này).
 */
@Service
public class AdmissionApplicationService {

    public static final String ENTITY_TYPE = "M02.ADMISSION_APPLICATION";

    static final AuditAction CREATED = new AuditAction("M02_APPLICATION_CREATED", false);
    static final AuditAction UPDATED = new AuditAction("M02_APPLICATION_UPDATED", true);

    private static final int PAGE = 50;

    /** Lượt kèm ý kiến về việc vào ở (hiện hành trước rồi lịch sử). */
    public record Detail(AdmissionApplication application, List<AdmissionOpinion> opinions) {}

    private final AdmissionApplicationStore store;
    private final ResidencyLock residencyLock;
    private final ResidentApi residents;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    AdmissionApplicationService(
            AdmissionApplicationStore store,
            ResidencyLock residencyLock,
            ResidentApi residents,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.residents = residents;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
    }

    public static Target target(long applicationId, long residentId) {
        return new Target(ENTITY_TYPE, String.valueOf(applicationId), residentId, null);
    }

    /** Đối tượng kiểm quyền của lệnh lập: người cao tuổi, chưa có lượt. */
    public static Target targetForResident(long residentId) {
        return new Target(ENTITY_TYPE, null, residentId, null);
    }

    /** Đối tượng kiểm quyền của một lượt; không có lượt thì 404. */
    @Transactional(readOnly = true)
    public Target targetOf(long applicationId) {
        AdmissionApplication application = store.find(applicationId).orElseThrow(NotFoundException::new);
        return target(application.id(), application.residentId());
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.CREATE)
    @Transactional
    public AdmissionApplication create(Target target, AdmissionApplication.Content content) {
        long residentId = target.residentId();
        residencyLock.lock(residentId);
        ResidentStatus status = residents.status(residentId);
        if (status != ResidentStatus.ADMISSION_IN_PROGRESS) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "RESIDENT_NOT_ADMISSION_IN_PROGRESS",
                    "Chỉ lập lượt đăng ký cho người cao tuổi Đang tiếp nhận (FR-001)");
        }
        if (store.findOpen(residentId).isPresent()) {
            throw alreadyExists();
        }
        CurrentUser user = currentUser.getObject();
        long id;
        try {
            id = store.insert(residentId, content, user.accountId(), time.now());
        } catch (DuplicateKeyException e) {
            throw alreadyExists();
        }
        AdmissionApplication created = store.find(id).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY_TYPE, id)
                .resident(residentId)
                .after(snapshot(created))
                .build());
        return created;
    }

    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public Detail get(Target target, long applicationId) {
        AdmissionApplication application = store.find(applicationId).orElseThrow(NotFoundException::new);
        return new Detail(application, store.opinions(applicationId));
    }

    /**
     * Danh sách cho Hành chính, Quản lý viện (FR-006), mã giảm dần; {@code cursor} là mã của dòng cuối trang trước (hợp
     * đồng api-004 trả mảng trơn nên không có con trỏ kế).
     */
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<AdmissionApplication> list(Target target, Set<ApplicationStatus> statuses, String cursor) {
        return store.list(statuses, parseCursor(cursor), PAGE);
    }

    /**
     * Cập nhật nội dung (FR-001): chỉ khi Đang xử lý hoặc Đang chờ; ngày đăng ký không đổi khi đã có hồ sơ chờ, tức lượt
     * Đang chờ (BR-M02-15); {@code If-Match} sai thì 412.
     */
    @BusinessCommand
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.UPDATE)
    @Transactional
    public AdmissionApplication update(
            Target target, long applicationId, String ifMatch, AdmissionApplication.Content content, Reason reason) {
        AdmissionApplication found = store.find(applicationId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        AdmissionApplication current = store.lock(applicationId).orElseThrow(NotFoundException::new);
        if (!current.status().isOpen()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "APPLICATION_CLOSED",
                    "Lượt đăng ký đã đóng (Đã tiếp nhận hoặc Đã hủy), không sửa được (FR-001)");
        }
        VersionEtag.requireMatch(ifMatch, current.version(), current);
        if (current.status() == ApplicationStatus.WAITING
                && !current.applicationDate().equals(content.applicationDate())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "APPLICATION_DATE_LOCKED",
                    "Ngày đăng ký không đổi được khi đã có hồ sơ chờ (BR-M02-15)");
        }
        if (store.updateContent(applicationId, current.version(), content) == 0) {
            throw new VersionMismatchException(current);
        }
        AdmissionApplication updated = store.find(applicationId).orElseThrow();
        auditLog.write(AuditLogEntry.builder()
                .action(UPDATED)
                .entity(ENTITY_TYPE, applicationId)
                .resident(current.residentId())
                .before(snapshot(current))
                .after(snapshot(updated))
                .reason(reason)
                .build());
        return updated;
    }

    static Map<String, Object> snapshot(AdmissionApplication application) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("residentId", application.residentId());
        map.put("applicationDate", String.valueOf(application.applicationDate()));
        map.put("contactPerson", application.contactPerson());
        map.put("needs", application.needs());
        map.put("desiredResidenceType", application.desiredResidenceType().name());
        map.put("expectedCareLevel", application.expectedCareLevel().name());
        map.put(
                "desiredAdmissionDate",
                application.desiredAdmissionDate() == null
                        ? null
                        : application.desiredAdmissionDate().toString());
        map.put("note", application.note());
        map.put("status", application.status().name());
        return map;
    }

    private static BusinessException alreadyExists() {
        return new BusinessException(
                HttpStatus.CONFLICT,
                "APPLICATION_ALREADY_EXISTS",
                "Người cao tuổi đã có lượt đăng ký Đang xử lý hoặc Đang chờ (FR-001)");
    }

    private static Long parseCursor(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(cursor);
        } catch (NumberFormatException e) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "INVALID_CURSOR", "Con trỏ trang không hợp lệ");
        }
    }
}
