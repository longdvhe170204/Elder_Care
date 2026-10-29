package com.eldercare.backend.roombedallocation.api.dto;

/** Kiểu trao đổi AreaView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record AreaView(long id, String code, String name, boolean active) {}
