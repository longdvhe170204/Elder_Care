package com.eldercare.backend.incident.web.request;

import com.eldercare.backend.incident.internal.alert.domain.AssistFloorChoice;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Tầng xảy ra của yêu cầu hỗ trợ, thân {@code POST /emergency-assist} (không bắt buộc) và {@code /emergency-assist/{id}/floor}
 * (đề xuất 2026-11-23): một tầng đang có ca hoặc khu vực chung, không chọn cả hai.
 */
@Schema(name = "AssistFloor")
public record AssistFloorRequest(
        @Schema(
                types = {"integer", "null"},
                description = "Tầng xảy ra, phải đang có ca")
        Long floorAreaId,

        @Schema(
                types = {"boolean", "null"},
                description = "Khu vực chung (sân, sảnh, cầu thang): báo Người phụ trách ca của mọi tầng đang có ca")
        Boolean commonArea) {
    public static AssistFloorChoice choiceOf(AssistFloorRequest request) {
        if (request == null) return AssistFloorChoice.NONE;
        return new AssistFloorChoice(request.floorAreaId(), Boolean.TRUE.equals(request.commonArea()));
    }
}
