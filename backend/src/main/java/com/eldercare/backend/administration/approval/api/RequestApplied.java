package com.eldercare.backend.administration.approval.api;

/** Đã áp dụng ({@code YeuCauDaApDung}, FR-037, FR-039). */
public record RequestApplied(ApprovalRequestView request, ResultRef result) {}
