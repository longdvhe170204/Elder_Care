package com.cambae.backend.m03.api.dto;

/** Kiểu trao đổi BedAlertView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record BedAlertView(long allocationId, long residentId, long bedId, String code) {}
