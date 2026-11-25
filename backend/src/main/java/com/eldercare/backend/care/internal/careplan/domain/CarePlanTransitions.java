package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.common.error.BusinessException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;

/**
 * FR-003 (sửa Q-447): bảng chuyển thuần; quyền, lý do và điều kiện nội dung do lệnh kiểm riêng. Ngày hiệu lực không
 * còn do người lập đề xuất mà đặt khi Duyệt theo {@link #effectiveFromOnApproval}.
 */
public final class CarePlanTransitions {
    private CarePlanTransitions() {}

    private static final Map<String, Map<CarePlanStatus, CarePlanStatus>> TRANSITIONS = Map.of(
            "SUBMIT", Map.of(CarePlanStatus.DRAFT, CarePlanStatus.PENDING_APPROVAL),
            "RETRACT", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.DRAFT),
            "APPROVE", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.PENDING_EFFECT),
            "RETURN", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.DRAFT),
            "CANCEL",
                    Map.of(
                            CarePlanStatus.DRAFT,
                            CarePlanStatus.CANCELLED,
                            CarePlanStatus.PENDING_APPROVAL,
                            CarePlanStatus.CANCELLED));

    public static CarePlanStatus next(CarePlanStatus status, String command) {
        var target = TRANSITIONS.getOrDefault(command, Map.of()).get(status);
        if (target == null)
            throw new BusinessException(HttpStatus.CONFLICT, "REVISION_LOCKED", "Trạng thái không cho phép lệnh này");
        return target;
    }

    /** Duyệt: chưa có kế hoạch Hiệu lực thì hiệu lực ngay, đã có thì chờ tới ngày mai (Q-447). */
    public static CarePlanStatus approvedStatus(boolean hasEffective) {
        return hasEffective ? CarePlanStatus.PENDING_EFFECT : CarePlanStatus.EFFECTIVE;
    }

    public static LocalDate effectiveFromOnApproval(boolean hasEffective, LocalDate today) {
        return hasEffective ? today.plusDays(1) : today;
    }
}
