package com.eldercare.backend.nutrition.internal.menu.service;

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
import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewRecordEvent;
import com.eldercare.backend.administration.shared.api.dto.RecordEvent;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
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
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.menu.domain.AllergyWarningRule.AllergyWarning;
import com.eldercare.backend.nutrition.internal.menu.domain.CoverageCheck.Gap;
import com.eldercare.backend.nutrition.internal.menu.domain.DishSwapRecord;
import com.eldercare.backend.nutrition.internal.menu.domain.Menu;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuCheckRecord;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuCheckRecord.Occasion;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuContent;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuDish;
import com.eldercare.backend.nutrition.internal.menu.domain.MenuStatus;
import com.eldercare.backend.nutrition.internal.menu.persistence.MenuStore;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Thực đơn tuần (mục D, FR-023 → FR-032, BR-M08-06 → 08, Q-308; research M-07): Nhân viên bếp tạo, sao chép, sửa Nháp,
 * gửi kiểm tra, rút lại, hủy, đổi món; Bác sĩ trả lại, công bố. Kiểm tra độ phủ, lặp món, dị ứng chạy trong một lần với dữ
 * liệu tại thời điểm lệnh; mọi lỗi báo cùng một lần. Phát sinh cho bữa đã chốt khi đổi món có ở phase chốt suất (FR-039).
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
    private final MenuChecker checker;
    private final NutritionCatalogStore catalog;
    private final RecordEventApi records;
    private final ResidentApi residents;
    private final PermissionApi permissions;
    private final MealTimes mealTimes;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;

    public MenuService(
            MenuStore store,
            MenuChecker checker,
            NutritionCatalogStore catalog,
            RecordEventApi records,
            ResidentApi residents,
            PermissionApi permissions,
            MealTimes mealTimes,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events) {
        this.store = store;
        this.checker = checker;
        this.catalog = catalog;
        this.records = records;
        this.residents = residents;
        this.permissions = permissions;
        this.mealTimes = mealTimes;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
    }

    /** Cảnh báo dị ứng đã lọc theo quyền xem: bếp chỉ có số người ({@code residents} {@code null}; 19.3, SC-014). */
    public record AllergyView(
            LocalDate date, long mealId, long dietId, long dishId, int residentCount, List<NamedResident> residents) {}

    public record NamedResident(long id, String name) {}

    /** Kết quả kiểm tra trả cho người gọi (api-011 {@code MenuCheckResult}). */
    public record CheckView(
            List<Gap> coverageGaps,
            List<RepetitionView> repetitions,
            List<AllergyView> allergies,
            List<String> codes) {}

    public record RepetitionView(long dietId, long dishId, LocalDate from, LocalDate to, String code) {}

    /** Chi tiết thực đơn: nội dung, ô cần thay (bản sao), lịch sử đổi món, các lần kiểm tra (FR-024, FR-031). */
    public record MenuDetail(
            Menu menu,
            List<MenuDish> dishesToReplace,
            List<RecordEvent<DishSwapRecord>> dishSwaps,
            List<CheckView> checks) {}

    /** Số người dùng một chế độ ăn trong tuần, không tên (FR-025a). */
    public record DietCount(long dietId, String dietName, int residentCount) {}

    /** Đổi món (api-011 {@code DishSwap}): món trước rỗng là thêm, món sau rỗng là bớt. */
    public record SwapInput(
            LocalDate date,
            Long mealId,
            Long dietId,
            Long oldDishId,
            Long newDishId,
            List<Long> substituteDishIds,
            List<String> confirmedWarnings) {}

    public static Target target(long id) {
        return new Target(ENTITY, Long.toString(id), null, null);
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<Menu> list(Target target, LocalDate weekStart) {
        return store.list(weekStart);
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public MenuDetail detail(Target target, long id) {
        Menu menu = store.find(id).orElseThrow(NotFoundException::new);
        EntityRef parent = new EntityRef(ENTITY, id);
        List<CheckView> checks = records.list(MenuCheckRecord.KIND, parent).stream()
                .map(r -> view(r.content()))
                .toList();
        return new MenuDetail(
                menu, checker.toReplace(menu.content().dishes()), records.list(DishSwapRecord.KIND, parent), checks);
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
                    .map(d -> new MenuDish(
                            d.date().plusDays(shift), d.mealId(), d.dietId(), d.dishId(), d.substituteDishIds()))
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

    /** Sửa nội dung Nháp (FR-024): món Hiệu lực, thỏa FR-005; {@code If-Match}. */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public Menu updateContent(Target target, long id, String ifMatch, MenuContent content) {
        Menu menu = locked(id);
        VersionEtag.requireMatch(
                ifMatch, menu.version(), Map.of("status", menu.status().name()));
        requireStatus(menu, MenuStatus.DRAFT);
        MenuContent input = content == null ? new MenuContent(List.of()) : content;
        List<UnmetCondition> unmet = checker.dishProblems(menu.weekStart(), input.dishes());
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

    /** Số người dùng từng chế độ ăn trong tuần (FR-025a, FR-027); không tên, không dị ứng. */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<DietCount> preSubmitInfo(Target target, long id) {
        Menu menu = store.find(id).orElseThrow(NotFoundException::new);
        MenuChecker.WeekUsers users = checker.users(menu.weekStart());
        return users.dietsInUse().stream()
                .map(d -> new DietCount(d.dietId(), catalog.diet(d.dietId()).name(), users.count(d.dietId())))
                .toList();
    }

    /** Chạy kiểm tra một lần, không đổi trạng thái (FR-025). */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public CheckView check(Target target, long id) {
        Menu menu = store.find(id).orElseThrow(NotFoundException::new);
        MenuChecker.Outcome outcome =
                checker.check(menu.weekStart(), menu.content().dishes());
        return view(outcome.gaps(), outcome.repetitions(), outcome.allergies());
    }

    /**
     * Gửi kiểm tra (FR-025, FR-025a): mọi điều kiện chặn báo cùng một lần — xác nhận kiểm tra chế biến, món Hiệu lực, độ
     * phủ, đã xác nhận mọi cảnh báo.
     */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CheckView submit(
            Target target, long id, boolean processingCheckConfirmed, String processingNote, List<String> confirmed) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.DRAFT);
        if (processingNote != null && processingNote.length() > 2000) {
            throw new InvalidValueException("Ghi chú kiểm tra chế biến tối đa 2000 ký tự");
        }
        List<UnmetCondition> unmet = new ArrayList<>();
        if (!processingCheckConfirmed) {
            unmet.add(new UnmetCondition(
                    "PROCESSING_CHECK_REQUIRED",
                    "Chưa xác nhận đã kiểm tra khả năng chế biến, nguyên liệu, suất ăn (FR-025a)"));
        }
        unmet.addAll(checker.dishProblems(menu.weekStart(), menu.content().dishes()));
        MenuChecker.Outcome outcome =
                checker.check(menu.weekStart(), menu.content().dishes());
        unmet.addAll(coverage(outcome.gaps()));
        List<String> warnings = outcome.warningCodes();
        List<String> accepted = confirmed == null ? List.of() : confirmed;
        if (!accepted.containsAll(warnings)) {
            unmet.add(warningsNotConfirmed());
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        Instant now = time.now();
        store.submit(
                id,
                blankToNull(processingNote),
                warnings,
                user.accountId(),
                user.staffId().orElse(null),
                now);
        recordCheck(id, Occasion.SUBMIT, outcome, warnings, now);
        audit.write(AuditLogEntry.builder()
                .action(SUBMITTED)
                .entity(ENTITY, id)
                .before(Map.of("status", menu.status().name()))
                .after(Map.of("status", MenuStatus.PENDING_PUBLICATION.name(), "confirmedWarnings", warnings))
                .build());
        events.publishEvent(new MenuSubmitted(id, menu.weekStart(), false, user.accountId(), now));
        return view(outcome.gaps(), outcome.repetitions(), outcome.allergies());
    }

    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public void withdraw(Target target, long id) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.PENDING_PUBLICATION);
        store.toDraft(id, null);
        audit.write(transition(WITHDRAWN, menu, MenuStatus.DRAFT).build());
        events.publishEvent(new MenuSubmitted(id, menu.weekStart(), true, user.accountId(), time.now()));
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

    /** Bác sĩ trả lại về Nháp kèm lý do; bếp người gửi được báo (FR-029). */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.APPROVE)
    @BusinessCommand
    @Transactional
    public void returnToKitchen(Target target, long id, Reason reason) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.PENDING_PUBLICATION);
        store.toDraft(id, reason.value());
        audit.write(transition(RETURNED, menu, MenuStatus.DRAFT).reason(reason).build());
        long staff = menu.submitterStaffId() != null ? menu.submitterStaffId() : menu.submittedBy();
        events.publishEvent(
                new MenuReturned(id, menu.weekStart(), reason.value(), staff, user.accountId(), time.now()));
    }

    /**
     * Bác sĩ công bố (FR-029, FR-030): chạy lại độ phủ (thiếu thì chặn) và cảnh báo với dữ liệu tại lúc công bố; cảnh báo
     * mới so với lần gửi phải được xác nhận; tuần đã bắt đầu thì Đã áp dụng ngay.
     */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.APPROVE)
    @BusinessCommand(requiresReason = false)
    @Transactional
    public CheckView publish(Target target, long id, List<String> confirmed) {
        Menu menu = locked(id);
        requireStatus(menu, MenuStatus.PENDING_PUBLICATION);
        List<UnmetCondition> unmet = new ArrayList<>(
                checker.dishProblems(menu.weekStart(), menu.content().dishes()));
        MenuChecker.Outcome outcome =
                checker.check(menu.weekStart(), menu.content().dishes());
        unmet.addAll(coverage(outcome.gaps()));
        Set<String> accepted = new LinkedHashSet<>(menu.confirmedWarnings());
        if (confirmed != null) {
            accepted.addAll(confirmed);
        }
        if (!accepted.containsAll(outcome.warningCodes())) {
            unmet.add(warningsNotConfirmed());
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        Instant now = time.now();
        MenuStatus to = menu.weekStart().isAfter(time.today()) ? MenuStatus.PUBLISHED : MenuStatus.APPLIED;
        List<String> stored = new ArrayList<>(accepted);
        store.publish(id, to, stored, user.accountId(), now);
        recordCheck(id, Occasion.PUBLISH, outcome, stored, now);
        audit.write(transition(PUBLISHED, menu, to).build());
        events.publishEvent(new MenuPublished(id, menu.weekStart(), to == MenuStatus.APPLIED, user.accountId(), now));
        return view(outcome.gaps(), outcome.repetitions(), outcome.allergies());
    }

    /**
     * Đổi món trên thực đơn Công bố, Đã áp dụng (FR-031, FR-032; hợp đồng {@code DishSwap} sửa 2026-11-06): thay món (món
     * trước, món sau), thêm món (chỉ món sau; bổ sung bữa trống), bớt món (chỉ món trước), đổi món thay thế (món trước bằng
     * món sau kèm {@code substituteDishIds}). Lý do; món Hiệu lực, thỏa FR-005; không cho bữa đã qua giờ dự kiến (ngày đã
     * qua luôn chặn; hôm nay chặn khi bữa có giờ CFG-M08-04 và đã qua, chủ dự án chốt 2026-11-06); độ phủ của bữa bị đổi
     * chặn; cảnh báo mới phải được xác nhận. Áp ngay, lưu trước/sau, báo Bác sĩ.
     */
    @RequiresPermission(function = Function.DISH_MENU, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional
    public CheckView swap(Target target, long id, SwapInput input, Reason reason) {
        if (input.date() == null || input.mealId() == null || input.dietId() == null) {
            throw new InvalidValueException("Thiếu ngày, bữa hoặc chế độ ăn");
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
            oldRow = before.stream()
                    .filter(row -> row.date().equals(input.date())
                            && row.mealId() == input.mealId()
                            && row.dietId() == input.dietId()
                            && row.dishId() == input.oldDishId())
                    .findFirst()
                    .orElse(null);
            if (oldRow == null) {
                unmet.add(new UnmetCondition("DISH_NOT_IN_MENU", "Món trước không có ở bữa, chế độ ăn, ngày đã chọn"));
            }
        }
        MenuDish newRow = null;
        if (input.newDishId() != null) {
            List<Long> substitutes = input.substituteDishIds() != null
                    ? input.substituteDishIds()
                    : oldRow != null ? oldRow.substituteDishIds() : List.of();
            newRow = new MenuDish(input.date(), input.mealId(), input.dietId(), input.newDishId(), substitutes);
            unmet.addAll(checker.dishProblems(menu.weekStart(), List.of(newRow)));
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        List<MenuDish> after = new ArrayList<>(before);
        if (oldRow == null) {
            after.add(newRow);
        } else if (newRow == null) {
            after.remove(after.indexOf(oldRow));
        } else {
            after.set(after.indexOf(oldRow), newRow);
        }
        MenuChecker.Outcome previous = checker.check(menu.weekStart(), before);
        MenuChecker.Outcome outcome = checker.checkSlot(menu.weekStart(), after, input.date(), input.mealId());
        unmet.addAll(coverage(outcome.gaps()));
        Set<String> accepted = new LinkedHashSet<>(previous.warningCodes());
        if (input.confirmedWarnings() != null) {
            accepted.addAll(input.confirmedWarnings());
        }
        if (!accepted.containsAll(outcome.warningCodes())) {
            unmet.add(warningsNotConfirmed());
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        Instant now = time.now();
        DishSwapRecord swap = new DishSwapRecord(
                input.date(),
                input.mealId(),
                input.dietId(),
                input.oldDishId(),
                input.newDishId(),
                oldRow == null ? List.of() : oldRow.substituteDishIds(),
                newRow == null ? List.of() : newRow.substituteDishIds(),
                reason.value(),
                false);
        long swapId = records.add(
                        DishSwapRecord.KIND,
                        new EntityRef(ENTITY, id),
                        new NewRecordEvent<>(swap, null, user.accountId(), false, now, null, now, null, null))
                .id();
        store.swap(id, new MenuContent(after), swapId);
        recordCheck(id, Occasion.DISH_SWAP, outcome, input.confirmedWarnings(), now);
        Map<String, Object> beforeLog = new LinkedHashMap<>();
        beforeLog.put("date", input.date().toString());
        beforeLog.put("mealId", input.mealId());
        beforeLog.put("dietId", input.dietId());
        beforeLog.put("dishId", input.oldDishId());
        beforeLog.put("substituteDishIds", swap.oldSubstituteDishIds());
        Map<String, Object> afterLog = new LinkedHashMap<>();
        afterLog.put("dishId", input.newDishId());
        afterLog.put("substituteDishIds", swap.substituteDishIds());
        audit.write(AuditLogEntry.builder()
                .action(DISH_SWAPPED)
                .entity(ENTITY, id)
                .before(beforeLog)
                .after(afterLog)
                .reason(reason)
                .build());
        events.publishEvent(new DishSwapped(
                id,
                swapId,
                input.date(),
                input.mealId(),
                input.dietId(),
                input.oldDishId(),
                input.newDishId(),
                input.date().equals(time.today()),
                user.accountId(),
                now));
        return view(outcome.gaps(), outcome.repetitions(), outcome.allergies());
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

    private void recordCheck(
            long id, Occasion occasion, MenuChecker.Outcome outcome, List<String> confirmed, Instant at) {
        records.add(
                MenuCheckRecord.KIND,
                new EntityRef(ENTITY, id),
                new NewRecordEvent<>(
                        new MenuCheckRecord(
                                occasion, outcome.gaps(), outcome.repetitions(), outcome.allergies(), confirmed),
                        null,
                        user.accountId(),
                        false,
                        at,
                        null,
                        at,
                        null,
                        null));
    }

    private CheckView view(MenuCheckRecord record) {
        return view(record.coverageGaps(), record.repetitions(), record.allergies());
    }

    private CheckView view(
            List<Gap> gaps,
            List<com.eldercare.backend.nutrition.internal.menu.domain.DishRepetitionCheck.Repetition> repetitions,
            List<AllergyWarning> allergies) {
        List<String> codes = new ArrayList<>();
        List<RepetitionView> reps = repetitions.stream()
                .map(r -> new RepetitionView(r.dietId(), r.dishId(), r.from(), r.to(), r.code()))
                .toList();
        reps.forEach(r -> codes.add(r.code()));
        List<AllergyView> allergyViews = new ArrayList<>();
        for (AllergyWarning a : allergies) {
            codes.add(a.code());
            List<NamedResident> names = a.residentIds().stream()
                    .filter(this::mayViewAllergy)
                    .map(r -> new NamedResident(r, residents.identity(r).fullName()))
                    .toList();
            allergyViews.add(new AllergyView(
                    a.date(),
                    a.mealId(),
                    a.dietId(),
                    a.dishId(),
                    a.residentIds().size(),
                    names.isEmpty() ? null : names));
        }
        return new CheckView(gaps, reps, allergyViews, codes);
    }

    /** Tên người trong cảnh báo dị ứng chỉ cho người có quyền xem dị ứng của người đó (FR-025 c, 19.3). */
    private boolean mayViewAllergy(long residentId) {
        return permissions
                .hasPermission(
                        user,
                        new PermissionKey(Function.ALLERGY_CONDITION, Operation.VIEW),
                        new Target("M01.RESIDENT", Long.toString(residentId), residentId, null))
                .allowed();
    }

    private Menu locked(long id) {
        return store.lock(id).orElseThrow(NotFoundException::new);
    }

    private static void requireStatus(Menu menu, MenuStatus expected) {
        if (menu.status() != expected) {
            throw stateChanged();
        }
    }

    private static List<UnmetCondition> coverage(List<Gap> gaps) {
        return gaps.stream()
                .map(g -> new UnmetCondition(
                        "INSUFFICIENT_COVERAGE",
                        "Chế độ ăn " + g.dietId() + " thiếu món ngày " + g.date() + ", bữa " + g.mealId()))
                .toList();
    }

    private static UnmetCondition warningsNotConfirmed() {
        return new UnmetCondition(
                "WARNINGS_NOT_CONFIRMED", "Chưa xác nhận đã xem các cảnh báo lặp món, dị ứng (FR-025)");
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

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
}
