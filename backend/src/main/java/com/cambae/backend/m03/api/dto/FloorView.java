package com.cambae.backend.m03.api.dto;

/** Kiểu trao đổi FloorView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record FloorView(long id, long areaId, String code, String name, String buildingName, boolean active) {}
