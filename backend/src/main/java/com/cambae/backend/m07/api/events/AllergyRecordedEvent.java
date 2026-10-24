package com.cambae.backend.m07.api.events;

/**
 * Event fired when a new allergy is recorded (from Module 001 per FR-012, BR-M01-12).
 */
public record AllergyRecordedEvent(long allergyId, long residentId, long allergenId) {}
