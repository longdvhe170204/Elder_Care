package com.eldercare.backend.resident.internal.health.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.CatalogBase;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.eldercare.backend.resident.internal.health.domain.Allergen;
import com.eldercare.backend.resident.internal.health.domain.CatalogItemView;
import com.eldercare.backend.resident.internal.health.domain.CatalogOrigin;
import com.eldercare.backend.resident.internal.health.persistence.AllergenRepository;
import java.util.Comparator;
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
 * nhật ký. Mức chăm sóc cố định, chỉ đọc. Mục đã được tham chiếu không xóa, chỉ ngừng hiệu lực (FR-003). Ba danh mục
 * mức độ dị ứng, nguồn thông tin, nhu cầu chăm sóc và mức chăm sóc nằm ở {@code catalog_item} (001 T084); dị nguyên còn
 * ở bảng riêng.
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

    /** Giá trị trước và sau một lệnh; {@code before} rỗng với lệnh tạo. */
    private record Result(CatalogItemView before, CatalogItemView after) {}

    /** Một loại danh mục: các lệnh trên loại đó, trả về bản xem trước và sau để ghi nhật ký. */
    private interface Kind {
        String entityType();

        List<CatalogItemView> all();

        Result create(Input input);

        Result update(long id, String ifMatch, Input input);

        Result deactivate(long id);

        Result reactivate(long id);
    }

    private final Map<String, Kind> kinds = new LinkedHashMap<>();
    private final CatalogApi catalog;
    private final AuditLogApi auditLog;

    CatalogService(AllergenRepository allergens, CatalogApi catalog, AuditLogApi auditLog) {
        this.catalog = catalog;
        this.auditLog = auditLog;
        kinds.put("allergens", new AllergenKind(allergens));
        kinds.put("allergy-severities", new SharedCatalogKind(M01Catalogs.ALLERGY_SEVERITY, true));
        kinds.put("information-sources", new SharedCatalogKind(M01Catalogs.INFORMATION_SOURCE, false));
        kinds.put("care-needs", new SharedCatalogKind(M01Catalogs.CARE_NEED, false));
    }

    /** {@code group} chỉ lọc dị nguyên; {@code status} rỗng là mọi trạng thái. */
    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogItemView> list(String type, String group, Optional<CatalogStatus> status) {
        if (CARE_LEVELS.equals(type)) {
            return catalog.list(M01Catalogs.CARE_LEVEL, null, true).stream()
                    .map(i ->
                            new CatalogItemView(i.sortOrder(), i.name(), null, i.sortOrder(), CatalogStatus.ACTIVE, 0))
                    .toList();
        }
        return kind(type).all().stream()
                .filter(i -> status.isEmpty() || i.status() == status.get())
                .filter(i -> group == null || group.equals(i.group()))
                .toList();
    }

    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.CREATE)
    @Transactional
    public CatalogItemView create(String type, Input input) {
        Kind kind = kind(type);
        Result result = kind.create(input);
        audit(CREATED, kind, result.after().id(), null, snapshot(result.after()), null);
        return result.after();
    }

    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.UPDATE)
    @Transactional
    public CatalogItemView update(String type, long id, String ifMatch, Input input) {
        Kind kind = kind(type);
        Result result = kind.update(id, ifMatch, input);
        audit(UPDATED, kind, id, snapshot(result.before()), snapshot(result.after()), null);
        return result.after();
    }

    @BusinessCommand
    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.UPDATE)
    @Transactional
    public CatalogItemView deactivate(String type, long id, Reason reason) {
        Kind kind = kind(type);
        Result result = kind.deactivate(id);
        audit(DEACTIVATED, kind, id, snapshot(result.before()), snapshot(result.after()), reason);
        return result.after();
    }

    @BusinessCommand
    @RequiresPermission(function = Function.HEALTH_CATALOGS, operation = Operation.UPDATE)
    @Transactional
    public CatalogItemView reactivate(String type, long id, Reason reason) {
        Kind kind = kind(type);
        Result result = kind.reactivate(id);
        audit(REACTIVATED, kind, id, snapshot(result.before()), snapshot(result.after()), reason);
        return result.after();
    }

    private Kind kind(String type) {
        if (CARE_LEVELS.equals(type)) {
            throw new BusinessException(
                    HttpStatus.METHOD_NOT_ALLOWED, "READ_ONLY_CATALOG", "Mức chăm sóc cố định, chỉ đọc");
        }
        Kind kind = kinds.get(type);
        if (kind == null) {
            throw new NotFoundException();
        }
        return kind;
    }

    private static BusinessException duplicateName() {
        return new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_NAME", "Đã có mục đang hiệu lực cùng tên");
    }

    private static AllergenGroup allergenGroup(String group) {
        try {
            return AllergenGroup.valueOf(group);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new InvalidValueException("Dị nguyên cần nhóm ACTIVE_INGREDIENT, FOOD hoặc OTHER");
        }
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
            Kind kind,
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

    /** Dị nguyên: còn ở bảng riêng {@code allergen} (id được 006 lưu, chờ A và D duyệt cách chuyển). */
    private static final class AllergenKind implements Kind {

        private final AllergenRepository repository;

        AllergenKind(AllergenRepository repository) {
            this.repository = repository;
        }

        @Override
        public String entityType() {
            return "M01.ALLERGEN";
        }

        @Override
        public List<CatalogItemView> all() {
            return repository.findAll().stream()
                    .sorted(Comparator.comparing(CatalogBase::getId))
                    .map(AllergenKind::view)
                    .toList();
        }

        @Override
        public Result create(Input input) {
            Allergen created = new Allergen(input.name().strip(), allergenGroup(input.group()));
            requireNameFree(created, null);
            return new Result(null, save(created));
        }

        @Override
        public Result update(long id, String ifMatch, Input input) {
            Allergen item = locked(id);
            CatalogItemView before = view(item);
            VersionEtag.requireMatch(ifMatch, item.getVersion(), before);
            item.change(input.name().strip(), allergenGroup(input.group()));
            if (item.isActive()) {
                requireNameFree(item, id);
            }
            return new Result(before, save(item));
        }

        @Override
        public Result deactivate(long id) {
            Allergen item = locked(id);
            CatalogItemView before = view(item);
            item.deactivate();
            return new Result(before, save(item));
        }

        @Override
        public Result reactivate(long id) {
            Allergen item = locked(id);
            CatalogItemView before = view(item);
            item.reactivate(repository.findByNameAndStatus(item.getName(), CatalogStatus.ACTIVE));
            return new Result(before, save(item));
        }

        /** Kiểm trước để không đẩy lỗi khóa duy nhất vào phiên; UNIQUE (active_name) vẫn chặn khi hai lệnh chạy cùng lúc. */
        private void requireNameFree(Allergen item, Long selfId) {
            repository
                    .findByNameAndStatus(item.getName(), CatalogStatus.ACTIVE)
                    .filter(other -> !other.getId().equals(selfId))
                    .ifPresent(other -> {
                        throw duplicateName();
                    });
        }

        private CatalogItemView save(Allergen item) {
            try {
                return view(repository.saveAndFlush(item));
            } catch (DataIntegrityViolationException e) {
                // UNIQUE (active_name): hai mục Hiệu lực không trùng tên (BR-M15-06)
                throw duplicateName();
            }
        }

        private Allergen locked(long id) {
            return repository.lockById(id).orElseThrow(NotFoundException::new);
        }

        private static CatalogItemView view(Allergen e) {
            return new CatalogItemView(
                    e.getId(), e.getName(), e.getGroup().name(), null, e.getStatus(), e.getVersion());
        }
    }

    /** Danh mục ở {@code catalog_item}: nhật ký, ETag do dịch vụ này làm; khóa, trùng tên, phiên bản do {@link CatalogApi}. */
    private final class SharedCatalogKind implements Kind {

        private final SharedKind<CatalogOrigin> kind;
        private final boolean ordered;

        SharedCatalogKind(SharedKind<CatalogOrigin> kind, boolean ordered) {
            this.kind = kind;
            this.ordered = ordered;
        }

        @Override
        public String entityType() {
            return kind.code();
        }

        @Override
        public List<CatalogItemView> all() {
            return catalog.list(kind, null, false).stream()
                    .sorted(Comparator.comparing(CatalogItem::id))
                    .map(this::view)
                    .toList();
        }

        @Override
        public Result create(Input input) {
            int order = input.order() != null && ordered ? input.order() : nextOrder();
            CatalogItem<CatalogOrigin> created = duplicateAsConflict(() -> catalog.create(
                    kind, new NewCatalogItem<>(null, input.name().strip(), null, order, new CatalogOrigin(null))));
            return new Result(null, view(created));
        }

        @Override
        public Result update(long id, String ifMatch, Input input) {
            CatalogItem<CatalogOrigin> current = catalog.require(kind, id);
            CatalogItemView before = view(current);
            VersionEtag.requireMatch(ifMatch, current.version(), before);
            int order = ordered && input.order() != null ? input.order() : current.sortOrder();
            var change = duplicateAsConflict(() ->
                    catalog.update(kind, id, current.version(), input.name().strip(), order, current.attributes()));
            return new Result(before, view(change.after()));
        }

        @Override
        public Result deactivate(long id) {
            CatalogItem<CatalogOrigin> current = catalog.require(kind, id);
            var change = catalog.deactivate(kind, id, current.version());
            return new Result(view(current), view(change.after()));
        }

        @Override
        public Result reactivate(long id) {
            CatalogItem<CatalogOrigin> current = catalog.require(kind, id);
            var change = duplicateAsConflict(() -> catalog.reactivate(kind, id, current.version()));
            return new Result(view(current), view(change.after()));
        }

        /** Trùng tên đang Hiệu lực trả mã {@code DUPLICATE_NAME} như các danh mục khác của M01 (BR-M15-06). */
        private <R> R duplicateAsConflict(java.util.function.Supplier<R> command) {
            try {
                return command.get();
            } catch (BusinessException e) {
                if (e.status() == HttpStatus.CONFLICT) {
                    throw duplicateName();
                }
                throw e;
            }
        }

        private int nextOrder() {
            return catalog.list(kind, null, false).stream()
                            .mapToInt(CatalogItem::sortOrder)
                            .max()
                            .orElse(0)
                    + 1;
        }

        private CatalogItemView view(CatalogItem<CatalogOrigin> i) {
            return new CatalogItemView(i.id(), i.name(), null, ordered ? i.sortOrder() : null, i.status(), i.version());
        }
    }
}
