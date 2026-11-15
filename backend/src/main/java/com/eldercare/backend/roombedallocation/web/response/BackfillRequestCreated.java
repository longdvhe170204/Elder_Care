package com.eldercare.backend.roombedallocation.web.response;

/** T036: phản hồi 202 khi nhập bù lùi quá CFG-M03-07 đã lập yêu cầu chờ Quản lý viện duyệt (FR-027 (b)). */
public record BackfillRequestCreated(long approvalRequestId, String status) {}
