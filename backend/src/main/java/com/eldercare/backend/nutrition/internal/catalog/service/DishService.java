package com.eldercare.backend.nutrition.internal.catalog.service;

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
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.api.events.DishConfirmationRequested;
import com.eldercare.backend.nutrition.api.events.DishConfirmationResult;
import com.eldercare.backend.nutrition.api.events.MenuDishViolation;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishAttributes;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishCommand;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishTransitions.Outcome;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersion;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersionContent;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishVersionStatus;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import com.eldercare.backend.resident.api.AllergenApi;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Món ăn có xác nhận của Bác sĩ (FR-004, FR-004a, Q-308; research M-04): Nhân viên bếp tạo, sửa, gửi, rút, hủy, gửi bản
 * sửa; Bác sĩ xác nhận, trả lại; cả hai ngừng hiệu lực. Mỗi nội dung là một phiên bản ({@code M08.DISH_VERSION}); món
 * Hiệu lực dùng phiên bản đã xác nhận cho tới khi Bác sĩ xác nhận bản sửa. Kiểm tra lại thực đơn chưa hết tuần khi xác
 * nhận bản sửa (FR-004) cài ở phase thực đơn.
 */
@Service
public class DishService {

    private static final String ENTITY = "M08.DISH";

    private final NutritionCatalogStore store;
    private final AllergenApi allergens;
    private final AuditLogApi audit;
    private final TechnicalLocks locks;
    private final PermissionApi permissions;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final MenuStore menus;
    private final PortionClosingService closing;

    public DishService(
            NutritionCatalogStore store,
            AllergenApi allergens,
            AuditLogApi audit,
            TechnicalLocks locks,
            PermissionApi permissions,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events,
            MenuStore menus,
            PortionClosingService closing) {
        this.store = store;
        this.allergens = allergens;
        this.audit = audit;
        this.locks = locks;
        this.permissions = permissions;
        this.user = user;
        this.time = time;
        this.events = events;
        this.menus = menus;
        this.closing = closing;
    }

    /** Thân lệnh tạo, sửa món, nội dung bản sửa (api-011 {@code DishCommand}). */
    public record DishInput(
            String name,
            String note,
            List<Long> ingredients,
            boolean noCatalogIngredient,
            List<Long> suitableDietIds,
            Texture texture) {}

