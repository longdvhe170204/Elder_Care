package com.cambae.backend.m02.internal.application.service;

import static com.cambae.backend.m02.internal.application.service.AdmissionApplicationService.ENTITY_TYPE;

import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m01.api.OpinionSourceApi;
import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m02.internal.application.domain.AdmissionApplication;
import com.cambae.backend.m02.internal.application.domain.AdmissionOpinion;
import com.cambae.backend.m02.internal.application.domain.OpinionChoice;
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
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ý kiến của người cao tuổi về việc vào ở (004 US1; FR-001, FR-004a, Q-356; research T-09a). Mỗi lần ghi một dòng; ý
 * kiến Hiệu lực cũ chuyển Được thay thế trong cùng transaction, dưới khóa {@code resident_residency}. Nguồn ý kiến do
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
