package com.eldercare.backend.administration.approval.api.events;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;

/** Đã áp dụng ({@code YeuCauDaApDung}, FR-037, FR-039). */
public record RequestApplied(ApprovalRequestView request, ResultRef result) {}
