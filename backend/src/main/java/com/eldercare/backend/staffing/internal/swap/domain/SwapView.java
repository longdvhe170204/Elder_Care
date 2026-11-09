package com.eldercare.backend.staffing.internal.swap.domain;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * Yêu cầu đổi ca đối với người xem (FR-022, FR-045): lý do chỉ có với người nhường, người nhận, người duyệt và Quản lý
 * viện; lý do từ chối của người nhận chỉ có với người nhường.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SwapView(
        long id,
        String status,
        SwapKind kind,
        long giverStaffId,
        long recipientStaffId,
        long shiftXId,
        Long shiftYId,
        String reason,
        String recipientDeclineReason,
        boolean selfApproved,
        List<String> allowedCommands) {}
