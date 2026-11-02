package com.eldercare.backend.admission.internal.application.service;

import static com.eldercare.backend.admission.internal.application.service.AdmissionApplicationService.ENTITY_TYPE;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.internal.application.domain.AdmissionApplication;
import com.eldercare.backend.admission.internal.application.domain.AdmissionOpinion;
import com.eldercare.backend.admission.internal.application.domain.OpinionChoice;
import com.eldercare.backend.admission.internal.application.persistence.AdmissionApplicationStore;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.OpinionSourceApi;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ý kiến của người cao tuổi về việc vào ở (004 US1; FR-001, FR-004a, Q-356; research T-09a). Mỗi lần ghi một dòng; ý
 * kiến Hiệu lực cũ chuyển Được thay thế trong cùng transaction, dưới khóa theo người. Nguồn ý kiến do
 * 001 kiểm (BR-M01-14). Ý kiến không phải điều kiện tiếp nhận.
 */
@Service
public class AdmissionOpinionService {

    static final AuditAction RECORDED = new AuditAction("M02_APPLICATION_OPINION", false);

    private final AdmissionApplicationStore store;
    private final ResidencyLock residencyLock;
    private final OpinionSourceApi opinionSources;
    private final AuditLogApi auditLog;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> currentUser;

    AdmissionOpinionService(
            AdmissionApplicationStore store,
            ResidencyLock residencyLock,
            OpinionSourceApi opinionSources,
            AuditLogApi auditLog,
            BusinessTime time,
            ObjectProvider<CurrentUser> currentUser) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.opinionSources = opinionSources;
        this.auditLog = auditLog;
        this.time = time;
        this.currentUser = currentUser;
    }

    @BusinessCommand(requiresReason = false)
    @RequiresPermission(function = Function.ADMISSION_CONTRACT_DEPOSIT, operation = Operation.CREATE)
    @Transactional
    public AdmissionOpinion record(
            Target target, long applicationId, OpinionChoice opinion, OpinionSource source, String note) {
        AdmissionApplication found = store.find(applicationId).orElseThrow(NotFoundException::new);
        residencyLock.lock(found.residentId());
        AdmissionApplication application = store.lock(applicationId).orElseThrow(NotFoundException::new);
        if (!application.status().isOpen()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "APPLICATION_CLOSED",
                    "Lượt đăng ký đã đóng, không ghi thêm ý kiến được (FR-001)");
        }
        opinionSources.validate(application.residentId(), source);
        CurrentUser user = currentUser.getObject();

        store.supersedeActive(applicationId);
        long id = store.insertOpinion(applicationId, opinion, source, note, user.accountId(), time.now());
        store.supersededBy(applicationId, id);
        AdmissionOpinion saved = store.findActiveOpinion(applicationId).orElseThrow();

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("opinionId", id);
        after.put("opinion", opinion.name());
        after.put("sourceType", source.type().name());
        after.put("sourceFamilyMemberId", source.familyMemberId());
        after.put("note", note);
        auditLog.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY_TYPE, applicationId)
                .resident(application.residentId())
                .after(after)
                .build());
        return saved;
    }
}
