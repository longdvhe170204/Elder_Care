package com.eldercare.backend.staffing.internal.persistence;

import static com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.*;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.staffing.internal.credential.domain.CredentialType;
import com.eldercare.backend.staffing.internal.credential.domain.PracticeScope;
import com.eldercare.backend.staffing.internal.shift.domain.ShiftTemplate;
import com.eldercare.backend.staffing.internal.shift.domain.TemplateInput;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;
import org.springframework.stereotype.Repository;

/** T050: danh mục dùng CatalogApi thật; ID logic cũ độc lập ID vật lý ở bảng chung. */
@Repository
public class StaffingCatalogStore {
    private final CatalogApi catalogs;
    private final TechnicalLocks locks;

    public StaffingCatalogStore(CatalogApi catalogs, TechnicalLocks locks) {
        this.catalogs = catalogs;
        this.locks = locks;
    }

    public List<CatalogItem<DepartmentAttributes>> departments() {
        return catalogs.list(DEPARTMENT, null, false).stream()
                .sorted(Comparator.comparingLong(this::departmentId))
                .toList();
    }

    public long departmentId(CatalogItem<DepartmentAttributes> row) {
        return Legacy.logicalId(row.id(), row.attributes().legacy(), "department");
    }

    public Optional<CatalogItem<DepartmentAttributes>> department(long logicalId) {
        return departments().stream().filter(r -> departmentId(r) == logicalId).findFirst();
    }

    public CatalogItem<DepartmentAttributes> requireDepartment(long logicalId, boolean lock) {
        if (lock) locks.lock("M09.DEPARTMENT:" + logicalId);
        return department(logicalId).orElseThrow(NotFoundException::new);
    }

    public Long departmentLogicalId(Long physicalId) {
        return physicalId == null ? null : departmentId(catalogs.require(DEPARTMENT, physicalId));
    }

    public long addDepartment(String name, boolean active, Instant at) {
        var row = catalogs.create(
                DEPARTMENT,
                new NewCatalogItem<>(null, name.strip(), null, 0, new DepartmentAttributes(null, at, null)));
        if (!active) catalogs.deactivate(DEPARTMENT, row.id(), row.version());
        return row.id();
    }

    public void departmentLead(long logicalId, long lead) {
        var row = requireDepartment(logicalId, false);
        var old = row.attributes();
        catalogs.update(
                DEPARTMENT,
                row.id(),
                row.version(),
                row.name(),
                row.sortOrder(),
                new DepartmentAttributes(lead, old.createdAt(), old.legacy()));
    }

    public List<CredentialType> credentialTypes() {
        return catalogs.list(CREDENTIAL_TYPE, null, false).stream()
                .map(this::credentialType)
                .sorted(Comparator.comparingLong(CredentialType::id))
                .toList();
    }

    public Optional<CredentialType> credentialType(long logicalId) {
        return credentialTypes().stream().filter(r -> r.id() == logicalId).findFirst();
    }

