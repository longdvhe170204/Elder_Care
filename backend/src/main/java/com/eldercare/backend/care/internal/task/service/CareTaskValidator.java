package com.eldercare.backend.care.internal.task.service;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.*;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.careplan.domain.CarePlanItemSequence;
import com.eldercare.backend.care.internal.careplan.service.CarePlanQueries;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** T022/C-07: một đường kiểm dùng chung cho lập lô và phát sinh. Không ghi dữ liệu. */
@Service
public class CareTaskValidator {
    private final CareCatalogStore catalogs;
    private final ResidentApi residents;
    private final ObjectProvider<ContractApi> contracts;
    private final ObjectProvider<DayCareAttendanceApi> attendance;
    private final ObjectProvider<ShiftStaffPort> staffing;
    private final List<TaskSourcePort> sources;
    private final CarePlanQueries plans;
    private final CareTaskStore tasks;
    private final ParameterApi parameters;
    private final ResidentStatusApi statuses;

    public CareTaskValidator(
            CareCatalogStore catalogs,
            ResidentApi residents,
            ObjectProvider<ContractApi> contracts,
            ObjectProvider<DayCareAttendanceApi> attendance,
            ObjectProvider<ShiftStaffPort> staffing,
            List<TaskSourcePort> sources,
            CarePlanQueries plans,
            CareTaskStore tasks,
            ParameterApi parameters,
            ResidentStatusApi statuses) {
        this.catalogs = catalogs;
        this.residents = residents;
        this.contracts = contracts;
        this.attendance = attendance;
        this.staffing = staffing;
        this.sources = List.copyOf(sources);
        this.plans = plans;
        this.tasks = tasks;
        this.parameters = parameters;
        this.statuses = statuses;
    }

    public static boolean roleMatches(Role required, Role actual) {
        return required == actual || (required == Role.CAREGIVER && actual == Role.NURSING_STAFF);
    }

    public CareTaskValidation validate(
            CareTaskDraft d, ShiftStaffPort.ShiftView shift, Instant now, long actor, Long originalTaskId) {
        var errors = new LinkedHashSet<String>();
        // Làm lại chỉ đi qua lệnh đã kiểm kết quả Không đạt, không nhận trực tiếp từ lô.
        if (d.basisType() == SourceType.REWORK && originalTaskId == null) errors.add("INVALID_BASIS");
        var date = d.plannedAt().atZone(BusinessTime.ZONE).toLocalDate();
        var port = staffing.getIfAvailable();
        if (port == null)
            throw new BusinessException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "CARE_SOURCE_UNAVAILABLE",
                    "Chưa có nguồn ca/phân công chính thức");
        if (!shift.published()
                || d.plannedAt().isBefore(shift.startsAt())
                || !d.plannedAt().isBefore(shift.endsAt())) errors.add("OUTSIDE_SHIFT");
        var catalog = catalogs.require(CareCatalogKinds.TASK_TYPES, d.taskTypeId());
        var type = catalog.attributes();
        if (catalog.status() != CatalogStatus.ACTIVE) errors.add("INVALID_BASIS");
        if (type.group().equals("MEDICATION")) errors.add("MEDICATION_IS_NOT_A_TASK");
        boolean special = d.basisType() == SourceType.SHIFT_START_CHECK || d.basisType() == SourceType.SUPERVISION;
        if (d.residentId() == null && !special) errors.add("INVALID_BASIS");
        if (d.residentId() != null) checkResident(d, date, now, errors);

        var plan = d.basisType() == SourceType.CARE_PLAN_ITEM && d.residentId() != null
                ? plans.effectiveOn(d.residentId(), date)
                : Optional.<CarePlanView>empty();
        CarePlanItemView planItem = plan.flatMap(p -> p.items().stream()
                        .filter(i -> Objects.equals(i.id(), d.basisId()))
                        .findFirst())
                .orElse(null);
        Long planId = plan.isPresent() ? plan.get().id() : null;
        if (d.basisType() == SourceType.CARE_PLAN_ITEM) checkPlanItem(d, shift, planItem, errors);
        else if (special) checkShiftLevelBasis(d, shift, type, errors);
        else if (d.basisType() != SourceType.AD_HOC && d.basisType() != SourceType.REWORK) checkSourceBasis(d, errors);

        TimeWindow window = window(d, shift, type, planItem, special, errors);
        Role role = planItem == null ? type.defaultRole() : planItem.performerRole();
        checkPerformer(d, shift, port, type, role, date, errors);

