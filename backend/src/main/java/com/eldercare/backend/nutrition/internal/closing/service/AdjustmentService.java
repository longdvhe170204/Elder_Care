package com.eldercare.backend.nutrition.internal.closing.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.events.AdjustmentsPending;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.Adjustment;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.ExtraPortionRequest;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SupplementaryPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionFlag;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck;
import com.eldercare.backend.nutrition.internal.conflict.domain.ConflictCheck.AllergyItem;
import com.eldercare.backend.resident.api.HealthItemApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashSet;
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
 * Lệnh và việc theo giờ trên phát sinh, suất đặc biệt sau chốt (FR-037, FR-039 → FR-041, FR-040a; research M-08): bếp xác
 * nhận phát sinh (quyền "Chuẩn bị, giao phiếu" CONFIRM, chủ dự án chọn 2026-11-06), người nhận tại tầng yêu cầu suất bổ
 * sung, phục vụ hoặc hủy suất giữ; Bác sĩ chỉ định món riêng (DIET UPDATE, chủ dự án chọn 2026-11-06). Phiếu tầng/khu chưa
 * có được lập khi bếp xác nhận phát sinh "+" (Q-306).
 */
@Service
public class AdjustmentService {

    public static final String SPECIAL = "M08.SPECIAL_PORTION";

    private static final AuditAction CONFIRMED = new AuditAction("ADJUSTMENT_CONFIRMED", false);
    private static final AuditAction EXTRA = new AuditAction("EXTRA_PORTION_REQUESTED", true);
    private static final AuditAction HELD_SERVED = new AuditAction("HELD_PORTION_SERVED", false);
    private static final AuditAction HELD_DISCARDED = new AuditAction("HELD_PORTION_DISCARDED", false);
    private static final AuditAction DISH_ASSIGNED = new AuditAction("SPECIAL_PORTION_DISH_ASSIGNED", false);

    /** Phiếu đã giao: phát sinh cần giao thêm thành phần bổ sung, phiếu không quay lại (FR-048b). */
    private static final Set<String> AFTER_DELIVERY = Set.of(
            MealSlipStatus.DELIVERED.name(), MealSlipStatus.HAS_DISCREPANCY.name(), MealSlipStatus.RECEIVED.name());

    private final PortionClosingService closingService;
    private final PortionClosingStore closings;
    private final ClosingRecordStore records;
    private final DailyMeals dailyMeals;
    private final NutritionCatalogStore catalog;
    private final ResidentApi residents;
    private final HealthItemApi healthItems;
    private final DietAssignmentStore assignments;
    private final TechnicalLocks locks;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    AdjustmentService(
            PortionClosingService closingService,
            PortionClosingStore closings,
            ClosingRecordStore records,
            DailyMeals dailyMeals,
            NutritionCatalogStore catalog,
            ResidentApi residents,
            HealthItemApi healthItems,
            DietAssignmentStore assignments,
            TechnicalLocks locks,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.closingService = closingService;
        this.closings = closings;
        this.records = records;
        this.dailyMeals = dailyMeals;
        this.catalog = catalog;
        this.residents = residents;
        this.healthItems = healthItems;
        this.assignments = assignments;
        this.locks = locks;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
    }

    public static Target specialTarget(long id, Long residentId) {
        return new Target(SPECIAL, Long.toString(id), residentId, null);
    }

    /** Đối tượng kiểm quyền của suất đặc biệt: người cao tuổi, tầng/khu của phiếu (phạm vi người nhận tại tầng). */
    @Transactional(readOnly = true)
    public Target targetOfSpecial(long id) {
        SimpleRecord<SpecialPortion> row = records.special(id);
        long slipFloor = records.slips(row.content().dailyMealId()).stream()
                .filter(s -> s.id() == row.parent().entityId())
                .findFirst()
                .map(s -> s.content().floorAreaId())
                .orElse(0L);
        return new Target(SPECIAL, Long.toString(id), row.content().residentId(), slipFloor);
    }

