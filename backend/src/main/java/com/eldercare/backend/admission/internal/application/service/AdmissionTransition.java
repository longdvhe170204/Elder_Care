package com.eldercare.backend.admission.internal.application.service;

import static com.eldercare.backend.admission.internal.application.service.AdmissionApplicationService.ENTITY_TYPE;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.admission.api.dto.ContractView;
import com.eldercare.backend.admission.api.events.AdmissionCompleted;
import com.eldercare.backend.admission.internal.application.domain.AdmissionApplication;
import com.eldercare.backend.admission.internal.application.domain.AdmissionOpinion;
import com.eldercare.backend.admission.internal.application.persistence.AdmissionApplicationStore;
import com.eldercare.backend.admission.internal.transition.service.ResidencyLock;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.dto.Warning;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phần của lượt đăng ký trong lệnh Hoàn tất tiếp nhận và Hủy tiếp nhận của 001 (FR-002, FR-004a; research T-08, T-09a),
 * do {@code M02StatusParticipant} gọi trong transaction của lệnh (001 đã khóa dòng hồ sơ; ở đây khóa tiếp theo người, đúng thứ tự khóa của research T-03). Hoàn tất tiếp nhận: lượt Đã tiếp nhận, ghi ý kiến Hiệu lực
 * lúc đó, phát {@link AdmissionCompleted}. Hủy tiếp nhận: lượt Đã hủy. Người chưa có lượt mở thì không có gì để chuyển.
 * Hợp đồng Nháp, Chờ ký, hồ sơ chờ và giường theo user story của chúng.
 */
@Component
public class AdmissionTransition {

    static final AuditAction ADMITTED = new AuditAction("M02_APPLICATION_ADMITTED", false);
    static final AuditAction CANCELLED = new AuditAction("M02_APPLICATION_CANCELLED", false);

    private final AdmissionApplicationStore store;
    private final ResidencyLock residencyLock;
    private final AdmissionContractConditions contractConditions;
    private final AdmissionOpinionWarnings opinionWarnings;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;

    AdmissionTransition(
            AdmissionApplicationStore store,
            ResidencyLock residencyLock,
            AdmissionContractConditions contractConditions,
            AdmissionOpinionWarnings opinionWarnings,
            AuditLogApi auditLog,
            ApplicationEventPublisher events) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.contractConditions = contractConditions;
        this.opinionWarnings = opinionWarnings;
        this.auditLog = auditLog;
        this.events = events;
    }

    /** Cảnh báo ý kiến của lệnh Hoàn tất tiếp nhận: tính trên lượt mới nhất để vẫn đúng sau khi lượt đã Đã tiếp nhận. */
    @Transactional(readOnly = true)
    public List<Warning> completionWarnings(long residentId) {
        return opinionWarnings.of(store.findLatest(residentId));
    }

    /**
     * Hoàn tất tiếp nhận: luôn ghi ngày vào ở và phát {@link AdmissionCompleted}; lượt đăng ký mở (nếu có) chuyển Đã tiếp nhận.
     * Hoàn tất tiếp nhận hợp lệ không cần lượt đăng ký mở (FR-001, FR-004; chủ dự án duyệt 2026-10-27). Lệnh chỉ chạy một lần
     * vì 001 từ chối lệnh lặp, nên sự kiện không phát trùng.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void completed(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        store.recordAdmittedOn(residentId, result.at().atZone(BusinessTime.ZONE).toLocalDate());
        store.lockOpen(residentId).ifPresent(application -> markApplicationAdmitted(application, result));

        // Điều kiện (b) bảo đảm có hợp đồng Hiệu lực khi lệnh đã qua kiểm tra
        ContractView contract = contractConditions
                .activeContract(residentId)
                .orElseThrow(() -> new IllegalStateException("Hoàn tất tiếp nhận mà không có hợp đồng Hiệu lực"));
        events.publishEvent(new AdmissionCompleted(
                residentId, contract.id(), contract.residenceType(), contract.startDate(), contract.currentEndDate()));
    }

    private void markApplicationAdmitted(AdmissionApplication application, TransitionResult result) {
        Long opinionId = store.findActiveOpinion(application.id())
                .map(AdmissionOpinion::id)
                .orElse(null);
        store.markAdmitted(application.id(), opinionId);

        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", "ADMITTED");
        after.put("opinionAtAdmissionId", opinionId);
        auditLog.write(AuditLogEntry.builder()
                .action(ADMITTED)
                .entity(ENTITY_TYPE, application.id())
                .resident(application.residentId())
                .before(Map.of("status", application.status().name()))
                .after(after)
                .details(Map.of("historyId", result.historyId()))
                .build());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void cancelled(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        Optional<AdmissionApplication> open = store.lockOpen(residentId);
        if (open.isEmpty()) {
            return;
        }
        AdmissionApplication application = open.get();
        store.markCancelled(application.id());
        auditLog.write(AuditLogEntry.builder()
                .action(CANCELLED)
                .entity(ENTITY_TYPE, application.id())
                .resident(residentId)
                .before(Map.of("status", application.status().name()))
                .after(Map.of("status", "CANCELLED"))
                .details(Map.of("historyId", result.historyId()))
                .build());
    }
}
