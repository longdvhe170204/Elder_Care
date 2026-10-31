package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;

/** Đã hủy ({@code YeuCauDaHuy}). */
public record RequestCancelled(ApprovalRequestView request) {}
