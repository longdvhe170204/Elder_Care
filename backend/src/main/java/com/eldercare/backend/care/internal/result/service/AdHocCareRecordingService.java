package com.eldercare.backend.care.internal.result.service;

import com.eldercare.backend.administration.account.api.*;
import com.eldercare.backend.administration.account.api.dto.*;
import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.result.domain.*;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.*;
import com.eldercare.backend.common.data.*;
import com.eldercare.backend.common.error.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.ContractApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.time.Instant;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

/** FR-025: chỉ lệnh người dùng tạo task và kết quả trong một transaction. */
@Service
public class AdHocCareRecordingService {
    private final CareTaskStore tasks;
    private final CareLockStore locks;
    private final CareCatalogStore catalogs;
    private final CareTaskService taskCommands;
    private final CareResultService results;
    private final CurrentUser user;
    private final BusinessTime time;
    private final BedAllocationApi beds;
    private final ResidentLocationApi locations;
    private final ResidentApi residents;
    private final ResidentStatusApi statuses;
    private final ObjectProvider<ContractApi> contracts;
    private final ObjectProvider<DayCareAttendanceApi> attendance;

    public AdHocCareRecordingService(
            CareTaskStore tasks,
            CareLockStore locks,
            CareCatalogStore catalogs,
            CareTaskService taskCommands,
            CareResultService results,
            CurrentUser user,
            BusinessTime time,
            BedAllocationApi beds,
            ResidentLocationApi locations,
            ResidentApi residents,
            ResidentStatusApi statuses,
            ObjectProvider<ContractApi> contracts,
            ObjectProvider<DayCareAttendanceApi> attendance) {
        this.tasks = tasks;
        this.locks = locks;
        this.catalogs = catalogs;
        this.taskCommands = taskCommands;
        this.results = results;
        this.user = user;
        this.time = time;
        this.beds = beds;
        this.locations = locations;
        this.residents = residents;
        this.statuses = statuses;
        this.contracts = contracts;
        this.attendance = attendance;
    }

    public Target target(long resident, Instant performedAt) {
        var at = performedAt == null ? time.now() : performedAt;
        Long floor = beds.openAt(resident, at)
                .map(b -> b.floorAreaId())
                .orElseGet(() -> locations
                        .at(resident, at)
                        .map(l -> l.floorId() == null ? l.areaId() : l.floorId())
                        .orElse(null));
        return new Target("M01.RESIDENT", Long.toString(resident), resident, floor);
    }

