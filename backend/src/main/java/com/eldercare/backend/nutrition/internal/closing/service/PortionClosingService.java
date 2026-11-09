package com.eldercare.backend.nutrition.internal.closing.service;

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
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.billing.api.events.CostSourceBilled;
import com.eldercare.backend.billing.api.events.CostSourceChanged;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.TechnicalLocks;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.dto.MealSlipStatus;
import com.eldercare.backend.nutrition.api.events.AdjustmentsPending;
import com.eldercare.backend.nutrition.api.events.PortionsClosed;
import com.eldercare.backend.nutrition.api.events.SafeDishApplied;
import com.eldercare.backend.nutrition.internal.closing.domain.AdjustmentCalculator;
import com.eldercare.backend.nutrition.internal.closing.domain.AdjustmentCalculator.Change;
import com.eldercare.backend.nutrition.internal.closing.domain.AdjustmentCalculator.Delta;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.Adjustment;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.ExtraPortionRequest;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.FamilyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.MealSlip;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedKind;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionClosing.FloorDietCount;
import com.eldercare.backend.nutrition.internal.closing.domain.PortionFlag;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Chốt suất và phát sinh (mục E, FR-033 → FR-043, BR-M08-01, 09, 13, Q-306; research M-08). Nhân viên bếp chốt từ giờ bữa −
 * CFG-M08-01 (quyền dòng "Chuẩn bị, giao phiếu", chủ dự án chọn 2026-11-06); lệnh tính lại tại lúc chốt, ghi lần chốt, lập
 * phiếu mỗi tầng/khu, suất đặc biệt, suất ăn người thân trong cùng transaction. Hệ thống không tự chốt. Sau chốt, mỗi thay
 * đổi chưa tới giờ bữa được tính lại theo người và ghi phát sinh ({@link #recompute}). Lệnh dưới khóa {@code M08.DAILY_MEAL}
 * của bữa ngày, đọc lại dưới {@code READ_COMMITTED} như bản gán.
 */
@Service
public class PortionClosingService {

    public static final String ENTITY = "M08.PORTION_CLOSING";
    public static final String FAMILY_MEAL_SOURCE = "FAMILY_MEAL";
    public static final String FAMILY_MEAL_UNIT = "PORTION";

    private static final AuditAction CLOSED = new AuditAction("PORTIONS_CLOSED", false);
    private static final List<PermissionKey> DAILY_MEAL_VIEWERS = List.of(
            new PermissionKey(Function.MEAL_COUNT_TICKET, Operation.VIEW),
            new PermissionKey(Function.MEAL_RECEIPT, Operation.VIEW),
            new PermissionKey(Function.SPECIAL_MEAL_SERVICE, Operation.VIEW));

    private final DailyMeals dailyMeals;
    private final PortionCounter counter;
    private final PortionClosingStore closings;
    private final ClosingRecordStore records;
    private final TechnicalLocks locks;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final PermissionApi permissions;

    PortionClosingService(
            DailyMeals dailyMeals,
            PortionCounter counter,
            PortionClosingStore closings,
            ClosingRecordStore records,
            TechnicalLocks locks,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events,
            PermissionApi permissions) {
        this.dailyMeals = dailyMeals;
        this.counter = counter;
        this.closings = closings;
        this.records = records;
        this.locks = locks;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
        this.permissions = permissions;
    }

    /** Kết quả chốt hoặc tính sẵn (api-011 {@code ClosingResult}). */
    public record ClosingView(
            long dailyMealId,
            List<FloorDietCount> portions,
            int specialPortions,
            int familyMealPortions,
            Set<String> flags,
            Set<String> missingData) {}

    /** Bữa trong ngày cho {@code GET /daily-meals}: mốc mở chốt (rỗng khi bữa chưa đặt giờ). */
    public record DailyMealRow(DailyMeal meal, Optional<Instant> closingOpensAt) {}

    /** Phát sinh hoặc yêu cầu suất bổ sung cho bếp (api-011 {@code Adjustment}; không có thông tin sức khỏe). */
    public record AdjustmentRow(
            long id,
            long floorAreaId,
            long dietId,
            int portionCount,
            String changeType,
            boolean needsDelivery,
            Instant kitchenConfirmedAt) {}

    public static Target target(long dailyMealId) {
        return new Target(ClosingRecordStore.DAILY_MEAL, Long.toString(dailyMealId), null, null);
    }

    /**
     * Bữa trong ngày (tên, giờ, đã chốt chưa; không có số suất): người xem số suất và người nhận tại tầng, người xác nhận
     * phục vụ cần bữa ngày để mở phiếu (Phụ lục 27 chú thích 15; chủ dự án chọn 2026-11-08). Một trong ba quyền nên tự kiểm.
     */
    @Transactional(readOnly = true)
    public List<DailyMealRow> dailyMeals(Target target, LocalDate date) {
        Set<PermissionKey> held = permissions.permissions(user);
        if (DAILY_MEAL_VIEWERS.stream().noneMatch(held::contains)) {
            throw new PermissionDeniedException("Không có quyền xem bữa trong ngày");
        }
        var lead = dailyMeals.closingLead();
        return dailyMeals.on(date).stream()
                .map(m -> new DailyMealRow(m, m.closingOpensAt(lead)))
                .toList();
    }

    /** Tính sẵn khi bếp mở lệnh, chỉ xem, không lưu (FR-033); bữa đã chốt trả kết quả đã chốt. */
    @RequiresPermission(function = Function.MEAL_COUNT_TICKET, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public ClosingView precomputed(Target target, long dailyMealId) {
        DailyMeal meal = dailyMeals.require(dailyMealId);
        Optional<PortionClosing> closed = closings.ofDailyMeal(dailyMealId);
        if (closed.isPresent()) {
            return view(closed.get());
        }
        requireMealTime(meal);
        Instant now = time.now();
        PortionCounter.Count count = counter.count(meal, Optional.empty(), Map.of(), !dailyMeals.officeHours(now));
        return view(dailyMealId, count, flags(meal, count, now));
    }

    /**
     * Lệnh Chốt suất (FR-033 → FR-038, FR-043, FR-044): chặn trước mốc mở chốt, bữa chưa đặt giờ (chủ dự án chốt 2026-11-06)
     * và lần chốt thứ hai; chốt sau giờ bữa được phép, mang dấu. Món an toàn áp ngay khi ngoài giờ hành chính (FR-037a i).
     */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ClosingView close(Target target, long dailyMealId) {
        DailyMeal meal = dailyMeals.require(dailyMealId);
        locks.lock(ClosingRecordStore.DAILY_MEAL + ":" + dailyMealId);
        if (closings.ofDailyMeal(dailyMealId).isPresent()) {
            throw alreadyClosed();
        }
        requireMealTime(meal);
        Instant now = time.now();
        if (now.isBefore(meal.closingOpensAt(dailyMeals.closingLead()).orElseThrow())) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "BEFORE_CLOSING_TIME", "Chưa tới mốc chốt suất (giờ bữa − CFG-M08-01, FR-033)")));
        }
        PortionCounter.Count count = counter.count(meal, Optional.empty(), Map.of(), !dailyMeals.officeHours(now));
        Set<String> flags = flags(meal, count, now);
        List<FloorDietCount> portions = portions(count.persons());
        PortionClosing closing = new PortionClosing(
                0,
                dailyMealId,
                meal.mealId(),
                meal.date(),
                meal.mealTime().orElseThrow(),
                meal.lateThresholdMinutes(),
                user.accountId(),
                now,
                now.isAfter(meal.mealTime().orElseThrow()),
                count.menu().map(m -> m.id()).orElse(null),
                count.persons().size(),
                count.persons(),
                portions,
                flags,
                count.missingData());
        long id;
        try {
            id = closings.insert(closing);
        } catch (DuplicateKeyException twice) {
            throw alreadyClosed();
        }
        Set<String> slipFlags = new LinkedHashSet<>();
        if (flags.contains("NO_MENU")) {
            slipFlags.add("NO_MENU");
        }
        if (flags.contains("CLOSED_AFTER_MEAL_TIME")) {
            slipFlags.add("CLOSED_AFTER_MEAL_TIME");
        }
        Map<Long, Long> slips = new HashMap<>();
        for (FloorDietCount p : portions) {
            slips.computeIfAbsent(
                    p.floorAreaId(),
                    floor -> records.createSlip(MealSlip.of(dailyMealId, floor, "CLOSING", slipFlags), user.accountId())
                            .id());
        }
        int specials = 0;
        int family = 0;
        for (CountedPerson person : count.persons()) {
            if (person.kind() == CountedKind.FAMILY) {
                recordFamilyMeal(meal, person, now);
                family++;
            } else if (needsRecord(person)) {
                records.createSpecial(
                        slips.get(person.floorAreaId()), SpecialPortion.of(dailyMealId, person), user.accountId());
                specials += person.special() ? 1 : 0;
            }
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("dailyMealId", dailyMealId);
        after.put("totalPortions", closing.totalPortions());
        after.put("flags", flags);
        audit.write(AuditLogEntry.builder()
                .action(CLOSED)
                .entity(ENTITY, id)
                .after(after)
                .build());
        Set<String> noticeFlags = new LinkedHashSet<>(flags);
        noticeFlags.remove("NO_DIET_ASSIGNED");
        if (count.persons().stream().anyMatch(p -> p.flags().contains(PortionFlag.NO_DISH_IN_MENU))) {
            noticeFlags.add("NO_DISH_IN_MENU");
        }
        events.publishEvent(new PortionsClosed(
                dailyMealId,
                meal.mealId(),
                meal.date(),
                noticeFlags,
                (int) count.persons().stream()
                        .filter(p -> p.flags().contains(PortionFlag.NO_DIET_ASSIGNED))
                        .count(),
                (int) count.persons().stream()
                        .filter(p -> p.flags().contains(PortionFlag.SAFE_DISH))
                        .count(),
                count.safeDishConflicts(),
                user.accountId(),
                now));
        return new ClosingView(dailyMealId, portions, specials, family, flags, count.missingData());
    }

    /** Phát sinh và yêu cầu suất bổ sung của bữa ngày (FR-039 → FR-041); bếp thấy dạng không có thông tin sức khỏe. */
    @RequiresPermission(function = Function.MEAL_COUNT_TICKET, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<AdjustmentRow> adjustments(Target target, long dailyMealId) {
        List<AdjustmentRow> out = new ArrayList<>();
        for (RecordEvent<Adjustment> row : records.adjustments(dailyMealId)) {
            Adjustment a = row.content();
            out.add(new AdjustmentRow(
                    row.id(),
                    a.floorAreaId(),
                    a.dietId(),
                    a.portionCount(),
                    a.changeType(),
                    a.needsDelivery(),
                    a.needsDelivery() ? row.confirmedAt() : null));
        }
        for (RecordEvent<ExtraPortionRequest> row : records.extras(dailyMealId)) {
            ExtraPortionRequest r = row.content();
            out.add(new AdjustmentRow(
                    row.id(), r.floorAreaId(), 0, 1, "EXTRA_PORTION_REQUEST", true, row.confirmedAt()));
        }
        return out;
    }

    /**
     * Tính lại suất của các người cao tuổi ({@code only} rỗng là mọi người) ở mọi bữa đã chốt chưa tới giờ và ghi phát sinh
     * (FR-039, FR-043, FR-037a): mỗi bữa một phát sinh, khóa {@code sourceKey} chống trùng (NFR-04). Gỡ dấu "dự kiến trở
     * về" khi người đã về trước giờ bữa không tạo phát sinh (FR-040a). {@code forceSafeDish}: mốc CFG-M08-05 (FR-037a ii).
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public int recompute(Optional<Collection<Long>> only, String sourceKey, String changeType, boolean forceSafeDish) {
        Instant now = time.now();
        int created = 0;
        for (PortionClosing closing : closings.beforeMealTime(now)) {
            created += recompute(closing, only, sourceKey, changeType, forceSafeDish, now);
        }
        return created;
    }

    int recompute(
            PortionClosing closing,
            Optional<Collection<Long>> only,
            String sourceKey,
            String changeType,
            boolean forceSafeDish,
            Instant now) {
        locks.lock(ClosingRecordStore.DAILY_MEAL + ":" + closing.dailyMealId());
        DailyMeal meal = dailyMeals.of(closing);
        Map<String, CountedPerson> current = current(closing);
        if (only.isPresent()) {
            current.values().removeIf(p -> !only.get().contains(p.residentId()));
        }
        boolean applySafe = forceSafeDish
                || !dailyMeals.officeHours(now)
                || !now.isBefore(meal.mealTime().orElseThrow().minus(dailyMeals.reminderLead()));
        PortionCounter.Count fresh = counter.count(meal, only, assignedDishes(closing.dailyMealId()), applySafe);
        Map<String, CountedPerson> next = new LinkedHashMap<>();
        fresh.persons().forEach(p -> next.put(p.key(), p));
        int created = 0;
        for (Delta delta : AdjustmentCalculator.diff(current, next)) {
            if (delta.change() == Change.PORTION_CHANGED && onlyExpectedReturnCleared(delta)) {
                updateSpecial(closing.dailyMealId(), delta.after());
                continue;
            }
            String type = delta.change() == Change.PORTION_CHANGED ? "PORTION_CHANGED" : changeType;
            CountedPerson person = delta.after() != null ? delta.after() : delta.before();
            Adjustment adjustment = new Adjustment(
                    closing.dailyMealId(),
                    delta.floorAreaId(),
                    delta.dietId(),
                    delta.portionCount(),
                    type,
                    sourceKey,
                    true,
                    person.key(),
                    delta.before(),
                    delta.after(),
                    null);
            String key = sourceKey + ":" + closing.dailyMealId() + ":" + person.key() + ":" + delta.change();
            if (records.addAdjustment(adjustment, null, now, key).isEmpty()) {
                continue;
            }
            created++;
            reopenPrepared(closing.dailyMealId(), delta.floorAreaId());
            if (delta.change() != Change.MOVED_OUT) {
                updateSpecial(closing.dailyMealId(), delta.after());
            }
            if (delta.change() == Change.REMOVED) {
                dropSpecial(closing.dailyMealId(), delta.before());
            }
            familyMealEffects(meal, delta, now);
        }
        int safeApplied = (int) AdjustmentCalculator.diff(current, next).stream()
                .filter(d -> d.after() != null && d.after().flags().contains(PortionFlag.SAFE_DISH))
                .filter(d -> d.before() == null || !d.before().flags().contains(PortionFlag.SAFE_DISH))
                .count();
        List<Long> conflicts = forceSafeDish ? fresh.safeDishConflicts() : List.of();
        if (safeApplied > 0 || !conflicts.isEmpty()) {
            events.publishEvent(new SafeDishApplied(
                    closing.dailyMealId(), closing.mealDate(), safeApplied, conflicts, sourceKey, now));
        }
        if (created > 0) {
            boolean reminder = !now.isBefore(meal.mealTime().orElseThrow().minus(dailyMeals.reminderLead()));
            events.publishEvent(new AdjustmentsPending(
                    closing.dailyMealId(),
                    closing.mealDate(),
                    created,
                    reminder,
                    "M08.ADJUSTMENT_CREATED:" + closing.dailyMealId() + ":" + sourceKey,
                    now));
        }
        return created;
    }

    /**
     * Đổi món của bữa đã chốt chưa tới giờ (FR-031, FR-039): một phát sinh 0 suất mỗi tầng/khu có người dùng chế độ ăn đó
     * (bếp nấu món mới), rồi tính lại suất đặc biệt của mọi người.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void dishSwapped(long swapId, LocalDate date, long mealId, long dietId) {
        Instant now = time.now();
        long dailyMealId = DailyMeal.id(mealId, date);
        Optional<PortionClosing> closing = closings.ofDailyMeal(dailyMealId);
        if (closing.isEmpty() || !now.isBefore(closing.get().mealTime())) {
            return;
        }
        locks.lock(ClosingRecordStore.DAILY_MEAL + ":" + dailyMealId);
        String source = "M08.DISH_SWAP:" + swapId;
        int created = 0;
        Set<Long> floors = new java.util.TreeSet<>();
        current(closing.get()).values().stream()
                .filter(p -> p.dietId() == dietId)
                .forEach(p -> floors.add(p.floorAreaId()));
        for (long floor : floors) {
            Adjustment swap =
                    new Adjustment(dailyMealId, floor, dietId, 0, "DISH_SWAP", source, true, null, null, null, null);
            if (records.addAdjustment(swap, null, now, source + ":" + floor).isPresent()) {
                created++;
                reopenPrepared(dailyMealId, floor);
            }
        }
        recompute(closing.get(), Optional.empty(), source, "DISH_SWAP", false, now);
        if (created > 0) {
            events.publishEvent(new AdjustmentsPending(
                    dailyMealId,
                    date,
                    created,
                    !now.isBefore(closing.get().mealTime().minus(dailyMeals.reminderLead())),
                    "M08.ADJUSTMENT_CREATED:" + dailyMealId + ":" + source,
                    now));
        }
    }

    /** Phát sinh cần giao mới của tầng/khu: phiếu Đã chuẩn bị quay về Đã chốt (data-model mục 7, bảng phiếu). */
    void reopenPrepared(long dailyMealId, long floorAreaId) {
        records.slip(dailyMealId, floorAreaId)
                .filter(s -> MealSlipStatus.PREPARED.name().equals(s.status()))
                .ifPresent(
                        s -> records.changeSlip(records.lockSlip(s.id()), MealSlipStatus.CLOSED.name(), s.content()));
    }

    /** Trạng thái suất hiện hành theo người: lần chốt, rồi lần lượt trạng thái sau mỗi phát sinh. */
    Map<String, CountedPerson> current(PortionClosing closing) {
        Map<String, CountedPerson> out = new LinkedHashMap<>();
        closing.persons().forEach(p -> out.put(p.key(), p));
        for (RecordEvent<Adjustment> row : records.adjustments(closing.dailyMealId())) {
            Adjustment a = row.content();
            if (a.personKey() == null) {
                continue;
            }
            if (a.after() == null) {
                out.remove(a.personKey());
            } else {
                out.put(a.personKey(), a.after());
            }
        }
        return out;
    }

    /** Món Bác sĩ đã chỉ định riêng theo người ở bữa ngày (FR-037). */
    Map<Long, Long> assignedDishes(long dailyMealId) {
        Map<Long, Long> out = new HashMap<>();
        for (SimpleRecord<MealSlip> slip : records.slips(dailyMealId)) {
            for (SimpleRecord<SpecialPortion> row : records.specials(slip.id())) {
                if (ClosingRecordStore.ACTIVE.equals(row.status())
                        && row.content().assignedDishId() != null) {
                    out.put(row.content().residentId(), row.content().assignedDishId());
                }
            }
        }
        return out;
    }

    /**
     * Suất của người sau phát sinh trên phiếu tầng/khu mới: suất cũ (tầng khác) {@code DROPPED}, suất mới tạo trên phiếu nếu
     * phiếu đã có (tầng chưa có phiếu: lập khi bếp xác nhận phát sinh "+", Q-306).
     */
    void updateSpecial(long dailyMealId, CountedPerson after) {
        if (after == null || after.kind() == CountedKind.FAMILY) {
            return;
        }
        Optional<SimpleRecord<SpecialPortion>> existing = activeSpecial(dailyMealId, after.residentId());
        Optional<SimpleRecord<MealSlip>> slip = records.slip(dailyMealId, after.floorAreaId());
        if (existing.isPresent()) {
            SimpleRecord<SpecialPortion> row = existing.get();
            boolean sameSlip = slip.map(s -> s.id() == row.parent().entityId()).orElse(false);
            SpecialPortion old = row.content();
            if (sameSlip && needsRecord(after)) {
                records.changeSpecial(
                        row,
                        ClosingRecordStore.ACTIVE,
                        new SpecialPortion(
                                dailyMealId,
                                after.residentId(),
                                after.dietId(),
                                after.texture(),
                                after.dishes(),
                                after.flags(),
                                after.special(),
                                old.assignedDishId(),
                                old.assignNote(),
                                old.assignedBy(),
                                old.assignedAt(),
                                old.heldServedAt(),
                                old.heldDiscardedAt(),
                                false));
                return;
            }
            records.changeSpecial(row, ClosingRecordStore.DROPPED, old);
        }
        if (needsRecord(after) && slip.isPresent()) {
            records.createSpecial(slip.get().id(), SpecialPortion.of(dailyMealId, after), currentActor());
        }
    }

    void dropSpecial(long dailyMealId, CountedPerson before) {
        if (before != null && before.kind() != CountedKind.FAMILY) {
            activeSpecial(dailyMealId, before.residentId())
                    .ifPresent(row -> records.changeSpecial(row, ClosingRecordStore.DROPPED, row.content()));
        }
    }

    Optional<SimpleRecord<SpecialPortion>> activeSpecial(long dailyMealId, long residentId) {
        for (SimpleRecord<MealSlip> slip : records.slips(dailyMealId)) {
            for (SimpleRecord<SpecialPortion> row : records.specials(slip.id())) {
                if (ClosingRecordStore.ACTIVE.equals(row.status())
                        && row.content().residentId() == residentId) {
                    return Optional.of(row);
                }
            }
        }
        return Optional.empty();
    }

    /** Suất ăn người thân theo phát sinh (FR-043): "+" ghi thêm; "−" trước giờ bữa thì Đã hủy và báo 010. */
    private void familyMealEffects(DailyMeal meal, Delta delta, Instant now) {
        CountedPerson person = delta.after() != null ? delta.after() : delta.before();
        if (person.kind() != CountedKind.FAMILY) {
            return;
        }
        if (delta.change() == Change.ADDED) {
            recordFamilyMeal(meal, person, now);
        } else if (delta.change() == Change.REMOVED
                && now.isBefore(meal.mealTime().orElseThrow())) {
            for (SimpleRecord<FamilyMeal> row : records.familyMeals(meal.id())) {
                FamilyMeal m = row.content();
                if (ClosingRecordStore.RECORDED.equals(row.status())
                        && m.familyMemberId() == person.familyMemberId()
                        && m.residentId() == person.residentId()) {
                    records.cancelFamilyMeal(row, now);
                    events.publishEvent(new CostSourceChanged(
                            FAMILY_MEAL_SOURCE,
                            Long.toString(row.id()),
                            FAMILY_MEAL_UNIT,
                            Optional.empty(),
                            true,
                            "Phát sinh trước giờ bữa (FR-043)",
                            "M08.FAMILY_MEAL_CANCELLED:" + row.id()));
                }
            }
        }
    }

    private void recordFamilyMeal(DailyMeal meal, CountedPerson person, Instant now) {
        SimpleRecord<FamilyMeal> row = records.createFamilyMeal(
                new FamilyMeal(
                        meal.id(), meal.date(), person.familyMemberId(), person.residentId(), person.stayId(), null),
                currentActor());
        events.publishEvent(new CostSourceBilled(
                FAMILY_MEAL_SOURCE,
                Long.toString(row.id()),
                FAMILY_MEAL_UNIT,
                person.residentId(),
                Optional.empty(),
                BigDecimal.ONE,
                meal.date(),
                now,
                "M08.FAMILY_MEAL:" + row.id()));
    }

    private long currentActor() {
        try {
            return user.accountId();
        } catch (IllegalStateException noUser) {
            return 0;
        }
    }

    /** Chỉ gỡ dấu "dự kiến trở về" (người đã về trước giờ bữa): không phát sinh (FR-040a). */
    private static boolean onlyExpectedReturnCleared(Delta delta) {
        Set<PortionFlag> before = EnumSet.noneOf(PortionFlag.class);
        before.addAll(delta.before().flags());
        before.remove(PortionFlag.EXPECTED_RETURN);
        return delta.before().flags().contains(PortionFlag.EXPECTED_RETURN)
                && !delta.after().flags().contains(PortionFlag.EXPECTED_RETURN)
                && before.equals(delta.after().flags())
                && delta.before().dishes().equals(delta.after().dishes())
                && delta.before().texture() == delta.after().texture()
                && delta.before().special() == delta.after().special();
    }

    /** Suất cần bản ghi trên phiếu: suất đặc biệt hoặc có dấu (FR-035, FR-036a, FR-040a). */
    static boolean needsRecord(CountedPerson person) {
        return person.kind() != CountedKind.FAMILY
                && (person.special() || !person.flags().isEmpty());
    }

    private Set<String> flags(DailyMeal meal, PortionCounter.Count count, Instant now) {
        Set<String> flags = new LinkedHashSet<>();
        if (count.menu().isEmpty()) {
            flags.add("NO_MENU");
        }
        if (now.isAfter(meal.mealTime().orElseThrow())) {
            flags.add("CLOSED_AFTER_MEAL_TIME");
        }
        if (count.persons().stream().anyMatch(p -> p.flags().contains(PortionFlag.MISSING_SUBSTITUTE_DISH))) {
            flags.add("MISSING_SUBSTITUTE_DISH");
        }
        if (count.persons().stream().anyMatch(p -> p.flags().contains(PortionFlag.NO_DIET_ASSIGNED))) {
            flags.add("NO_DIET_ASSIGNED");
        }
        return flags;
    }

    static List<FloorDietCount> portions(List<CountedPerson> persons) {
        Map<List<Long>, Integer> counts = new TreeMap<>((a, b) -> a.get(0).equals(b.get(0))
                ? a.get(1).compareTo(b.get(1))
                : a.get(0).compareTo(b.get(0)));
        persons.forEach(p -> counts.merge(List.of(p.floorAreaId(), p.dietId()), 1, Integer::sum));
        return counts.entrySet().stream()
                .map(e -> new FloorDietCount(e.getKey().get(0), e.getKey().get(1), e.getValue()))
                .collect(Collectors.toList());
    }

    private ClosingView view(long dailyMealId, PortionCounter.Count count, Set<String> flags) {
        return new ClosingView(
                dailyMealId,
                portions(count.persons()),
                (int) count.persons().stream().filter(CountedPerson::special).count(),
                (int) count.persons().stream()
                        .filter(p -> p.kind() == CountedKind.FAMILY)
                        .count(),
                flags,
                count.missingData());
    }

    private ClosingView view(PortionClosing closing) {
        return new ClosingView(
                closing.dailyMealId(),
                closing.portions(),
                (int) closing.persons().stream().filter(CountedPerson::special).count(),
                (int) closing.persons().stream()
                        .filter(p -> p.kind() == CountedKind.FAMILY)
                        .count(),
                closing.flags(),
                closing.missingData());
    }

    private static void requireMealTime(DailyMeal meal) {
        if (meal.mealTime().isEmpty()) {
            throw new ConditionNotMetException(List.of(new UnmetCondition(
                    "MEAL_TIME_NOT_SET", "Bữa chưa đặt giờ dự kiến (CFG-M08-04); viện đặt giờ trước khi chốt suất")));
        }
    }

    private static ConditionNotMetException alreadyClosed() {
        return new ConditionNotMetException(
                List.of(new UnmetCondition("ALREADY_CLOSED", "Bữa đã được chốt suất (FR-033)")));
    }
}
