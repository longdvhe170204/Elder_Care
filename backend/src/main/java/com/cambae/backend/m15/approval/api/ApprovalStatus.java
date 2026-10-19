package com.cambae.backend.m15.approval.api;

/**
 * Mã trạng thái chung của yêu cầu phê duyệt (data-model mục 3, glossary 6.3). Loại yêu cầu có thể khai báo thêm trạng
 * thái qua {@link RequestTypeHandler#extendedTransitions()}, nên trạng thái là chuỗi, không phải enum.
 */
public final class ApprovalStatus {

    public static final String DRAFT = "DRAFT";
    public static final String PENDING_APPROVAL = "PENDING_APPROVAL";
    public static final String APPROVED = "APPROVED";
    public static final String APPLIED = "APPLIED";
    public static final String APPLY_FAILED = "APPLY_FAILED";
    public static final String REJECTED = "REJECTED";
    public static final String CANCELLED = "CANCELLED";

    private ApprovalStatus() {}
}
