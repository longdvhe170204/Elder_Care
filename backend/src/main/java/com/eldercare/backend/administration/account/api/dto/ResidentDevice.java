package com.eldercare.backend.administration.account.api.dto;

/** Người cao tuổi của một thiết bị đã gắn ({@code NguoiCaoTuoiCuaThietBi}, module-interfaces 1.11). */
public record ResidentDevice(long residentId, long accountId, String deviceId) {}
