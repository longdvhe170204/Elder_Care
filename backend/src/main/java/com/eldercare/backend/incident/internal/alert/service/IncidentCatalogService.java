package com.eldercare.backend.incident.internal.alert.service;

import static com.eldercare.backend.incident.internal.alert.domain.IncidentCatalogActions.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.incident.internal.alert.domain.*;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lệnh cấu hình danh mục M05 theo FR-039/042/048a, qua API bảng chung và nhật ký cùng transaction. */
@Service
public class IncidentCatalogService {
    private final CatalogApi catalogs;
    private final AuditLogApi audit;
    private final ObjectProvider<CurrentUser> users;

    public IncidentCatalogService(CatalogApi catalogs, AuditLogApi audit, ObjectProvider<CurrentUser> users) {
        this.catalogs = catalogs;
        this.audit = audit;
        this.users = users;
    }

    @RequiresPermission(function = Function.ALERT_HANDLING, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<TrendRuleView> trendRules() {
        return catalogs.list(IncidentCatalogs.TREND_RULE, null, false).stream()
                .map(IncidentCatalogService::trend)
                .toList();
    }

    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<IncidentTypeView> incidentTypes() {
        return catalogs.list(IncidentCatalogs.INCIDENT_TYPE, null, false).stream()
                .map(IncidentCatalogService::incident)
                .toList();
    }

    @RequiresPermission(function = Function.INCIDENT_EMERGENCY, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ProgressionTypeView> progressionTypes() {
        return catalogs.list(IncidentCatalogs.PROGRESSION_TYPE, null, false).stream()
                .map(item -> new ProgressionTypeView(
                        item.id(),
                        item.code(),
                        item.name(),
                        item.attributes().group(),
                        item.attributes().resuscitation(),
                        item.version()))
                .toList();
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public TrendRuleView updateTrendRule(String code, String ifMatch, TrendRuleUpdate input) {
        requireManager();
        if (input == null || input.reason() == null) throw new ReasonRequiredException();
        if (input.level() == null) throw invalid();
        var before =
                catalogs.findByCode(IncidentCatalogs.TREND_RULE, null, code).orElseThrow(NotFoundException::new);
        VersionEtag.requireMatch(ifMatch, before.version(), trend(before));
        var changed = catalogs.update(
                        IncidentCatalogs.TREND_RULE,
                        before.id(),
                        before.version(),
                        before.name(),
                        before.sortOrder(),
                        new TrendRuleAttributes(
                                input.level(), before.attributes().parameterCode()))
                .after();
        boolean enabled = before.status() == CatalogStatus.ACTIVE;
        if (enabled != input.enabled()) {
            changed = input.enabled()
                    ? catalogs.reactivate(IncidentCatalogs.TREND_RULE, changed.id(), changed.version())
                            .after()
                    : catalogs.deactivate(IncidentCatalogs.TREND_RULE, changed.id(), changed.version())
                            .after();
        }
        var after = trend(changed);
        audit.write(AuditLogEntry.builder()
                .action(TREND_RULE_UPDATED)
                .entity(IncidentCatalogs.TREND_RULE.code(), before.id())
                .before(trendSnapshot(trend(before)))
                .after(trendSnapshot(after))
                .reason(input.reason())
                .build());
        return after;
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public IncidentTypeView createIncidentType(IncidentTypeInput input) {
        requireManager();
        validate(input);
        var created = catalogs.create(
                IncidentCatalogs.INCIDENT_TYPE,
                new NewCatalogItem<>(
                        input.code().strip(),
                        input.name().strip(),
                        null,
                        input.sortOrder(),
                        new IncidentTypeAttributes(input.defaultLevel(), input.nonHealth(), false)));
        var after = incident(created);
        audit.write(AuditLogEntry.builder()
                .action(INCIDENT_TYPE_CREATED)
                .entity(IncidentCatalogs.INCIDENT_TYPE.code(), created.id())
                .after(incidentSnapshot(after))
                .build());
        return after;
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public IncidentTypeView updateIncidentType(long id, String ifMatch, IncidentTypeInput input) {
        return updateIncidentTypeState(id, ifMatch, input, null);
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public IncidentTypeView updateIncidentType(long id, String ifMatch, IncidentTypeInput input, boolean active) {
        return updateIncidentTypeState(id, ifMatch, input, active);
    }

    private IncidentTypeView updateIncidentTypeState(long id, String ifMatch, IncidentTypeInput input, Boolean active) {
        requireManager();
        validate(input);
        var before = catalogs.require(IncidentCatalogs.INCIDENT_TYPE, id);
        VersionEtag.requireMatch(ifMatch, before.version(), incident(before));
        if (!before.code().equals(input.code().strip())) throw invalid();
        if (Boolean.FALSE.equals(active) && IncidentCatalogs.PROTECTED_TYPES.contains(before.code())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROTECTED_CATALOG_CODE",
                    "Loại sự cố có quy tắc riêng không được Ngừng hiệu lực");
        }
        var changed = catalogs.update(
                        IncidentCatalogs.INCIDENT_TYPE,
                        id,
                        before.version(),
                        input.name().strip(),
                        input.sortOrder(),
                        new IncidentTypeAttributes(
                                input.defaultLevel(),
                                input.nonHealth(),
                                before.attributes().allowsNoResident()))
                .after();
        if (active != null && active != (before.status() == CatalogStatus.ACTIVE)) {
            changed = active
                    ? catalogs.reactivate(IncidentCatalogs.INCIDENT_TYPE, id, changed.version())
                            .after()
                    : catalogs.deactivate(IncidentCatalogs.INCIDENT_TYPE, id, changed.version())
                            .after();
        }
        var after = incident(changed);
        audit.write(AuditLogEntry.builder()
                .action(INCIDENT_TYPE_UPDATED)
                .entity(IncidentCatalogs.INCIDENT_TYPE.code(), id)
                .before(incidentSnapshot(incident(before)))
                .after(incidentSnapshot(after))
                .build());
        return after;
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public ProgressionTypeView createProgressionType(ProgressionTypeInput input) {
        requireManager();
        if (input == null || input.group() == null) throw invalid();
        validateText(input.code(), input.name());
        var created = catalogs.create(
                IncidentCatalogs.PROGRESSION_TYPE,
                new NewCatalogItem<>(
                        input.code().strip(),
                        input.name().strip(),
                        null,
                        input.sortOrder(),
                        new ProgressionTypeAttributes(input.group(), input.resuscitation())));
        var after = new ProgressionTypeView(
                created.id(),
                created.code(),
                created.name(),
                created.attributes().group(),
                created.attributes().resuscitation(),
                created.version());
        audit.write(AuditLogEntry.builder()
                .action(PROGRESSION_TYPE_CREATED)
                .entity(IncidentCatalogs.PROGRESSION_TYPE.code(), created.id())
                .after(Map.of(
                        "code",
                        after.code(),
                        "name",
                        after.name(),
                        "group",
                        after.group().name(),
                        "resuscitation",
                        after.resuscitation()))
                .build());
        return after;
    }

    private void requireManager() {
        if (!users.getObject().roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện cấu hình danh mục M05 (FR-039, FR-042, FR-048a)");
    }

    private static void validate(IncidentTypeInput input) {
        if (input == null || input.defaultLevel() == null) throw invalid();
        validateText(input.code(), input.name());
    }

    private static void validateText(String code, String name) {
        if (code == null || code.isBlank() || name == null || name.isBlank()) throw invalid();
    }

    private static BusinessException invalid() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_CATALOG_INPUT",
                "Nội dung danh mục thiếu trường bắt buộc hoặc thay đổi mã đã có");
    }

    private static TrendRuleView trend(CatalogItem<TrendRuleAttributes> item) {
        return new TrendRuleView(
                item.code(),
                item.name(),
                item.status() == CatalogStatus.ACTIVE,
                item.attributes().level(),
                item.attributes().parameterCode(),
                item.version());
    }

    private static IncidentTypeView incident(CatalogItem<IncidentTypeAttributes> item) {
        return new IncidentTypeView(
                item.id(),
                item.code(),
                item.name(),
                item.attributes().defaultLevel(),
                item.attributes().nonHealth(),
                item.attributes().allowsNoResident(),
                item.status() == CatalogStatus.ACTIVE,
                item.version());
    }

    private static Map<String, Object> trendSnapshot(TrendRuleView item) {
        return Map.of("enabled", item.enabled(), "level", item.level().name());
    }

    private static Map<String, Object> incidentSnapshot(IncidentTypeView item) {
        return Map.of(
                "name",
                item.name(),
                "defaultLevel",
                item.defaultLevel().name(),
                "nonHealth",
                item.nonHealth(),
                "active",
                item.active());
    }
}
