package com.eldercare.backend.resident.api.dto;

import java.util.Set;

/** Miễn điều kiện bằng yêu cầu phê duyệt đã duyệt (chỉ 004). */
public record ConditionWaiver(long approvalRequestId, Set<String> conditionCodes) {}
