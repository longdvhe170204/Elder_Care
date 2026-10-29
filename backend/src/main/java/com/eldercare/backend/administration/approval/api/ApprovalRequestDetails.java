package com.eldercare.backend.administration.approval.api;

import java.util.List;

/** Yêu cầu kèm lệnh người xem được phép (FR-042), cho chi tiết và phản hồi 202 của module khác (cách C, 000 FR-026). */
public record ApprovalRequestDetails(ApprovalRequestView request, List<String> allowedCommands) {}
