package com.eldercare.backend.incident.internal.emergency.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.FailedLayer;
import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.error.PermissionDeniedException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Danh sách chọn người bàn giao cho lệnh Chuyển viện (FR-049; 004 bắt buộc {@code handoverStaffId}). Lấy nhân viên đang trong
 * ca ở tầng của người cao tuổi lúc này, không theo ca của người lập lệnh: Bác sĩ trực toàn viện không chung ca với tầng nên
 * danh sách nhân viên của 008 (chỉ người cùng ca với người xem) trả rỗng cho họ. Chỉ đọc, không đổi quy tắc xem hồ sơ nhân
 * viên của 008: chỉ trả mã, họ tên, chức vụ qua cổng {@link AlertStaffPort} của incident, module nhân sự cài ngược.
 */
@Service
public class HandoverStaffService {
    private static final Set<Role> ROLES = Set.of(Role.NURSING_STAFF, Role.DOCTOR);
    private static final String ENTITY = "M05.INCIDENT";

    private final PermissionApi permissions;
    private final ResidentLocationApi locations;
    private final Optional<AlertStaffPort> staff;
    private final BusinessTime time;

    public HandoverStaffService(
            PermissionApi permissions,
            ResidentLocationApi locations,
            @Autowired(required = false) AlertStaffPort staff,
            BusinessTime time) {
        this.permissions = permissions;
        this.locations = locations;
        this.staff = Optional.ofNullable(staff);
        this.time = time;
    }

    /** Một nhân viên có thể được chọn bàn giao. */
    public record Candidate(long id, String fullName, String position) {}

    @Transactional(readOnly = true)
    public List<Candidate> forResident(long residentId, CurrentUser user) {
        if (ROLES.stream().noneMatch(user.roles()::contains)) {
            throw new PermissionDeniedException("Chỉ Điều dưỡng, Bác sĩ chuyển viện từ sự cố");
        }
        var now = time.now();
        var location = locations.current(residentId);
        Long floor = location.map(l -> l.floorId() != null ? l.floorId() : l.areaId())
                .orElse(null);
        var permission = permissions.hasPermission(
                user,
                new PermissionKey(Function.INCIDENT_EMERGENCY, Operation.UPDATE),
                new Target(ENTITY, null, residentId, floor));
        if (!permission.allowed()) {
            if (permission.failedLayer().orElse(null) == FailedLayer.SCOPE) throw new NotFoundException();
            throw new PermissionDeniedException("Không có quyền xử lý sự cố");
        }
        if (floor == null) return List.of();
        return staff.map(port -> port.handoverCandidates(floor, now)).orElse(List.of()).stream()
                .map(view -> new Candidate(view.id(), view.fullName(), view.position()))
                .toList();
    }
}
