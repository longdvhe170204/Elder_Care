package com.eldercare.backend.nutrition.internal.assignment.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.error.ConditionNotMetException;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.dto.DietAssignmentStatus;
import com.eldercare.backend.nutrition.api.dto.Texture;
import com.eldercare.backend.nutrition.api.events.DietAssignmentTookEffect;
import com.eldercare.backend.nutrition.api.events.ResidentWithoutDietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.domain.DietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.domain.NewDietAssignment;
import com.eldercare.backend.nutrition.internal.assignment.domain.ProfessionalReasonRule;
import com.eldercare.backend.nutrition.internal.assignment.domain.ProfessionalReasonRule.Side;
import com.eldercare.backend.nutrition.internal.assignment.persistence.DietAssignmentStore;
import com.eldercare.backend.nutrition.internal.assignment.persistence.ResidentNutritionLock;
import com.eldercare.backend.nutrition.internal.catalog.domain.Diet;
import com.eldercare.backend.nutrition.internal.catalog.persistence.NutritionCatalogStore;
import com.eldercare.backend.nutrition.internal.menu.service.ResidentMenuConflicts;
import com.eldercare.backend.nutrition.internal.review.service.ReviewNoticeService;
import com.eldercare.backend.resident.api.AllergenApi;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * Bản gán chế độ ăn (FR-010 → FR-017, BR-M08-03, Q-283; research M-03, M-05): Bác sĩ lập đề xuất, Áp dụng (Hiệu lực
 * ngay hoặc Chờ hiệu lực, bản cũ Ngừng cùng lệnh), Hủy; Bộ lập lịch chuyển Chờ hiệu lực → Hiệu lực; trạng thái cuối của
 * người cao tuổi Ngừng, Hủy bản gán (FR-015). Mọi lệnh khóa {@code M08.RESIDENT_NUTRITION} của người đó trước khi đọc lại.
 * Lập và Áp dụng đối chiếu thực đơn của chế độ ăn mới (FR-016, {@link ResidentMenuConflicts}); phát sinh cho bữa đã chốt
 * (FR-017) có ở phase chốt suất.
 */
@Service
public class DietAssignmentService {

    public static final String ENTITY = "M08.DIET_ASSIGNMENT";

    private static final AuditAction PROPOSED = new AuditAction("DIET_ASSIGNMENT_PROPOSED", false);
    private static final AuditAction APPLIED = new AuditAction("DIET_ASSIGNMENT_APPLIED", false);
    private static final AuditAction ACTIVATED = new AuditAction("DIET_ASSIGNMENT_ACTIVATED", false);
    private static final AuditAction DISCONTINUED = new AuditAction("DIET_ASSIGNMENT_DISCONTINUED", false);
    private static final AuditAction CANCELLED = new AuditAction("DIET_ASSIGNMENT_CANCELLED", true);
    /** Phụ lục 27 dòng "Chế độ ăn": bếp chỉ xem danh mục, không xem bản gán (FR-063; chủ dự án chọn 2026-11-06). */
    private static final Set<Role> ASSIGNMENT_VIEWERS =
            Set.of(Role.DOCTOR, Role.NURSING_STAFF, Role.NURSING_HOME_MANAGER);

    private final DietAssignmentStore store;
    private final ResidentNutritionLock lock;
    private final NutritionCatalogStore catalog;
    private final ResidentApi residents;
    private final AllergenApi allergens;
    private final AuditLogApi audit;
    private final CurrentUser user;
    private final BusinessTime time;
    private final ApplicationEventPublisher events;
    private final ResidentMenuConflicts menuConflicts;
    private final ReviewNoticeService reviewNotices;

    public DietAssignmentService(
            DietAssignmentStore store,
            ResidentNutritionLock lock,
            NutritionCatalogStore catalog,
            ResidentApi residents,
            AllergenApi allergens,
            AuditLogApi audit,
            CurrentUser user,
            BusinessTime time,
            ApplicationEventPublisher events,
            ResidentMenuConflicts menuConflicts,
            ReviewNoticeService reviewNotices) {
        this.store = store;
        this.lock = lock;
        this.catalog = catalog;
        this.residents = residents;
        this.allergens = allergens;
        this.audit = audit;
        this.user = user;
        this.time = time;
        this.events = events;
        this.menuConflicts = menuConflicts;
        this.reviewNotices = reviewNotices;
    }

