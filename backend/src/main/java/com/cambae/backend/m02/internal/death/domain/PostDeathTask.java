package com.cambae.backend.m02.internal.death.domain;

import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import java.time.Instant;

/** Kết quả tính của một mục việc sau qua đời (data-model mục 10; FR-071). */
public record PostDeathTask(
        long deathRecordId, PostDeathItemCode code, PostDeathItemStatus status, String basis, Instant updatedAt) {

    /** Mục đã xong: Hoàn thành hoặc Không áp dụng (FR-072). */
    public boolean done() {
        return status != PostDeathItemStatus.NOT_COMPLETED;
    }
}
