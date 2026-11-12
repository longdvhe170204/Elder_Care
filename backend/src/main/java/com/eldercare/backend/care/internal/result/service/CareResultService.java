package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.DutyApi;
import com.eldercare.backend.administration.auditlog.api.*;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.events.*;
import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.internal.catalog.domain.NonPerformanceReasonType;
import com.eldercare.backend.care.internal.catalog.persistence.NonPerformanceReasonStore;
import com.eldercare.backend.care.internal.result.domain.*;
import com.eldercare.backend.care.internal.result.persistence.CareResultStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.common.*;
import com.eldercare.backend.common.data.*;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** T036: cùng transaction khóa người/task, kiểm nguồn, đóng, audit và outbox. */
@Service
public class CareResultService {
    private final CareTaskStore tasks;
    private final CareResultStore results;
    private final CareLockStore locks;
    private final CareTaskService taskCommands;
    private final CurrentUser user;
    private final PermissionApi permissions;
    private final ResidentApi residents;
    private final NonPerformanceReasonStore reasons;
    private final CareResultValidation validation;
    private final ObjectProvider<VitalSignPort> vitalSigns;
    private final List<TaskSourcePort> sources;
    private final BusinessTime time;
    private final ParameterApi parameters;
    private final ApplicationEventPublisher events;
    private final ObjectProvider<DutyApi> duties;
    private final AuditLogApi audit;
    private final com.eldercare.backend.care.internal.rules.service.CareResultRules rules;

    public CareResultService(
            CareTaskStore tasks,
            CareResultStore results,
            CareLockStore locks,
            CareTaskService taskCommands,
            CurrentUser user,
            PermissionApi permissions,
            ResidentApi residents,
            NonPerformanceReasonStore reasons,
            CareResultValidation validation,
            ObjectProvider<VitalSignPort> vitalSigns,
            List<TaskSourcePort> sources,
            BusinessTime time,
            ParameterApi parameters,
            ApplicationEventPublisher events,
            ObjectProvider<DutyApi> duties,
            AuditLogApi audit,
            com.eldercare.backend.care.internal.rules.service.CareResultRules rules) {
        this.tasks = tasks;
        this.results = results;
        this.locks = locks;
        this.taskCommands = taskCommands;
        this.user = user;
        this.permissions = permissions;
        this.residents = residents;
        this.reasons = reasons;
        this.validation = validation;
        this.vitalSigns = vitalSigns;
        this.sources = List.copyOf(sources);
        this.time = time;
        this.parameters = parameters;
        this.events = events;
        this.duties = duties;
        this.audit = audit;
        this.rules = rules;
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.CONFIRM)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean record(Target target, long id, CareResultInput input) {
        return write(id, input, null, false);
    }

