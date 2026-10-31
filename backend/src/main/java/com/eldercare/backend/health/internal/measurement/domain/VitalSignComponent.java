package com.eldercare.backend.health.internal.measurement.domain;

import com.eldercare.backend.health.api.dto.VitalSignLevel;
import java.math.BigDecimal;

/** Thành phần giữ nguyên giá trị và mức tại lúc đo khi gộp vào JSON. */
public record VitalSignComponent(BigDecimal value, VitalSignLevel level) {}
