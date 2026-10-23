package com.cambae.backend.m01.internal.health.service;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.common.data.CatalogBase;
import com.cambae.backend.common.data.CatalogStatus;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.dto.AllergenGroup;
import com.cambae.backend.m01.internal.health.domain.Allergen;
import com.cambae.backend.m01.internal.health.domain.AllergySeverity;
import com.cambae.backend.m01.internal.health.domain.CareNeed;
import com.cambae.backend.m01.internal.health.domain.CatalogItemView;
import com.cambae.backend.m01.internal.health.domain.InformationSource;
import com.cambae.backend.m01.internal.health.persistence.AllergenRepository;
import com.cambae.backend.m01.internal.health.persistence.AllergySeverityRepository;
import com.cambae.backend.m01.internal.health.persistence.CareLevelStore;
import com.cambae.backend.m01.internal.health.persistence.CareNeedRepository;
import com.cambae.backend.m01.internal.health.persistence.CatalogItemRepository;
import com.cambae.backend.m01.internal.health.persistence.InformationSourceRepository;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.auditlog.api.AuditAction;
import com.cambae.backend.m15.auditlog.api.AuditLogApi;
import com.cambae.backend.m15.auditlog.api.AuditLogEntry;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lệnh danh mục chung của M01 (001 T012; data-model mục 5 "Danh mục"; BR-M01-11, BR-M15-06; FR-018, FR-018a): dị
 * nguyên, mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc. Tạo, sửa, ngừng hiệu lực, kích hoạt lại theo quyền cấu hình
 * danh mục (khóa {@code HEALTH_CATALOGS}, Quản lý viện); ngừng hiệu lực và kích hoạt lại bắt buộc lý do; mọi lệnh ghi
 * nhật ký. Mức chăm sóc cố định, chỉ đọc. Mục đã được tham chiếu không xóa, chỉ ngừng hiệu lực (FR-003).
 */
@Service
public class CatalogService {

    static final AuditAction CREATED = new AuditAction("CATALOG_CREATED", false);
    static final AuditAction UPDATED = new AuditAction("CATALOG_UPDATED", false);
    static final AuditAction DEACTIVATED = new AuditAction("CATALOG_DEACTIVATED", true);
    static final AuditAction REACTIVATED = new AuditAction("CATALOG_REACTIVATED", true);
    private static final String CARE_LEVELS = "care-levels";

    /** Nội dung lệnh tạo, sửa; {@code group} chỉ cho dị nguyên, {@code order} chỉ cho mức độ dị ứng. */
    public record Input(String name, String group, Integer order) {}

    /** Một loại danh mục: cách tạo, đổi, dựng bản xem từ entity của nó. */
    private record Kind<E extends CatalogBase>(
            String entityType,
            CatalogItemRepository<E> repository,
            java.util.function.Function<Input, E> create,
            java.util.function.BiConsumer<E, Input> change,
            java.util.function.Function<E, CatalogItemView> view) {}

    private final Map<String, Kind<?>> kinds = new LinkedHashMap<>();
    private final CareLevelStore careLevels;
    private final AllergySeverityRepository severities;
    private final AuditLogApi auditLog;

    CatalogService(
            AllergenRepository allergens,
            AllergySeverityRepository severities,
            InformationSourceRepository sources,
            CareNeedRepository careNeeds,
            CareLevelStore careLevels,
            AuditLogApi auditLog) {
        this.severities = severities;
        this.careLevels = careLevels;
        this.auditLog = auditLog;
        kinds.put(
                "allergens",
                new Kind<Allergen>(
                        "M01.ALLERGEN",
                        allergens,
                        in -> new Allergen(in.name().strip(), allergenGroup(in.group())),
                        (e, in) -> e.change(in.name().strip(), allergenGroup(in.group())),
                        e -> view(e, e.getGroup().name(), null)));
        kinds.put(
                "allergy-severities",
                new Kind<AllergySeverity>(
                        "M01.ALLERGY_SEVERITY",
                        severities,
                        in -> new AllergySeverity(in.name().strip(), orderOrNext(in.order())),
                        (e, in) -> e.change(in.name().strip(), in.order() == null ? e.getDisplayOrder() : in.order()),
                        e -> view(e, null, e.getDisplayOrder())));
        kinds.put(
                "information-sources",
                new Kind<InformationSource>(
                        "M01.INFORMATION_SOURCE",
                        sources,
                        in -> new InformationSource(in.name().strip()),
                        (e, in) -> e.rename(in.name().strip()),
                        e -> view(e, null, null)));
        kinds.put(
                "care-needs",
                new Kind<CareNeed>(
                        "M01.CARE_NEED",
                        careNeeds,
                        in -> new CareNeed(in.name().strip()),
                        (e, in) -> e.rename(in.name().strip()),
                        e -> view(e, null, null)));
    }

