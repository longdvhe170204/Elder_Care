package com.eldercare.backend.roombedallocation.api.dto;

import java.time.Instant;

/** Giường Chờ sẵn sàng quá CFG-M03-04; 003 tự kiểm ngưỡng. */
public record AwaitingReadyBedView(long bedId, long roomId, String description, Instant awaitingSince) {}
