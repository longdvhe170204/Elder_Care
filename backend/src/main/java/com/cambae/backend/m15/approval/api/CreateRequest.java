package com.cambae.backend.m15.approval.api;

import com.cambae.backend.common.Reason;
import java.time.LocalDate;
import tools.jackson.databind.JsonNode;

/** Lệnh lập yêu cầu ở Nháp, có thể gửi duyệt ngay (FR-031, FR-032; hợp đồng {@code POST /approval-requests}). */
public record CreateRequest(
        String type, JsonNode content, LocalDate requestedEffectiveDate, Reason reason, boolean submitImmediately) {}
