package com.cambae.backend.m03.api.events;

import java.time.Instant;

/** BedAllocationClosed: hợp đồng 003 mục 1.11; không gửi thông báo trực tiếp. */
public record BedAllocationClosed(long allocationId, long residentId, long bedId, Instant at, String reason) {}
