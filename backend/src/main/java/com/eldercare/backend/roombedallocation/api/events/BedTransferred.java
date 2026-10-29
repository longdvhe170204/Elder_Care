package com.eldercare.backend.roombedallocation.api.events;

import java.time.Instant;

/** BedTransferred: hợp đồng 003 mục 1.11; không gửi thông báo trực tiếp. */
public record BedTransferred(
        long residentId,
        long previousAllocationId,
        long nextAllocationId,
        long previousFloorId,
        long nextFloorId,
        Instant at) {}
