package com.eldercare.backend.belongings.internal.catalog.service;

import static com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogActions.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.StatusHistoryApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.HistoryEntry;
import com.eldercare.backend.administration.shared.api.dto.HistoryKind;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.belongings.api.dto.BelongingView;
import com.eldercare.backend.belongings.internal.belonging.domain.BelongingNotices;
import com.eldercare.backend.belongings.internal.belonging.persistence.BelongingStore;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogViews.BelongingTypeView;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogViews.ConditionLevelView;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogViews.StorageLocationView;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingCatalogs;
import com.eldercare.backend.belongings.internal.catalog.domain.BelongingTypeAttributes;
import com.eldercare.backend.belongings.internal.catalog.domain.ConditionLevelAttributes;
import com.eldercare.backend.belongings.internal.catalog.domain.StorageLocationAttributes;
import com.eldercare.backend.belongings.internal.catalog.domain.ValuableCategories;
import com.eldercare.backend.belongings.internal.consent.service.SelfCustodyConsentService;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục đồ gửi ({@code DanhMucM12Service}; FR-001, FR-001a, FR-002, FR-015a). Loại đồ, mức tình trạng: Quản lý viện
 * (khóa cấu hình danh mục như M05, plan mục "Kế hoạch triển khai R-15" điểm 3); vị trí lưu giữ: Hành chính (khóa "Đồ
 * gửi") và Quản lý viện. Nhật ký cùng transaction (FR-031a).
 */
@Service
public class BelongingCatalogService {

    private static final Set<Role> CATALOG_READERS =
            Set.of(Role.NURSING_HOME_MANAGER, Role.FLOOR_MANAGER, Role.NURSING_STAFF, Role.OFFICE_STAFF);

    private final CatalogApi catalogs;
    private final StatusHistoryApi history;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final BelongingStore belongings;
    private final SelfCustodyConsentService consents;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final ObjectProvider<CurrentUser> users;

    BelongingCatalogService(
            CatalogApi catalogs,
            StatusHistoryApi history,
            ParameterApi parameters,
            AuditLogApi audit,
            BelongingStore belongings,
            SelfCustodyConsentService consents,
            ApplicationEventPublisher events,
            BusinessTime time,
            ObjectProvider<CurrentUser> users) {
        this.catalogs = catalogs;
        this.history = history;
        this.parameters = parameters;
        this.audit = audit;
        this.belongings = belongings;
        this.consents = consents;
        this.events = events;
        this.time = time;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<BelongingTypeView> types() {
        requireCatalogReader();
        ValuableCategories valuable = valuableCategories();
        return catalogs.list(BelongingCatalogs.BELONGING_TYPE, null, false).stream()
                .map(item -> type(item, valuable))
                .toList();
    }

    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public long createType(String name, Boolean hasCashAmount, Boolean requiresConsentForUse) {
        requireRole(Role.NURSING_HOME_MANAGER);
        var created = catalogs.create(
                BelongingCatalogs.BELONGING_TYPE,
                new NewCatalogItem<>(
                        null,
                        requireName(name),
                        null,
                        catalogs.list(BelongingCatalogs.BELONGING_TYPE, null, false)
                                        .size()
                                + 1,
                        new BelongingTypeAttributes(
                                Boolean.TRUE.equals(hasCashAmount), Boolean.TRUE.equals(requiresConsentForUse))));
        audit.write(AuditLogEntry.builder()
                .action(BELONGING_TYPE_CREATED)
                .entity(BelongingCatalogs.BELONGING_TYPE.code(), created.id())
                .after(typeSnapshot(created))
                .build());
        return created.id();
    }

    /** Sửa, ngừng, kích hoạt lại loại đồ; đổi dấu "cần đồng ý" bắt buộc lý do và ghi lịch sử (FR-001, FR-015a). */
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public void updateType(
            long id, String name, Boolean hasCashAmount, Boolean requiresConsentForUse, String status, String reason) {
        CurrentUser user = requireRole(Role.NURSING_HOME_MANAGER);
        var before = catalogs.require(BelongingCatalogs.BELONGING_TYPE, id);
        boolean consent =
                requiresConsentForUse == null ? before.attributes().requiresConsentForUse() : requiresConsentForUse;
        boolean consentChanged = consent != before.attributes().requiresConsentForUse();
        Reason why = reason == null || reason.isBlank() ? null : Reason.of(reason);
        if (consentChanged && why == null) {
            throw new ReasonRequiredException();
        }
        var changed = catalogs.update(
                        BelongingCatalogs.BELONGING_TYPE,
                        id,
                        before.version(),
                        requireName(name),
                        before.sortOrder(),
                        new BelongingTypeAttributes(
                                hasCashAmount == null ? before.attributes().hasCashAmount() : hasCashAmount, consent))
                .after();
        changed = applyStatus(BelongingCatalogs.BELONGING_TYPE, changed, status);
        if (consentChanged) {
            history.add(
                    BelongingCatalogs.CONSENT_FLAG_HISTORY,
                    id,
                    new HistoryEntry<>(
                            HistoryKind.POINT,
                            Boolean.toString(!consent),
                            Boolean.toString(consent),
                            null,
                            time.now(),
                            "UPDATE_BELONGING_TYPE",
                            user.accountId(),
                            false,
                            why,
                            null,
                            null,
                            null,
                            null,
                            null));
        }
        audit.write(AuditLogEntry.builder()
                .action(BELONGING_TYPE_UPDATED)
                .entity(BelongingCatalogs.BELONGING_TYPE.code(), id)
                .before(typeSnapshot(before))
                .after(typeSnapshot(changed))
                .reason(why)
                .build());
        if (consentChanged && consent) {
            notifyInUseWithoutConsent(id, changed.name(), user.accountId());
        }
    }

    /**
     * FR-015a: bật dấu "cần đồng ý" thì đồ loại đó Đang được sử dụng mà chưa có đồng ý được báo Hành chính và điều dưỡng
     * phụ trách để Thu lại (Trung bình); trạng thái không tự đổi.
     */
    private void notifyInUseWithoutConsent(long typeId, String typeName, long accountId) {
        for (BelongingView b : belongings.inUseOfType(typeId)) {
            if (consents.hasEffective(b.belongingId())) {
                continue;
            }
            events.publishEvent(BelongingNotices.notice(
                    "SELF_CUSTODY_CONSENT_REQUIRED_IN_USE",
                    "M12.BELONGING",
                    b.belongingId(),
                    NotificationLevel.MEDIUM,
                    b.residentId(),
                    List.of(
                            new RecipientGroup.FacilityRole(Role.OFFICE_STAFF),
                            new RecipientGroup.AssignedNurse(b.residentId(), new Substitution.Default())),
                    typeName + ": loại đồ nay cần đồng ý cho tự giữ, đồ đang được sử dụng chưa có đồng ý, cần thu lại",
                    accountId,
                    time.now()));
        }
    }

    @Transactional(readOnly = true)
    public List<ConditionLevelView> conditionLevels() {
        requireCatalogReader();
        return catalogs.list(BelongingCatalogs.CONDITION_LEVEL, null, false).stream()
                .map(item -> new ConditionLevelView(
                        item.id(),
                        item.name(),
                        item.sortOrder(),
                        item.attributes().unusable(),
                        item.status()))
                .toList();
    }

    /**
     * Thêm mức tình trạng (FR-001a). Mức "không dùng được" mang mã {@code UNUSABLE} nên mức cuối thứ hai bị 409. Hợp đồng
     * không có mã mức để ngừng nên trạng thái khác Hiệu lực bị 422 (plan "Kế hoạch triển khai R-15" điểm 7).
     */
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public long createConditionLevel(String name, int order, Boolean unusable, String status) {
        requireRole(Role.NURSING_HOME_MANAGER);
        if (status != null && !CatalogStatus.ACTIVE.name().equals(status)) {
            throw invalid("Chỉ thêm được mức Hiệu lực");
        }
        boolean last = Boolean.TRUE.equals(unusable);
        var created = catalogs.create(
                BelongingCatalogs.CONDITION_LEVEL,
                new NewCatalogItem<>(
                        last ? BelongingCatalogs.UNUSABLE_CODE : null,
                        requireName(name),
                        null,
                        order,
                        new ConditionLevelAttributes(last)));
        audit.write(AuditLogEntry.builder()
                .action(CONDITION_LEVEL_CREATED)
                .entity(BelongingCatalogs.CONDITION_LEVEL.code(), created.id())
                .after(Map.of("name", created.name(), "order", created.sortOrder(), "unusable", last))
                .build());
        return created.id();
    }

    @Transactional(readOnly = true)
    public List<StorageLocationView> storageLocations() {
        requireCatalogReader();
        return catalogs.list(BelongingCatalogs.STORAGE_LOCATION, null, false).stream()
                .map(BelongingCatalogService::location)
                .toList();
    }

    /** Hành chính tạo, sửa, ngừng vị trí (FR-002; khóa "Đồ gửi", thao tác Sửa). */
    @RequiresPermission(function = Function.BELONGINGS, operation = Operation.UPDATE)
    @Transactional
    public long saveStorageLocationAsOfficeStaff(
            Long id, String name, String description, Long floorAreaId, String status) {
        requireRole(Role.OFFICE_STAFF);
        return saveStorageLocation(id, name, description, floorAreaId, status);
    }

    /** Quản lý viện tạo, sửa, ngừng vị trí (FR-002; khóa cấu hình danh mục). */
    @RequiresPermission(function = Function.ACCOUNT_PERMISSION_PARAMETER, operation = Operation.UPDATE)
    @Transactional
    public long saveStorageLocationAsManager(
            Long id, String name, String description, Long floorAreaId, String status) {
        requireRole(Role.NURSING_HOME_MANAGER);
        return saveStorageLocation(id, name, description, floorAreaId, status);
    }

    private long saveStorageLocation(Long id, String name, String description, Long floorAreaId, String status) {
        var attributes = new StorageLocationAttributes(blankToNull(description), floorAreaId);
        if (id == null) {
            if (status != null && !CatalogStatus.ACTIVE.name().equals(status)) {
                throw invalid("Vị trí mới phải Hiệu lực");
            }
            var created = catalogs.create(
                    BelongingCatalogs.STORAGE_LOCATION,
                    new NewCatalogItem<>(null, requireName(name), null, 0, attributes));
            audit.write(AuditLogEntry.builder()
                    .action(STORAGE_LOCATION_SAVED)
                    .entity(BelongingCatalogs.STORAGE_LOCATION.code(), created.id())
                    .after(locationSnapshot(created))
                    .build());
            return created.id();
        }
        var before = catalogs.require(BelongingCatalogs.STORAGE_LOCATION, id);
        if (CatalogStatus.INACTIVE.name().equals(status)
                && before.status() == CatalogStatus.ACTIVE
                && belongings.countHeldOrDamagedAt(id) > 0) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "BELONGINGS_AT_LOCATION",
                    "Vị trí còn đồ Đang giữ hoặc Hư hỏng; chuyển giữ hết đồ trước khi ngừng (FR-002)");
        }
        var changed = catalogs.update(
                        BelongingCatalogs.STORAGE_LOCATION,
                        id,
                        before.version(),
                        requireName(name),
                        before.sortOrder(),
                        attributes)
                .after();
        changed = applyStatus(BelongingCatalogs.STORAGE_LOCATION, changed, status);
        audit.write(AuditLogEntry.builder()
                .action(STORAGE_LOCATION_SAVED)
                .entity(BelongingCatalogs.STORAGE_LOCATION.code(), id)
                .before(locationSnapshot(before))
                .after(locationSnapshot(changed))
                .build());
        return id;
    }

    private <T> CatalogItem<T> applyStatus(
            com.eldercare.backend.administration.shared.api.dto.SharedKind<T> kind,
            CatalogItem<T> item,
            String status) {
        if (status == null || status.equals(item.status().name())) {
            return item;
        }
        CatalogStatus wanted;
        try {
            wanted = CatalogStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw invalid("Trạng thái không hợp lệ: " + status);
        }
        return wanted == CatalogStatus.ACTIVE
                ? catalogs.reactivate(kind, item.id(), item.version()).after()
                : catalogs.deactivate(kind, item.id(), item.version()).after();
    }

    private ValuableCategories valuableCategories() {
        var value = parameters.read(ParameterCode.CFG_M12_01);
        return value.isSet() ? value.asTable(ValuableCategories.class) : new ValuableCategories(List.of());
    }

    private static BelongingTypeView type(CatalogItem<BelongingTypeAttributes> item, ValuableCategories valuable) {
        return new BelongingTypeView(
                item.id(),
                item.name(),
                item.attributes().hasCashAmount(),
                item.attributes().requiresConsentForUse(),
                valuable.contains(item.name()),
                item.status(),
                item.version());
    }

    private static StorageLocationView location(CatalogItem<StorageLocationAttributes> item) {
        return new StorageLocationView(
                item.id(),
                item.name(),
                item.attributes().description(),
                item.attributes().floorAreaId(),
                item.status());
    }

    private static Map<String, Object> typeSnapshot(CatalogItem<BelongingTypeAttributes> item) {
        return Map.of(
                "name", item.name(),
                "hasCashAmount", item.attributes().hasCashAmount(),
                "requiresConsentForUse", item.attributes().requiresConsentForUse(),
                "status", item.status().name());
    }

    private static Map<String, Object> locationSnapshot(CatalogItem<StorageLocationAttributes> item) {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("name", item.name());
        snapshot.put("description", item.attributes().description());
        snapshot.put("floorAreaId", item.attributes().floorAreaId());
        snapshot.put("status", item.status().name());
        return snapshot;
    }

    /**
     * Đọc danh mục dùng chung: vai trò có dòng "Đồ gửi" ở Phụ lục 27 (Quản lý viện, Trưởng tầng, Điều dưỡng, Hành chính).
     * Không dùng {@code @RequiresPermission} vì danh mục không gắn người cao tuổi nên phạm vi "được phân công" của
     * Điều dưỡng, Trưởng tầng luôn trượt lớp phạm vi (plan "Kế hoạch triển khai R-15" điểm 3).
     */
    private void requireCatalogReader() {
        Set<Role> roles = users.getObject().roles();
        if (roles.stream().noneMatch(CATALOG_READERS::contains)) {
            throw new PermissionDeniedException("Không có quyền xem danh mục đồ gửi");
        }
    }

    private CurrentUser requireRole(Role role) {
        CurrentUser user = users.getObject();
        if (!user.roles().contains(role)) {
            throw new PermissionDeniedException("Thao tác danh mục đồ gửi chỉ dành cho vai trò " + role);
        }
        return user;
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw invalid("Thiếu tên");
        }
        return name.strip();
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "INVALID_VALUE", message);
    }
}