    /** Món kèm nội dung đã xác nhận và nội dung đang soạn hoặc chờ xác nhận. */
    public record DishDetail(Dish dish, DishVersionContent effective, DishVersionContent pending) {}

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<DishDetail> list(DishStatus status, Long dietId) {
        return store.dishes().stream()
                .filter(d -> status == null || d.status() == status)
                .map(this::detail)
                .filter(d -> dietId == null || suitableFor(d, dietId))
                .toList();
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Dish create(DishInput input) {
        String name = name(input.name());
        DishVersionContent content = content(input);
        Dish dish = store.createDish(
                name, new DishAttributes(DishStatus.DRAFT, input.note(), null, false, null, null, null));
        store.createVersion(dish.id(), DishVersionStatus.DRAFT, content, user.accountId());
        log(dish.id(), "DISH_CREATED", Map.of(), snapshot(dish, content), null);
        return dish;
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Dish update(long id, String ifMatch, DishInput input) {
        locks.lock(ENTITY + ":" + id);
        Dish dish = store.dish(id);
        VersionEtag.requireMatch(ifMatch, dish.version(), Map.of("id", id, "version", dish.version()));
        String name = name(input.name());
        DishVersionContent content = content(input);
        if (dish.status() == DishStatus.DRAFT) {
            DishVersion draft = store.openVersion(id).orElseThrow(NotFoundException::new);
            store.changeVersion(draft, DishVersionStatus.DRAFT, content);
        } else if (dish.status() == DishStatus.ACTIVE) {
            DishVersionContent effective = effective(dish);
            if (!effective.sameContent(content)) {
                throw new ConditionNotMetException(List.of(new UnmetCondition(
                        "CONTENT_NEEDS_REVISION",
                        "Đổi thành phần, chế độ ăn phù hợp, kết cấu của món Hiệu lực qua bản sửa (FR-004a)")));
            }
        } else {
            throw stateChanged();
        }
        Dish after = store.updateDish(dish, name, dish.attributes().withNote(input.note()));
        log(id, "DISH_UPDATED", Map.of("name", dish.name()), Map.of("name", after.name()), null);
        return after;
    }

    /** Lệnh trên món theo bảng trạng thái; quyền theo người thực hiện của lệnh (Q-308). */
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void command(long id, String path, Reason reason, DishInput revision) {
        DishCommand command = DishCommand.fromPath(path).orElseThrow(NotFoundException::new);
        requireActor(command);
        if (command.requiresReason() && reason == null) {
            throw new ReasonRequiredException();
        }
        locks.lock(ENTITY + ":" + id);
        Dish dish = store.dish(id);
        Optional<DishVersion> open = store.openVersion(id);
        boolean pendingRevision = dish.status() == DishStatus.ACTIVE && open.isPresent();
        Outcome outcome = DishTransitions.apply(dish.status(), pendingRevision, command);
        if (!outcome.allowed()) {
            throw new BusinessException(HttpStatus.CONFLICT, outcome.errorCode(), "Lệnh không hợp lệ ở trạng thái món");
        }
        Instant now = time.now();
        switch (command) {
            case SUBMIT -> submit(dish, open.orElseThrow(NotFoundException::new), false, now);
            case WITHDRAW -> reopen(dish, open.orElseThrow(NotFoundException::new), DishVersionStatus.WITHDRAWN, null);
            case RETURN -> {
                DishVersion version = open.orElseThrow(NotFoundException::new);
                reopen(dish, version, DishVersionStatus.RETURNED, reason.value());
                decided(dish, version, false, false, reason.value(), now);
            }
            case CONFIRM -> confirm(dish, open.orElseThrow(NotFoundException::new), false, now);
            case CANCEL -> cancel(dish);
            case SUBMIT_REVISION -> {
                if (revision == null) {
                    throw new InvalidValueException("Thiếu nội dung bản sửa");
                }
                DishVersion version =
                        store.createVersion(id, DishVersionStatus.DRAFT, content(revision), user.accountId());
                submit(dish, version, true, now);
            }
            case CONFIRM_REVISION -> confirm(dish, open.get(), true, now);
            case RETURN_REVISION -> {
                DishVersion version = open.get();
                store.changeVersion(
                        version, DishVersionStatus.RETURNED, version.content().returned(reason.value()));
                decided(dish, version, true, false, reason.value(), now);
                log(id, "DISH_REVISION_RETURNED", Map.of(), Map.of(), reason);
            }
            case WITHDRAW_REVISION -> {
                DishVersion version = open.get();
                store.changeVersion(version, DishVersionStatus.WITHDRAWN, version.content());
                log(id, "DISH_REVISION_WITHDRAWN", Map.of(), Map.of(), null);
            }
            case DEACTIVATE -> deactivate(dish, open, reason, now);
        }
    }

    private void submit(Dish dish, DishVersion version, boolean revision, Instant now) {
        List<UnmetCondition> unmet = version.content().submitConditions();
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        long staff = user.staffId().orElse(user.accountId());
        store.changeVersion(
                version,
                DishVersionStatus.PENDING_CONFIRMATION,
                version.content().submitted(staff, now));
        if (!revision) {
            store.updateDish(dish, dish.name(), dish.attributes().withStatus(DishStatus.PENDING_CONFIRMATION));
        }
        log(dish.id(), revision ? "DISH_REVISION_SUBMITTED" : "DISH_SUBMITTED", Map.of(), Map.of(), null);
        events.publishEvent(
                new DishConfirmationRequested(dish.id(), dish.name(), version.id(), revision, user.accountId(), now));
    }

    /** Trả lại hoặc rút món Chờ xác nhận: khóa phiên bản đã gửi, mở phiên bản Nháp mới cùng nội dung để bếp sửa tiếp. */
    private void reopen(Dish dish, DishVersion version, DishVersionStatus closedAs, String reason) {
        DishVersionContent closed =
                closedAs == DishVersionStatus.RETURNED ? version.content().returned(reason) : version.content();
        store.changeVersion(version, closedAs, closed);
        DishVersionContent c = version.content();
        store.createVersion(
                dish.id(),
                DishVersionStatus.DRAFT,
                DishVersionContent.draft(c.ingredients(), c.noCatalogIngredient(), c.suitableDietIds(), c.texture()),
                user.accountId());
        store.updateDish(dish, dish.name(), dish.attributes().withStatus(DishStatus.DRAFT));
        log(
                dish.id(),
                closedAs == DishVersionStatus.RETURNED ? "DISH_RETURNED" : "DISH_WITHDRAWN",
                Map.of("status", DishStatus.PENDING_CONFIRMATION.name()),
                Map.of("status", DishStatus.DRAFT.name()),
                reason == null ? null : Reason.of(reason));
    }

    private void confirm(Dish dish, DishVersion version, boolean revision, Instant now) {
        store.changeVersion(
                version, DishVersionStatus.CONFIRMED, version.content().confirmed(user.accountId(), now));
        Dish updated = store.updateDish(dish, dish.name(), dish.attributes().activatedWith(version.id()));
        store.confirmDish(updated, user.accountId());
        log(
                dish.id(),
                revision ? "DISH_REVISION_CONFIRMED" : "DISH_CONFIRMED",
                Map.of(
                        "status",
                        dish.status().name(),
                        "effectiveVersionId",
                        String.valueOf(dish.attributes().effectiveVersionId())),
                Map.of("status", DishStatus.ACTIVE.name(), "effectiveVersionId", String.valueOf(version.id())),
                null);
        decided(dish, version, revision, true, null, now);
    }

    private void cancel(Dish dish) {
        if (dish.attributes().everActive()) {
            throw stateChanged();
        }
        store.openVersion(dish.id()).ifPresent(v -> store.changeVersion(v, DishVersionStatus.WITHDRAWN, v.content()));
        Dish updated = store.updateDish(dish, dish.name(), dish.attributes().withStatus(DishStatus.CANCELLED));
        store.closeDish(updated);
        log(dish.id(), "DISH_CANCELLED", Map.of("status", dish.status().name()), Map.of("status", "CANCELLED"), null);
    }

    private void deactivate(Dish dish, Optional<DishVersion> open, Reason reason, Instant now) {
        boolean safeDish = store.diets().stream()
                .anyMatch(d -> d.active()
                        && Long.valueOf(dish.id()).equals(d.attributes().safeDishId()));
        if (safeDish) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "IS_SAFE_DISH", "Món đang là món an toàn của chế độ ăn Hiệu lực (FR-004)");
        }
        open.ifPresent(v -> store.changeVersion(v, DishVersionStatus.WITHDRAWN, v.content()));
        Dish updated = store.updateDish(
                dish, dish.name(), dish.attributes().deactivated(reason.value(), user.accountId(), now));
        store.closeDish(updated);
        log(dish.id(), "DISH_DEACTIVATED", Map.of("status", "ACTIVE"), Map.of("status", "INACTIVE"), reason);
        int inMenus = menus.notEnded(time.today()).stream()
                .filter(m -> m.status().published())
                .flatMap(m -> m.content().dishes().stream())
                .mapToInt(d -> (d.dishId() == dish.id() ? 1 : 0)
                        + (d.substituteDishIds().contains(dish.id()) ? 1 : 0))
                .sum();
        if (inMenus > 0) {
            events.publishEvent(
                    new MenuDishViolation("DISH_DEACTIVATED", dish.id(), dish.name(), inMenus, user.accountId(), now));
        }
    }

