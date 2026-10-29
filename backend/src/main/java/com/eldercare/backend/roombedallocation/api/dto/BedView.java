package com.eldercare.backend.roombedallocation.api.dto;

/** Kiểu trao đổi BedView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record BedView(long id, long roomId, String code, BedStatus state) {}
