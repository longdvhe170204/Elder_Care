package com.eldercare.backend.nutrition.internal.catalog.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.DietAttributes;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.SafeDishRule;
import com.eldercare.backend.nutrition.internal.catalog.domain.SafeDishRule.SafeDish;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.eldercare.backend.resident.api.dto.CreateAllergen;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục chế độ ăn (FR-001 → FR-003, FR-005, FR-014; research M-04): Bác sĩ tạo, sửa, ngừng; đúng một chế độ ăn mặc
 * định (đặt mặc định cho chế độ khác thì chuyển trong cùng lệnh, chủ dự án duyệt 2026-11-06). Chặn ngừng khi còn bản gán
 * hoặc thực đơn chưa hết tuần (FR-002) và kiểm lại thực đơn khi đổi hạn chế cài ở phase của bản gán, thực đơn.
 */
@Service
public class DietService {

    private static final AuditAction CREATED = new AuditAction("DIET_CREATED", false);
    private static final AuditAction UPDATED = new AuditAction("DIET_UPDATED", false);
    private static final AuditAction DEACTIVATED = new AuditAction("DIET_DEACTIVATED", true);
    private static final String ENTITY = "M08.DIET";
    private static final String DEFAULT_LOCK = "M08.DIET.DEFAULT";

    private final NutritionCatalogStore store;
    private final AllergenApi allergens;
    private final AuditLogApi audit;
    private final TechnicalLocks locks;

    public DietService(NutritionCatalogStore store, AllergenApi allergens, AuditLogApi audit, TechnicalLocks locks) {
        this.store = store;
        this.allergens = allergens;
        this.audit = audit;
        this.locks = locks;
    }

    /** Thân lệnh tạo, sửa chế độ ăn (api-011 {@code DietCommand}). */
    public record DietInput(
            String name,
            String description,
            boolean treatmentRelated,
            boolean isDefault,
            List<Long> restrictions,
            Long safeDishId) {}

    @RequiresPermission(function = Function.DIET, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Diet> list() {
        return store.diets();
    }

    @RequiresPermission(function = Function.DIET, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Diet create(DietInput input) {
        String name = name(input.name());
        DietAttributes attributes = attributes(input);
        validate(attributes);
        if (attributes.isDefault()) {
            clearOtherDefault(null);
        }
        Diet diet = store.createDiet(name, attributes);
        audit.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY, diet.id())
                .after(snapshot(diet))
                .build());
        return diet;
    }

    @RequiresPermission(function = Function.DIET, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Diet update(long id, String ifMatch, DietInput input) {
        Diet current = store.diet(id);
        VersionEtag.requireMatch(ifMatch, current.version(), snapshot(current));
        String name = name(input.name());
        DietAttributes attributes = attributes(input);
        if (current.attributes().isDefault() && !attributes.isDefault()) {
            throw blocked("DEFAULT_DIET_REQUIRED", "Phải đặt chế độ ăn khác làm mặc định trước (FR-001)");
        }
        validate(attributes);
        if (attributes.isDefault() && !current.attributes().isDefault()) {
            clearOtherDefault(id);
            current = store.diet(id);
        }
        Diet after = store.updateDiet(current, name, attributes);
        audit.write(AuditLogEntry.builder()
                .action(UPDATED)
                .entity(ENTITY, id)
                .before(snapshot(current))
                .after(snapshot(after))
                .build());
        return after;
    }

    @RequiresPermission(function = Function.DIET, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public Diet deactivate(long id, Reason reason) {
        Diet current = store.diet(id);
        if (!current.active()) {
            throw new com.eldercare.backend.common.error.BusinessException(
                    org.springframework.http.HttpStatus.CONFLICT, "STATE_CHANGED", "Chế độ ăn đã Ngừng hiệu lực");
        }
        if (current.attributes().isDefault()) {
            throw blocked("DEFAULT_DIET", "Chế độ ăn mặc định không Ngừng hiệu lực được (FR-001)");
        }
        Diet after = store.deactivateDiet(current);
        audit.write(AuditLogEntry.builder()
                .action(DEACTIVATED)
                .entity(ENTITY, id)
                .before(Map.of("status", current.status().name()))
                .after(Map.of("status", after.status().name()))
                .reason(reason)
                .build());
        return after;
    }

    /** FR-003: Bác sĩ thêm thành phần thực phẩm vào danh mục dị nguyên của 001 (001 ghi nhật ký). */
    @RequiresPermission(function = Function.DIET, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long addFoodIngredient(String name) {
        return allergens.addAllergen(new CreateAllergen(name, AllergenGroup.FOOD));
    }

    private void clearOtherDefault(Long keepId) {
        locks.lock(DEFAULT_LOCK);
        for (Diet diet : store.diets()) {
            if (diet.active() && diet.attributes().isDefault() && (keepId == null || diet.id() != keepId)) {
                Diet after =
                        store.updateDiet(diet, diet.name(), diet.attributes().withDefault(false));
                audit.write(AuditLogEntry.builder()
                        .action(UPDATED)
                        .entity(ENTITY, diet.id())
                        .before(Map.of("isDefault", true))
                        .after(Map.of("isDefault", after.attributes().isDefault()))
                        .build());
            }
        }
    }

    private void validate(DietAttributes attributes) {
        List<UnmetCondition> unmet = new ArrayList<>();
        if (attributes.isDefault() && attributes.treatmentRelated()) {
            unmet.add(new UnmetCondition(
                    "DEFAULT_DIET_TREATMENT_RELATED", "Chế độ ăn mặc định không liên quan điều trị (FR-001)"));
        }
        for (Long allergen : attributes.restrictions()) {
            if (allergens.allergen(allergen).isEmpty()) {
                throw new InvalidValueException("Hạn chế không có trong danh mục dị nguyên: " + allergen);
            }
        }
        List<Dish> dishes = store.dishes();
        Optional<SafeDish> safeDish = Optional.ofNullable(attributes.safeDishId())
                .map(id -> dishes.stream()
                        .filter(d -> d.id() == id)
                        .findFirst()
                        .map(d -> new SafeDish(d.status(), new HashSet<>(effectiveIngredients(d))))
                        .orElse(new SafeDish(null, java.util.Set.of())));
        boolean anyActive = dishes.stream().anyMatch(d -> d.status() == DishStatus.ACTIVE);
        unmet.addAll(SafeDishRule.check(safeDish, new HashSet<>(attributes.restrictions()), anyActive));
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
    }

    private List<Long> effectiveIngredients(Dish dish) {
        Long versionId = dish.attributes().effectiveVersionId();
        return versionId == null
                ? List.of()
                : store.version(versionId).content().ingredients();
    }

    private static DietAttributes attributes(DietInput input) {
        List<Long> restrictions = input.restrictions() == null
                ? List.of()
                : input.restrictions().stream().distinct().toList();
        return new DietAttributes(
                input.description(), input.treatmentRelated(), input.isDefault(), restrictions, input.safeDishId());
    }

    private static String name(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 200) {
            throw new InvalidValueException("Tên chế độ ăn có từ 1 đến 200 ký tự");
        }
        return name.strip();
    }

    private static ConditionNotMetException blocked(String code, String description) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, description)));
    }

    private static Map<String, Object> snapshot(Diet diet) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", diet.name());
        out.put("treatmentRelated", diet.attributes().treatmentRelated());
        out.put("isDefault", diet.attributes().isDefault());
        out.put("restrictions", diet.attributes().restrictions());
        out.put("safeDishId", diet.attributes().safeDishId());
        out.put("status", diet.status().name());
        return out;
    }
}
