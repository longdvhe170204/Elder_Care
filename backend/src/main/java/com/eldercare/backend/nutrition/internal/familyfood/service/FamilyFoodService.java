package com.eldercare.backend.nutrition.internal.familyfood.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipApi;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.ReasonRequiredException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.dto.FamilyFoodStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.api.events.FamilyFoodNotice;
import com.eldercare.backend.nutrition.api.events.FamilyFoodNotice.Kind;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFood;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck.Food;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck.Person;
import com.eldercare.backend.nutrition.internal.familyfood.domain.FamilyFoodCheck.Result;
import com.eldercare.backend.nutrition.internal.familyfood.persistence.FamilyFoodStore;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.AllergenGroup;
import com.eldercare.backend.resident.api.dto.AllergenView;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Đồ ăn gia đình mang vào (mục H, FR-052 → FR-057, BR-M08-04; research M-11): ghi nhận chạy {@link FamilyFoodCheck} để gán
 * Không sử dụng / Cần xác nhận / Được sử dụng; Cần xác nhận chỉ sang Được sử dụng khi điều dưỡng, Bác sĩ xác nhận kèm ghi chú;
 * không có lệnh nào đưa Không sử dụng sang Được sử dụng. Hệ thống chuyển Được sử dụng sang Không sử dụng khi có dị ứng mới,
 * chế độ ăn mới gây xung đột (FR-056) hoặc tới hạn dùng (FR-055). Quyền "Đối chiếu đồ ăn gia đình"; Điều dưỡng theo phạm vi
 * phân công.
 */
@Service
public class FamilyFoodService {

    public static final String ENTITY = "M08.FAMILY_FOOD";

    private static final AuditAction RECORDED = new AuditAction("FAMILY_FOOD_RECORDED", false);
    private static final AuditAction APPROVED = new AuditAction("FAMILY_FOOD_APPROVED", false);
    private static final AuditAction REJECTED = new AuditAction("FAMILY_FOOD_REJECTED", false);
    private static final AuditAction DISPOSED = new AuditAction("FAMILY_FOOD_DISPOSED", false);
    private static final AuditAction ENDED = new AuditAction("FAMILY_FOOD_ENDED", false);
    private static final AuditAction MOVED = new AuditAction("FAMILY_FOOD_MOVED_BY_SYSTEM", false);
    private static final Set<String> DISPOSAL_METHODS = Set.of("RETURNED", "DISCARDED");
    private static final Set<String> END_METHODS = Set.of("RETURNED", "DISCARDED", "CONSUMED");

    private final FamilyFoodStore store;
    private final DietAssignmentStore assignments;
    private final NutritionCatalogStore catalog;
    private final HealthItemApi healthItems;
    private final AllergenApi allergens;
    private final ResidentApi residents;
    private final FamilyRelationshipApi relationships;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public FamilyFoodService(
            FamilyFoodStore store,
            DietAssignmentStore assignments,
            NutritionCatalogStore catalog,
            HealthItemApi healthItems,
            AllergenApi allergens,
            ResidentApi residents,
            FamilyRelationshipApi relationships,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.store = store;
        this.assignments = assignments;
        this.catalog = catalog;
        this.healthItems = healthItems;
        this.allergens = allergens;
        this.residents = residents;
        this.relationships = relationships;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
    }

    /** Lý do đối chiếu lại (FR-056): dị ứng mới ghi nhận, hoặc bản gán chế độ ăn mới có hiệu lực. */
    public enum Cause {
        ALLERGY,
        DIET
    }

    /** Thân lệnh ghi nhận (api-011 {@code FamilyFoodCommand}). */
    public record Command(
            String senderName,
            Long familyMemberId,
            String type,
            String description,
            List<Long> ingredients,
            boolean ingredientsUnknown,
            Instant expiresAt,
            String storageMethod) {}

    public static Target target(long residentId, long id) {
        return new Target(ENTITY, Long.toString(id), residentId, null);
    }

    /** Đối tượng kiểm quyền của đồ ăn {@code id}, mang người cao tuổi của đồ ăn. */
    @Transactional(readOnly = true)
    public Target targetOf(long id) {
        return target(store.require(id).residentId(), id);
    }

