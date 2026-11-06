package com.eldercare.backend.nutrition.internal.familyfood.service;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.nutrition.internal.familyfood.domain.ForbiddenFoodType;
import com.eldercare.backend.nutrition.internal.familyfood.persistence.FamilyFoodStore;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục loại đồ ăn bị cấm theo chính sách của cơ sở (FR-007): thêm loại mới, hoặc ngừng loại đang Hiệu lực (gửi lại tên
 * với trạng thái {@code INACTIVE}, vì hợp đồng chỉ có GET và POST). Đồ ăn gia đình đối chiếu theo tên loại (FR-053).
 *
 * <p>Quyền: Phụ lục 27 chưa có dòng quyền riêng cho danh mục này; tạm dùng "Chế độ ăn" (đọc: "Đối chiếu đồ ăn gia đình")
 * cho tới khi A thêm Function (team-plan mục 8).
 */
@Service
public class ForbiddenFoodTypeService {

    private static final String ENTITY = "M08.FORBIDDEN_FOOD_TYPE";
    private static final AuditAction CREATED = new AuditAction("FORBIDDEN_FOOD_TYPE_CREATED", false);
    private static final AuditAction DEACTIVATED = new AuditAction("FORBIDDEN_FOOD_TYPE_DEACTIVATED", false);

    private final FamilyFoodStore store;
    private final AuditLogApi audit;

    public ForbiddenFoodTypeService(FamilyFoodStore store, AuditLogApi audit) {
        this.store = store;
        this.audit = audit;
    }

    @RequiresPermission(function = Function.FAMILY_FOOD_CHECK, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<CatalogItem<ForbiddenFoodType>> list() {
        return store.forbiddenTypes();
    }

    @RequiresPermission(function = Function.DIET, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void upsert(String name, String description, boolean active) {
        if (name == null || name.isBlank()) {
            throw new InvalidValueException("Tên loại đồ ăn bị cấm là bắt buộc");
        }
        String clean = name.strip();
        if (active) {
            CatalogItem<ForbiddenFoodType> created = store.createForbidden(
                    clean, new ForbiddenFoodType(description == null ? null : description.strip()));
            audit.write(AuditLogEntry.builder()
                    .action(CREATED)
                    .entity(ENTITY, created.id())
                    .after(Map.of("name", clean))
                    .build());
            return;
        }
        CatalogItem<ForbiddenFoodType> current = store.forbiddenTypes().stream()
                .filter(t -> t.status() == com.eldercare.backend.common.data.CatalogStatus.ACTIVE
                        && t.name().equalsIgnoreCase(clean))
                .findFirst()
                .orElseThrow(NotFoundException::new);
        store.deactivateForbidden(current);
        audit.write(AuditLogEntry.builder()
                .action(DEACTIVATED)
                .entity(ENTITY, current.id())
                .before(Map.of("name", clean, "status", "ACTIVE"))
                .after(Map.of("name", clean, "status", "INACTIVE"))
                .build());
    }
}
