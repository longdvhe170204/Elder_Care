package com.eldercare.backend.resident.api.dto;

import java.time.Instant;

/** Lượt Tạm vắng đang mở với mốc dự kiến trở lại (FR-050). */
public record OpenAbsence(long absenceId, Instant expectedReturnAt) {}
