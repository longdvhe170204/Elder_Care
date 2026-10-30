package com.eldercare.backend.administration.account.api.spi;

/**
 * Loại quan hệ của người thân với người cao tuổi (012 data-model {@code quan_he_nguoi_than.quan_he}, FR-002; glossary 7):
 * {@code VO_CHONG}, {@code CON}, {@code CHAU}, {@code ANH_CHI_EM}, {@code KHAC}.
 */
public enum FamilyRelationshipType {
    SPOUSE,
    CHILD,
    GRANDCHILD,
    SIBLING,
    OTHER
}