        String content = planItem == null ? d.content() : planItem.content();
        var data = new CareTaskData(
                d.taskTypeId(),
                catalog.name(),
                content == null ? catalog.name() : content,
                planItem == null ? type.resultType() : planItem.resultType().name(),
                planItem == null ? type.billable() : planItem.billable(),
                type.serviceId(),
                type.certificates(),
                type.upperLimit(),
                type.vitalSignType(),
                true);
        var task = new CareTask(
                0,
                d.residentId(),
                d.locationType(),
                d.locationId(),
                catalog.id(),
                d.basisType(),
                d.basisId(),
                planId,
                originalTaskId,
                shift.id(),
                shift.floorAreaId(),
                d.plannedAt(),
                window.from(),
                window.to(),
                d.importance(),
                role,
                d.shared() ? null : d.performerId(),
                CareTaskTransitions.initialStatus(window.from(), window.to(), now),
                data,
                actor,
                now,
                0,
                false,
                null);
        if (tasks.duplicate(task)) errors.add(special ? "SHIFT_CHECK_ALREADY_EXISTS" : "DUPLICATE_BASIS");
        if (tasks.overlapsOldPlan(task)) errors.add("OLD_REVISION_WINDOW_OVERLAP");
        return new CareTaskValidation(task, List.copyOf(errors));
    }

    /** Khung giờ cho phép thực hiện công việc. */
    private record TimeWindow(Instant from, Instant to) {}

    /** Người cao tuổi phải đang lưu trú, không vắng lúc dự kiến; bán trú thì phải có mặt trong buổi. */
    private void checkResident(CareTaskDraft d, LocalDate date, Instant now, Set<String> errors) {
        var state = residents.status(d.residentId());
        if (Set.of(ResidentStatus.TEMPORARILY_ABSENT, ResidentStatus.OFF_SITE_ACTIVITY, ResidentStatus.HOSPITALIZED)
                .contains(state)) errors.add("RESIDENT_ABSENT");
        else if (state != ResidentStatus.IN_RESIDENCE) errors.add("RESIDENT_NOT_STAYING");
        if (d.plannedAt().isBefore(now)
                && !statuses.statusPeriods(
                                d.residentId(),
                                Set.of(
                                        ResidentStatus.TEMPORARILY_ABSENT,
                                        ResidentStatus.OFF_SITE_ACTIVITY,
                                        ResidentStatus.HOSPITALIZED),
                                d.plannedAt(),
                                d.plannedAt().plusNanos(1))
                        .isEmpty()) errors.add("RESIDENT_ABSENT");
        var contractSource = contracts.getIfAvailable();
        if (contractSource == null)
            throw new BusinessException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "CARE_SOURCE_UNAVAILABLE",
                    "Chưa có nguồn hợp đồng xác định loại lưu trú");
        var residence = contractSource.currentResidenceType(d.residentId(), date);
        if (residence.isEmpty()) errors.add("INVALID_BASIS");
        if (residence.filter(r -> r == ResidenceType.DAY_CARE).isPresent()) checkDayCarePresence(d, date, errors);
    }

    /** Người bán trú: giờ dự kiến phải nằm giữa lúc đến và giờ về dự kiến của buổi có mặt. */
    private void checkDayCarePresence(CareTaskDraft d, LocalDate date, Set<String> errors) {
        var provider = attendance.getIfAvailable();
        var presence = provider == null ? Optional.<AttendanceView>empty() : provider.onDate(d.residentId(), date);
        if (presence.isEmpty()
                || presence.get().status() != AttendanceStatus.PRESENT
                || presence.get().arrivedAt().isEmpty()
                || d.plannedAt().isBefore(presence.get().arrivedAt().get())
                || presence.get().currentExpectedReturn().isEmpty()
                || d.plannedAt()
                        .isAfter(date.atTime(
                                        presence.get().currentExpectedReturn().orElseThrow())
                                .atZone(BusinessTime.ZONE)
                                .toInstant())) errors.add("DAY_CARE_NOT_PRESENT");
    }

    /** Căn cứ là mục kế hoạch chăm sóc: công việc phải khớp mục đang hiệu lực (loại, mức, giờ, khung giờ). */
    private static void checkPlanItem(
            CareTaskDraft d, ShiftStaffPort.ShiftView shift, CarePlanItemView planItem, Set<String> errors) {
        if (planItem == null) {
            errors.add("PLAN_ITEM_MISMATCH");
            return;
        }
        Integer before = d.windowBeforeMinutes(), after = d.windowAfterMinutes();
        if (planItem.taskTypeId() != d.taskTypeId()
                || planItem.importance() != d.importance()
                || !CarePlanItemSequence.withinShift(planItem, shift.startsAt(), shift.endsAt())
                        .contains(d.plannedAt())) errors.add("PLAN_ITEM_MISMATCH");
        if ((before != null && before != planItem.windowBeforeMinutes())
                || (after != null && after != planItem.windowAfterMinutes())) errors.add("PLAN_ITEM_MISMATCH");
    }

    /** Căn cứ là kiểm tra đầu ca hoặc giám sát: việc của tầng, khu vực của ca, không gắn người cao tuổi. */
    private static void checkShiftLevelBasis(
            CareTaskDraft d, ShiftStaffPort.ShiftView shift, TaskTypeAttributes type, Set<String> errors) {
        if (d.residentId() != null
                || d.locationId() == null
                || d.locationId() != shift.floorAreaId()
                || !Set.of("FLOOR", "AREA").contains(d.locationType())
                || !type.group().equals(d.basisType().name())
                || type.defaultRole() != Role.NURSING_STAFF
                || d.importance() != Importance.valueOf(type.defaultImportance())
                || d.shared()) errors.add("INVALID_BASIS");
    }

    /** Căn cứ do module khác quản (lịch đo, theo dõi sau ngã…): hỏi đúng một nguồn quản loại căn cứ đó. */
    private void checkSourceBasis(CareTaskDraft d, Set<String> errors) {
        var supported = sources.stream()
                .filter(s -> s.managedTypes().contains(d.basisType()))
                .toList();
        if (supported.size() != 1 || d.basisId() == null || d.residentId() == null) {
            errors.add("INVALID_BASIS");
            return;
        }
        var result = supported.getFirst().validate(d.basisType(), d.basisId(), d.residentId(), d.plannedAt());
        if (!result.valid()
                || !result.residentMatches()
                || !result.timeMatches()
                || (d.basisType() == SourceType.MEASUREMENT
                        && result.prescribed()
                        && d.importance() != Importance.MANDATORY)) errors.add("INVALID_BASIS");
    }

    /**
     * Khung giờ: lấy theo mục kế hoạch, rồi bản nháp, rồi loại công việc, cuối cùng tham số CFG-M04-02; việc cấp ca và mục
     * "một lần mỗi ca" hoặc không có giờ thì khung giờ là cả ca.
     */
    private TimeWindow window(
            CareTaskDraft d,
            ShiftStaffPort.ShiftView shift,
            TaskTypeAttributes type,
            CarePlanItemView planItem,
            boolean special,
            Set<String> errors) {
        Integer before = d.windowBeforeMinutes(), after = d.windowAfterMinutes();
        if (planItem != null) {
            before = planItem.windowBeforeMinutes();
            after = planItem.windowAfterMinutes();
        }
        if (before == null) before = type.windowBeforeMinutes();
        if (after == null) after = type.windowAfterMinutes();
        if (before == null || after == null) {
            int configured = Math.toIntExact(
                    parameters.read(ParameterCode.CFG_M04_02).asDuration().toMinutes());
            if (before == null) before = configured;
            if (after == null) after = configured;
        }
        if (before < 0 || after < 0) errors.add("INVALID_BASIS");
        if (special
                || (planItem != null
                        && (planItem.frequencyType().equals("ONCE_PER_SHIFT")
                                || (planItem.startsAt().isEmpty()
                                        && planItem.times().isEmpty())))) {
            return new TimeWindow(shift.startsAt(), shift.endsAt());
        }
        return new TimeWindow(
                d.plannedAt().minusSeconds(Math.max(0, before) * 60L),
                d.plannedAt().plusSeconds(Math.max(0, after) * 60L));
    }

    /** Người thực hiện: việc chung thì không gán ai; việc riêng thì người được gán phải đúng vai trò, có ca, đủ chứng chỉ. */
    private static void checkPerformer(
            CareTaskDraft d,
            ShiftStaffPort.ShiftView shift,
            ShiftStaffPort port,
            TaskTypeAttributes type,
            Role role,
            LocalDate date,
            Set<String> errors) {
        List<ShiftStaffPort.CareAssignmentView> assigned = d.residentId() == null
                ? port.members(shift.id()).stream()
                        .map(m -> new ShiftStaffPort.CareAssignmentView(m.staffId(), m.role(), false))
                        .toList()
                : port.assignments(d.residentId(), shift.id());
        var matching = assigned.stream()
                .filter(a -> roleMatches(role, a.role()) && port.onShift(a.staffId(), d.plannedAt()))
                .toList();
        if (d.shared()) {
            if (d.performerId() != null || (d.basisType() != SourceType.REWORK && !matching.isEmpty()))
                errors.add("INVALID_PERFORMER");
            return;
        }
        if (d.performerId() == null || matching.stream().noneMatch(a -> Objects.equals(a.staffId(), d.performerId())))
            errors.add("INVALID_PERFORMER");
        if (d.performerId() != null && !port.qualified(d.performerId(), type.certificates(), date))
            errors.add("MISSING_CERTIFICATE");
    }
}
