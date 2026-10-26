package com.cambae.backend.m03.api.dto;

import java.time.LocalDate;

/** Một buổi bán trú trong một ngày (FR-039, FR-040). */
public record SessionDate(LocalDate date, long sessionId) {}
