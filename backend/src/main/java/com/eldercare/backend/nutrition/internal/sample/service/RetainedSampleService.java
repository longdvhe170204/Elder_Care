package com.eldercare.backend.nutrition.internal.sample.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.events.RetainedSampleNotice;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.RetainedSample;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.RetainedSample.SampleDish;
import com.eldercare.backend.nutrition.internal.closing.domain.ClosingRecords.SpecialPortion;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson;
import com.eldercare.backend.nutrition.internal.closing.domain.CountedPerson.DishChoice;
import com.eldercare.backend.nutrition.internal.closing.domain.DailyMeal;
import com.eldercare.backend.nutrition.internal.closing.persistence.ClosingRecordStore;
import com.eldercare.backend.nutrition.internal.closing.persistence.PortionClosingStore;
import com.eldercare.backend.nutrition.internal.sample.domain.DishesToRetain;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lưu mẫu thức ăn mỗi bữa (mục J, FR-060 → FR-062, BR-M08-15; research M-11): bếp ghi mọi món được nấu của bữa, mỗi món đã
 * lưu hoặc có lý do không lưu; gọi lại để thêm món mới phát sinh sau đó; hết hạn lưu = lúc ghi + CFG-M08-06; hủy mẫu đặt một
 * lần. Thiếu bản ghi chặn giao phiếu ({@code MealSlipService}). Quyền "Chuẩn bị, giao phiếu, lưu mẫu": xem (VIEW, gồm Bác
 * sĩ, Quản lý viện), ghi (CREATE), hủy mẫu (UPDATE).
 */
@Service
public class RetainedSampleService {

    public static final String ENTITY = "M08.RETAINED_SAMPLE";

    private static final String DISCARDED = "DISCARDED";
    private static final AuditAction RECORDED = new AuditAction("RETAINED_SAMPLE_RECORDED", false);
    private static final AuditAction DISCARD = new AuditAction("RETAINED_SAMPLE_DISCARDED", false);

    private final ClosingRecordStore records;
    private final PortionClosingStore closings;
    private final NutritionCatalogStore catalog;
    private final ParameterApi parameters;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    RetainedSampleService(
            ClosingRecordStore records,
            PortionClosingStore closings,
            NutritionCatalogStore catalog,
            ParameterApi parameters,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.records = records;
        this.closings = closings;
        this.catalog = catalog;
        this.parameters = parameters;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
    }

    /** Một món trong lệnh ghi (api-011 {@code RetainedSampleCommand.dishes}). */
    public record DishEntry(long dishId, boolean retained, String notRetainedReason) {}

    /** Thân lệnh ghi lưu mẫu. */
    public record Command(String storageLocation, List<DishEntry> dishes) {}

    /** Một dòng món của bản ghi, đã ghi hay chưa (món cần lưu mà bếp chưa ghi: {@code recorded = false}). */
    public record DishLine(
            long dishId,
            String dishName,
            boolean recorded,
            boolean retained,
            String notRetainedReason,
            boolean addedLater) {}

    public record View(long dailyMealId, List<DishLine> dishes, SimpleRecord<RetainedSample> sample) {}

    public static Target target(long id) {
        return new Target(ENTITY, Long.toString(id), null, null);
    }

    /** Món cần lưu mẫu của bữa (tính từ thực đơn và kết quả chốt) kèm bản ghi lưu mẫu nếu đã ghi. */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public View view(Target target, long dailyMealId) {
        requireMeal(dailyMealId);
        var sample = records.retainedSample(dailyMealId);
        Map<Long, DishLine> lines = new LinkedHashMap<>();
        for (long dish : requiredDishes(dailyMealId)) {
            lines.put(dish, new DishLine(dish, dishName(dish), false, false, null, false));
        }
        sample.ifPresent(row -> {
            for (SampleDish d : row.content().dishes()) {
                lines.put(
                        d.dishId(),
                        new DishLine(
                                d.dishId(),
                                dishName(d.dishId()),
                                true,
                                d.retained(),
                                d.notRetainedReason(),
                                d.addedLater()));
            }
        });
        return new View(dailyMealId, new ArrayList<>(lines.values()), sample.orElse(null));
    }

