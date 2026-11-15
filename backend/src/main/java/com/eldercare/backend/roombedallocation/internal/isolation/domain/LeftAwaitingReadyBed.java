package com.eldercare.backend.roombedallocation.internal.isolation.domain;

import java.time.Instant;

/** T054 (FR-045): giường còn Chờ sẵn sàng mà phân bổ vừa đóng là của người được thêm vào danh sách tiếp xúc. */
public record LeftAwaitingReadyBed(long bedId, long floorId, Instant awaitingSince) {}