    public CatalogItem<CredentialTypeAttributes> credentialTypeRow(long logicalId) {
        return catalogs.list(CREDENTIAL_TYPE, null, false).stream()
                .filter(r -> Legacy.logicalId(r.id(), r.attributes().legacy(), "credential_type") == logicalId)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public CredentialType credentialTypePhysical(long physicalId) {
        return credentialType(catalogs.require(CREDENTIAL_TYPE, physicalId));
    }

    private CredentialType credentialType(CatalogItem<CredentialTypeAttributes> r) {
        var a = r.attributes();
        return new CredentialType(
                Legacy.logicalId(r.id(), a.legacy(), "credential_type"),
                a.legacyCode() == null ? r.code() : a.legacyCode(),
                r.name(),
                a.credentialGroup(),
                a.hasExpiry(),
                a.requiredForRoles() == null ? Set.of() : Set.copyOf(a.requiredForRoles()),
                a.hasPracticeScopes(),
                r.status() == CatalogStatus.ACTIVE);
    }

    public long addCredentialType(
            String name, String group, boolean expiry, Set<String> roles, boolean scopes, boolean active, Instant at) {
        var r = catalogs.create(
                CREDENTIAL_TYPE,
                new NewCatalogItem<>(
                        "CT-" + UUID.randomUUID(),
                        name.strip(),
                        null,
                        0,
                        new CredentialTypeAttributes(group, expiry, Set.copyOf(roles), scopes, null, at, null)));
        if (!active) catalogs.deactivate(CREDENTIAL_TYPE, r.id(), r.version());
        return r.id();
    }

    public List<PracticeScope> practiceScopes() {
        return catalogs.list(PRACTICE_SCOPE, null, false).stream()
                .map(this::practiceScope)
                .sorted(Comparator.comparingLong(PracticeScope::id))
                .toList();
    }

    public Optional<PracticeScope> practiceScope(long logicalId) {
        return practiceScopes().stream().filter(r -> r.id() == logicalId).findFirst();
    }

    public PracticeScope practiceScopePhysical(long physicalId) {
        return practiceScope(catalogs.require(PRACTICE_SCOPE, physicalId));
    }

    private PracticeScope practiceScope(CatalogItem<PracticeScopeAttributes> r) {
        var a = r.attributes();
        return new PracticeScope(
                Legacy.logicalId(r.id(), a.legacy(), "practice_scope"),
                r.name(),
                r.status() == CatalogStatus.ACTIVE,
                a.legacyCode() == null ? r.code() : a.legacyCode());
    }

    public long practiceScopePhysicalId(String code) {
        return catalogs.list(PRACTICE_SCOPE, null, false).stream()
                .filter(r -> Objects.equals(
                        r.attributes().legacyCode() == null
                                ? r.code()
                                : r.attributes().legacyCode(),
                        code))
                .mapToLong(CatalogItem::id)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public List<ShiftTemplate> templates() {
        return catalogs.list(SHIFT_TEMPLATE, null, false).stream()
                .map(this::template)
                .sorted(Comparator.comparingLong(ShiftTemplate::id))
                .toList();
    }

    public ShiftTemplate template(long logicalId, boolean lock) {
        return template(templateRow(logicalId, lock));
    }

    private CatalogItem<ShiftTemplateAttributes> templateRow(long logicalId, boolean lock) {
        if (lock) locks.lock("M09.SHIFT_TEMPLATE:" + logicalId);
        return catalogs.list(SHIFT_TEMPLATE, null, false).stream()
                .filter(r -> Legacy.logicalId(r.id(), r.attributes().legacy(), "shift_template") == logicalId)
                .findFirst()
                .orElseThrow(NotFoundException::new);
    }

    public long templatePhysicalId(long logicalId) {
        return templateRow(logicalId, false).id();
    }

    public long templateLogicalId(long physicalId) {
        var row = catalogs.require(SHIFT_TEMPLATE, physicalId);
        return Legacy.logicalId(row.id(), row.attributes().legacy(), "shift_template");
    }

    private ShiftTemplate template(CatalogItem<ShiftTemplateAttributes> r) {
        var a = r.attributes();
        return new ShiftTemplate(
                Legacy.logicalId(r.id(), a.legacy(), "shift_template"),
                a.originalName() == null ? r.name() : a.originalName(),
                a.startTime(),
                a.endTime(),
                a.scopeType(),
                a.requiresHandover(),
                r.status() == CatalogStatus.ACTIVE,
                r.version());
    }

    public long addTemplate(TemplateInput input, boolean handover, Instant at) {
        var row = catalogs.create(
                SHIFT_TEMPLATE,
                new NewCatalogItem<>(
                        null,
                        catalogName(input.name().strip()),
                        null,
                        0,
                        new ShiftTemplateAttributes(
                                input.startTime(),
                                input.endTime(),
                                input.scopeType(),
                                handover,
                                input.name().strip(),
                                at,
                                null)));
        if (!input.active()) catalogs.deactivate(SHIFT_TEMPLATE, row.id(), row.version());
        return row.id();
    }

    public void updateTemplate(long logicalId, TemplateInput input, boolean handover) {
        var row = templateRow(logicalId, true);
        var old = row.attributes();
        var updated = catalogs.update(
                        SHIFT_TEMPLATE,
                        row.id(),
                        row.version(),
                        catalogName(input.name().strip()),
                        row.sortOrder(),
                        new ShiftTemplateAttributes(
                                input.startTime(),
                                input.endTime(),
                                input.scopeType(),
                                handover,
                                input.name().strip(),
                                old.createdAt(),
                                old.legacy()))
                .after();
        if (input.active() && updated.status() != CatalogStatus.ACTIVE)
            catalogs.reactivate(SHIFT_TEMPLATE, updated.id(), updated.version());
        if (!input.active() && updated.status() == CatalogStatus.ACTIVE)
            catalogs.deactivate(SHIFT_TEMPLATE, updated.id(), updated.version());
    }

    private static String catalogName(String name) {
        if (name.length() <= 200) return name;
        try {
            String digest = HexFormat.of()
                    .formatHex(MessageDigest.getInstance("SHA-256").digest(name.getBytes(StandardCharsets.UTF_8)));
            return name.substring(0, 158) + " #" + digest.substring(0, 40);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
