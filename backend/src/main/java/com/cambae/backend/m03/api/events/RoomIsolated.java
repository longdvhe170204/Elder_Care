package com.cambae.backend.m03.api.events;
/** RoomIsolated: hợp đồng 003 mục 1.11; không gửi thông báo trực tiếp. */
public record RoomIsolated(long roomId, Long incidentId) {}
