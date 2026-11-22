package com.eldercare.backend.care.internal.careplan.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.internal.careplan.domain.*;
import com.eldercare.backend.care.internal.careplan.persistence.CarePlanStore;
import com.eldercare.backend.care.internal.careplan.persistence.CareRiskStore;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.*;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.web.VersionEtag;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.*;
import java.util.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** FR-001–005: lệnh ghi khóa theo người, kiểm điều kiện, audit cùng transaction. */
@Service
public class CarePlanService {
    private final CarePlanStore store;
    private final CareLockStore locks;
    private final CareCatalogStore catalogs;
    private final ParameterApi parameters;
    private final ResidentApi residents;
    private final CurrentUser user;
    private final AuditLogApi audit;
    private final ApplicationEventPublisher events;
    private final BusinessTime time;
    private final CareRiskStore risks;
    private final AccountApi accounts;

    public CarePlanService(
            CarePlanStore store,
            CareLockStore locks,
            CareCatalogStore catalogs,
            ParameterApi parameters,
            ResidentApi residents,
            CurrentUser user,
            AuditLogApi audit,
            ApplicationEventPublisher events,
            BusinessTime time,
            CareRiskStore risks,
            AccountApi accounts) {
        this.store = store;
        this.locks = locks;
        this.catalogs = catalogs;
        this.parameters = parameters;
        this.residents = residents;
        this.user = user;
        this.audit = audit;
        this.events = events;
        this.time = time;
        this.risks = risks;
        this.accounts = accounts;
    }

    public Target target(long id) {
        var row = store.require(id);
        return residentTarget(row.residentId(), id);
    }

    public static Target residentTarget(long residentId, long id) {
        return new Target("M04.CARE_PLAN", Long.toString(id), residentId, null);
    }

    private CarePlan locked(long id) {
        var row = store.require(id);
        locks.lock(List.of(row.residentId()));
        return store.locked(id);
    }