    /**
     * 005 T037 (C-14, BR-M04-12, FR-035): ghi nhận đồng bộ sau mất kết nối; quyền tại thời điểm thiết bị đã được 002 kiểm
     * và kiểm lại ở đây theo thời điểm thiết bị; thời điểm thiết bị là "thời điểm ghi" cho nhãn ghi muộn.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public boolean recordOffline(long id, CareResultInput input, Instant deviceAt, boolean permissionLost) {
        return write(id, input, deviceAt, permissionLost);
    }

    private CareTask lock(long id) {
        var before = tasks.get(id, false);
        if (before.residentId() != null) locks.lock(List.of(before.residentId()));
        return tasks.get(id, true);
    }

    private void open(CareTask task, Object rejected) {
        if (Set.of(TaskStatus.DUE, TaskStatus.OVERDUE).contains(task.status())) return;
        var properties = new LinkedHashMap<String, Object>();
        properties.put("currentStatus", task.status());
        properties.put("rejectedContent", rejected);
        var closure = tasks.closure(task.id());
        if (closure.at() != null) properties.put("closedAt", closure.at());
        Long actor = results.closedBy(task.id());
        if (actor != null) properties.put("closedBy", actor.toString());
        throw new BusinessException(
                HttpStatus.CONFLICT, "CARE_TASK_CLOSED", "Công việc đã đóng hoặc chưa đến hạn", properties);
    }

    private String basis(CareTask task, Instant deviceAt) {
        var key = new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM);
        var permission = deviceAt == null
                ? permissions.hasPermission(user, key, CareTaskService.target(task))
                : permissions.hasPermissionAt(user, key, CareTaskService.target(task), deviceAt);
        if (!permission.allowed()) throw new PermissionDeniedException("Không có quyền ghi nhận công việc này");
        if (user.staffId().isPresent()
                && Objects.equals(task.performerId(), user.staffId().get())) return "ASSIGNED";
        if (user.roles().contains(Role.FLOOR_MANAGER)) return "FLOOR_MANAGER_ROLE";
        var dutySource = duties.getIfAvailable();
        if (task.status() == TaskStatus.OVERDUE
                && user.staffId().isPresent()
                && dutySource != null
                && dutySource
                        .shiftLead(task.floorAreaId(), deviceAt == null ? time.now() : deviceAt)
                        .equals(user.staffId())) return "SHIFT_LEAD_DUTY";
        throw new PermissionDeniedException(
                "Chỉ người được giao, Trưởng tầng hoặc Người phụ trách ca xử lý việc Quá hạn được ghi");
    }

    public boolean canRecord(CareTask task) {
        if (!Set.of(TaskStatus.DUE, TaskStatus.OVERDUE).contains(task.status()) || task.awaitingRemeasurement())
            return false;
        try {
            basis(task, null);
            return true;
        } catch (PermissionDeniedException denied) {
            return false;
        }
    }

    private boolean write(long id, CareResultInput input, Instant deviceAt, boolean permissionLost) {
        var task = lock(id);
        open(task, input);
        var now = time.now();
        var effectiveAt = deviceAt == null ? now : deviceAt;
        String basis = basis(task, deviceAt);
        requireBillableQuantity(task);
        if (task.data().resultType().equals("ML")
                && task.data().upperLimit() == null
                && !Boolean.TRUE.equals(task.data().resultConfigurationCaptured()))
            throw unavailable("Snapshot công việc cũ chưa có cấu hình giới hạn kết quả");
        if (results.pending(id).isPresent())
            throw new BusinessException(HttpStatus.CONFLICT, "AWAITING_REMEASUREMENT", "Đang chờ xác nhận đo lại");
        var performed = input.performedAt() == null ? effectiveAt : input.performedAt();
        // CHK056: Ghi nhận phát sinh trỏ công việc gốc bị đóng vì trạng thái cuối, thực hiện trước lúc đó (005 T037)
        boolean beforeFinal = task.originalTaskId() != null
                && task.residentId() != null
                && residents
                        .finalStatusAt(task.residentId())
                        .filter(performed::isBefore)
                        .isPresent();
        if (task.residentId() != null && residents.isFinalStatus(task.residentId()) && !beforeFinal)
            throw new BusinessException(HttpStatus.CONFLICT, "RESIDENT_NOT_STAYING", "Hồ sơ đã ở trạng thái cuối");
        if (performed.isAfter(effectiveAt) || (task.windowFrom() != null && performed.isBefore(task.windowFrom())))
            throw invalid("Thời điểm thực hiện ngoài khoảng được ghi");
        validation.validate(task.data(), input);
        var next = task.basisType() == TaskSourcePort.SourceType.AD_HOC_RECORDING
                ? TaskStatus.COMPLETED
                : CareResultTiming.completedStatus(task.windowFrom(), task.windowTo(), performed);
        if (next == TaskStatus.COMPLETED_LATE && task.importance() != Importance.NORMAL && input.reason() == null)
            throw new ReasonRequiredException();
        if (task.data().resultType().equals("EATING") && task.basisType() == TaskSourcePort.SourceType.MEAL) {
            var source = sources.stream()
                    .filter(s -> s.managedTypes().contains(TaskSourcePort.SourceType.MEAL))
                    .findFirst()
                    .orElseThrow(() -> unavailable("Chưa có nguồn xác nhận phục vụ bữa"));
            if (!source.confirmServing(task.basisId(), task.residentId()))
                throw new BusinessException(
                        HttpStatus.CONFLICT, "SERVICE_NOT_CONFIRMED", "Bữa chưa được xác nhận phục vụ");
        }
        boolean late = Duration.between(performed, effectiveAt)
                        .compareTo(parameters.read(ParameterCode.CFG_M04_06).asDuration())
                > 0;
        Long vitalId = null;
        if (task.data().resultType().equals("MEASURED_VALUE")) {
            if (task.residentId() == null || task.data().vitalSignType() == null)
                throw unavailable("Công việc chưa có snapshot loại chỉ số");
            var source = vitalSigns.getIfAvailable();
            if (source == null) throw unavailable("Chưa có nguồn ghi chỉ số");
            var outcome = source.record(
                    task.residentId(),
                    task.data().vitalSignType(),
                    input.numericValue(),
                    performed,
                    user.staffId().orElseThrow(),
                    task.id(),
                    Optional.ofNullable(deviceAt));
            if (outcome.status() == VitalSignPort.RecordingStatus.BLOCKED)
                throw new BusinessException(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                        "VALUE_BLOCKED",
                        outcome.reason().orElse("Giá trị chỉ số bị chặn"));
            if (outcome.status() == VitalSignPort.RecordingStatus.AWAITING_REMEASUREMENT) {
                results.await(
                        task,
                        new PendingCareResult(
                                input,
                                performed,
                                now,
                                deviceAt,
                                user.accountId(),
                                user.staffId().orElseThrow(),
                                basis,
                                late,
                                permissionLost));
                taskCommands.record(task, tasks.get(id, false), "AWAITING_REMEASUREMENT", input.reason(), false);
                return true;
            }
            vitalId = outcome.vitalSignRecordId().orElseThrow(() -> unavailable("Nguồn chỉ số không trả id bản ghi"));
        }
        results.complete(
                task, input, next, performed, now, deviceAt, user.accountId(), basis, late, permissionLost, vitalId);
        taskCommands.record(
                task.basisType() == TaskSourcePort.SourceType.AD_HOC_RECORDING ? null : task,
                tasks.get(id, false),
                "RECORDED",
                input.reason(),
                false);
        auditResult(task, input, performed, now, deviceAt, basis, late, permissionLost);
        recordedEvents(task, input, performed, next, now);
        return false;
    }

    private void auditResult(
            CareTask task,
            CareResultInput input,
            Instant performed,
            Instant recorded,
            Instant deviceAt,
            String basis,
            boolean late,
            boolean permissionLost) {
        var after = new LinkedHashMap<String, Object>();
        after.put("values", input.values());
        after.put("resultType", task.data().resultType());
        after.put("performedAt", performed);
        after.put("recordedAt", recorded);
        after.put("permissionBasis", basis);
        after.put("lateRecording", late);
        after.put("permissionLost", permissionLost);
        if (deviceAt != null) after.put("deviceAt", deviceAt);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_RESULT_RECORDED", false))
                .entity("M04.CARE_RESULT", task.id())
                .resident(task.residentId())
                .before(Map.of())
                .after(after)
                .deviceTime(deviceAt)
                .build());
    }

    /**
     * Callback 007 trong transaction nguồn; không ghi lại giá trị đo đầu thành giá trị đã xác nhận. Chủ dự án chốt
     * 2026-11-12: lần đo lại khác người hoặc khác giờ thì người thực hiện, thời điểm thực hiện, trạng thái đóng và nhãn ghi
     * muộn theo lần đo đầu; kết quả trỏ bản ghi đã xác nhận của 007.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void confirmed(long id, long vitalSignRecordId, Instant measuredAt, long measuredById) {
        var task = lock(id);
        var existing = results.find(id);
        if (existing.isPresent() && Objects.equals(existing.get().vitalSignRecordId(), vitalSignRecordId)) return;
        requireBillableQuantity(task);
        open(task, Map.of("vitalSignRecordId", vitalSignRecordId));
        var pending = results.pending(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "AWAITING_REMEASUREMENT", "Không có lần đo đang chờ xác nhận"));
        var performed = pending.performedAt();
        var input = new CareResultInput(
                performed,
                null,
                null,
                Map.of(),
                pending.input().upperLimitConfirmed(),
                pending.input().note(),
                pending.input().reason());
        var next = task.basisType() == TaskSourcePort.SourceType.AD_HOC_RECORDING
                ? TaskStatus.COMPLETED
                : CareResultTiming.completedStatus(task.windowFrom(), task.windowTo(), performed);
        results.complete(
                task,
                input,
                next,
                performed,
                time.now(),
                pending.deviceAt(),
                pending.accountId(),
                pending.permissionBasis(),
                pending.late(),
                pending.permissionLost(),
                vitalSignRecordId);
        taskCommands.record(task, tasks.get(id, false), "MEASUREMENT_CONFIRMED", input.reason(), false);
        auditResult(
                task,
                input,
                performed,
                time.now(),
                pending.deviceAt(),
                pending.permissionBasis(),
                pending.late(),
                pending.permissionLost());
        recordedEvents(task, input, performed, next, time.now());
    }

    /** FR-051: chỉ loại kết quả có lượng xác định ({@link CareBillableQuantity}); có dịch vụ và người cao tuổi. */
    private void requireBillableQuantity(CareTask task) {
        if (!task.data().billable()) return;
        if (!CareBillableQuantity.supported(task.data().resultType()))
            throw unavailable(
                    "Chưa xác định được đơn vị tính phí của loại kết quả này; không quy đổi ml, mức hay giá trị đo");
        if (task.data().serviceId() == null || task.residentId() == null)
            throw unavailable("Công việc có tính phí cần dịch vụ và người cao tuổi");
    }

