package com.eldercare.backend.care.internal.catalog.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditAction;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogEntry;
import com.eldercare.backend.care.internal.catalog.domain.NonPerformanceReasonType;
import com.eldercare.backend.care.internal.catalog.persistence.NonPerformanceReasonStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.web.VersionEtag;
import java.util.List;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T007a: quyền Q-391, vòng đời danh mục và audit trong transaction của lệnh. */
@Service
public class NonPerformanceReasonService {
    private final NonPerformanceReasonStore store;
    private final AuditLogApi audit;

    public NonPerformanceReasonService(NonPerformanceReasonStore store, AuditLogApi audit) {
        this.store = store;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<NonPerformanceReasonType> list() {
        return store.list();
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public NonPerformanceReasonType create(String name) {
        validate(name);
        NonPerformanceReasonType row;
        try {
            row = store.create(name);
        } catch (DataIntegrityViolationException duplicate) {
            throw duplicateName();
        }
        log(
                row,
                "CREATED",
                Map.of(),
                Map.of(
                        "name",
                        row.getName(),
                        "fixed",
                        row.fixed(),
                        "status",
                        row.getStatus().name()),
                null);
        return row;
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public NonPerformanceReasonType rename(long id, String ifMatch, String name) {
        validate(name);
        var row = store.lock(id);
        VersionEtag.requireMatch(
                ifMatch,
                row.getVersion(),
                Map.of(
                        "id",
                        id,
                        "name",
                        row.getName(),
                        "fixed",
                        row.fixed(),
                        "status",
                        row.getStatus().name(),
                        "version",
                        row.getVersion()));
        mutable(row);
        String before = row.getName();
        try {
            row = store.rename(row, name);
        } catch (DataIntegrityViolationException duplicate) {
            throw duplicateName();
        }
        boolean changed = !before.equals(name);
        log(
                row,
                "UPDATED",
                changed ? Map.of("name", before) : Map.of(),
                changed ? Map.of("name", name) : Map.of(),
                null);
        return row;
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void deactivate(long id, Reason reason) {
        var row = store.lock(id);
        mutable(row);
        String before = row.getStatus().name();
        row = store.deactivate(row);
        log(
                row,
                "DEACTIVATED",
                Map.of("status", before),
                Map.of("status", row.getStatus().name()),
                reason);
    }

    private void validate(String name) {
        if (name == null || name.isBlank() || name.length() > 200)
            throw new InvalidValueException("Tên loại lý do phải có từ 1 đến 200 ký tự");
    }

    private void mutable(NonPerformanceReasonType row) {
        if (row.fixed())
            throw new BusinessException(
                    HttpStatus.CONFLICT, "REASON_TYPE_FIXED", "Không đổi hoặc ngừng loại lý do cố định");
    }

    private BusinessException duplicateName() {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Tên loại lý do đang hiệu lực đã tồn tại");
    }

    private void log(
            NonPerformanceReasonType row,
            String action,
            Map<String, Object> before,
            Map<String, Object> after,
            Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("NON_PERFORMANCE_REASON_" + action, reason != null))
                .entity("M04.NON_PERFORMANCE_REASON_TYPE", row.getId())
                .before(before)
                .after(after)
                .reason(reason)
                .build());
    }
}
