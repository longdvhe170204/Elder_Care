package com.eldercare.backend.roombedallocation.internal.asset.service;

import static com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds.ASSET_TYPE;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetKinds;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T018 (FR-011, W-08 {@code DANH_MUC_LOAI}): Quản lý viện tạo, đổi tên, ngừng, kích hoạt lại loại tài sản. Hai loại
 * hệ thống Giường, Xe (V019_05) đổi tên được, không ngừng được.
 */
@Service
public class AssetTypeService {
    private final CatalogApi catalog;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final PermissionApi permissions;

    public AssetTypeService(CatalogApi catalog, CurrentUser user, AuditLogApi audit, PermissionApi permissions) {
        this.catalog = catalog;
        this.user = user;
        this.audit = audit;
        this.permissions = permissions;
    }

    /**
     * Danh mục loại, gồm loại đã ngừng (api-019 GET /asset-types, bổ sung 2026-11-10): người có một quyền bất kỳ của
     * "Tài sản, lịch xe" (Phụ lục 27); tự kiểm vì danh mục không gắn đối tượng.
     */
    @Transactional(readOnly = true)
    public List<CatalogItem<AssetKinds.TypeAttributes>> list() {
        if (permissions.permissions(user).stream()
                .map(PermissionKey::function)
                .noneMatch(Function.ASSET_VEHICLE::equals))
            throw new PermissionDeniedException("Không có quyền xem tài sản");
        return catalog.list(ASSET_TYPE, null, false);
    }

    @RequiresPermission(function = Function.ASSET_VEHICLE, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long save(Target target, Long id, String name, String status) {
        if (!user.roles().contains(Role.NURSING_HOME_MANAGER))
            throw new PermissionDeniedException("Chỉ Quản lý viện quản lý danh mục loại tài sản");
        if (name == null || name.isBlank()) throw new InvalidValueException("Cần tên loại tài sản");
        if (status != null && !List.of("ACTIVE", "INACTIVE").contains(status))
            throw new InvalidValueException("Trạng thái là ACTIVE hoặc INACTIVE");
        if (id == null) {
            long created = catalog.create(
                            ASSET_TYPE,
                            new NewCatalogItem<>(null, name.strip(), null, 0, new AssetKinds.TypeAttributes(false)))
                    .id();
            record(created, "CREATE");
            return created;
        }
        var item = catalog.require(ASSET_TYPE, id);
        boolean system = item.attributes() != null
                && Boolean.TRUE.equals(item.attributes().system());
        var updated = catalog.update(ASSET_TYPE, id, item.version(), name.strip(), item.sortOrder(), item.attributes())
                .after();
        if ("INACTIVE".equals(status) && item.status() == CatalogStatus.ACTIVE) {
            if (system)
                throw new ConditionNotMetException(List.of(new UnmetCondition(
                        "SYSTEM_ASSET_TYPE",
                        "Loại Giường, Xe là loại hệ thống, không ngừng được",
                        "FR-011",
                        "M03",
                        false)));
            catalog.deactivate(ASSET_TYPE, id, updated.version());
        } else if ("ACTIVE".equals(status) && item.status() == CatalogStatus.INACTIVE)
            catalog.reactivate(ASSET_TYPE, id, updated.version());
        record(id, "UPDATE");
        return id;
    }

    private void record(long id, String action) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("M03_ASSET_TYPE_" + action, false))
                .entity(ASSET_TYPE.code(), id)
                .before(Map.of())
                .after(Map.of())
                .build());
    }
}
