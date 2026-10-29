package com.eldercare.backend.medication.api.dto;

import java.time.Instant;

/**
 * Time interval between start and end instants.
 */
public record TimeInterval(Instant from, Instant to) {}
