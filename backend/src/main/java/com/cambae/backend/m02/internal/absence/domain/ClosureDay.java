package com.cambae.backend.m02.internal.absence.domain;

import java.time.LocalDate;

/** Ngày khu bán trú nghỉ (data-model mục 8; FR-060a). {@code lateDeclaration}: khai báo sau khi ngày đã qua. */
public record ClosureDay(
        long id, LocalDate date, String reason, boolean lateDeclaration, boolean active, long declaredBy) {}