    private void recordedEvents(CareTask task, CareResultInput input, Instant performed, TaskStatus next, Instant now) {
        rules.onResult(task.id());
        if (task.data().billable())
            events.publishEvent(new BillableCareResultRecorded(
                    task.id(),
                    task.residentId(),
                    task.data().publicTaskTypeId(),
                    task.data().serviceId(),
                    CareBillableQuantity.of(task.data().resultType(), input.numericValue())
                            .orElseThrow(),
                    performed,
                    new CareEventContext(
                            "M04.CARE_RESULT",
                            task.id(),
                            "BILLABLE_RECORDED",
                            now,
                            Optional.of(task.floorAreaId()),
                            List.of())));
        var context = new CareEventContext(
                "M04.TASK", task.id(), "RESULT_RECORDED", now, Optional.of(task.floorAreaId()), List.of());
        events.publishEvent(new AlertClosureRequested(
                task.id(), next == TaskStatus.COMPLETED ? "PERFORMED_IN_WINDOW" : "TASK_COMPLETED_LATE", context));
        if (task.residentId() != null && task.data().resultType().equals("SLEEP"))
            events.publishEvent(new SleepResultRecorded(
                    task.id(),
                    task.residentId(),
                    performed.atZone(BusinessTime.ZONE).toLocalDate(),
                    validation.catalogGroup("SLEEP_QUALITY", input.codeValue()),
                    context));
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.CONFIRM)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void notPerformed(Target target, long id, Long reasonTypeId, String note) {
        notPerformed(id, reasonTypeId, note, null);
    }

