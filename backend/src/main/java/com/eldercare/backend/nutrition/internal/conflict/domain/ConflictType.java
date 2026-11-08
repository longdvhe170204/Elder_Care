package com.eldercare.backend.nutrition.internal.conflict.domain;

/** Loại xung đột giữa món và một người (api-011 {@code Conflict.type}); thứ tự khai báo là thứ tự báo. */
public enum ConflictType {
    ALLERGY,
    PERSONAL_RESTRICTION,
    DIET_RESTRICTION
}
