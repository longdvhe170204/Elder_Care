package com.eldercare.backend.health.internal.catalog.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.*;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.health.internal.catalog.domain.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

/** T006: danh mục M06 qua API bảng chung, khoảng hợp lệ có duy nhất một nguồn CFG-M06-03. */
@Service
public class VitalSignCatalogService {
    private final CatalogApi catalogs;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final ObjectProvider<CurrentUser> users;

    public VitalSignCatalogService(
            CatalogApi catalogs, ParameterApi parameters, AuditLogApi audit, ObjectProvider<CurrentUser> users) {
        this.catalogs = catalogs;
        this.parameters = parameters;
        this.audit = audit;
        this.users = users;
    }

    @RequiresPermission(function = Function.VITAL_SIGN, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<VitalSignCatalogView> list() {
        JsonNode ranges = ranges();
        return catalogs.list(HealthCatalogs.VITAL_SIGN, null, false).stream()
                .map(item -> view(item, ranges))
                .toList();
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public VitalSignCatalogView create(VitalSignCatalogInput input) {
        manager();
        validate(input);
        var item = catalogs.create(
                HealthCatalogs.VITAL_SIGN,
                new NewCatalogItem<>(
                        input.code().strip(),
                        input.name().strip(),
                        null,
                        0,
                        new VitalSignAttributes(input.unit().strip(), input.vitalSign())));
        for (var component : input.components()) {
            catalogs.create(
                    HealthCatalogs.VITAL_SIGN_COMPONENT,
                    new NewCatalogItem<>(
                            component.code().strip(),
                            component.name().strip(),
                            item.id(),
                            input.components().indexOf(component),
                            new VitalSignComponentAttributes(component.decimalPlaces())));
        }
        var after = view(item, ranges());
        write(VitalSignCatalogActions.CREATED, item.id(), null, snapshot(after), null);
        return after;
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public VitalSignCatalogView update(long id, String ifMatch, VitalSignCatalogInput input) {
        manager();
        validate(input);
        JsonNode ranges = ranges();
        var before = catalogs.require(HealthCatalogs.VITAL_SIGN, id);
        var old = view(before, ranges);
        VersionEtag.requireMatch(ifMatch, before.version(), old);
        // FR-002 chỉ cho đổi tên, đơn vị, nhóm sinh hiệu; không đổi mã/cấu trúc thành phần.
        if (!before.code().equals(input.code().strip()) || !old.components().equals(input.components()))
            throw invalid();
        var changed = catalogs.update(
                        HealthCatalogs.VITAL_SIGN,
                        id,
                        before.version(),
                        input.name().strip(),
                        before.sortOrder(),
                        new VitalSignAttributes(input.unit().strip(), input.vitalSign()))
                .after();
        var after = view(changed, ranges);
        write(VitalSignCatalogActions.UPDATED, id, snapshot(old), snapshot(after), null);
        return after;
    }

    @BusinessCommand
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public VitalSignCatalogView deactivate(long id, Reason reason) {
        manager();
        if (reason == null) throw new ReasonRequiredException();
        JsonNode ranges = ranges();
        var before = catalogs.require(HealthCatalogs.VITAL_SIGN, id);
        var after = view(
                catalogs.deactivate(HealthCatalogs.VITAL_SIGN, id, before.version())
                        .after(),
                ranges);
        write(VitalSignCatalogActions.DEACTIVATED, id, snapshot(view(before, ranges)), snapshot(after), reason);
        return after;
    }

    private JsonNode ranges() {
        var value = parameters.read(ParameterCode.CFG_M06_03);
        return value.isSet() ? value.raw() : null;
    }

    private VitalSignCatalogView view(CatalogItem<VitalSignAttributes> item, JsonNode ranges) {
        var components = catalogs.list(HealthCatalogs.VITAL_SIGN_COMPONENT, item.id(), false).stream()
                .map(c -> new VitalSignComponentDefinition(
                        c.code(), c.name(), c.attributes().decimalPlaces()))
                .toList();
        JsonNode range = ranges == null ? null : ranges.get(item.code());
        boolean missing = range == null
                || !range.isObject()
                || components.isEmpty()
                || components.stream()
                        .anyMatch(c -> !range.has(c.code())
                                || !range.get(c.code()).hasNonNull("min")
                                || !range.get(c.code()).hasNonNull("max"));
        return new VitalSignCatalogView(
                item.id(),
                item.code(),
                item.name(),
                item.attributes().unit(),
                item.attributes().vitalSign(),
                components,
                range,
                missing,
                item.status() == CatalogStatus.ACTIVE,
                item.version());
    }

    private void manager() {
        if (!users.getObject().roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện cấu hình danh mục chỉ số (FR-002)");
    }

    private static void validate(VitalSignCatalogInput input) {
        if (input == null
                || blank(input.code())
                || blank(input.name())
                || blank(input.unit())
                || input.components() == null
                || input.components().isEmpty()) throw invalid();
        Set<String> codes = new HashSet<>();
        for (var c : input.components()) {
            if (c == null
                    || blank(c.code())
                    || blank(c.name())
                    || c.decimalPlaces() < 0
                    || !codes.add(c.code().strip())) throw invalid();
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static BusinessException invalid() {
        return new BusinessException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "INVALID_CATALOG_INPUT",
                "Nội dung danh mục thiếu trường, trùng thành phần hoặc thay đổi mã/cấu trúc thành phần");
    }

    private static Map<String, Object> snapshot(VitalSignCatalogView item) {
        return Map.of(
                "code",
                item.code(),
                "name",
                item.name(),
                "unit",
                item.unit(),
                "vitalSign",
                item.vitalSign(),
                "components",
                item.components(),
                "active",
                item.active());
    }

    private void write(
            AuditAction action, long id, Map<String, Object> before, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(action)
                .entity(HealthCatalogs.VITAL_SIGN.code(), id)
                .before(before)
                .after(after)
                .reason(reason)
                .build());
    }
}
