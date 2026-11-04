package com.eldercare.backend.family.internal.relationship.domain;

/** Hồ sơ người thân ({@code nguoi_than}; data-model mục 2, FR-001). */
public record FamilyMember(
        long id,
        String fullName,
        String phoneNumber,
        String idDocumentType,
        String idDocumentNumber,
        String address,
        int version) {}
