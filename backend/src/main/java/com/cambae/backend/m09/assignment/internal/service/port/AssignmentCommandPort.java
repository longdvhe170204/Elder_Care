package com.cambae.backend.m09.assignment.internal.service.port;

import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m09.assignment.internal.domain.AssignmentInput;
import com.cambae.backend.m15.auditlog.api.Basis;
import java.util.List;

/** 008 N-07: MANDATORY, lệnh 015 tự kiểm quyền/yêu cầu; dùng cùng luật phân công 008. */
public interface AssignmentCommandPort {
    List<UnmetCondition> validate(long shiftId, AssignmentInput input);

    long create(long shiftId, AssignmentInput input, Basis basis);

    void cancel(long assignmentId, Basis basis);
}
