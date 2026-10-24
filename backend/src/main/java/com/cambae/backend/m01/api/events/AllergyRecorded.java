package com.cambae.backend.m01.api.events;

/** Dị ứng chọn từ danh mục được ghi nhận; 006, 011 kiểm tra, idempotent theo itemId (FR-020, BR-M01-12). */
public record AllergyRecorded(long itemId, long residentId, long allergenId) {}
