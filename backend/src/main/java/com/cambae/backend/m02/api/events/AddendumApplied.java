package com.cambae.backend.m02.api.events;

import java.time.LocalDate;
import java.util.Set;

/** Phụ lục đã áp dụng; 010, 018, 005, 011 nghe (FR-042, FR-033b). {@code changeTypes} theo glossary mục 10.3. */
public record AddendumApplied(
        long addendumId, long contractId, long residentId, Set<String> changeTypes, LocalDate effectiveDate) {}
