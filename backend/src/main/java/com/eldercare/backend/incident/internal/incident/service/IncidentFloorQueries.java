package com.eldercare.backend.incident.internal.incident.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.incident.domain.IncidentFloor;
import com.eldercare.backend.roombedallocation.api.LocationStructureApi;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tầng để báo sự cố cơ sở vật chất (BR-M05-17) và gửi yêu cầu hỗ trợ (BR-M05-29): tầng của ca người xem đang trực và các
 * tầng đang có ca, vì người nhận là Người phụ trách ca của tầng đó. Chỉ mã và tên tầng, không cần quyền "Cơ cấu phòng,
 * giường" của 003.
 */
@Service
public class IncidentFloorQueries {
    private static final Set<PermissionKey> USERS = Set.of(
            new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.CREATE),
            new PermissionKey(Function.EMERGENCY_ASSIST_BUTTON, Operation.CREATE));

    private final PermissionApi permissions;
    private final BusinessTime time;
    private final Optional<AlertStaffPort> staff;
    private final Optional<LocationStructureApi> structure;

    public IncidentFloorQueries(
            PermissionApi permissions,
            BusinessTime time,
            @Autowired(required = false) AlertStaffPort staff,
            @Autowired(required = false) LocationStructureApi structure) {
        this.permissions = permissions;
        this.time = time;
        this.staff = Optional.ofNullable(staff);
        this.structure = Optional.ofNullable(structure);
    }

    @Transactional(readOnly = true)
    public List<IncidentFloor> floors(CurrentUser user) {
        if (permissions.permissions(user).stream().noneMatch(USERS::contains)) {
            throw new PermissionDeniedException("Không có quyền ghi nhận sự cố hoặc gửi yêu cầu hỗ trợ");
        }
        if (staff.isEmpty() || structure.isEmpty()) return List.of();
        var now = time.now();
        Optional<Long> own = user.staffId().flatMap(id -> staff.get().floorDuringShift(id, now));
        var ids = new LinkedHashSet<Long>();
        own.ifPresent(ids::add);
        ids.addAll(staff.get().floorsWithShift(now));
        return ids.stream()
                .flatMap(id -> structure.get().floor(id).stream())
                .filter(floor -> floor.active())
                .map(floor -> new IncidentFloor(
                        floor.id(),
                        floor.name(),
                        floor.buildingName(),
                        own.filter(id -> id == floor.id()).isPresent()))
                .sorted(Comparator.comparing(IncidentFloor::current).reversed().thenComparing(IncidentFloor::name))
                .toList();
    }
}
