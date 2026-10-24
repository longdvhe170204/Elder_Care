package com.cambae.backend.m01.internal.health.domain;

/** Nội dung hồ sơ sức khỏe ban đầu (FR-017); mọi trường tùy chọn. */
public record InitialHealthRecordContent(
        String bloodType,
        String mobility,
        String selfCareAbility,
        String cognition,
        String initialNeedsNote,
        String nutritionNeeds,
        String currentMedications,
        String rehabilitationNeeds) {}
