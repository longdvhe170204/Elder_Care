package com.eldercare.backend.family.api.events;

/** Lệnh đã đổi quan hệ (012 contracts mục 1.5; lệnh ở glossary 18.1). */
public enum RelationshipChange {
    CREATED,
    PRIMARY_CONTACT_CHANGED,
    REPRESENTATIVE_CHANGED,
    CONTACT_ORDER_CHANGED,
    ENDED
}
