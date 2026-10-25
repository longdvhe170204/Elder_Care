package com.cambae.backend.m03.internal.bed.domain;
/** T008: dữ liệu của máy trạng thái giường, research G-04. */
import com.cambae.backend.m03.api.dto.BedStatus;
import java.time.Instant;

public record BedStateChange(BedStatus before, BedStatus after, Instant occurredAt, boolean executedLate) {}
