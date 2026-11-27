package com.eldercare.backend.nutrition.internal.menu.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.nutrition.api.events.DishSwapped;
import com.eldercare.backend.nutrition.api.events.MenuPublished;
import com.eldercare.backend.nutrition.api.events.MenuReturned;
import com.eldercare.backend.nutrition.api.events.MenuSubmitted;
import com.eldercare.backend.nutrition.internal.catalog.domain.Dish;
import com.eldercare.backend.nutrition.internal.catalog.domain.DishStatus;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.closing.service.MealPlanning;
import com.eldercare.backend.nutrition.internal.closing.service.MealPlanning.PlanningInfo;
import com.eldercare.backend.nutrition.internal.closing.service.PortionClosingService;
import com.eldercare.backend.nutrition.internal.menu.domain.DishSwapRecord;
import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuContent;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuStatus;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thực đơn tuần chung của viện (mục D, FR-023 → FR-031, BR-M08-08, Q-445): Nhân viên bếp tạo, sao chép, sửa Nháp, gửi
 * duyệt, rút lại, hủy, đổi món; Quản lý viện trả lại, công bố (quyền {@code DISH_MENU} {@code APPROVE}). Không kiểm tra độ
 * phủ, lặp món, dị ứng; chỉ chặn món không Hiệu lực. Đổi món sau chốt tạo phát sinh (FR-039).
 */
@Service
public class MenuService {

    public static final String ENTITY = "M08.MENU";

    private static final AuditAction CREATED = new AuditAction("MENU_CREATED", false);
    private static final AuditAction CONTENT_UPDATED = new AuditAction("MENU_CONTENT_UPDATED", false);
    private static final AuditAction SUBMITTED = new AuditAction("MENU_SUBMITTED", false);
    private static final AuditAction WITHDRAWN = new AuditAction("MENU_WITHDRAWN", false);
    private static final AuditAction RETURNED = new AuditAction("MENU_RETURNED", true);
    private static final AuditAction CANCELLED = new AuditAction("MENU_CANCELLED", true);
    private static final AuditAction PUBLISHED = new AuditAction("MENU_PUBLISHED", false);
    private static final AuditAction APPLIED = new AuditAction("MENU_APPLIED", false);
    private static final AuditAction DISH_SWAPPED = new AuditAction("MENU_DISH_SWAPPED", true);

    private final MenuStore store;
    private final NutritionCatalogStore catalog;
    private final RecordEventApi records;
    private final MealTimes mealTimes;
    private final MealPlanning planning;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final PortionClosingService closing;

    public MenuService(
            MenuStore store,
            NutritionCatalogStore catalog,
            RecordEventApi records,
            MealTimes mealTimes,
            MealPlanning planning,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events,
            PortionClosingService closing) {
        this.store = store;
        this.catalog = catalog;
        this.records = records;
        this.mealTimes = mealTimes;
        this.planning = planning;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
        this.closing = closing;
    }

    /** Chi tiết thực đơn: nội dung, món cần thay (không còn Hiệu lực), lịch sử đổi món (FR-024, FR-031). */
    public record MenuDetail(Menu menu, List<MenuDish> dishesToReplace, List<RecordEvent<DishSwapRecord>> dishSwaps) {}

    /** Đổi món (api-011 {@code DishSwap}): món trước rỗng là thêm, món sau rỗng là bớt. */
    public record SwapInput(LocalDate date, Long mealId, Long oldDishId, Long newDishId) {}

