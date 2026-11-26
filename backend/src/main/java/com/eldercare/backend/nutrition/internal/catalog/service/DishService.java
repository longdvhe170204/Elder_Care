package com.eldercare.backend.nutrition.internal.catalog.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.file.FileApi;
import com.eldercare.backend.common.file.FileTypes;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.nutrition.api.events.MenuDishViolation;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishAttributes;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishCommand;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions.Outcome;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh mục món ăn (spec 011 mục A, FR-004, FR-004a; Q-445): Nhân viên bếp tạo món gồm tên, mô tả, ảnh; Nháp → Hiệu lực
 * bằng lệnh Đưa vào dùng, không có bước xác nhận của Bác sĩ; sửa áp dụng ngay.
 */
@Service
public class DishService {

    /** Loại chủ sở hữu tệp ảnh món ({@code FileApi.attach}). */
    public static final String IMAGE_OWNER = "M08.DISH_IMAGE";

    /** Ảnh món tối đa 5 MB (spec 011 FR-004). */
    static final long MAX_IMAGE_SIZE = 5L * 1024 * 1024;

    private static final Set<String> IMAGE_TYPES = Set.of(FileTypes.JPEG, FileTypes.PNG);
    private static final String ENTITY = "M08.DISH";

    private final NutritionCatalogStore store;
    private final AuditLogApi audit;
    private final TechnicalLocks locks;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final MenuStore menus;
    private final FileApi files;

    public DishService(
            NutritionCatalogStore store,
            AuditLogApi audit,
            TechnicalLocks locks,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events,
            MenuStore menus,
            FileApi files) {
        this.store = store;
        this.audit = audit;
        this.locks = locks;
        this.user = user;
        this.time = time;
        this.events = events;
        this.menus = menus;
        this.files = files;
    }

