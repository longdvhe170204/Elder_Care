package com.eldercare.backend.resident.api.events;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import java.time.Instant;

/** Đổi trạng thái; 009 gửi Khẩn cấp cho người liên hệ chính khi TRANSFER_OUT, RECORD_DEATH (Q-97, Q-354); 002, 016 nghe (FR-046). */
public record ResidentStatusChanged(
        long residentId, ResidentStatus from, ResidentStatus to, StatusCommand command, long historyId, Instant at) {}
