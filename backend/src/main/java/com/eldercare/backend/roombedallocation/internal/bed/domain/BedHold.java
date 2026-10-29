package com.eldercare.backend.roombedallocation.internal.bed.domain;
/** T008: dữ liệu của máy trạng thái giường, research G-04. */
import java.time.Instant;

public record BedHold(HoldReason reason, long residentId, Long waitingRecordId, Instant until) {}
