package com.eldercare.backend.administration.correction.api;

/** Sự kiện sau khi một bản đính chính được lưu (contracts/module-interfaces.md mục 1.6). */
public record CorrectionMade(String recordType, long originalRecordId, CorrectionType type, long correctionId) {}
