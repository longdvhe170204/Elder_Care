package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.common.Reason;
import java.time.Instant;

/** Người duyệt trả yêu cầu Chờ duyệt về Nháp kèm lý do (000 FR-041a (1); Q-418). {@code request} là bản sau khi trả. */
public record RequestReturnedToDraft(ApprovalRequestView request, Reason reason, Instant at) {}
