package com.eldercare.backend.care.api.dto;

import java.time.LocalDate;

/** 014 ActivityStatisticsApi.isolationRisks (FR-058): chuỗi ngày không tham gia hoạt động nhóm. */
public record IsolationRiskView(long residentId, LocalDate since, int days) {}