    /** 005 T037 (C-14, FR-028a): Không thực hiện đồng bộ sau mất kết nối; loại lý do xét tại thời điểm thiết bị. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void notPerformedOffline(long id, Long reasonTypeId, String note, Instant deviceAt) {
        notPerformed(id, reasonTypeId, note, deviceAt);
    }

    /**
     * FR-028a, C-14: loại lý do Hiệu lực, hoặc (ngoại tuyến) đã ngừng sau thời điểm thiết bị; lần ngừng hiệu lực đọc từ
     * nhật ký {@code NON_PERFORMANCE_REASON_DEACTIVATED} do danh mục 005 ghi (người dùng chọn 2026-11-06).
     */
    private boolean activeAt(NonPerformanceReasonType reason, Instant deviceAt) {
        if (reason.status() == CatalogStatus.ACTIVE) return true;
        if (deviceAt == null) return false;
        return audit
                .recorded(
                        new AuditAction("NON_PERFORMANCE_REASON_DEACTIVATED", true),
                        "M04.NON_PERFORMANCE_REASON_TYPE",
                        Long.toString(reason.id()),
                        null)
                .stream()
                .map(r -> r.occurredAt())
                .max(Comparator.naturalOrder())
                .filter(deactivatedAt -> deactivatedAt.isAfter(deviceAt))
                .isPresent();
    }

    private void notPerformed(long id, Long reasonTypeId, String note, Instant deviceAt) {
        var task = lock(id);
        open(task, Map.of("reasonTypeId", reasonTypeId == null ? 0 : reasonTypeId));
        basis(task, deviceAt);
        if (results.pending(id).isPresent())
            throw new BusinessException(HttpStatus.CONFLICT, "AWAITING_REMEASUREMENT", "Đang chờ xác nhận đo lại");
        if (reasonTypeId == null) throw invalid("Phải chọn loại lý do Không thực hiện");
        var reason = reasons.list().stream()
                .filter(r -> r.id() == reasonTypeId && activeAt(r, deviceAt))
                .findFirst()
                .orElseThrow(() -> invalid("Loại lý do không còn hiệu lực"));
        if (note != null && note.length() > 2000) throw invalid("Ghi chú tối đa 2000 ký tự");
        results.notPerformed(task, reason.catalogId(), note, time.now(), user.accountId());
        taskCommands.record(
                task,
                tasks.get(id, false),
                "NOT_PERFORMED",
                note == null || note.isBlank() ? null : Reason.of(note),
                false);
        var details = new LinkedHashMap<String, Object>();
        details.put("nonPerformanceReasonTypeId", reason.id());
        if (note != null) details.put("nonPerformanceNote", note);
        audit.write(AuditLogEntry.builder()
                .action(new AuditAction("CARE_TASK_NON_PERFORMANCE_DETAILS", false))
                .entity("M04.TASK", id)
                .resident(task.residentId())
                .before(Map.of())
                .after(details)
                .build());
        events.publishEvent(new AlertClosureRequested(
                id,
                "TASK_NOT_PERFORMED",
                new CareEventContext(
                        "M04.TASK", id, "NOT_PERFORMED", time.now(), Optional.of(task.floorAreaId()), List.of())));
    }

    private BusinessException invalid(String message) {
        return new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_VALUE", message);
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", message);
    }
}
