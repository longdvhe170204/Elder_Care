package com.cambae.backend.m05.api.dto;

import java.time.Instant;

/** CaView do SPI 007 sở hữu, gồm giờ bắt đầu/kết thúc để tính hạn cảnh báo. */
public record AlertShiftView(long id, Long floorAreaId, Instant startsAt, Instant endsAt) {}
