package com.eldercare.backend.belongings.internal.ending.domain;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import java.time.Instant;

/** Đồ còn cần nhận của một người cao tuổi: mã, người cao tuổi, trạng thái, mốc tính xử lý, lần nhắc cuối. */
public record OpenBelonging(
        long id, long residentId, BelongingStatus status, Instant clockStartAt, Instant lastRemindedAt) {}