    private void decided(
            Dish dish, DishVersion version, boolean revision, boolean confirmed, String reason, Instant now) {
        Long submitter = version.content().submittedBy();
        if (submitter != null) {
            events.publishEvent(new DishConfirmationResult(
                    dish.id(),
                    dish.name(),
                    version.id(),
                    revision,
                    confirmed,
                    reason,
                    submitter,
                    user.accountId(),
                    now));
        }
        if (confirmed && revision) {
            closing.recompute(
                    java.util.Optional.empty(), "M08.CLOSING.DISH_REVISION:" + version.id(), "DISH_REVISION", false);
        }
    }

    private void requireActor(DishCommand command) {
        boolean allowed =
                (command.byKitchen() && has(Operation.UPDATE)) || (command.byDoctor() && has(Operation.APPROVE));
        if (!allowed) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Không có quyền thực hiện lệnh trên món", Map.of("failedLayer", "ROLE"));
        }
    }

    private boolean has(Operation operation) {
        return permissions
                .hasPermission(user, new PermissionKey(Function.DISH_MENU, operation), Target.UNSPECIFIED)
                .allowed();
    }

    private DishDetail detail(Dish dish) {
        DishVersionContent effective = dish.attributes().effectiveVersionId() == null ? null : effective(dish);
        DishVersionContent pending =
                store.openVersion(dish.id()).map(DishVersion::content).orElse(null);
        return new DishDetail(dish, effective, pending);
    }

    private static boolean suitableFor(DishDetail detail, long dietId) {
        DishVersionContent content = detail.effective() != null ? detail.effective() : detail.pending();
        return content != null && content.suitableDietIds().contains(dietId);
    }

    private DishVersionContent effective(Dish dish) {
        return store.version(dish.attributes().effectiveVersionId()).content();
    }

    private DishVersionContent content(DishInput input) {
        if (input.texture() == null) {
            throw new InvalidValueException("Kết cấu là bắt buộc");
        }
        List<Long> ingredients = input.ingredients() == null
                ? List.of()
                : input.ingredients().stream().distinct().toList();
        for (Long allergen : ingredients) {
            if (allergens.allergen(allergen).isEmpty()) {
                throw new InvalidValueException("Thành phần không có trong danh mục dị nguyên: " + allergen);
            }
        }
        List<Long> diets = input.suitableDietIds() == null
                ? List.of()
                : input.suitableDietIds().stream().distinct().toList();
        List<Long> known =
                store.diets().stream().filter(Diet::active).map(Diet::id).toList();
        for (Long diet : diets) {
            if (!known.contains(diet)) {
                throw new InvalidValueException("Chế độ ăn phù hợp không có hoặc đã Ngừng hiệu lực: " + diet);
            }
        }
        return DishVersionContent.draft(ingredients, input.noCatalogIngredient(), diets, input.texture());
    }

    private static String name(String name) {
        if (name == null || name.isBlank() || name.strip().length() > 200) {
            throw new InvalidValueException("Tên món có từ 1 đến 200 ký tự");
        }
        return name.strip();
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

    private static Map<String, Object> snapshot(Dish dish, DishVersionContent content) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("name", dish.name());
        out.put("status", dish.status().name());
        out.put("ingredients", content.ingredients());
        out.put("noCatalogIngredient", content.noCatalogIngredient());
        out.put("suitableDietIds", content.suitableDietIds());
        out.put("texture", content.texture().name());
        return out;
    }
}
