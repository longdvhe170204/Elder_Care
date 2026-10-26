package com.cambae.backend.m03.internal.allocation.domain;

import com.cambae.backend.m03.api.spi.CareLevelCode;

/** Mức và nguồn dùng thực tế để kiểm theo Q-301. */
public record CheckedCareLevel(CareLevelCode level, CareLevelSource source) {}