    @RequiresPermission(function = Function.FAMILY_FOOD_CHECK, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<SimpleRecord<FamilyFood>> list(Target target, long residentId) {
        return store.ofResident(residentId);
    }

    @RequiresPermission(function = Function.FAMILY_FOOD_CHECK, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SimpleRecord<FamilyFood> register(Target target, long residentId, Command command) {
        residents.status(residentId);
        Instant now = time.now();
        boolean related = command.familyMemberId() != null
                && relationships.belongsToResident(command.familyMemberId(), residentId);
        Set<Long> ingredients = new LinkedHashSet<>(command.ingredients() == null ? List.of() : command.ingredients());
        boolean otherGroup = otherGroupIngredient(ingredients);
        Result result = FamilyFoodCheck.check(
                new Food(
                        command.type(),
                        ingredients,
                        command.ingredientsUnknown(),
                        otherGroup,
                        command.expiresAt(),
                        related),
                personOf(residentId),
                store.activeForbiddenNames(),
                now);
        FamilyFood content = FamilyFood.recorded(
                command.senderName().strip(),
                command.familyMemberId(),
                command.type().strip(),
                command.description().strip(),
                new ArrayList<>(ingredients),
                command.ingredientsUnknown(),
                command.expiresAt(),
                command.storageMethod().strip(),
                now,
                result.reasons());
        SimpleRecord<FamilyFood> row = store.create(residentId, result.status(), content, user.accountId());
        audit.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY, row.id())
                .resident(residentId)
                .after(Map.of("status", result.status().name(), "systemReasons", result.reasons()))
                .build());
        if (result.status() == FamilyFoodStatus.NOT_ALLOWED) {
            notifySender(row.id(), residentId, content, healthText(result.reasons()), now);
        }
        return row;
    }

