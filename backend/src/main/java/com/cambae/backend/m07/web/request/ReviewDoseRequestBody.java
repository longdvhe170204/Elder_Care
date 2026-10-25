package com.cambae.backend.m07.web.request;

/**
 * Request payload for reviewing an offline or conflicting medication dose (FR-029).
 */
public record ReviewDoseRequestBody(boolean accept, String note) {}