    /** {@code group} chỉ lọc dị nguyên; {@code status} rỗng là mọi trạng thái. */
    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogItemView> list(String type, String group, Optional<CatalogStatus> status) {
        if (CARE_LEVELS.equals(type)) {
            return careLevels.all();
        }
        Kind<?> kind = kind(type);
        List<CatalogItemView> items = viewsOf(kind);
        return items.stream()
                .filter(i -> status.isEmpty() || i.status() == status.get())
                .filter(i -> group == null || group.equals(i.group()))
                .toList();
    }

    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.CREATE)
    @Transactional
    public CatalogItemView create(String type, Input input) {
        return create(kind(type), input);
    }

    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.UPDATE)
    @Transactional
    public CatalogItemView update(String type, long id, String ifMatch, Input input) {
        return update(kind(type), id, ifMatch, input);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.UPDATE)
    @Transactional
    public CatalogItemView deactivate(String type, long id, Reason reason) {
        return deactivate(kind(type), id, reason);
    }

    @BusinessCommand
    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.UPDATE)
    @Transactional
    public CatalogItemView reactivate(String type, long id, Reason reason) {
        return reactivate(kind(type), id, reason);
    }

    private <E extends CatalogBase> CatalogItemView create(Kind<E> kind, Input input) {
        E created = kind.create().apply(input);
        requireNameFree(kind, created, null);
        CatalogItemView after = save(kind, created);
        audit(CREATED, kind, after.id(), null, snapshot(after), null);
        return after;
    }

    private <E extends CatalogBase> CatalogItemView update(Kind<E> kind, long id, String ifMatch, Input input) {
        E item = locked(kind, id);
        CatalogItemView before = kind.view().apply(item);
        VersionEtag.requireMatch(ifMatch, item.getVersion(), before);
        kind.change().accept(item, input);
        if (item.isActive()) {
            requireNameFree(kind, item, id);
        }
        CatalogItemView after = save(kind, item);
        audit(UPDATED, kind, id, snapshot(before), snapshot(after), null);
        return after;
    }

    private <E extends CatalogBase> CatalogItemView deactivate(Kind<E> kind, long id, Reason reason) {
        E item = locked(kind, id);
        CatalogItemView before = kind.view().apply(item);
        item.deactivate();
        CatalogItemView after = save(kind, item);
        audit(DEACTIVATED, kind, id, snapshot(before), snapshot(after), reason);
        return after;
    }

    private <E extends CatalogBase> CatalogItemView reactivate(Kind<E> kind, long id, Reason reason) {
        E item = locked(kind, id);
        CatalogItemView before = kind.view().apply(item);
        item.reactivate(kind.repository().findByNameAndStatus(item.getName(), CatalogStatus.ACTIVE));
        CatalogItemView after = save(kind, item);
        audit(REACTIVATED, kind, id, snapshot(before), snapshot(after), reason);
        return after;
    }

    /** Kiểm trước để không đẩy lỗi khóa duy nhất vào phiên; UNIQUE (active_name) vẫn chặn khi hai lệnh chạy cùng lúc. */
    private <E extends CatalogBase> void requireNameFree(Kind<E> kind, E item, Long selfId) {
        kind.repository()
                .findByNameAndStatus(item.getName(), CatalogStatus.ACTIVE)
                .filter(other -> !other.getId().equals(selfId))
                .ifPresent(other -> {
                    throw duplicateName();
                });
    }

    private <E extends CatalogBase> CatalogItemView save(Kind<E> kind, E item) {
        try {
            return kind.view().apply(kind.repository().saveAndFlush(item));
        } catch (DataIntegrityViolationException e) {
            // UNIQUE (active_name): hai mục Hiệu lực không trùng tên (BR-M15-06)
            throw duplicateName();
        }
    }

    private static BusinessException duplicateName() {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Đã có mục đang hiệu lực cùng tên");
    }

    private <E extends CatalogBase> E locked(Kind<E> kind, long id) {
        return kind.repository().lockById(id).orElseThrow(NotFoundException::new);
    }

    private <E extends CatalogBase> List<CatalogItemView> viewsOf(Kind<E> kind) {
        return kind.repository().findAll().stream()
                .sorted(java.util.Comparator.comparing(CatalogBase::getId))
                .map(kind.view())
                .toList();
    }

    private Kind<?> kind(String type) {
        if (CARE_LEVELS.equals(type)) {
            throw new BusinessException(
                    HttpStatus.METHOD_NOT_ALLOWED, "READ_ONLY_CATALOG", "Mức chăm sóc cố định, chỉ đọc");
        }
        Kind<?> kind = kinds.get(type);
        if (kind == null) {
            throw new NotFoundException();
        }
        return kind;
    }

    private int orderOrNext(Integer order) {
        if (order != null) {
            return order;
        }
        return severities.findAll().stream()
                        .mapToInt(AllergySeverity::getDisplayOrder)
                        .max()
                        .orElse(0)
                + 1;
    }

    private static AllergenGroup allergenGroup(String group) {
        try {
            return AllergenGroup.valueOf(group);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException("Dị nguyên cần nhóm ACTIVE_INGREDIENT, FOOD hoặc OTHER");
        }
    }

    private static CatalogItemView view(CatalogBase e, String group, Integer order) {
        return new CatalogItemView(e.getId(), e.getName(), group, order, e.getStatus(), e.getVersion());
    }

    private static Map<String, Object> snapshot(CatalogItemView v) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", v.name());
        if (v.group() != null) {
            m.put("group", v.group());
        }
        if (v.order() != null) {
            m.put("order", v.order());
        }
        m.put("status", v.status().name());
        return m;
    }

    private void audit(
            AuditAction action,
            Kind<?> kind,
            long id,
            Map<String, Object> before,
            Map<String, Object> after,
            Reason reason) {
        AuditLogEntry.Builder entry = AuditLogEntry.builder()
                .action(action)
                .entity(kind.entityType(), id)
                .after(after);
        if (before != null) {
            entry.before(before);
        }
        if (reason != null) {
            entry.reason(reason);
        }
        auditLog.write(entry.build());
    }
}
