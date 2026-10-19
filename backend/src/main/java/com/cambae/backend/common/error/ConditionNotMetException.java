package com.cambae.backend.common.error;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Lệnh nghiệp vụ có điều kiện chưa thỏa: 409 kèm danh sách điều kiện (FR-006). */
public class ConditionNotMetException extends BusinessException {

    private final List<UnmetCondition> unmetConditions;

    public ConditionNotMetException(List<UnmetCondition> unmetConditions) {
        super(HttpStatus.CONFLICT, "CONDITION_NOT_MET", "Điều kiện chưa thỏa");
        this.unmetConditions = List.copyOf(unmetConditions);
    }

    public List<UnmetCondition> unmetConditions() {
        return unmetConditions;
    }
}
