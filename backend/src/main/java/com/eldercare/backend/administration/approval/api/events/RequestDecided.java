package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;

/**
 * Đã duyệt hoặc từ chối ({@code YeuCauDaQuyetDinh}); {@code outcome} là {@link ApprovalStatus#APPROVED} hoặc
 * {@link ApprovalStatus#REJECTED}. Thông báo cho người yêu cầu chờ 009 bổ sung nhóm người nhận theo tài khoản.
 */
public record RequestDecided(ApprovalRequestView request, String outcome) {}
