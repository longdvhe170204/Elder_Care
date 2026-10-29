package com.eldercare.backend.roombedallocation.web.response;

import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;

/** Chỉ mô tả schema tra cứu của các story phân bổ; Phase 3 chưa trả dữ liệu nguồn M01. */
public record ResidentSummarySchema(Long residentId, String fullName, String gender, CareLevelCode careLevel) {}
