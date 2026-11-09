package com.eldercare.backend.nutrition.internal.catalog.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.nutrition.internal.catalog.domain.MealAttributes;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.menu.service.MealTimes;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục bữa ăn (FR-006, Q-401: Quản lý viện tạo, sửa; Bác sĩ, Điều dưỡng, Nhân viên bếp xem). Bữa không xóa được, chỉ
 * Ngừng hiệu lực ({@code status = INACTIVE}). Giờ dự kiến, ngưỡng giao trễ là CFG-M08-04 nên chỉ đọc; lần chốt đã lưu giờ
 * của nó (FR-006 "đổi giờ chỉ áp cho bữa chưa chốt"). Mã bữa là khóa của {@code mealTimes} ở CFG-M08-04 nên không đổi.
 */
@Service
public class MealService {

    private static final String ENTITY = "M08.MEAL";
    private static final AuditAction CREATED = new AuditAction("MEAL_CREATED", false);
    private static final AuditAction UPDATED = new AuditAction("MEAL_UPDATED", false);

    private final NutritionCatalogStore store;
    private final MealTimes times;
    private final AuditLogApi audit;

    public MealService(NutritionCatalogStore store, MealTimes times, AuditLogApi audit) {
        this.store = store;
        this.times = times;
        this.audit = audit;
    }

    /** Một bữa kèm giờ dự kiến ({@code null} khi viện chưa đặt) và ngưỡng giao trễ (phút). */
    public record MealRow(CatalogItem<MealAttributes> item, LocalTime expectedTime, int lateDeliveryThresholdMinutes) {}

    public record MealInput(
            String code, String name, boolean servesResidential, boolean servesDayCare, int sortOrder, String status) {}

    @RequiresPermission(function = Function.NUTRITION_CATALOG, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<MealRow> list() {
        int threshold = (int) times.lateThreshold().toMinutes();
        return store.mealItems().stream()
                .map(m -> new MealRow(m, times.expected(m.id()).orElse(null), threshold))
                .toList();
    }

    @RequiresPermission(function = Function.NUTRITION_CATALOG, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long create(MealInput input) {
        String code = required(input.code(), "Mã bữa");
        String name = required(input.name(), "Tên bữa");
        CatalogItem<MealAttributes> meal = store.createMeal(code, name, input.sortOrder(), attributes(input));
        if (inactive(input.status())) {
            meal = store.deactivateMeal(meal);
        }
        audit.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY, meal.id())
                .after(snapshot(meal))
                .build());
        return meal.id();
    }

    @RequiresPermission(function = Function.NUTRITION_CATALOG, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void update(long id, MealInput input) {
        CatalogItem<MealAttributes> before = store.mealItem(id);
        if (input.code() != null && !input.code().strip().equals(before.code())) {
            throw new InvalidValueException("Mã bữa là khóa giờ bữa của CFG-M08-04, không đổi được");
        }
        CatalogItem<MealAttributes> after =
                store.updateMeal(before, required(input.name(), "Tên bữa"), input.sortOrder(), attributes(input));
        boolean wantInactive = inactive(input.status());
        if (wantInactive && after.status() == CatalogStatus.ACTIVE) {
            after = store.deactivateMeal(after);
        } else if (!wantInactive && after.status() != CatalogStatus.ACTIVE) {
            after = store.reactivateMeal(after);
        }
        audit.write(AuditLogEntry.builder()
                .action(UPDATED)
                .entity(ENTITY, id)
                .before(snapshot(before))
                .after(snapshot(after))
                .build());
    }

    private static MealAttributes attributes(MealInput input) {
        return new MealAttributes(input.servesResidential(), input.servesDayCare());
    }

    private static boolean inactive(String status) {
        return "INACTIVE".equals(status);
    }

    private static String required(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new InvalidValueException(label + " là bắt buộc");
        }
        return value.strip();
    }

    private static Map<String, Object> snapshot(CatalogItem<MealAttributes> m) {
        return Map.of(
                "name", m.name(),
                "status", m.status().name(),
                "servesResidential", m.attributes().servesResidential(),
                "servesDayCare", m.attributes().servesDayCare(),
                "sortOrder", m.sortOrder());
    }
}
