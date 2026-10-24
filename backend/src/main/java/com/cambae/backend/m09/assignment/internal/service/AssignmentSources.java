package com.cambae.backend.m09.assignment.internal.service;

import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m03.api.*;
import com.cambae.backend.m03.api.dto.*;
import com.cambae.backend.m04.api.*;
import com.cambae.backend.m09.assignment.internal.domain.StaffAssignment;
import com.cambae.backend.m09.shift.internal.domain.WorkShift;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/** Chỉ API của nguồn; thiếu nguồn thì từ chối, không tạo vị trí hoặc yêu cầu chứng chỉ giả. */
@Service
public class AssignmentSources {
    private final ObjectProvider<ResidentApi> residents;
    private final ObjectProvider<LocationApi> locations;
    private final ObjectProvider<LocationStructureApi> structures;
    private final ObjectProvider<LocationHistoryApi> history;
    private final ObjectProvider<TaskTypeApi> types;
    private final ObjectProvider<CarePlanApi> plans;
    private final ObjectProvider<ShiftTaskApi> tasks;

    public AssignmentSources(
            ObjectProvider<ResidentApi> residents,
            ObjectProvider<LocationApi> locations,
            ObjectProvider<LocationStructureApi> structures,
            ObjectProvider<LocationHistoryApi> history,
            ObjectProvider<TaskTypeApi> types,
            ObjectProvider<CarePlanApi> plans,
            ObjectProvider<ShiftTaskApi> tasks) {
        this.residents = residents;
        this.locations = locations;
        this.structures = structures;
        this.history = history;
        this.types = types;
        this.plans = plans;
        this.tasks = tasks;
    }

    public static <T> T required(ObjectProvider<T> source) {
        var api = source.getIfAvailable();
        if (api == null)
            throw new BusinessException(
                    HttpStatus.UNPROCESSABLE_ENTITY,
                    "ASSIGNMENT_SOURCE_UNAVAILABLE",
                    "Chưa có nguồn dữ liệu cần cho phân công");
        return api;
    }

    public LocationStructureApi structure() {
        return required(structures);
    }

    public boolean notificationSourcesAvailable() {
        return residents.getIfAvailable() != null
                && locations.getIfAvailable() != null
                && history.getIfAvailable() != null;
    }

    public Optional<LocationView> position(long resident, Instant at) {
        return required(locations).at(resident, at);
    }

    public boolean terminal(long resident) {
        return required(residents).isFinalStatus(resident);
    }

    public com.cambae.backend.m04.api.dto.TaskRequirements requirements(long id) {
        return required(types).requirements(id);
    }

    public Set<Long> taskTypes(long resident) {
        return required(plans).currentTaskTypes(resident);
    }

    public void changed(long resident, long shift) {
        required(tasks).onAssignmentChanged(resident, shift);
    }

    public void ended(long assignment, long shift) {
        required(tasks).onAssignmentEnded(assignment, shift);
    }

    public Set<Long> floors(String type, long target, Instant at) {
        var s = structure();
        return switch (type) {
            case "FLOOR" -> s.floor(target).map(f -> Set.of(f.id())).orElse(Set.of());
            case "AREA" -> s.floorsInArea(target);
            case "ROOM" -> s.room(target).map(r -> Set.of(r.floorAreaId())).orElse(Set.of());
            case "RESIDENT" ->
                position(target, at)
                        .map(p -> p.floorAreaId() == null ? Set.<Long>of() : Set.of(p.floorAreaId()))
                        .orElse(Set.of());
            default -> Set.of();
        };
    }

    public Set<Long> residents(String type, long target, Instant at) {
        if ("RESIDENT".equals(type)) return Set.of(target);
        return required(history).atLocation("FACILITY".equals(type) ? null : new LocationRef(type, target), at).stream()
                .map(LocationStay::residentId)
                .collect(java.util.stream.Collectors.toSet());
    }

    public List<LocationStay> stays(long resident, Instant from, Instant to) {
        return required(history).byResident(resident, from, to);
    }

    public boolean matches(StaffAssignment a, long resident, Instant at) {
        if ("RESIDENT".equals(a.targetType())) return a.targetId() == resident;
        return position(resident, at)
                .map(p -> matches(a.targetType(), a.targetId(), p))
                .orElse(false);
    }

    public static boolean matches(String type, long id, LocationView p) {
        return switch (type) {
            case "ROOM" -> p.roomId() == id;
            case "FLOOR" -> Objects.equals(p.floorAreaId(), id);
            case "AREA" -> Objects.equals(p.areaId(), id);
            default -> false;
        };
    }

    public void validateTarget(StaffAssignment a, WorkShift shift) {
        String type = a.targetType();
        long target = a.targetId();
        Instant at = a.startsAt();
        if (!Set.of("FLOOR", "AREA", "ROOM", "RESIDENT").contains(type))
            throw invalid("Loại đối tượng phân công không hợp lệ");
        if ("RESIDENT".equals(type)) {
            if (terminal(target)) throw invalid("Người cao tuổi đã ở trạng thái cuối");
            if (position(target, at).isEmpty()) throw invalid("Không xác định được vị trí người cao tuổi");
        } else if (!structure().active(new LocationRef(type, target)))
            throw invalid("Vị trí đã ngừng hiệu lực hoặc không tồn tại");
        if ("FACILITY".equals(shift.scopeType())) return;
        if ("FLOOR".equals(shift.scopeType())) {
            if ("AREA".equals(type) || !floors(type, target, at).equals(Set.of(shift.scopeId())))
                throw invalid("Đối tượng nằm ngoài phạm vi ca");
        } else {
            Long area = switch (type) {
                case "AREA" -> target;
                case "FLOOR" -> structure().floor(target).map(FloorView::areaId).orElse(null);
                case "ROOM" ->
                    structure()
                            .room(target)
                            .flatMap(r -> structure().floor(r.floorAreaId()))
                            .map(FloorView::areaId)
                            .orElse(null);
                case "RESIDENT" ->
                    position(target, at).map(LocationView::areaId).orElse(null);
                default -> null;
            };
            if (!Objects.equals(area, shift.scopeId())) throw invalid("Đối tượng nằm ngoài phạm vi ca");
        }
    }

    private static BusinessException invalid(String detail) {
        return new BusinessException(HttpStatus.CONFLICT, "INVALID_ASSIGNMENT_TARGET", detail);
    }
}
