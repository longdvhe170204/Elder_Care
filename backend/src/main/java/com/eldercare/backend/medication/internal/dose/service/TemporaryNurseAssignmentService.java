package com.eldercare.backend.medication.internal.dose.service;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.medication.api.dto.NursingShiftView;
import com.eldercare.backend.medication.api.spi.NursingShiftPort;
import com.eldercare.backend.medication.internal.dose.domain.TemporaryFloorNurseAssignment;
import com.eldercare.backend.medication.internal.dose.persistence.TemporaryFloorNurseAssignmentRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Quản lý viện chỉ định Điều dưỡng tầng khác phụ trách tạm khi tầng không có Điều dưỡng nào có ca (BR-M07-31, Q-436). Người
 * được chỉ định xác nhận được liều của tầng đó như Điều dưỡng có ca tại tầng; quyền hết khi hết ca của tầng.
 */
@Service
public class TemporaryNurseAssignmentService {

    private final TemporaryFloorNurseAssignmentRepository assignments;
    private final NursingShiftPort shifts;

    public TemporaryNurseAssignmentService(
            TemporaryFloorNurseAssignmentRepository assignments, NursingShiftPort shifts) {
        this.assignments = assignments;
        this.shifts = shifts;
    }

    @Transactional
    public TemporaryFloorNurseAssignment assign(long floorAreaId, long nurseAccountId, long assignedById, Instant now) {
        if (!shifts.nursesWithShift(floorAreaId, now).isEmpty()) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "FLOOR_HAS_NURSE",
                    "Tầng đang có Điều dưỡng có ca, không cần chỉ định phụ trách tạm (BR-M07-31)");
        }
        NursingShiftView shift = shifts.shiftAt(floorAreaId, now)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.CONFLICT, "NO_SHIFT", "Tầng không có ca đang diễn ra (BR-M07-31)"));
        if (!shifts.inProgressShiftMember(nurseAccountId, now)) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "NURSE_NOT_ON_SHIFT",
                    "Điều dưỡng được chỉ định phải đang có ca ở tầng khác (BR-M07-31)");
        }
        for (TemporaryFloorNurseAssignment existing : assignments.findActive(floorAreaId, now)) {
            if (existing.getNurseAccountId().equals(nurseAccountId)) {
                return existing;
            }
        }
        return assignments.save(new TemporaryFloorNurseAssignment(
                floorAreaId, shift.id(), nurseAccountId, assignedById, now, shift.endsAt()));
    }

    @Transactional(readOnly = true)
    public List<TemporaryFloorNurseAssignment> active(long floorAreaId, Instant now) {
        return assignments.findActive(floorAreaId, now);
    }

    @Transactional(readOnly = true)
    public boolean hasActive(long floorAreaId, Instant now) {
        return !assignments.findActive(floorAreaId, now).isEmpty();
    }
}
