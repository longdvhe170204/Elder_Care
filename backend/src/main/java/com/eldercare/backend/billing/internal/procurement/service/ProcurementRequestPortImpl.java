package com.eldercare.backend.billing.internal.procurement.service;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.billing.api.dto.ProcurementStatus;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementRequestRow.LinkedSource;
import com.eldercare.backend.billing.internal.procurement.persistence.ProcurementRequestStore;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementSourceType;
import com.eldercare.backend.roombedallocation.api.dto.ProcurementType;
import com.eldercare.backend.roombedallocation.api.spi.ProcurementRequestPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T007 (contracts mục 2.1; FR-008c, DBR-36, research W-05): M03 kiểm và gắn tài sản mua mới, bảo trì thuê ngoài vào
 * yêu cầu Đã duyệt cùng bộ phận trong cùng lệnh. Nguồn gắn nằm ở cột JSON (R-15); một nguồn một yêu cầu kiểm dưới khóa
 * kỹ thuật của nguồn.
 */
@Service
public class ProcurementRequestPortImpl implements ProcurementRequestPort {
    static final String NOT_USABLE = "PROCUREMENT_REQUEST_NOT_USABLE";

    private final ProcurementRequestStore store;
    private final TechnicalLocks locks;
    private final BusinessTime time;
    private final AuditLogApi audit;

    public ProcurementRequestPortImpl(
            ProcurementRequestStore store, TechnicalLocks locks, BusinessTime time, AuditLogApi audit) {
        this.store = store;
        this.locks = locks;
        this.time = time;
        this.audit = audit;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnmetCondition> checkUsable(long requestId, long departmentId, ProcurementType type) {
        return problem(store.find(requestId), departmentId, type)
                .map(text -> List.of(new UnmetCondition(NOT_USABLE, text, "FR-008c", "M11", false)))
                .orElse(List.of());
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void linkSource(long requestId, ProcurementSourceType sourceType, long sourceId, long amount) {
        if (amount < 0) throw new IllegalArgumentException("amount");
        locks.lock("M11.PROCUREMENT_SOURCE:" + sourceType + ":" + sourceId);
        var row = store.lock(requestId);
        if (row.isEmpty() || row.get().status() != ProcurementStatus.APPROVED)
            throw new BusinessException(HttpStatus.CONFLICT, NOT_USABLE, "Yêu cầu mua sắm không còn ở Đã duyệt");
        if (store.sourceLinked(sourceType, sourceId))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "SOURCE_ALREADY_LINKED", "Nguồn này đã gắn với một yêu cầu mua sắm (DBR-36)");
        var sources = new ArrayList<>(row.get().linkedSources());
        sources.add(new LinkedSource(sourceType, sourceId, amount, time.now()));
        store.linkSources(requestId, sources);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("PROCUREMENT_SOURCE_LINKED", false))
                .entity(ProcurementRequestService.ENTITY, requestId)
                .before(Map.of())
                .after(Map.of("sourceType", sourceType.name(), "sourceId", sourceId, "amount", amount))
                .build());
    }

    /** Loại dùng gắn: tài sản mua mới gắn yêu cầu ASSET, bảo trì thuê ngoài gắn SERVICE (FR-008c). */
    private static Optional<String> problem(
            Optional<ProcurementRequestRow> found, long departmentId, ProcurementType type) {
        if (found.isEmpty()) return Optional.of("Không có yêu cầu mua sắm này");
        var row = found.get();
        if (row.status() != ProcurementStatus.APPROVED) return Optional.of("Yêu cầu mua sắm chưa ở Đã duyệt");
        if (row.departmentId() != departmentId) return Optional.of("Yêu cầu mua sắm thuộc bộ phận khác");
        if (row.type() != type) return Optional.of("Loại yêu cầu mua sắm không phù hợp");
        return Optional.empty();
    }
}
