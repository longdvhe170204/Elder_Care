package com.cambae.backend.m02.internal.death.domain;

import java.time.Instant;

/**
 * Bản ghi qua đời (nhóm 3, data-model mục 10; FR-069). Chỉ thêm: nguyên nhân bổ sung và sửa sai đi đường đính chính {@code
 * M02.DEATH_RECORD}. {@code causeOfDeath} thuộc nhóm thông tin sức khỏe (BR-M02-21). {@code confirmedById} là mã hồ sơ nhân
 * viên xác nhận.
 */
public record DeathRecord(
        long id,
        long residentId,
        Instant deathAt,
        String place,
        String discoveredBy,
        long confirmedById,
        String causeOfDeath,
        Long evidenceFileId,
        long historyId,
        long recordedBy,
        Instant recordedAt) {}
