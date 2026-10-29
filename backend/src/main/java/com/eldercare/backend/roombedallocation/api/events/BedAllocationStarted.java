package com.eldercare.backend.roombedallocation.api.events;

import java.time.Instant;

/** BedAllocationStarted: hợp đồng 003 mục 1.11; không gửi thông báo trực tiếp. */
public record BedAllocationStarted(long allocationId, long residentId, long bedId, Instant at) {}