    /** Đề xuất vừa lập và có cần lý do chuyên môn khi Áp dụng (api-011 {@code MenuCheckResult}). */
    public record Proposal(long id, boolean professionalReasonRequired, ResidentMenuConflicts.Result menuCheck) {

        public List<String> warningCodes() {
            return menuCheck.warningCodes();
        }
    }

    /** Một dòng của danh sách: bản gán, hoặc chế độ ăn mặc định khi chưa có bản Hiệu lực ({@code assignment} rỗng). */
    public record AssignmentRow(Optional<DietAssignment> assignment, long dietId) {}

    public static Target target(long residentId, long id) {
        return new Target(ENTITY, Long.toString(id), residentId, null);
    }

    /** Hồ sơ người cao tuổi làm đối tượng kiểm quyền xem dị ứng (FR-022). */
    public static Target residentTarget(long residentId) {
        return new Target("M01.RESIDENT", Long.toString(residentId), residentId, null);
    }

    /**
     * Kết quả đối chiếu của một người với thực đơn chưa hết tuần theo chế độ ăn hiện hành (bản Hiệu lực, hoặc chế độ mặc
     * định không hạn chế riêng; FR-020, FR-022): nêu bữa, món, thành phần, nên chỉ người xem được dị ứng của người đó.
     */
    @RequiresPermission(function = Function.ALLERGY_CONDITION, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<ResidentMenuConflicts.Conflict> dishConflictCheck(Target target, long residentId) {
        Optional<DietAssignment> active = store.active(residentId);
        if (active.isPresent()) {
            return menuConflicts
                    .check(
                            residentId,
                            active.get().dietId(),
                            new HashSet<>(active.get().personalRestrictions()))
                    .conflicts();
        }
        return defaultDiet()
                .map(d -> menuConflicts.check(residentId, d.id(), Set.of()).conflicts())
                .orElseGet(List::of);
    }

    /** Đối tượng kiểm quyền của bản gán {@code id}, mang người cao tuổi của bản gán (mẫu {@code CarePlanService}). */
    @Transactional(readOnly = true)
    public Target targetOf(long id) {
        DietAssignment row = store.find(id).orElseThrow(NotFoundException::new);
        return target(row.residentId(), id);
    }

    /** Bản gán hiện hành và lịch sử (Bác sĩ, Điều dưỡng, Quản lý viện); chưa có bản Hiệu lực thì kèm chế độ mặc định. */
    @RequiresPermission(function = Function.DIET, operation = Operation.VIEW)
    @Transactional(readOnly = true)
    public List<AssignmentRow> list(Target target, long residentId) {
        if (user.roles().stream().noneMatch(ASSIGNMENT_VIEWERS::contains)) {
            throw new PermissionDeniedException(
                    "PERMISSION_DENIED", "Không có quyền xem bản gán chế độ ăn", Map.of("failedLayer", "ROLE"));
        }
        List<AssignmentRow> rows = new ArrayList<>();
        List<DietAssignment> all = store.ofResident(residentId);
        boolean hasActive = all.stream().anyMatch(a -> a.status() == DietAssignmentStatus.ACTIVE);
        if (!hasActive && !residents.isFinalStatus(residentId)) {
            defaultDiet().ifPresent(d -> rows.add(new AssignmentRow(Optional.empty(), d.id())));
        }
        all.forEach(a -> rows.add(new AssignmentRow(Optional.of(a), a.dietId())));
        return rows;
    }

    @RequiresPermission(function = Function.DIET, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public Proposal propose(Target target, long residentId, NewDietAssignment content) {
        NewDietAssignment input = validated(content);
        if (input.reviewNoticeId() != null) {
            reviewNotices.requireOfResident(input.reviewNoticeId(), residentId);
        }
        lock.lock(residentId);
        List<UnmetCondition> unmet = new ArrayList<>();
        if (residents.isFinalStatus(residentId)) {
            unmet.add(new UnmetCondition(
                    "RESIDENT_FINAL_STATUS", "Người cao tuổi đã Kết thúc lưu trú, Qua đời hoặc Hủy tiếp nhận"));
        }
        Diet diet = catalog.diet(input.dietId());
        if (!diet.active()) {
            unmet.add(new UnmetCondition("DIET_NOT_ACTIVE", "Chế độ ăn đã Ngừng hiệu lực"));
        }
        if (diet.attributes().safeDishId() == null) {
            unmet.add(new UnmetCondition(
                    "SAFE_DISH_MISSING", "Chế độ ăn chưa có món an toàn; Bác sĩ bổ sung trước khi gán (FR-001)"));
        }
        if (store.waiting(residentId).isPresent()) {
            unmet.add(new UnmetCondition(
                    "PENDING_ASSIGNMENT_EXISTS", "Đã có bản gán Đề xuất hoặc Chờ hiệu lực; hủy bản đó trước (FR-012)"));
        }
        if (!unmet.isEmpty()) {
            throw new ConditionNotMetException(unmet);
        }
        Instant now = time.now();
        long id = store.insert(residentId, input, user.accountId(), now);
        audit.write(AuditLogEntry.builder()
                .action(PROPOSED)
                .entity(ENTITY, id)
                .resident(residentId)
                .after(snapshot(input))
                .build());
        boolean required = ProfessionalReasonRule.required(currentSide(residentId), side(diet, input));
        return new Proposal(
                id,
                required,
                menuConflicts.check(residentId, input.dietId(), new HashSet<>(input.personalRestrictions())));
    }

    /** Áp dụng (FR-011, FR-013, FR-016, FR-017): lý do chuyên môn khi cần; Hiệu lực ngay hoặc Chờ hiệu lực. */
    @RequiresPermission(function = Function.DIET, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void apply(Target target, long id, String professionalReason, List<String> confirmedWarnings) {
        DietAssignment current = locked(id);
        if (current.status() != DietAssignmentStatus.PROPOSED) {
            throw stateChanged();
        }
        Diet diet = catalog.diet(current.dietId());
        boolean required = ProfessionalReasonRule.required(currentSide(current.residentId()), side(diet, current));
        String reason = professionalReason == null || professionalReason.isBlank() ? null : professionalReason.strip();
        if (required && reason == null) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "PROFESSIONAL_REASON_REQUIRED",
                    "Thay đổi thuộc FR-013 (a)–(d) cần lý do chuyên môn");
        }
        if (reason != null && reason.length() > 2000) {
            throw new InvalidValueException("Lý do chuyên môn tối đa 2000 ký tự");
        }
        ResidentMenuConflicts.Result check = menuConflicts.check(
                current.residentId(), current.dietId(), new HashSet<>(current.personalRestrictions()));
        List<String> warnings = check.warningCodes();
        if (confirmedWarnings == null || !confirmedWarnings.containsAll(warnings)) {
            // Liệt kê từng mã cảnh báo để bản Đề xuất mở lại sau vẫn xác nhận được (chủ dự án chọn 2026-11-08)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "WARNINGS_NOT_CONFIRMED",
                    "Chưa xác nhận đã xem các cảnh báo (FR-016)",
                    Map.of("unmetConditions", warningConditions(check)));
        }
        Instant now = time.now();
        boolean immediate = !current.requestedEffectiveFrom().isAfter(now);
        if (immediate) {
            discontinueActive(current.residentId(), now);
        }
        DietAssignmentStatus to = immediate ? DietAssignmentStatus.ACTIVE : DietAssignmentStatus.PENDING_EFFECTIVE;
        store.apply(id, to, immediate ? now : null, reason, warnings, user.accountId(), now);
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("status", to.name());
        after.put("professionalReason", reason);
        audit.write(AuditLogEntry.builder()
                .action(APPLIED)
                .entity(ENTITY, id)
                .resident(current.residentId())
                .before(Map.of("status", current.status().name()))
                .after(after)
                .build());
        if (immediate) {
            events.publishEvent(
                    new DietAssignmentTookEffect(current.residentId(), id, current.dietId(), current.texture(), now));
        }
    }

