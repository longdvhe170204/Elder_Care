package com.eldercare.backend.staffing.internal.swap.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Yêu cầu nghỉ đối với người xem; lý do chỉ có với người nghỉ, người lập, người duyệt (FR-045). */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LeaveView(
        long id,
        String status,
        LeaveDetail.LeaveKind kind,
        long staffId,
        String scopeType,
        Long scopeId,
        boolean onBehalf,
        String reason,
        List<LeaveDetail.Item> items,
        boolean selfApproved,
        List<String> allowedCommands) {}