    private void writable(long residentId) {
        residents.status(residentId);
        if (residents.isFinalStatus(residentId)) throw conflict("RESIDENT_FINAL_STATUS", "Hồ sơ đã ở trạng thái cuối");
    }

    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public CarePlan create(Target target, long residentId, boolean copyCurrent) {
        locks.lock(List.of(residentId));
        writable(residentId);
        var revisions = store.list(residentId, true);
        if (revisions.stream()
                .anyMatch(p -> Set.of(CarePlanStatus.DRAFT, CarePlanStatus.PENDING_EFFECT)
                        .contains(p.status()))) throw conflict("OPEN_REVISION_EXISTS", "Đã có phiên bản đang mở");
        var previous = revisions.stream()
                .filter(p -> p.status() == CarePlanStatus.EFFECTIVE)
                .findFirst();
        var plan = store.create(
                residentId,
                revisions.stream().mapToInt(CarePlan::revision).max().orElse(0) + 1,
                previous.map(CarePlan::id).orElse(null),
                user.accountId(),
                time.now());
        if (copyCurrent && previous.isPresent()) {
            store.saveItems(
                    plan.id(),
                    store.lockedItems(previous.get().id()).stream()
                            .map(item -> new CarePlanItem(
                                    0, plan.id(), item.lineage(), item.physicalTaskTypeId(), item.data()))
                            .toList());
            risks.copy(previous.get().id(), plan.id());
        }
        log("CREATED", null, plan, null);
        return plan;
    }

    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void replaceItems(Target target, long id, String etag, List<CarePlanItem> requested) {
        var plan = locked(id);
        writable(plan.residentId());
        draft(plan);
        VersionEtag.requireMatch(etag, plan.version(), Map.of("id", id, "version", plan.version()));
        if (requested.stream()
                        .filter(item -> item.data().waterTargetMl() != null)
                        .count()
                > 1) throw new InvalidValueException("Mỗi phiên bản chỉ có một mục đặt mục tiêu lượng nước mỗi ngày");
        var existing = new HashMap<Long, CarePlanItem>();
        var oldItems = store.lockedItems(id);
        oldItems.forEach(item -> existing.put(item.id(), item));
        var seen = new HashSet<Long>();
        var items = new ArrayList<CarePlanItem>();
        for (var item : requested) {
            if (item.id() != 0 && (!existing.containsKey(item.id()) || !seen.add(item.id())))
                throw new InvalidValueException("Mục không thuộc bản Nháp hoặc bị lặp");
            var data = item.data();
            validate(data);
            var type = taskType(data.taskTypeId());
            var before = existing.get(item.id());
            if (item.sourceItemId() != null
                    && (before == null || !Objects.equals(item.sourceItemId(), before.sourceItemId())))
                throw new InvalidValueException("Không được tự chọn gốc sao chép");
            items.add(new CarePlanItem(item.id(), id, before == null ? null : before.sourceItemId(), type.id(), data));
        }
        store.saveItems(id, items);
        store.touch(id);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_PLAN_ITEMS_REPLACED", false))
                .entity("M04.CARE_PLAN", id)
                .before(Map.of("status", plan.status(), "version", plan.version(), "items", oldItems))
                .after(Map.of("status", plan.status(), "version", plan.version() + 1, "items", store.lockedItems(id)))
                .build());
    }

    private com.eldercare.backend.administration.shared.api.dto.CatalogItem<TaskTypeAttributes> taskType(long id) {
        var type = catalogs.require(CareCatalogKinds.TASK_TYPES, id);
        if (type.status() != CatalogStatus.ACTIVE
                || "MEDICATION".equals(type.attributes().group()))
            throw new InvalidValueException("Loại công việc phải hiệu lực và không thuộc nhóm uống thuốc");
        return type;
    }

    private void validate(CarePlanItemData item) {
        if (item.content() == null
                || item.content().isBlank()
                || item.content().length() > 1000
                || item.performerRole() == null
                || item.resultType() == null
                || item.importance() == null
                || item.frequencyType() == null
                || !Set.of("TIMES_PER_DAY", "EVERY_N_HOURS", "DAYS_OF_WEEK", "ONCE_PER_SHIFT")
                        .contains(item.frequencyType()))
            throw new InvalidValueException("Mục thiếu trường bắt buộc hoặc tần suất không hợp lệ");
        if ("EVERY_N_HOURS".equals(item.frequencyType())
                && (item.intervalHours() == null
                        || item.intervalHours() < 1
                        || item.intervalHours() > 23
                        || item.startsAt() == null))
            throw new InvalidValueException("Mỗi N giờ cần N từ 1 đến 23 và giờ bắt đầu");
        if ("TIMES_PER_DAY".equals(item.frequencyType()) && (item.timesPerDay() == null || item.timesPerDay() < 1))
            throw new InvalidValueException("Số lần trong ngày phải dương");
        if ("DAYS_OF_WEEK".equals(item.frequencyType()) && item.weekdays().isEmpty())
            throw new InvalidValueException("Cần chọn ngày trong tuần");
        if (item.weekdays().stream().anyMatch(day -> day < 1 || day > 7)
                || new HashSet<>(item.weekdays()).size() != item.weekdays().size()
                || new HashSet<>(item.times()).size() != item.times().size())
            throw new InvalidValueException("Ngày hoặc giờ bị lặp/không hợp lệ");
        if (item.windowBeforeMinutes() != null
                        && (item.windowBeforeMinutes() < 0 || item.windowBeforeMinutes() > Short.MAX_VALUE)
                || item.windowAfterMinutes() != null
                        && (item.windowAfterMinutes() < 0 || item.windowAfterMinutes() > Short.MAX_VALUE))
            throw new InvalidValueException("Khung phút không hợp lệ");
        var type = taskType(item.taskTypeId());
        if (item.waterTargetMl() != null
                && !"DRINKING_ASSISTANCE".equals(type.attributes().group()))
            throw new InvalidValueException("Mục tiêu ml chỉ dùng cho hỗ trợ uống nước");
    }

    private void draft(CarePlan plan) {
        if (plan.status() != CarePlanStatus.DRAFT) throw conflict("REVISION_LOCKED", "Chỉ sửa bản Nháp");
    }

    /**
     * Q-414: Bác sĩ ban hành bản Nháp (Nháp → Chờ hiệu lực), không qua duyệt; ngày hiệu lực sớm nhất là ngày hôm sau.
     * Phiên bản được phép không có mục nào (người khỏe bình thường chỉ có lịch sinh hoạt chung).
     */
    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.UPDATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void issue(Target target, long id, LocalDate effectiveFrom, Reason reason) {
        var plan = locked(id);
        writable(plan.residentId());
        var status = CarePlanTransitions.next(plan.status(), "ISSUE", effectiveFrom, time.today());
        var items = store.lockedItems(id);
        if (plan.previousPlanId() != null && reason == null) throw new ReasonRequiredException();
        var frozen = new ArrayList<CarePlanItem>();
        for (var item : items) {
            validate(item.data());
            var type = taskType(item.data().taskTypeId()).attributes();
            Integer before = item.data().windowBeforeMinutes(),
                    after = item.data().windowAfterMinutes();
            if (before == null) before = type.windowBeforeMinutes();
            if (after == null) after = type.windowAfterMinutes();
            if (before == null || after == null) {
                int configured = Math.toIntExact(
                        parameters.read(ParameterCode.CFG_M04_02).asDuration().toMinutes());
                if (before == null) before = configured;
                if (after == null) after = configured;
            }
            frozen.add(new CarePlanItem(
                    item.id(),
                    id,
                    item.sourceItemId(),
                    item.physicalTaskTypeId(),
                    item.data().windows(before, after)));
        }
        store.saveItems(id, frozen);
        // Người ban hành ghi vào cột người duyệt cũ (approved_by, approved_at) để giữ ràng buộc ngày hiệu lực của bảng.
        store.transition(
                plan,
                status,
                effectiveFrom,
                reason == null ? null : reason.value(),
                user.accountId(),
                time.now(),
                false,
                time.now(),
                null);
        log("ISSUED", plan, store.locked(id), reason);
    }

    /** Q-414: Bác sĩ hủy bản Nháp (bắt buộc lý do). */
    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancel(Target target, long id, Reason reason) {
        change(id, "CANCEL", reason);
    }

    /** Q-414: Bác sĩ thu hồi bản Chờ hiệu lực trước ngày hiệu lực (bắt buộc lý do). */
    @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.UPDATE)
    @BusinessCommand
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void withdraw(Target target, long id, Reason reason) {
        change(id, "WITHDRAW", reason);
    }

    private void change(long id, String command, Reason reason) {
        changeLocked(locked(id), command, reason);
    }

    private void changeLocked(CarePlan plan, String command, Reason reason) {
        if (reason == null) throw new ReasonRequiredException();
        writable(plan.residentId());
        var next = CarePlanTransitions.next(plan.status(), command, plan.effectiveFrom(), time.today());
        store.transition(
                plan,
                next,
                plan.effectiveFrom(),
                plan.changeReason(),
                command.equals("WITHDRAW") ? plan.approvedBy() : null,
                command.equals("WITHDRAW") ? plan.approvedAt() : null,
                command.equals("WITHDRAW") && plan.selfApproved(),
                plan.submittedAt(),
                command.equals("WITHDRAW") ? "WITHDRAWN" : "CANCELLED");
        var after = store.locked(plan.id());
        log(command, plan, after, reason);
        // Báo người lập khi Bác sĩ khác thu hồi bản đã ban hành
        if (command.equals("WITHDRAW") && plan.createdBy() != user.accountId()) {
            long creatorStaff = accounts.byId(plan.createdBy())
                    .map(account -> account.staffId())
                    .orElseThrow(() -> new BusinessException(
                            HttpStatus.SERVICE_UNAVAILABLE,
                            "CARE_SOURCE_UNAVAILABLE",
                            "Chưa tra cứu được người lập kế hoạch"));
            var context = context(after, command, List.of(new RecipientGroup.Staff(creatorStaff, "CARE_PLAN_CREATOR")));
            events.publishEvent(new CarePlanApprovalWithdrawn(plan.id(), plan.residentId(), context));
        }
    }

    private CareEventContext context(CarePlan plan, String action, List<RecipientGroup> recipients) {
        return new CareEventContext(
                "M04.CARE_PLAN", plan.id(), action + ":" + plan.version(), time.now(), Optional.empty(), recipients);
    }

    private void log(String action, CarePlan before, CarePlan after, Reason reason) {
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_PLAN_" + action, reason != null))
                .entity("M04.CARE_PLAN", after.id())
                .before(before == null ? Map.of() : Map.of("status", before.status(), "version", before.version()))
                .after(Map.of(
                        "status", after.status(), "version", after.version(), "selfApproved", after.selfApproved()))
                .reason(reason)
                .build());
    }

    private BusinessException conflict(String code, String message) {
        return new BusinessException(HttpStatus.CONFLICT, code, message);
    }
}
