package com.eldercare.backend.resident.api.events;

/** Dị ứng bị loại trừ; 011, 007 nghe (FR-020). */
public record AllergyExcluded(long itemId, long residentId) {}
