package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/** CaView của 005: phạm vi, khoảng giờ và đã công bố. */
public record CareShiftView(long id, Long floorAreaId, Instant startsAt, Instant endsAt, boolean published) {}
