package com.eldercare.backend.incident.internal.incident.domain;

/** Tầng chọn được khi ghi sự cố cơ sở vật chất hoặc gửi yêu cầu hỗ trợ; {@code current} là tầng của ca đang trực. */
public record IncidentFloor(long id, String name, String buildingName, boolean current) {}
