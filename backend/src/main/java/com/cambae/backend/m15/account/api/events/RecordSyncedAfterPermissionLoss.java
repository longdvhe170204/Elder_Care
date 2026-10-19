package com.cambae.backend.m15.account.api.events;

/** Bản ghi ngoại tuyến được chấp nhận kèm dấu vì mất quyền lúc đồng bộ (FR-041a, CHK056). */
public record RecordSyncedAfterPermissionLoss(String recordType, String recordId, Long floorAreaId) {}
