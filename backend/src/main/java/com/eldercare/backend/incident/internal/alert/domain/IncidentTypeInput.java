package com.eldercare.backend.incident.internal.alert.domain;

/** Nội dung lệnh danh mục sự cố; không nhận cờ cho phép không có người cao tuổi. */
public record IncidentTypeInput(
        String code, String name, IncidentLevel defaultLevel, boolean nonHealth, int sortOrder) {}
