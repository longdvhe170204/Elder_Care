package com.eldercare.backend.care.internal.catalog.service;

import static com.eldercare.backend.care.internal.catalog.domain.CareCatalogKinds.*;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.web.VersionEtag;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** T007: quyềnQ-391,transaction và audit thuộc lệnh củaM04. */
@Service
public class CareCatalogService {
    private final CareCatalogStore store;
    private final AuditLogApi audit;

    public CareCatalogService(CareCatalogStore store, AuditLogApi audit) {
        this.store = store;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogItem<TaskTypeAttributes>> taskTypes() {
        return store.list(TASK_TYPES);
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogItem<ResultValueAttributes>> resultValues() {
        return store.list(RESULT_VALUES);
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CatalogItem<TaskTypeAttributes> createTaskType(String name, TaskTypeAttributes attributes) {
        return create(TASK_TYPES, name, attributes);
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CatalogItem<ResultValueAttributes> createResultValue(String name, ResultValueAttributes attributes) {
        return create(RESULT_VALUES, name, attributes);
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CatalogItem<TaskTypeAttributes> updateTaskType(
            long id, String etag, String name, TaskTypeAttributes attributes) {
        var before = matched(TASK_TYPES, id, etag);
        return update(
                TASK_TYPES,
                before,
                name,
                new TaskTypeAttributes(
                        attributes.group(),
                        attributes.professional(),
                        attributes.resultType(),
                        attributes.defaultRole(),
                        attributes.defaultImportance(),
                        attributes.windowBeforeMinutes(),
                        attributes.windowAfterMinutes(),
                        attributes.billable(),
                        attributes.serviceId(),
                        attributes.upperLimit(),
                        attributes.vitalSignType(),
                        attributes.certificates(),
                        before.attributes().legacy()));
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CatalogItem<ResultValueAttributes> updateResultValue(
            long id, String etag, String name, ResultValueAttributes attributes) {
        var before = matched(RESULT_VALUES, id, etag);
        return update(
                RESULT_VALUES,
                before,
                name,
                new ResultValueAttributes(
                        attributes.type(),
                        attributes.code(),
                        attributes.ruleGroup(),
                        before.attributes().legacy()));
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void deactivateTaskType(long id, Reason reason) {
        deactivate(TASK_TYPES, id, reason);
    }

    @RequiresPermission(function = Function.CARE_TASK_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void deactivateResultValue(long id, Reason reason) {
        deactivate(RESULT_VALUES, id, reason);
    }

    private <T> CatalogItem<T> create(SharedKind<T> kind, String name, T attributes) {
        validName(name);
        var row = store.create(kind, name, attributes);
        log(kind, "CREATED", null, row, null);
        return row;
    }

    private <T> CatalogItem<T> matched(SharedKind<T> kind, long id, String etag) {
        var row = store.require(kind, id);
        VersionEtag.requireMatch(etag, row.version(), Map.of("id", id, "name", row.name(), "version", row.version()));
        return row;
    }

    private <T> CatalogItem<T> update(SharedKind<T> kind, CatalogItem<T> before, String name, T attributes) {
        validName(name);
        var row = store.update(kind, before, name, attributes);
        log(kind, "UPDATED", before, row, null);
        return row;
    }

    private <T> void deactivate(SharedKind<T> kind, long id, Reason reason) {
        var before = store.require(kind, id);
        var row = store.deactivate(kind, before);
        log(kind, "DEACTIVATED", before, row, reason);
    }

    private void validName(String name) {
        if (name == null || name.isBlank() || name.length() > 200)
            throw new InvalidValueException("Tên danh mục phải có từ1đến200ký tự");
    }

    private <T> Map<String, Object> snapshot(CatalogItem<T> row) {
        return row == null
                ? Map.of()
                : Map.of(
                        "name",
                        row.name(),
                        "attributes",
                        row.attributes(),
                        "status",
                        row.status().name());
    }

    private <T> void log(
            SharedKind<T> kind, String action, CatalogItem<T> before, CatalogItem<T> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(kind.code().substring(4) + "_" + action, reason != null))
                .entity(kind.code(), publicId(after))
                .before(snapshot(before))
                .after(snapshot(after))
                .reason(reason)
                .build());
    }
}
