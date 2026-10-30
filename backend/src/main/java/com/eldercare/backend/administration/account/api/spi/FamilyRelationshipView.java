package com.eldercare.backend.administration.account.api.spi;

/** Một quan hệ Hiệu lực của người thân với người cao tuổi (012 contracts mục 1.1, {@code QuanHeView}). */
public record FamilyRelationshipView(long relationshipId, long residentId) {}
