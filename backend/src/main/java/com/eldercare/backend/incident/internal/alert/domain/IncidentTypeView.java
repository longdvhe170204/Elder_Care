package com.eldercare.backend.incident.internal.alert.domain;

/** Danh mục sự cố nội bộ, FR-042. */
public record IncidentTypeView(
        long id,
        String code,
        String name,
        IncidentLevel defaultLevel,
        boolean nonHealth,
        boolean allowsNoResident,
        boolean active,
        int version) {}
