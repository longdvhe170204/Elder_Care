package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.medication.internal.domain.DoseStatus;
import java.time.Instant;

/**
 * Lightweight projection of a closed dose record for consecutive refusal evaluation (BR-M07-13).
 */
public record ClosedDoseView(Long doseId, Instant scheduledTime, DoseStatus status) {}
