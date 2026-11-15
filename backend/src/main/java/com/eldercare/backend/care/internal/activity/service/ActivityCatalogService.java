package com.eldercare.backend.care.internal.activity.service;

import static com.eldercare.backend.care.internal.activity.domain.ActivityKinds.*;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.*;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.InvalidValueException;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** FR-001, Q-401: danh mục loại hoạt động thuộc quyền danh mục riêng. */
@Service
public class ActivityCatalogService {
    private final CatalogApi catalogs;
    private final AuditLogApi audit;

    public ActivityCatalogService(CatalogApi catalogs, AuditLogApi audit) {
        this.catalogs = catalogs;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.ACTIVITY_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogItem<ActivityTypeAttributes>> list(Target target) {
        return catalogs.list(ACTIVITY_TYPES, null, false);
    }

    @RequiresPermission(function = Function.ACTIVITY_CATALOG, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void write(
            Target target, Long id, String name, Boolean group, Boolean offSite, List<Long> groups, String status) {
        if (name == null || name.isBlank()) throw new InvalidValueException("Cần tên loại hoạt động");
        if (status != null && !Set.of("ACTIVE", "INACTIVE").contains(status))
            throw new InvalidValueException("Trạng thái phải là ACTIVE hoặc INACTIVE");
        if (groups != null) {
            for (Long interestGroup : groups) {
                if (interestGroup == null) throw new InvalidValueException("Mã nhóm sở thích không hợp lệ");
                catalogs.requireActive(INTEREST_GROUPS, interestGroup);
            }
        }
        CatalogItem<ActivityTypeAttributes> before = id == null ? null : catalogs.require(ACTIVITY_TYPES, id);
        var old = before == null ? new ActivityTypeAttributes(false, false, List.of()) : before.attributes();
        var attributes = new ActivityTypeAttributes(
                group == null ? old.groupActivity() : group,
                offSite == null ? old.offSite() : offSite,
                groups == null ? old.interestGroupIds() : groups);
        var after = before == null
                ? catalogs.create(ACTIVITY_TYPES, new NewCatalogItem<>(null, name.trim(), null, 0, attributes))
                : catalogs.update(ACTIVITY_TYPES, id, before.version(), name.trim(), before.sortOrder(), attributes)
                        .after();
        if ("INACTIVE".equals(status) && after.status() == CatalogStatus.ACTIVE)
            after = catalogs.deactivate(ACTIVITY_TYPES, after.id(), after.version())
                    .after();
        else if ("ACTIVE".equals(status) && after.status() == CatalogStatus.INACTIVE)
            after = catalogs.reactivate(ACTIVITY_TYPES, after.id(), after.version())
                    .after();
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("ACTIVITY_TYPE_RECORDED", false))
                .entity(ACTIVITY_TYPES.code(), after.id())
                .before(
                        before == null
                                ? Map.of()
                                : Map.of(
                                        "name",
                                        before.name(),
                                        "status",
                                        before.status().name()))
                .after(Map.of("name", after.name(), "status", after.status().name(), "attributes", after.attributes()))
                .build());
    }
}
