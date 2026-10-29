package com.eldercare.backend.roombedallocation.api.events;

import java.time.Instant;

/** BedBecameEmpty: hợp đồng 003 mục 1.11; không gửi thông báo trực tiếp. */
public record BedBecameEmpty(long bedId, String reason, Instant at) {}
