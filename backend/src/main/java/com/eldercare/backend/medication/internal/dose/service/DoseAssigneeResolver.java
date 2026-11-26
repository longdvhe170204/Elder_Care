package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.medication.api.spi.ShiftNursePort;
import com.eldercare.backend.medication.internal.dose.domain.DoseAuthorityBasis;
import com.eldercare.backend.medication.internal.dose.persistence.TemporaryFloorNurseAssignmentRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Resolves and verifies the nurse responsible for a medication dose in the current shift (FR-026).
 */
@Component
public class DoseAssigneeResolver {

    private final ShiftNursePort shiftNursePort;
    private final ObjectProvider<TemporaryFloorNurseAssignmentRepository> temporaryAssignments;
    private final ObjectProvider<CurrentUser> currentUsers;

    public DoseAssigneeResolver(ShiftNursePort shiftNursePort) {
        this(shiftNursePort, null, null);
    }

    @Autowired
    public DoseAssigneeResolver(
            ShiftNursePort shiftNursePort,
            ObjectProvider<TemporaryFloorNurseAssignmentRepository> temporaryAssignments,
            ObjectProvider<CurrentUser> currentUsers) {
        this.shiftNursePort = shiftNursePort;
        this.temporaryAssignments = temporaryAssignments;
        this.currentUsers = currentUsers;
    }

    public record DoseAssignee(
            Long assignedNurseId,
            boolean floorShared,
            DoseAuthorityBasis authorityBasis,
            List<Long> eligibleNurseIds) {}

    public DoseAssignee resolve(Long residentId, Long floorAreaId, Instant scheduledTime, Instant now) {
        Optional<Long> assigned = residentId != null ? shiftNursePort.assignedNurse(residentId, now) : Optional.empty();

        if (assigned.isPresent()) {
            return new DoseAssignee(assigned.get(), false, DoseAuthorityBasis.ASSIGNED, List.of(assigned.get()));
        }

        return new DoseAssignee(null, false, DoseAuthorityBasis.ASSIGNED, List.of());
    }

    /**
     * Chỉ Điều dưỡng được phân công chăm sóc người cao tuổi trong ca hiện tại (hoặc Điều dưỡng phụ trách tạm khi tầng
     * trống Điều dưỡng) mới có quyền xác nhận liều.
     */
    public void requireAuthorizedNurse(Long residentId, Long floorAreaId, Long recorderAccountId, Instant now) {
        CurrentUser user = currentUsers != null ? currentUsers.getIfAvailable() : null;
        if (user != null && user.isScheduler()) {
            return;
        }
        long actorStaffId = user != null ? user.staffId().orElse(recorderAccountId) : recorderAccountId;
        Optional<Long> assigned = residentId != null ? shiftNursePort.assignedNurse(residentId, now) : Optional.empty();
        if (assigned.isPresent()) {
            if (!assigned.get().equals(actorStaffId)) {
                throw new BusinessException(
                        HttpStatus.FORBIDDEN,
                        "NURSE_NOT_ASSIGNED",
                        "Chỉ Điều dưỡng được phân công chăm sóc người cao tuổi trong ca hiện tại mới có quyền xác nhận liều");
            }
            return;
        }
        TemporaryFloorNurseAssignmentRepository repo =
                temporaryAssignments != null ? temporaryAssignments.getIfAvailable() : null;
        boolean tempAssigned = floorAreaId != null
                && repo != null
                && repo.findActive(floorAreaId, now).stream()
                        .anyMatch(a -> a.getNurseAccountId().equals(recorderAccountId));
        if (user != null && !tempAssigned) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "NURSE_NOT_ASSIGNED",
                    "Chỉ Điều dưỡng được phân công chăm sóc người cao tuổi trong ca hiện tại mới có quyền xác nhận liều");
        }
    }
}
