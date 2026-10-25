package com.cambae.backend.m03.api.dto;

/** Kiểu trao đổi RoomView, hợp đồng 003 mục 1.2–1.9 (T003). */
public record RoomView(long id, long floorId, long roomTypeId, String code, String name, boolean active) {}