    /**
     * Ghi lưu mẫu (FR-060, FR-061). Lần đầu phải nêu đủ mọi món cần lưu; các lần sau chỉ thêm món chưa có (món phát sinh
     * sau khi đã ghi). Món không lưu bắt buộc lý do; có món không lưu thì báo Bác sĩ, Quản lý viện (Nhẹ).
     */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public SimpleRecord<RetainedSample> recordSample(Target target, long dailyMealId, Command command) {
        requireMeal(dailyMealId);
        Instant now = time.now();
        List<DishEntry> entries = command.dishes() == null ? List.of() : command.dishes();
        if (entries.isEmpty()) {
            throw new InvalidValueException("Bản ghi lưu mẫu phải có ít nhất một món");
        }
        Set<Long> seen = new HashSet<>();
        for (DishEntry e : entries) {
            if (!seen.add(e.dishId())) {
                throw new InvalidValueException("Mỗi món chỉ được ghi một lần trong lệnh");
            }
            requireDish(e.dishId());
            if (!e.retained()
                    && (e.notRetainedReason() == null || e.notRetainedReason().isBlank())) {
                throw new InvalidValueException("Món không lưu mẫu phải có lý do");
            }
        }
        var existing = records.retainedSample(dailyMealId);
        SimpleRecord<RetainedSample> result;
        if (existing.isEmpty()) {
            List<Long> missing = DishesToRetain.notRecorded(requiredDishes(dailyMealId), seen);
            if (!missing.isEmpty()) {
                throw new InvalidValueException("Bản ghi lưu mẫu thiếu " + missing.size() + " món được nấu trong bữa");
            }
            Instant expires = now.plus(parameters.read(ParameterCode.CFG_M08_06).asDuration());
            result = records.createSample(
                    RetainedSample.recorded(
                            dailyMealId,
                            clean(command.storageLocation()),
                            user.accountId(),
                            now,
                            expires,
                            entries.stream().map(e -> dish(e, false)).toList()),
                    user.accountId());
        } else {
            SimpleRecord<RetainedSample> row = records.lockSample(existing.get().id());
            if (DISCARDED.equals(row.status())) {
                throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Mẫu đã hủy, không ghi thêm món");
            }
            Set<Long> recorded = new HashSet<>();
            row.content().dishes().forEach(d -> recorded.add(d.dishId()));
            for (DishEntry e : entries) {
                if (recorded.contains(e.dishId())) {
                    throw new InvalidValueException("Món " + e.dishId() + " đã có trong bản ghi lưu mẫu");
                }
            }
            List<SampleDish> all = new ArrayList<>(row.content().dishes());
            entries.forEach(e -> all.add(dish(e, true)));
            records.changeSample(row, row.status(), row.content().withDishes(all), false);
            result = records.retainedSample(dailyMealId).orElseThrow();
        }
        audit.write(AuditLogEntry.builder()
                .action(RECORDED)
                .entity(ENTITY, result.id())
                .after(Map.of("dailyMealId", dailyMealId, "dishCount", entries.size()))
                .build());
        long notRetained = entries.stream().filter(e -> !e.retained()).count();
        if (notRetained > 0) {
            events.publishEvent(new RetainedSampleNotice(
                    result.id(), dailyMealId, RetainedSampleNotice.Kind.NOT_RETAINED, (int) notRetained, now));
        }
        return result;
    }

    /** Ghi thời điểm hủy mẫu (FR-062): đặt một lần rồi khóa dòng; lần hai trả 409. */
    @RequiresPermission(function = Function.MEAL_PREPARATION, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void discard(Target target, long id) {
        SimpleRecord<RetainedSample> row = records.lockSample(id);
        if (DISCARDED.equals(row.status())) {
            throw new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Mẫu đã được hủy");
        }
        Instant now = time.now();
        records.changeSample(row, DISCARDED, row.content().discarded(user.accountId(), now), true);
        audit.write(AuditLogEntry.builder()
                .action(DISCARD)
                .entity(ENTITY, id)
                .after(Map.of("dailyMealId", row.content().dailyMealId()))
                .build());
    }

    /** Mẫu quá hạn lưu mà chưa hủy (FR-062); {@code null} khi bản ghi chưa có hạn (dấu tối thiểu). */
    public List<SimpleRecord<RetainedSample>> overdue(Instant now) {
        return records.samplesWithStatus(ClosingRecordStore.RECORDED).stream()
                .filter(row -> row.content().expiresAt() != null
                        && !row.content().expiresAt().isAfter(now))
                .toList();
    }

    /** Mã món cần lưu mẫu: món của lần chốt, phát sinh sau chốt, suất đặc biệt và món chỉ định riêng. */
    private List<Long> requiredDishes(long dailyMealId) {
        List<DishChoice> served = new ArrayList<>();
        List<Long> assigned = new ArrayList<>();
        closings.ofDailyMeal(dailyMealId).ifPresent(c -> c.persons().forEach(p -> served.addAll(p.dishes())));
        records.adjustments(dailyMealId).forEach(a -> {
            CountedPerson after = a.content().after();
            if (after != null) {
                served.addAll(after.dishes());
            }
        });
        records.slips(dailyMealId)
                .forEach(slip -> records.specials(slip.id()).stream()
                        .filter(s -> ClosingRecordStore.ACTIVE.equals(s.status()))
                        .map(SimpleRecord::content)
                        .forEach((SpecialPortion s) -> {
                            served.addAll(s.dishes());
                            if (s.assignedDishId() != null) {
                                assigned.add(s.assignedDishId());
                            }
                        }));
        return DishesToRetain.of(served, assigned);
    }

    private void requireMeal(long dailyMealId) {
        try {
            catalog.meal(DailyMeal.mealIdOf(dailyMealId));
        } catch (NotFoundException missing) {
            throw new NotFoundException();
        }
    }

    private void requireDish(long dishId) {
        try {
            catalog.dish(dishId);
        } catch (NotFoundException missing) {
            throw new InvalidValueException("Món " + dishId + " không có trong danh mục");
        }
    }

    private String dishName(long dishId) {
        return catalog.dish(dishId).name();
    }

    private static SampleDish dish(DishEntry e, boolean addedLater) {
        return new SampleDish(
                e.dishId(),
                e.retained(),
                e.retained() ? null : e.notRetainedReason().strip(),
                addedLater);
    }

    private static String clean(String text) {
        return text == null || text.isBlank() ? null : text.strip();
    }
}
