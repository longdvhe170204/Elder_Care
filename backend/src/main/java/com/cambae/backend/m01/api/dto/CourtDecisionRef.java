package com.cambae.backend.m01.api.dto;

import java.time.LocalDate;

/** Quyết định của Tòa án làm bằng chứng cho người đại diện theo pháp luật (FR-011, FR-023c). */
public record CourtDecisionRef(String number, LocalDate date, String court, Long fileId) {}
