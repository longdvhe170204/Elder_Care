package com.cambae.backend.m02.internal.contract.service;

import com.cambae.backend.m01.api.dto.TransitionResult;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.api.events.ContractStatusChanged;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.persistence.ContractStore;
import com.cambae.backend.m02.internal.transition.service.ResidencyLock;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Phần của hợp đồng trong lệnh Hủy tiếp nhận của 001 (FR-002, FR-027; bảng trạng thái hợp đồng), do {@code
 * M02StatusParticipant} gọi trong transaction của lệnh (001 đã khóa dòng hồ sơ; ở đây khóa tiếp {@code resident_residency},
 * rồi từng hợp đồng): hợp đồng Nháp và Chờ ký thành Đã hủy; hợp đồng Hiệu lực thành Chấm dứt, lý do chấm dứt trỏ về lịch sử
 * trạng thái của lệnh (lý do của lệnh nằm ở đó). Kết thúc lưu trú sớm, ghi nhận qua đời thuộc user story của chúng.
 */
@Component
public class ContractTransition {

    static final AuditAction CANCELLED_BY_ADMISSION = new AuditAction("M02_CONTRACT_CANCELLED", false);
    static final AuditAction TERMINATED = new AuditAction("M02_CONTRACT_TERMINATED", false);

    private final ContractStore store;
    private final ResidencyLock residencyLock;
    private final AuditLogApi auditLog;
    private final ApplicationEventPublisher events;

    ContractTransition(
            ContractStore store, ResidencyLock residencyLock, AuditLogApi auditLog, ApplicationEventPublisher events) {
        this.store = store;
        this.residencyLock = residencyLock;
        this.auditLog = auditLog;
        this.events = events;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void admissionCancelled(TransitionResult result) {
        long residentId = result.residentId();
        residencyLock.lock(residentId);
        for (Contract listed : store.ofResident(residentId)) {
            Contract contract = store.lock(listed.id()).orElseThrow();
            switch (contract.status()) {
                case DRAFT, PENDING_SIGNATURE -> {
                    store.transition(contract.id(), contract.status(), ContractStatus.CANCELLED);
                    record(contract, ContractStatus.CANCELLED, CANCELLED_BY_ADMISSION, result, null);
                }
                case ACTIVE -> {
                    String reason = "Hủy tiếp nhận (lịch sử trạng thái " + result.historyId() + ")";
                    store.markTerminated(contract.id(), reason);
                    record(contract, ContractStatus.TERMINATED, TERMINATED, result, reason);
                }
                default -> {}
            }
        }
    }

    private void record(
            Contract contract, ContractStatus to, AuditAction action, TransitionResult result, String reason) {
        auditLog.write(AuditLogEntry.builder()
                .action(action)
                .entity(ContractService.ENTITY_TYPE, contract.id())
                .resident(contract.residentId())
                .before(Map.of("status", contract.status().name()))
                .after(Map.of("status", to.name()))
                .details(Map.of("historyId", result.historyId()))
                .build());
        events.publishEvent(
                new ContractStatusChanged(contract.id(), contract.residentId(), contract.status(), to, reason));
    }
}
