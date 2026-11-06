package com.eldercare.backend.belongings.api.events;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.time.Instant;

/** Đồ gửi đổi trạng thái theo một bản ghi bàn giao ({@code DoGuiDaDoiTrangThai}; FR-011, FR-025): 004, 016 nghe. */
public record BelongingStatusChanged(
        long belongingId, long residentId, BelongingStatus from, BelongingStatus to, long handoverId, Instant at) {}
