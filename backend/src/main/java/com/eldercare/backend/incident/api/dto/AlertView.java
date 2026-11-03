package com.eldercare.backend.incident.api.dto;

import java.time.Instant;

/** Nguồn đọc cảnh báo (mục 1.7). */
public record AlertView(
        long id, Long residentId, String type, String level, String status, Instant firstAt, Long floorAreaId) {}
