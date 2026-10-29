package com.eldercare.backend.roombedallocation.api.dto;

import java.time.LocalDate;

/** Một buổi bán trú trong một ngày (FR-039, FR-040). */
public record SessionDate(LocalDate date, long sessionId) {}