    /** Xác nhận cho dùng (FR-055): chỉ từ Cần xác nhận, kèm ghi chú; đối chiếu lại tại lúc xác nhận. */
    @RequiresPermission(function = Function.FAMILY_FOOD_CHECK, operation = Operation.CONFIRM)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void approveUse(Target target, long id, String note) {
        SimpleRecord<FamilyFood> row = store.lock(id);
        if (row.status().equals(FamilyFoodStatus.NOT_ALLOWED.name())) {
            throw new BusinessException(
                    HttpStatus.CONFLICT, "NOT_ALLOWED_TO_USE", "Đồ ăn Không sử dụng không có lệnh nào cho dùng lại");
        }
        requireStatus(row, FamilyFoodStatus.NEEDS_CONFIRMATION);
        if (note == null || note.isBlank()) {
            throw new InvalidValueException("Xác nhận cho dùng phải kèm ghi chú");
        }
        Instant now = time.now();
        Result result = recheckOne(row, now);
        if (result.status() == FamilyFoodStatus.NOT_ALLOWED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NOT_ALLOWED_TO_USE",
                    "Đồ ăn đã có xung đột hoặc hết hạn dùng, không cho dùng");
        }
        store.change(row, FamilyFoodStatus.ALLOWED, row.content().approved(user.accountId(), now, note.strip()));
        audit.write(AuditLogEntry.builder()
                .action(APPROVED)
                .entity(ENTITY, id)
                .resident(row.residentId())
                .before(Map.of("status", row.status()))
                .after(Map.of("status", FamilyFoodStatus.ALLOWED.name()))
                .build());
    }

    /** Không cho dùng (lý do): Cần xác nhận → Không sử dụng, báo người gửi (FR-057). */
    @RequiresPermission(function = Function.FAMILY_FOOD_CHECK, operation = Operation.CONFIRM)
    @BusinessCommand
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void rejectUse(Target target, long id, Reason reason) {
        if (reason == null) {
            throw new ReasonRequiredException();
        }
        SimpleRecord<FamilyFood> row = store.lock(id);
        requireStatus(row, FamilyFoodStatus.NEEDS_CONFIRMATION);
        Instant now = time.now();
        FamilyFood after = row.content().rejected(user.accountId(), now, reason.value());
        store.change(row, FamilyFoodStatus.NOT_ALLOWED, after);
        audit.write(AuditLogEntry.builder()
                .action(REJECTED)
                .entity(ENTITY, id)
                .resident(row.residentId())
                .before(Map.of("status", row.status()))
                .after(Map.of("status", FamilyFoodStatus.NOT_ALLOWED.name()))
                .reason(reason)
                .build());
        notifySender(id, row.residentId(), after, reason.value(), now);
    }

    /** Ghi xử lý đồ ăn Không sử dụng: trả lại người gửi hoặc hủy bỏ → Đã kết thúc. */
    @RequiresPermission(function = Function.FAMILY_FOOD_CHECK, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void recordDisposal(Target target, long id, String method, String returnedTo) {
        String returned = endInput(method, returnedTo, DISPOSAL_METHODS);
        SimpleRecord<FamilyFood> row = store.lock(id);
        requireStatus(row, FamilyFoodStatus.NOT_ALLOWED);
        finish(row, method, returned, DISPOSED);
    }

    /** Kết thúc đồ ăn Được sử dụng: đã dùng hết, hủy bỏ hoặc trả lại → Đã kết thúc. */
    @RequiresPermission(function = Function.FAMILY_FOOD_CHECK, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void end(Target target, long id, String method, String returnedTo) {
        String returned = endInput(method, returnedTo, END_METHODS);
        SimpleRecord<FamilyFood> row = store.lock(id);
        requireStatus(row, FamilyFoodStatus.ALLOWED);
        finish(row, method, returned, ENDED);
    }

    /**
     * Đối chiếu lại đồ ăn Được sử dụng của một người sau dị ứng mới hoặc bản gán chế độ ăn mới (FR-056): xung đột thì chuyển
     * Không sử dụng, báo Điều dưỡng phụ trách (Trung bình) và người gửi (FR-057). Chạy lại không đổi gì khi đã chuyển.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void recheck(long residentId, Cause cause) {
        Instant now = time.now();
        for (SimpleRecord<FamilyFood> listed : store.ofResident(residentId)) {
            if (!listed.status().equals(FamilyFoodStatus.ALLOWED.name())) {
                continue;
            }
            SimpleRecord<FamilyFood> row = store.lock(listed.id());
            if (!row.status().equals(FamilyFoodStatus.ALLOWED.name())) {
                continue;
            }
            Result result = recheckOne(row, now);
            if (result.status() == FamilyFoodStatus.NOT_ALLOWED) {
                moveByRule(row, result, now);
            }
        }
    }

    /** Việc theo phút: đồ ăn Được sử dụng tới hạn dùng → Không sử dụng, báo Điều dưỡng phụ trách (FR-055). */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void expireDue(Instant now) {
        for (SimpleRecord<FamilyFood> listed : store.withStatus(FamilyFoodStatus.ALLOWED)) {
            if (listed.content().expiresAt().isAfter(now)) {
                continue;
            }
            SimpleRecord<FamilyFood> row = store.lock(listed.id());
            if (!row.status().equals(FamilyFoodStatus.ALLOWED.name())
                    || row.content().expiresAt().isAfter(now)) {
                continue;
            }
            moveByRule(row, recheckOne(row, now), now);
        }
    }

    private void moveByRule(SimpleRecord<FamilyFood> row, Result result, Instant now) {
        boolean onlyExpired = result.reasons().stream().allMatch(FamilyFoodCheck.EXPIRED::equals);
        String because = onlyExpired ? FamilyFood.EXPIRED : FamilyFood.NEW_CONFLICT;
        FamilyFood after = row.content().movedByRule(result.reasons(), because);
        store.change(row, FamilyFoodStatus.NOT_ALLOWED, after);
        audit.write(AuditLogEntry.builder()
                .action(MOVED)
                .entity(ENTITY, row.id())
                .resident(row.residentId())
                .before(Map.of("status", row.status()))
                .after(Map.of("status", FamilyFoodStatus.NOT_ALLOWED.name(), "systemReasons", result.reasons()))
                .build());
        events.publishEvent(new FamilyFoodNotice(
                row.id(), row.residentId(), onlyExpired ? Kind.NURSE_EXPIRED : Kind.NURSE_CONFLICT, null, null, now));
        notifySender(row.id(), row.residentId(), after, healthText(result.reasons()), now);
    }

    private void finish(SimpleRecord<FamilyFood> row, String method, String returnedTo, AuditAction action) {
        store.change(
                row, FamilyFoodStatus.ENDED, row.content().ended(user.accountId(), time.now(), method, returnedTo));
        audit.write(AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, row.id())
                .resident(row.residentId())
                .before(Map.of("status", row.status()))
                .after(Map.of("status", FamilyFoodStatus.ENDED.name(), "method", method))
                .build());
    }

    private static String endInput(String method, String returnedTo, Set<String> allowed) {
        if (method == null || !allowed.contains(method)) {
            throw new InvalidValueException("Cách xử lý phải là một trong " + allowed);
        }
        if (!method.equals("RETURNED")) {
            return null;
        }
        if (returnedTo == null || returnedTo.isBlank()) {
            throw new InvalidValueException("Trả lại phải ghi người nhận lại");
        }
        return returnedTo.strip();
    }

    private static void requireStatus(SimpleRecord<FamilyFood> row, FamilyFoodStatus expected) {
        if (!row.status().equals(expected.name())) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Đồ ăn đã đổi trạng thái");
        }
    }

    private Result recheckOne(SimpleRecord<FamilyFood> row, Instant now) {
        FamilyFood f = row.content();
        boolean related =
                f.familyMemberId() != null && relationships.belongsToResident(f.familyMemberId(), row.residentId());
        Set<Long> ingredients = new HashSet<>(f.ingredients());
        return FamilyFoodCheck.check(
                new Food(
                        f.type(),
                        ingredients,
                        f.ingredientsUnknown(),
                        otherGroupIngredient(ingredients),
                        f.expiresAt(),
                        related),
                personOf(row.residentId()),
                store.activeForbiddenNames(),
                now);
    }

    private void notifySender(long foodId, long residentId, FamilyFood food, String healthReason, Instant now) {
        boolean related =
                food.familyMemberId() != null && relationships.belongsToResident(food.familyMemberId(), residentId);
        if (related) {
            events.publishEvent(new FamilyFoodNotice(
                    foodId, residentId, Kind.SENDER_NOT_ALLOWED, food.familyMemberId(), healthReason, now));
        }
    }

    /** Lý do liên quan dị ứng, chế độ ăn (phần "sức khỏe" của thông báo cho người gửi, FR-057); rỗng nếu không có. */
    private static String healthText(List<String> reasons) {
        List<String> parts = new ArrayList<>();
        if (reasons.contains(FamilyFoodCheck.ALLERGY)) {
            parts.add("trùng dị ứng của người nhận");
        }
        if (reasons.contains(FamilyFoodCheck.PERSONAL_RESTRICTION)
                || reasons.contains(FamilyFoodCheck.DIET_RESTRICTION)) {
            parts.add("trái hạn chế ăn uống của người nhận");
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private boolean otherGroupIngredient(Set<Long> ingredients) {
        boolean other = false;
        for (long id : ingredients) {
            Optional<AllergenView> allergen = allergens.allergen(id);
            if (allergen.isEmpty()) {
                throw new InvalidValueException("Thành phần " + id + " không có trong danh mục dị nguyên");
            }
            other |= allergen.get().group() == AllergenGroup.OTHER;
        }
        return other;
    }

    /** Dị ứng, hạn chế, kết cấu, chế độ ăn hiện hành của người nhận (bản Hiệu lực, hoặc chế độ mặc định không hạn chế riêng). */
    private Person personOf(long residentId) {
        Optional<DietAssignment> active = assignments.active(residentId);
        Optional<Diet> diet = active.map(a -> catalog.diet(a.dietId()))
                .or(() -> catalog.diets().stream()
                        .filter(d -> d.active() && d.attributes().isDefault())
                        .findFirst());
        List<AllergyItem> allergies = healthItems.active(residentId, HealthItemType.ALLERGY).stream()
                .map(i -> new AllergyItem(i.allergenId().orElse(null), i.notAutoChecked()))
                .toList();
        return new Person(
                allergies,
                active.<Set<Long>>map(a -> new HashSet<>(a.personalRestrictions()))
                        .orElse(Set.of()),
                diet.<Set<Long>>map(d -> new HashSet<>(d.attributes().restrictions()))
                        .orElse(Set.of()),
                active.map(DietAssignment::texture).orElse(Texture.REGULAR),
                diet.map(d -> d.attributes().treatmentRelated()).orElse(false));
    }
}