    @RequiresPermission(function = Function.TASK_CHECKLIST, operation = Operation.CREATE)
    @BusinessCommand(requiresReason = false)
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AdHocCareRecording record(
            Target target, long resident, long taskType, CareResultInput input, boolean reviewed, Long originalTaskId) {
        if (originalTaskId != null)
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INVALID_BASIS",
                    "Liên kết công việc gốc chỉ dùng cho bản ghi đồng bộ của công việc đóng vì trạng thái cuối");
        return record(resident, taskType, input, reviewed, null, null, false);
    }

    /**
     * 005 T037 (C-14, FR-035, CHK056): Ghi nhận phát sinh đồng bộ sau mất kết nối; thời điểm thiết bị là "thời điểm ghi".
     * {@code originalTaskId} chỉ cho công việc bị hệ thống đóng vì người cao tuổi chuyển trạng thái cuối: thời điểm thực
     * hiện phải trước lúc chuyển trạng thái cuối, khi đó người cao tuổi còn Đang lưu trú.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AdHocCareRecording recordOffline(
            long resident,
            long taskType,
            CareResultInput input,
            boolean reviewed,
            Long originalTaskId,
            Instant deviceAt,
            boolean permissionLost) {
        return record(resident, taskType, input, reviewed, originalTaskId, deviceAt, permissionLost);
    }

    private AdHocCareRecording record(
            long resident,
            long taskType,
            CareResultInput input,
            boolean reviewed,
            Long originalTaskId,
            Instant deviceAt,
            boolean permissionLost) {
        locks.lock(List.of(resident));
        var recordedAt = deviceAt == null ? time.now() : deviceAt;
        var performed = input.performedAt() == null ? recordedAt : input.performedAt();
        if (performed.isAfter(recordedAt)) throw new InvalidValueException("Không thực hiện sau thời điểm ghi");
        boolean residing;
        if (originalTaskId != null) {
            var original = tasks.get(originalTaskId, false);
            var finalAt = residents.finalStatusAt(resident);
            if (!Objects.equals(original.residentId(), resident)
                    || !"FR_010_FINAL_STATE"
                            .equals(tasks.closure(originalTaskId).rule())
                    || finalAt.isEmpty())
                throw new BusinessException(
                        HttpStatus.CONFLICT, "INVALID_BASIS", "Công việc gốc không bị đóng vì trạng thái cuối");
            if (!performed.isBefore(finalAt.get()))
                throw new BusinessException(
                        HttpStatus.CONFLICT,
                        "AFTER_FINAL_STATUS",
                        "Thời điểm thực hiện sau lúc người cao tuổi chuyển trạng thái cuối (CHK056)");
            residing = true;
        } else residing = residents.status(resident) == ResidentStatus.IN_RESIDENCE;
        if (!residing
                || !statuses.statusPeriods(
                                resident,
                                Set.of(
                                        ResidentStatus.TEMPORARILY_ABSENT,
                                        ResidentStatus.HOSPITALIZED,
                                        ResidentStatus.OFF_SITE_ACTIVITY),
                                performed,
                                performed.plusNanos(1))
                        .isEmpty())
            throw new BusinessException(
                    HttpStatus.CONFLICT, "RESIDENT_ABSENT", "Thời điểm thực hiện không nằm trong khoảng có mặt");
        var contract = contracts.getIfAvailable();
        if (contract == null) throw unavailable("Chưa có nguồn loại lưu trú");
        var residence = contract.currentResidenceType(
                        resident, performed.atZone(BusinessTime.ZONE).toLocalDate())
                .orElseThrow(() -> new InvalidValueException("Không có loại lưu trú tại thời điểm thực hiện"));
        if (residence == ResidenceType.DAY_CARE) {
            var source = attendance.getIfAvailable();
            var day = source == null
                    ? Optional.<AttendanceView>empty()
                    : source.onDate(
                            resident, performed.atZone(BusinessTime.ZONE).toLocalDate());
            if (day.isEmpty()
                    || day.get().arrivedAt().isEmpty()
                    || performed.isBefore(day.get().arrivedAt().get())
                    || day.get()
                            .leftAt()
                            .filter(left -> !performed.isBefore(left))
                            .isPresent())
                throw new BusinessException(
                        HttpStatus.CONFLICT, "DAY_CARE_NOT_PRESENT", "Thực hiện ngoài khoảng điểm danh đến/về");
        }
        var catalog = catalogs.require(CareCatalogKinds.TASK_TYPES, taskType);
        var type = catalog.attributes();
        if (catalog.status() != CatalogStatus.ACTIVE || type.group().equals("MEDICATION"))
            throw new InvalidValueException("Loại công việc không được ghi phát sinh");
        if (type.resultType().equals("MEASURED_VALUE"))
            throw unavailable("Nhánh đo chỉ số phát sinh chờ thống nhất D/007 theo quyết định chủ dự án");
        long actor = user.staffId().orElseThrow(() -> new PermissionDeniedException("Chỉ nhân viên được ghi nhận"));
        if (user.roles().stream().noneMatch(role -> CareTaskValidator.roleMatches(type.defaultRole(), role)))
            throw new PermissionDeniedException("Vai trò không khớp loại công việc");
        var actualTarget = target(resident, performed);
        if (actualTarget.floorAreaId() == null)
            throw unavailable("Chưa xác định được tầng/khu tại thời điểm thực hiện");
        var staff = taskCommands.staff();
        var shift = staff.shiftContaining(actualTarget.floorAreaId(), performed)
                .filter(s -> s.published())
                .orElseThrow(() -> new PermissionDeniedException("Không có ca đã công bố tại thời điểm thực hiện"));
        if (!staff.onShift(actor, performed)
                || staff.members(shift.id()).stream().noneMatch(m -> m.staffId() == actor))
            throw new PermissionDeniedException("Người ghi không có tên trong ca");
        if (!staff.qualified(
                actor, type.certificates(), performed.atZone(BusinessTime.ZONE).toLocalDate()))
            throw new PermissionDeniedException("Chưa đủ chứng chỉ của loại công việc");
        var same = tasks.inShift(shift.id()).stream()
                .filter(t -> Objects.equals(t.residentId(), resident)
                        && t.taskTypeId() == catalog.id()
                        && Set.of(TaskStatus.DUE, TaskStatus.OVERDUE).contains(t.status()))
                .toList();
        if (!reviewed && !same.isEmpty()) return new AdHocCareRecording(null, same);
        var data = new CareTaskData(
                taskType,
                catalog.name(),
                catalog.name(),
                type.resultType(),
                type.billable(),
                type.serviceId(),
                type.certificates(),
                type.upperLimit(),
                type.vitalSignType(),
                true);
        var draft = new CareTask(
                0,
                resident,
                null,
                null,
                catalog.id(),
                SourceType.AD_HOC_RECORDING,
                null,
                null,
                originalTaskId,
                shift.id(),
                shift.floorAreaId(),
                performed,
                null,
                null,
                Importance.valueOf(type.defaultImportance()),
                type.defaultRole(),
                actor,
                TaskStatus.DUE,
                data,
                user.accountId(),
                time.now(),
                0,
                false,
                null);
        var saved = tasks.insert(draft);
        // DUE chỉ tồn tại trong transaction chưa commit; history/outbox chỉ ghi trạng thái Hoàn thành.
        if (deviceAt == null) results.record(CareTaskService.target(saved), saved.id(), input);
        else results.recordOffline(saved.id(), input, deviceAt, permissionLost);
        return new AdHocCareRecording(saved.id(), List.of());
    }

    private BusinessException unavailable(String message) {
        return new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "CARE_SOURCE_UNAVAILABLE", message);
    }
}