    public static Target target(long id) {
        return new Target(ENTITY, Long.toString(id), null, null);
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Menu> list(Target target, LocalDate weekStart, MenuStatus status) {
        return store.list(weekStart).stream()
                .filter(m -> status == null || m.status() == status)
                .toList();
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public MenuDetail detail(Target target, long id) {
        Menu menu = store.find(id).orElseThrow(NotFoundException::new);
        Set<Long> active = activeDishIds();
        List<MenuDish> toReplace = menu.content().dishes().stream()
                .filter(d -> !active.contains(d.dishId()))
                .toList();
        return new MenuDetail(menu, toReplace, records.list(DishSwapRecord.KIND, new EntityRef(ENTITY, id)));
    }

    /** Tạo Nháp, tùy chọn sao chép nội dung thực đơn trước (dời ngày theo tuần; món không còn Hiệu lực giữ để thay). */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public long create(Target target, LocalDate weekStart, Long copyFromId) {
        if (weekStart == null || weekStart.getDayOfWeek() != DayOfWeek.MONDAY) {
            throw new InvalidValueException("Tuần của thực đơn phải bắt đầu từ thứ Hai");
        }
        if (weekStart.plusDays(6).isBefore(time.today())) {
            throw new ConditionNotMetException(
                    List.of(new UnmetCondition("WEEK_ENDED", "Không tạo thực đơn cho tuần đã kết thúc (FR-023)")));
        }
        MenuContent content = new MenuContent(List.of());
        if (copyFromId != null) {
            Menu source = store.find(copyFromId).orElseThrow(NotFoundException::new);
            long shift = ChronoUnit.DAYS.between(source.weekStart(), weekStart);
            content = new MenuContent(source.content().dishes().stream()
                    .map(d -> new MenuDish(d.date().plusDays(shift), d.mealId(), d.dishId()))
                    .toList());
        }
        if (store.activeOfWeek(weekStart).isPresent()) {
            throw weekTaken();
        }
        long id;
        try {
            id = store.insert(weekStart, content, copyFromId, user.accountId(), time.now());
        } catch (DuplicateKeyException taken) {
            throw weekTaken();
        }
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("weekStart", weekStart.toString());
        after.put("copiedFromId", copyFromId);
        audit.write(AuditLogEntry.builder()
                .action(CREATED)
                .entity(ENTITY, id)
                .after(after)
                .build());
        return id;
    }

    /** Sửa nội dung Nháp (FR-024): món Hiệu lực, bữa trong danh mục, ngày trong tuần; {@code If-Match}. */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Menu updateContent(Target target, long id, String ifMatch, MenuContent content) {
        Menu menu = locked(id);
        VersionEtag.requireMatch(
                ifMatch, menu.version(), Map.of("status", menu.status().name()));
        requireStatus(menu, MenuStatus.DRAFT);
        MenuContent input = content == null ? new MenuContent(List.of()) : content;
        List<UnmetCondition> unmet = dishProblems(menu, input.dishes());
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        store.updateContent(id, menu.version(), input);
        audit.write(AuditLogEntry.builder()
                .action(CONTENT_UPDATED)
                .entity(ENTITY, id)
                .before(Map.of("dishes", menu.content().dishes().size()))
                .after(Map.of("dishes", input.dishes().size()))
                .build());
        return store.find(id).orElseThrow(NotFoundException::new);
    }

    /**
     * Số suất dự kiến và số người cao tuổi theo loại dị ứng mỗi ngày, bữa của tuần (FR-025a): không tên, phòng, tầng; chỉ
     * để Bếp, Quản lý viện xem, không lưu.
     */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<PlanningInfo> preSubmitInfo(Target target, long id) {
        Menu menu = store.find(id).orElseThrow(NotFoundException::new);
        return planning.week(menu.weekStart());
    }

    /** Gửi duyệt (FR-025, FR-029): chỉ chặn món không còn Hiệu lực; Quản lý viện được báo. */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void submit(Target target, long id) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.DRAFT);
        List<UnmetCondition> unmet = dishProblems(menu, menu.content().dishes());
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        Instant now = time.now();
        store.submit(id, null, List.of(), user.accountId(), user.staffId().orElse(null), now);
        audit.write(transition(SUBMITTED, menu, MenuStatus.PENDING_PUBLICATION).build());
        events.publishEvent(new MenuSubmitted(id, menu.weekStart(), user.accountId(), now));
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void withdraw(Target target, long id) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.PENDING_PUBLICATION);
        store.toDraft(id, null);
        audit.write(transition(WITHDRAWN, menu, MenuStatus.DRAFT).build());
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void cancel(Target target, long id, Reason reason) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.DRAFT);
        store.cancel(id, reason.value());
        audit.write(
                transition(CANCELLED, menu, MenuStatus.CANCELLED).reason(reason).build());
    }

    /** Quản lý viện trả lại về Nháp kèm lý do; bếp người gửi được báo (FR-029). */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.APPROVE)
    @BusinessCommand
    @Transactional
    public void returnToKitchen(Target target, long id, Reason reason) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.PENDING_PUBLICATION);
        store.toDraft(id, reason.value());
        audit.write(transition(RETURNED, menu, MenuStatus.DRAFT).reason(reason).build());
        events.publishEvent(
                new MenuReturned(id, menu.weekStart(), reason.value(), submitter(menu), user.accountId(), time.now()));
    }

    /**
     * Quản lý viện công bố (FR-028 → FR-030): kiểm lại món còn Hiệu lực; tuần đã bắt đầu thì Đã áp dụng ngay; bếp người gửi
     * được báo.
     */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.APPROVE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void publish(Target target, long id) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.PENDING_PUBLICATION);
        List<UnmetCondition> unmet = dishProblems(menu, menu.content().dishes());
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        Instant now = time.now();
        MenuStatus to = menu.weekStart().isAfter(time.today()) ? MenuStatus.PUBLISHED : MenuStatus.APPLIED;
        store.publish(id, to, List.of(), user.accountId(), now);
        audit.write(transition(PUBLISHED, menu, to).build());
        events.publishEvent(new MenuPublished(
                id, menu.weekStart(), to == MenuStatus.APPLIED, submitter(menu), user.accountId(), now));
    }

    /**
     * Đổi món trên thực đơn Công bố, Đã áp dụng (FR-031): thay (món trước, món sau), thêm (chỉ món sau), bớt (chỉ món
     * trước). Lý do; món Hiệu lực; không cho bữa đã qua giờ dự kiến. Áp ngay, lưu trước/sau, báo Quản lý viện.
     */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public void swap(Target target, long id, SwapInput input, Reason reason) {
        if (input.date() == null || input.mealId() == null) {
            throw new InvalidValueException("Thiếu ngày hoặc bữa");
        }
        if (input.oldDishId() == null && input.newDishId() == null) {
            throw new InvalidValueException("Cần món trước, món sau hoặc cả hai");
        }
        Menu menu = locked(id);
        if (!menu.status().published()) {
            throw stateChanged();
        }
        if (input.date().isBefore(menu.weekStart()) || input.date().isAfter(menu.weekEnd())) {
            throw new InvalidValueException("Ngày không thuộc tuần của thực đơn");
        }
        List<UnmetCondition> unmet = new ArrayList<>();
        if (mealPassed(input.date(), input.mealId())) {
            unmet.add(new UnmetCondition("PAST_MEAL_TIME", "Bữa đã qua giờ bữa dự kiến, không đổi món (FR-031)"));
        }
        List<MenuDish> before = menu.content().dishes();
        MenuDish oldRow = null;
        if (input.oldDishId() != null) {
            oldRow = new MenuDish(input.date(), input.mealId(), input.oldDishId());
            if (!before.contains(oldRow)) {
                unmet.add(new UnmetCondition("DISH_NOT_IN_MENU", "Món trước không có ở bữa, ngày đã chọn"));
                oldRow = null;
            }
        }
        MenuDish newRow = null;
        if (input.newDishId() != null) {
            newRow = new MenuDish(input.date(), input.mealId(), input.newDishId());
            unmet.addAll(dishProblems(menu, List.of(newRow)));
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        List<MenuDish> after = new ArrayList<>(before);
        if (oldRow == null) {
            after.add(newRow);
        } else if (newRow == null) {
            after.remove(oldRow);
        } else {
            after.set(after.indexOf(oldRow), newRow);
        }
        Instant now = time.now();
        DishSwapRecord swap = new DishSwapRecord(
                input.date(), input.mealId(), input.oldDishId(), input.newDishId(), reason.value(), false);
        long swapId = records.add(
                        DishSwapRecord.KIND,
                        new EntityRef(ENTITY, id),
                        new NewRecordEvent<>(swap, null, user.accountId(), false, now, null, now, null, null))
                .id();
        store.swap(id, new MenuContent(after), swapId);
        Map<String, Object> beforeLog = new LinkedHashMap<>();
        beforeLog.put("date", input.date().toString());
        beforeLog.put("mealId", input.mealId());
        beforeLog.put("dishId", input.oldDishId());
        Map<String, Object> afterLog = new LinkedHashMap<>();
        afterLog.put("dishId", input.newDishId());
        audit.write(AuditLogEntry.builder()
                .action(DISH_SWAPPED)
                .entity(ENTITY, id)
                .before(beforeLog)
                .after(afterLog)
                .reason(reason)
                .build());
        closing.dishSwapped(swapId, input.date(), input.mealId());
        events.publishEvent(new DishSwapped(
                id,
                swapId,
                input.date(),
                input.mealId(),
                input.oldDishId(),
                input.newDishId(),
                input.date().equals(time.today()),
                user.accountId(),
                now));
    }

    /** Id thực đơn Công bố có tuần đã bắt đầu (việc hằng ngày, FR-030). */
    @Transactional(readOnly = true)
    public List<Long> dueForApplied(LocalDate today) {
        return store.dueForApplied(today);
    }

    /** Bộ lập lịch chuyển Công bố → Đã áp dụng; chạy lại không đổi gì. */
    @Transactional
    public void markApplied(long id, Instant now) {
        store.lock(id).ifPresent(menu -> {
            if (menu.status() == MenuStatus.PUBLISHED && store.markApplied(id, now)) {
                audit.write(transition(APPLIED, menu, MenuStatus.APPLIED).build());
            }
        });
    }

    /** Thực đơn chưa hủy của tuần (nhắc CFG-M08-07, FR-027). */
    @Transactional(readOnly = true)
    public Optional<Menu> ofWeek(LocalDate weekStart) {
        return store.activeOfWeek(weekStart);
    }

    /** Món không có hoặc không Hiệu lực, bữa không có trong danh mục, ngày ngoài tuần: mọi lỗi báo cùng một lần. */
    private List<UnmetCondition> dishProblems(Menu menu, List<MenuDish> dishes) {
        List<UnmetCondition> unmet = new ArrayList<>();
        Map<Long, Dish> byId = catalog.dishes().stream().collect(Collectors.toMap(Dish::id, d -> d));
        Set<Long> meals = catalog.mealItems().stream()
                .filter(m -> m.status() == CatalogStatus.ACTIVE)
                .map(m -> m.id())
                .collect(Collectors.toSet());
        List<String> notActive = new ArrayList<>();
        for (MenuDish row : dishes) {
            if (row.date() == null
                    || row.date().isBefore(menu.weekStart())
                    || row.date().isAfter(menu.weekEnd())) {
                throw new InvalidValueException("Ngày " + row.date() + " không thuộc tuần của thực đơn");
            }
            if (!meals.contains(row.mealId())) {
                throw new InvalidValueException("Bữa không có trong danh mục hoặc đã Ngừng: " + row.mealId());
            }
            Dish dish = byId.get(row.dishId());
            if (dish == null) {
                throw new InvalidValueException("Món không có trong danh mục: " + row.dishId());
            }
            if (dish.status() != DishStatus.ACTIVE && !notActive.contains(dish.name())) {
                notActive.add(dish.name());
            }
        }
        if (!notActive.isEmpty()) {
            unmet.add(new UnmetCondition(
                    "DISH_NOT_ACTIVE", "Món chưa Hiệu lực hoặc đã Ngừng, cần thay: " + String.join(", ", notActive)));
        }
        return unmet;
    }

    private Set<Long> activeDishIds() {
        return catalog.dishes().stream()
                .filter(d -> d.status() == DishStatus.ACTIVE)
                .map(Dish::id)
                .collect(Collectors.toSet());
    }

    private static Long submitter(Menu menu) {
        return menu.submitterStaffId() != null ? menu.submitterStaffId() : menu.submittedBy();
    }

    private boolean mealPassed(LocalDate date, long mealId) {
        LocalDate today = time.today();
        if (date.isBefore(today)) {
            return true;
        }
        if (date.isAfter(today)) {
            return false;
        }
        Optional<LocalTime> expected = mealTimes.expected(mealId);
        return expected.isPresent()
                && !time.now()
                        .isBefore(time.startOfDay(today)
                                .plusSeconds(expected.get().toSecondOfDay()));
    }

    private Menu locked(long id) {
        return store.lock(id).orElseThrow(NotFoundException::new);
    }

    private static void requireStatus(Menu menu, MenuStatus expected) {
        if (menu.status() != expected) {
            throw stateChanged();
        }
    }

    private static AuditLogEntry.Builder transition(AuditAction action, Menu menu, MenuStatus to) {
        return AuditLogEntry.builder()
                .action(action)
                .entity(ENTITY, menu.id())
                .before(Map.of("status", menu.status().name()))
                .after(Map.of("status", to.name()));
    }

    private static BusinessException weekTaken() {
        return new ConditionNotMetException(
                List.of(new UnmetCondition("MENU_EXISTS_FOR_WEEK", "Tuần đã có thực đơn chưa hủy (FR-023)")));
    }

    private static BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Thực đơn đã đổi trạng thái");
    }
}
