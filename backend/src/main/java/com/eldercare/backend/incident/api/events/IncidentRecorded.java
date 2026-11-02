package com.eldercare.backend.incident.api.events;

/**
 * Sự cố đã được ghi nhận để module hồ sơ quyết định có báo đánh giá lại hay không
 * (007 FR-043; hợp đồng module 001 mục 2).
 */
public record IncidentRecorded(
        long incidentId, long residentId, String type, String level, boolean needsReassessment) {}
