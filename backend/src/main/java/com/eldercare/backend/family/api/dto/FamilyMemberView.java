package com.eldercare.backend.family.api.dto;

import com.eldercare.backend.administration.account.api.spi.FamilyRelationshipType;

/** Người thân trong một quan hệ Hiệu lực ({@code NguoiThanView}). */
public record FamilyMemberView(
        long familyMemberId, String fullName, String phoneNumber, FamilyRelationshipType relationship) {}