    /** Bếp xác nhận phát sinh hoặc yêu cầu suất bổ sung (đặt một lần); "+" của tầng/khu chưa có phiếu thì lập phiếu. */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.CONFIRM)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void confirm(Target target, long id) {
        Instant now = time.now();
        Optional<RecordEvent<ExtraPortionRequest>> extra = records.extra(id);
        if (extra.isPresent()) {
            if (extra.get().confirmedAt() != null) {
                throw alreadyConfirmed();
            }
            ExtraPortionRequest r = extra.get().content();
            locks.lock(ClosingRecordStore.DAILY_MEAL + ":" + r.dailyMealId());
            records.confirmExtra(extra.get(), user.accountId(), now);
            long slip = records.slip(r.dailyMealId(), r.floorAreaId())
                    .map(SimpleRecord::id)
                    .orElseGet(() -> records.createSlip(
                                    MealSlip.of(r.dailyMealId(), r.floorAreaId(), "EXTRA_PORTION_REQUEST", Set.of()),
                                    user.accountId())
                            .id());
            records.addSupplement(
                    new SupplementaryPortion(
                            r.dailyMealId(), slip, r.floorAreaId(), null, 1, null, id, null, null, null, null),
                    user.accountId(),
                    now);
            audit(id, r.dailyMealId());
            return;
        }
        RecordEvent<Adjustment> row = records.adjustment(id);
        Adjustment a = row.content();
        locks.lock(ClosingRecordStore.DAILY_MEAL + ":" + a.dailyMealId());
        if (!a.needsDelivery() || row.confirmedAt() != null) {
            throw alreadyConfirmed();
        }
        records.confirmAdjustment(row, user.accountId(), now);
        Optional<SimpleRecord<MealSlip>> delivered =
                records.slip(a.dailyMealId(), a.floorAreaId()).filter(s -> AFTER_DELIVERY.contains(s.status()));
        if (delivered.isPresent() && a.portionCount() >= 0) {
            records.addSupplement(
                    new SupplementaryPortion(
                            a.dailyMealId(),
                            delivered.get().id(),
                            a.floorAreaId(),
                            a.dietId(),
                            a.portionCount(),
                            id,
                            null,
                            null,
                            null,
                            null,
                            null),
                    user.accountId(),
                    now);
        }
        if (a.portionCount() > 0
                && records.slip(a.dailyMealId(), a.floorAreaId()).isEmpty()) {
            long slip = records.createSlip(
                            MealSlip.of(a.dailyMealId(), a.floorAreaId(), "ADJUSTMENT", Set.of()), user.accountId())
                    .id();
            PortionClosing closing = closings.ofDailyMeal(a.dailyMealId()).orElseThrow();
            for (CountedPerson person : closingService.current(closing).values()) {
                if (person.floorAreaId() == a.floorAreaId() && PortionClosingService.needsRecord(person)) {
                    records.createSpecial(slip, SpecialPortion.of(a.dailyMealId(), person), user.accountId());
                }
            }
        }
        audit(id, a.dailyMealId());
    }

    /** Yêu cầu suất bổ sung sau giờ bữa tới hết ngưỡng giao trễ cho người đang có mặt (FR-040); gửi bếp như phát sinh. */
    @RequiresPermission(function = Function.MEAL_RECEIPT, operation = Operation.CREATE)
    @BusinessCommand
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public long requestExtra(Target target, long dailyMealId, long residentId, long floorAreaId, Reason reason) {
        DailyMeal meal = dailyMeals.require(dailyMealId);
        Instant now = time.now();
        if (!meal.closed()) {
            throw blocked("NOT_CLOSED", "Bữa chưa chốt suất");
        }
        if (now.isBefore(meal.mealTime().orElseThrow())) {
            throw blocked("BEFORE_MEAL_TIME", "Chưa tới giờ bữa; thay đổi trước giờ bữa là phát sinh (FR-039, FR-040)");
        }
        if (now.isAfter(meal.lateDeadline())) {
            throw blocked("PAST_LATE_DELIVERY_THRESHOLD", "Đã hết ngưỡng giao trễ của bữa (CFG-M08-04, FR-040)");
        }
        if (residents.status(residentId) != ResidentStatus.IN_RESIDENCE) {
            throw blocked("RESIDENT_NOT_PRESENT", "Người cao tuổi không ở trạng thái Đang lưu trú");
        }
        long id = records.addExtra(
                        new ExtraPortionRequest(dailyMealId, floorAreaId, residentId, reason.value(), null),
                        user.accountId(),
                        now)
                .id();
        audit.write(AuditLogEntry.builder()
                .action(EXTRA)
                .entity(ExtraPortionRequest.KIND.code(), id)
                .resident(residentId)
                .after(Map.of("dailyMealId", dailyMealId, "floorAreaId", floorAreaId))
                .reason(reason)
                .build());
        events.publishEvent(
                new AdjustmentsPending(dailyMealId, meal.date(), 1, false, "M08.EXTRA_PORTION_REQUEST:" + id, now));
        return id;
    }

    /** Người về trong ngưỡng giao trễ: phục vụ suất giữ, phát sinh "+1" không cần giao (FR-040a). */
    @RequiresPermission(function = Function.MEAL_RECEIPT, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void serveHeld(Target target, long specialId) {
        SimpleRecord<SpecialPortion> row = held(specialId);
        DailyMeal meal = dailyMeals.require(row.content().dailyMealId());
        Instant now = time.now();
        if (now.isAfter(meal.lateDeadline())) {
            throw blocked(
                    "PAST_LATE_DELIVERY_THRESHOLD", "Đã hết ngưỡng giao trễ; dùng yêu cầu suất bổ sung (FR-040a)");
        }
        SpecialPortion s = row.content();
        PortionClosing closing = closings.ofDailyMeal(s.dailyMealId()).orElseThrow();
        CountedPerson person = closingService.current(closing).get("RESIDENT:" + s.residentId());
        CountedPerson served = withFlags(person, flagsWithout(person, PortionFlag.HELD_PORTION));
        records.addAdjustment(
                new Adjustment(
                        s.dailyMealId(),
                        person.floorAreaId(),
                        person.dietId(),
                        1,
                        "HELD_PORTION_SERVED",
                        "M08.HELD_PORTION_SERVED:" + specialId,
                        false,
                        person.key(),
                        person,
                        served,
                        null),
                user.accountId(),
                now,
                "M08.HELD_PORTION_SERVED:" + specialId);
        records.changeSpecial(row, ClosingRecordStore.ACTIVE, heldResult(s, now, null));
        audit.write(AuditLogEntry.builder()
                .action(HELD_SERVED)
                .entity(SPECIAL, specialId)
                .resident(s.residentId())
                .build());
    }

    /** Hết ngưỡng giao trễ mà người chưa về: người nhận tại tầng ghi hủy bỏ suất giữ (FR-040a). */
    @RequiresPermission(function = Function.MEAL_RECEIPT, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void discardHeld(Target target, long specialId) {
        SimpleRecord<SpecialPortion> row = held(specialId);
        DailyMeal meal = dailyMeals.require(row.content().dailyMealId());
        Instant now = time.now();
        if (!now.isAfter(meal.lateDeadline())) {
            throw blocked("HELD_PORTION_STILL_VALID", "Suất giữ còn trong ngưỡng giao trễ (FR-040a)");
        }
        records.changeSpecial(row, ClosingRecordStore.ACTIVE, heldResult(row.content(), null, now));
        audit.write(AuditLogEntry.builder()
                .action(HELD_DISCARDED)
                .entity(SPECIAL, specialId)
                .resident(row.content().residentId())
                .build());
    }

    /**
     * Bác sĩ chỉ định món thay thế riêng cho bữa chưa qua giờ (FR-037): món Hiệu lực, phù hợp chế độ ăn (FR-005), không xung
     * đột với người; sau chốt tạo phát sinh đổi suất.
     */
    @RequiresPermission(function = Function.DIET, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void assignDish(Target target, long specialId, long dishId, String note) {
        SimpleRecord<SpecialPortion> row = records.special(specialId);
        if (!ClosingRecordStore.ACTIVE.equals(row.status())) {
            throw stateChanged();
        }
        SpecialPortion s = row.content();
        DailyMeal meal = dailyMeals.require(s.dailyMealId());
        Instant now = time.now();
        if (!now.isBefore(meal.mealTime().orElseThrow())) {
            throw blocked("PAST_MEAL_TIME", "Bữa đã qua giờ bữa dự kiến (FR-037)");
        }
        Dish dish = catalog.dish(dishId);
        if (dish.status() != DishStatus.ACTIVE) {
            throw blocked("DISH_NOT_ACTIVE", "Món chưa Hiệu lực hoặc đã Ngừng");
        }
        Diet diet = catalog.diet(s.dietId());
        Set<Long> ingredients = catalog.effectiveIngredients(dishId);
        boolean suitable = Optional.ofNullable(dish.attributes().effectiveVersionId())
                .map(v -> catalog.version(v).content().suitableDietIds().contains(diet.id()))
                .orElse(false);
        List<AllergyItem> allergies = healthItems.active(s.residentId(), HealthItemType.ALLERGY).stream()
                .map(i -> new AllergyItem(i.allergenId().orElse(null), i.notAutoChecked()))
                .toList();
        if (!suitable
                || ConflictCheck.hasConflict(
                        ingredients,
                        allergies,
                        personalRestrictions(s),
                        new HashSet<>(diet.attributes().restrictions()))) {
            throw blocked("DISH_NOT_SUITABLE", "Món không phù hợp chế độ ăn hoặc xung đột với người cao tuổi (FR-037)");
        }
        records.changeSpecial(
                row,
                ClosingRecordStore.ACTIVE,
                new SpecialPortion(
                        s.dailyMealId(),
                        s.residentId(),
                        s.dietId(),
                        s.texture(),
                        s.dishes(),
                        s.flags(),
                        s.special(),
                        dishId,
                        note == null || note.isBlank() ? null : note.strip(),
                        user.accountId(),
                        now,
                        s.heldServedAt(),
                        s.heldDiscardedAt(),
                        s.prepared()));
        audit.write(AuditLogEntry.builder()
                .action(DISH_ASSIGNED)
                .entity(SPECIAL, specialId)
                .resident(s.residentId())
                .after(Map.of("dishId", dishId))
                .build());
        closingService.recompute(
                Optional.of(Set.of(s.residentId())),
                "M08.DISH_ASSIGNED:" + specialId + ":" + dishId,
                "DISH_ASSIGNED",
                false);
    }

    /**
     * Tại giờ bữa: suất "dự kiến trở về" của người chưa về thành suất giữ, phát sinh "−1" không cần giao (FR-040a; Q-333).
     * Người đã về (Đang lưu trú) thì bỏ qua. Chạy lại không trùng (khóa phát sinh theo suất).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void holdNotReturned(PortionClosing closing, Instant now) {
        locks.lock(ClosingRecordStore.DAILY_MEAL + ":" + closing.dailyMealId());
        for (CountedPerson person : closingService.current(closing).values()) {
            if (!person.flags().contains(PortionFlag.EXPECTED_RETURN)
                    || residents.status(person.residentId()) == ResidentStatus.IN_RESIDENCE) {
                continue;
            }
            Set<PortionFlag> flags = flagsWithout(person, PortionFlag.EXPECTED_RETURN);
            flags.add(PortionFlag.HELD_PORTION);
            CountedPerson heldPerson = withFlags(person, flags);
            String key = "M08.HELD_PORTION:" + closing.dailyMealId() + ":" + person.residentId();
            if (records.addAdjustment(
                            new Adjustment(
                                    closing.dailyMealId(),
                                    person.floorAreaId(),
                                    person.dietId(),
                                    -1,
                                    "NOT_RETURNED_AS_EXPECTED",
                                    key,
                                    false,
                                    person.key(),
                                    person,
                                    heldPerson,
                                    null),
                            null,
                            now,
                            key)
                    .isPresent()) {
                closingService.updateSpecial(closing.dailyMealId(), heldPerson);
            }
        }
    }

    /** Phát sinh cần giao chưa được bếp xác nhận của bữa (FR-041). */
    @Transactional(readOnly = true)
    public int unconfirmed(long dailyMealId) {
        int adjustments = (int) records.adjustments(dailyMealId).stream()
                .filter(r -> r.content().needsDelivery() && r.confirmedAt() == null)
                .count();
        int extras = (int) records.extras(dailyMealId).stream()
                .filter(r -> r.confirmedAt() == null)
                .count();
        return adjustments + extras;
    }

    private SimpleRecord<SpecialPortion> held(long specialId) {
        SimpleRecord<SpecialPortion> row = records.special(specialId);
        SpecialPortion s = row.content();
        if (!ClosingRecordStore.ACTIVE.equals(row.status())
                || !s.flags().contains(PortionFlag.HELD_PORTION)
                || s.heldServedAt() != null
                || s.heldDiscardedAt() != null) {
            throw stateChanged();
        }
        return row;
    }

    private static SpecialPortion heldResult(SpecialPortion s, Instant servedAt, Instant discardedAt) {
        Set<PortionFlag> flags = EnumSet.noneOf(PortionFlag.class);
        flags.addAll(s.flags());
        if (servedAt != null) {
            flags.remove(PortionFlag.HELD_PORTION);
        }
        return new SpecialPortion(
                s.dailyMealId(),
                s.residentId(),
                s.dietId(),
                s.texture(),
                s.dishes(),
                flags,
                s.special(),
                s.assignedDishId(),
                s.assignNote(),
                s.assignedBy(),
                s.assignedAt(),
                servedAt,
                discardedAt,
                s.prepared());
    }

    /** Hạn chế riêng của bản gán Hiệu lực (FR-020 b). */
    private Set<Long> personalRestrictions(SpecialPortion s) {
        return assignments
                .active(s.residentId())
                .map(a -> (Set<Long>) new HashSet<>(a.personalRestrictions()))
                .orElse(Set.of());
    }

    private static Set<PortionFlag> flagsWithout(CountedPerson person, PortionFlag flag) {
        Set<PortionFlag> flags = EnumSet.noneOf(PortionFlag.class);
        flags.addAll(person.flags());
        flags.remove(flag);
        return flags;
    }

    private static CountedPerson withFlags(CountedPerson p, Set<PortionFlag> flags) {
        return new CountedPerson(
                p.kind(),
                p.residentId(),
                p.familyMemberId(),
                p.stayId(),
                p.floorAreaId(),
                p.dietId(),
                p.assignmentId(),
                p.texture(),
                p.dishes(),
                flags,
                p.special());
    }

    private void audit(long id, long dailyMealId) {
        audit.write(AuditLogEntry.builder()
                .action(CONFIRMED)
                .entity(Adjustment.KIND.code(), id)
                .after(Map.of("dailyMealId", dailyMealId))
                .build());
    }

    private static ConditionNotMetException blocked(String code, String message) {
        return new ConditionNotMetException(List.of(new UnmetCondition(code, message)));
    }

    private static BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Suất đã đổi trạng thái");
    }

    private static BusinessException alreadyConfirmed() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Phát sinh đã được xác nhận");
    }
}
