package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;

/** Đã lập yêu cầu ở Nháp ({@code YeuCauDaLap}). */
public record RequestCreated(ApprovalRequestView request) {}