    /** Hủy bản Đề xuất hoặc Chờ hiệu lực (lý do); bản Hiệu lực hiện có giữ nguyên. */
    @RequiresPermission(function = Function.DIET, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(Target target, long id, Reason reason) {
        DietAssignment current = locked(id);
        if (!current.waiting()) {
            throw stateChanged();
        }
        store.cancel(id, reason.value(), user.accountId(), time.now());
        audit.write(AuditLogEntry.builder()
                .action(CANCELLED)
                .entity(ENTITY, id)
                .resident(current.residentId())
                .before(Map.of("status", current.status().name()))
                .after(Map.of("status", DietAssignmentStatus.CANCELLED.name()))
                .reason(reason)
                .build());
    }

    /** Id bản Chờ hiệu lực đã tới thời điểm hiệu lực (việc theo phút). */
    @Transactional(readOnly = true)
    public List<Long> duePending(Instant now) {
        return store.duePending(now).stream().map(DietAssignment::id).toList();
    }

    /**
     * Bộ lập lịch chuyển Chờ hiệu lực → Hiệu lực, bản cũ Ngừng trong cùng lệnh (FR-011); hiệu lực thực tế là thời điểm
     * mong muốn (FR-017). Chạy lại không đổi gì khi bản đã chuyển.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void activateIfDue(long id, Instant now) {
        DietAssignment current = locked(id);
        if (current.status() != DietAssignmentStatus.PENDING_EFFECTIVE
                || current.requestedEffectiveFrom().isAfter(now)) {
            return;
        }
        discontinueActive(current.residentId(), now);
        store.activate(id, current.requestedEffectiveFrom());
        audit.write(AuditLogEntry.builder()
                .action(ACTIVATED)
                .entity(ENTITY, id)
                .resident(current.residentId())
                .before(Map.of("status", current.status().name()))
                .after(Map.of("status", DietAssignmentStatus.ACTIVE.name()))
                .build());
        events.publishEvent(new DietAssignmentTookEffect(
                current.residentId(), id, current.dietId(), current.texture(), current.requestedEffectiveFrom()));
    }

    /**
     * Tác động trạng thái cuối trong lệnh của 001 (FR-015): bản Hiệu lực → Ngừng, Đề xuất/Chờ hiệu lực → Đã hủy với lý do
     * theo sự kiện nguồn; không thông báo (bảng FR-065), chỉ ghi nhật ký.
     */
    @Transactional
    public void onFinalStatus(long residentId, ResidentStatus to, Instant at) {
        lock.lock(residentId);
        discontinueActive(residentId, at);
        store.waiting(residentId).ifPresent(waiting -> {
            String reason = "Người cao tuổi chuyển " + finalStatusName(to) + " (FR-015)";
            store.cancel(waiting.id(), reason, null, at);
            audit.write(AuditLogEntry.builder()
                    .action(CANCELLED)
                    .entity(ENTITY, waiting.id())
                    .resident(residentId)
                    .before(Map.of("status", waiting.status().name()))
                    .after(Map.of("status", DietAssignmentStatus.CANCELLED.name()))
                    .reason(new Reason(reason))
                    .build());
        });
    }

    /** Hoàn tất tiếp nhận khi chưa có bản Hiệu lực: báo Bác sĩ mức Nhẹ (FR-014); không chặn lệnh của 001. */
    @Transactional
    public void onAdmissionCompleted(long residentId, Instant at) {
        if (store.active(residentId).isEmpty()) {
            events.publishEvent(new ResidentWithoutDietAssignment(residentId, at));
        }
    }

    private void discontinueActive(long residentId, Instant at) {
        store.active(residentId).ifPresent(active -> {
            store.discontinue(active.id(), at);
            audit.write(AuditLogEntry.builder()
                    .action(DISCONTINUED)
                    .entity(ENTITY, active.id())
                    .resident(residentId)
                    .before(Map.of("status", active.status().name()))
                    .after(Map.of("status", DietAssignmentStatus.DISCONTINUED.name()))
                    .build());
        });
    }

    private DietAssignment locked(long id) {
        DietAssignment row = store.find(id).orElseThrow(NotFoundException::new);
        lock.lock(row.residentId());
        return store.find(id).orElseThrow(NotFoundException::new);
    }

    /** Bản Hiệu lực, hoặc chế độ ăn mặc định với kết cấu thường, không hạn chế riêng (FR-013, FR-014). */
    private Side currentSide(long residentId) {
        Optional<DietAssignment> active = store.active(residentId);
        if (active.isPresent()) {
            return side(catalog.diet(active.get().dietId()), active.get());
        }
        return defaultDiet()
                .map(d -> new Side(d.id(), d.attributes().treatmentRelated(), Texture.REGULAR, Set.of()))
                .orElse(null);
    }

    private Optional<Diet> defaultDiet() {
        return catalog.diets().stream()
                .filter(d -> d.active() && d.attributes().isDefault())
                .findFirst();
    }

    private static Side side(Diet diet, DietAssignment assignment) {
        return new Side(
                diet.id(),
                diet.attributes().treatmentRelated(),
                assignment.texture(),
                new HashSet<>(assignment.personalRestrictions()));
    }

    private static Side side(Diet diet, NewDietAssignment input) {
        return new Side(
                diet.id(),
                diet.attributes().treatmentRelated(),
                input.texture(),
                new HashSet<>(input.personalRestrictions()));
    }

    private NewDietAssignment validated(NewDietAssignment input) {
        if (input.texture() == null || input.basis() == null || input.effectiveFrom() == null) {
            throw new InvalidValueException("Thiếu kết cấu, căn cứ hoặc thời điểm hiệu lực");
        }
        if (input.personalInstruction() != null && input.personalInstruction().length() > 2000) {
            throw new InvalidValueException("Ghi chú nhu cầu dinh dưỡng tối đa 2000 ký tự");
        }
        List<Long> restrictions = input.personalRestrictions() == null
                ? List.of()
                : input.personalRestrictions().stream().distinct().toList();
        for (Long allergen : restrictions) {
            if (allergen == null || allergens.allergen(allergen).isEmpty()) {
                throw new InvalidValueException("Hạn chế không có trong danh mục dị nguyên: " + allergen);
            }
        }
        return new NewDietAssignment(
                input.dietId(),
                input.texture(),
                restrictions,
                input.personalInstruction(),
                input.basis(),
                input.basisId(),
                input.reviewNoticeId(),
                input.effectiveFrom());
    }

    private static String finalStatusName(ResidentStatus to) {
        return switch (to) {
            case RESIDENCY_ENDED -> "Kết thúc lưu trú";
            case DECEASED -> "Qua đời";
            case ADMISSION_CANCELLED -> "Hủy tiếp nhận";
            default -> to.name();
        };
    }

    private static BusinessException stateChanged() {
        return new BusinessException(HttpStatus.CONFLICT, "STATE_CHANGED", "Bản gán đã đổi trạng thái");
    }

    /** Mỗi mã cảnh báo FR-016 một điều kiện, kèm mô tả món, ngày, bữa để Bác sĩ xem trước khi xác nhận. */
    private List<UnmetCondition> warningConditions(ResidentMenuConflicts.Result check) {
        java.time.format.DateTimeFormatter day = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<UnmetCondition> out = new ArrayList<>();
        check.conflicts().stream()
                .filter(c -> !c.hasSuitableSubstitute())
                .forEach(c -> out.add(new UnmetCondition(
                        c.code(),
                        "Món " + catalog.dish(c.dishId()).name() + ", "
                                + c.date().format(day) + ", "
                                + catalog.meal(c.mealId()).name() + ": " + conflictLabel(c.type())
                                + ", chưa có món thay thế phù hợp")));
        check.missingMeals()
                .forEach(g -> out.add(new UnmetCondition(
                        "NO_DISH:" + g.dietId() + ":" + g.date() + ":" + g.mealId(),
                        "Chế độ ăn chưa có món " + g.date().format(day) + ", "
                                + catalog.meal(g.mealId()).name())));
        return out.stream()
                .collect(java.util.stream.Collectors.toMap(
                        UnmetCondition::code, u -> u, (a, b) -> a, java.util.LinkedHashMap::new))
                .values()
                .stream()
                .toList();
    }

    private static String conflictLabel(com.eldercare.backend.nutrition.internal.conflict.domain.ConflictType type) {
        return switch (type) {
            case ALLERGY -> "có thành phần gây dị ứng";
            case PERSONAL_RESTRICTION -> "vi phạm hạn chế riêng";
            case DIET_RESTRICTION -> "vi phạm hạn chế của chế độ ăn";
        };
    }

    private static Map<String, Object> snapshot(NewDietAssignment input) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("dietId", input.dietId());
        out.put("texture", input.texture().name());
        out.put("personalRestrictions", input.personalRestrictions());
        out.put("personalInstruction", input.personalInstruction());
        out.put("basis", input.basis().name());
        out.put("effectiveFrom", input.effectiveFrom().toString());
        return out;
    }
}
