package com.eldercare.backend.staffing.assignment.internal.service.port;

import com.eldercare.backend.administration.auditlog.api.Basis;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.staffing.assignment.internal.domain.AssignmentInput;
import java.util.List;

/** 008 N-07: MANDATORY, lệnh 015 tự kiểm quyền/yêu cầu; dùng cùng luật phân công 008. */
public interface AssignmentCommandPort {
    List<UnmetCondition> validate(long shiftId, AssignmentInput input);

    long create(long shiftId, AssignmentInput input, Basis basis);

    void cancel(long assignmentId, Basis basis);
}