    /** Thân lệnh tạo, sửa món (api-011 {@code DishCommand}). */
    public record DishInput(String name, String description) {}

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Dish> list(DishStatus status) {
        return store.dishes().stream()
                .filter(d -> status == null || d.status() == status)
                .toList();
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Dish create(DishInput input) {
        Dish dish = store.createDish(name(input.name()), DishAttributes.draft(description(input.description())));
        log(dish.id(), "DISH_CREATED", Map.of(), snapshot(dish), null);
        return dish;
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Dish update(long id, String ifMatch, DishInput input) {
        locks.lock(ENTITY + ":" + id);
        Dish dish = store.dish(id);
        VersionEtag.requireMatch(ifMatch, dish.version(), Map.of("id", id, "version", dish.version()));
        if (dish.status() != DishStatus.DRAFT && dish.status() != DishStatus.ACTIVE) {
            throw stateChanged();
        }
        Dish after = store.updateDish(
                dish, name(input.name()), dish.attributes().withDescription(description(input.description())));
        log(id, "DISH_UPDATED", snapshot(dish), snapshot(after), null);
        return after;
    }

    /** Lệnh trên món theo bảng trạng thái (FR-004a); đều của Nhân viên bếp. */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void command(long id, String path, Reason reason) {
        DishCommand command = DishCommand.fromPath(path).orElseThrow(NotFoundException::new);
        if (command.requiresReason() && reason == null) {
            throw new ReasonRequiredException();
        }
        locks.lock(ENTITY + ":" + id);
        Dish dish = store.dish(id);
        Outcome outcome = DishTransitions.apply(dish.status(), command);
        if (!outcome.allowed()) {
            throw new BusinessException(HttpStatus.CONFLICT, outcome.errorCode(), "Lệnh không hợp lệ ở trạng thái món");
        }
        switch (command) {
            case ACTIVATE -> {
                Dish updated =
                        store.updateDish(dish, dish.name(), dish.attributes().activated());
                store.confirmDish(updated, user.accountId());
                log(id, "DISH_ACTIVATED", Map.of("status", "DRAFT"), Map.of("status", "ACTIVE"), null);
            }
            case CANCEL -> {
                if (dish.attributes().everActive()) {
                    throw stateChanged();
                }
                Dish updated =
                        store.updateDish(dish, dish.name(), dish.attributes().withStatus(DishStatus.CANCELLED));
                store.closeDish(updated);
                log(id, "DISH_CANCELLED", Map.of("status", "DRAFT"), Map.of("status", "CANCELLED"), null);
            }
            case DEACTIVATE -> deactivate(dish, reason);
        }
    }

    /** Tải lên hoặc thay ảnh món (FR-004): JPEG, PNG, tối đa 5 MB; tệp mới gắn vào món, tệp cũ không còn được trỏ tới. */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Dish setImage(long id, String originalName, String mimeType, byte[] content) {
        FileTypes.check(mimeType, content, IMAGE_TYPES);
        if (content.length > MAX_IMAGE_SIZE) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_CONTENT, "FILE_TOO_LARGE", "Ảnh món tối đa 5 MB");
        }
        locks.lock(ENTITY + ":" + id);
        Dish dish = editable(id);
        long fileId = files.upload(originalName, mimeType, content, user.accountId());
        files.attach(fileId, IMAGE_OWNER, id);
        Dish after = store.updateDish(dish, dish.name(), dish.attributes().withImage(fileId));
        log(id, "DISH_IMAGE_SET", image(dish), image(after), null);
        return after;
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void removeImage(long id) {
        locks.lock(ENTITY + ":" + id);
        Dish dish = editable(id);
        if (dish.attributes().imageFileId() == null) {
            return;
        }
        Dish after = store.updateDish(dish, dish.name(), dish.attributes().withImage(null));
        log(id, "DISH_IMAGE_REMOVED", image(dish), image(after), null);
    }

    private Dish editable(long id) {
        Dish dish = store.dish(id);
        if (dish.status() != DishStatus.DRAFT && dish.status() != DishStatus.ACTIVE) {
            throw stateChanged();
        }
        return dish;
    }

    private void deactivate(Dish dish, Reason reason) {
        Instant now = time.now();
        Dish updated = store.updateDish(
                dish, dish.name(), dish.attributes().deactivated(reason.value(), user.accountId(), now));
        store.closeDish(updated);
        log(dish.id(), "DISH_DEACTIVATED", Map.of("status", "ACTIVE"), Map.of("status", "INACTIVE"), reason);
        int inMenus = menus.notEnded(time.today()).stream()
                .filter(m -> m.status().published())
                .flatMap(m -> m.content().dishes().stream())
                .mapToInt(d -> d.dishId() == dish.id() ? 1 : 0)
                .sum();
        if (inMenus > 0) {
            events.publishEvent(
                    new MenuDishViolation("DISH_DEACTIVATED", dish.id(), dish.name(), inMenus, user.accountId(), now));
        }
    }

    private static String name(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 200) {
            throw new InvalidValueException("Tên món có từ 1 đến 200 ký tự");
        }
        return name.strip();
    }

    private static String description(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        if (description.strip().length() > 2000) {
            throw new InvalidValueException("Mô tả món tối đa 2000 ký tự");
        }
        return description.strip();
    }

    private static BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Lệnh không hợp lệ ở trạng thái món");
    }

    private void log(long id, String action, Map<String, Object> before, Map<String, Object> after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction(action, reason != null))
                .entity(ENTITY, id)
                .before(before)
                .after(after)
                .reason(reason)
                .build());
    }

    private static Map<String, Object> snapshot(Dish dish) {
        Map<String, Object> out = new HashMap<>();
        out.put("name", dish.name());
        out.put("description", dish.attributes().description());
        out.put("status", dish.status().name());
        return out;
    }

    private static Map<String, Object> image(Dish dish) {
        Map<String, Object> out = new HashMap<>();
        out.put("imageFileId", dish.attributes().imageFileId());
        return out;
    }
}
