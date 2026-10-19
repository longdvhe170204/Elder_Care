package com.cambae.backend.m15.account.api.events;

/** Bản ghi ngoại tuyến vào chờ xem lại (FR-041c). */
public record RecordPendingReview(long pendingRecordId, Long floorAreaId) {}
