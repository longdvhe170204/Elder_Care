package com.eldercare.backend.care.internal.careplan.domain;

import com.eldercare.backend.care.api.dto.CarePlanStatus;
import com.eldercare.backend.common.error.BusinessException;
import java.time.LocalDate;
import java.util.Map;
import org.springframework.http.HttpStatus;

/** FR-003: bảng chuyển thuần; quyền, lý do và điều kiện nội dung do lệnh kiểm riêng. */
public final class CarePlanTransitions {
    private CarePlanTransitions() {}

    private static final Map<String, Map<CarePlanStatus, CarePlanStatus>> TRANSITIONS = Map.of(
            "SUBMIT", Map.of(CarePlanStatus.DRAFT, CarePlanStatus.PENDING_APPROVAL),
            "APPROVE", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.PENDING_EFFECT),
            "RETURN", Map.of(CarePlanStatus.PENDING_APPROVAL, CarePlanStatus.DRAFT),
            "CANCEL",
                    Map.of(
                            CarePlanStatus.DRAFT,
                            CarePlanStatus.CANCELLED,
                            CarePlanStatus.PENDING_APPROVAL,
                            CarePlanStatus.CANCELLED),
            "WITHDRAW", Map.of(CarePlanStatus.PENDING_EFFECT, CarePlanStatus.CANCELLED));

    public static CarePlanStatus next(CarePlanStatus status, String command, LocalDate effectiveFrom, LocalDate today) {
        var target = TRANSITIONS.getOrDefault(command, Map.of()).get(status);
        if (target == null)
            throw new BusinessException(HttpStatus.CONFLICT, "REVISION_LOCKED", "Trạng thái không cho phép lệnh này");
        if ((command.equals("SUBMIT") || command.equals("APPROVE") || command.equals("WITHDRAW"))
                && (effectiveFrom == null || !effectiveFrom.isAfter(today)))
            throw new BusinessException(
                    HttpStatus.CONFLICT, "INVALID_EFFECTIVE_DATE", "Ngày hiệu lực phải sau ngày thực hiện lệnh");
        return target;
    }
}
