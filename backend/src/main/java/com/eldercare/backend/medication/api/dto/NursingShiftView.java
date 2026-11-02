package com.eldercare.backend.medication.api.dto;

import java.time.Instant;

/** CaView do SPI 006 sở hữu; không phụ thuộc ngược tới DTO M09. */
public record NursingShiftView(long id, Long floorAreaId, Instant startsAt, Instant endsAt) {}
