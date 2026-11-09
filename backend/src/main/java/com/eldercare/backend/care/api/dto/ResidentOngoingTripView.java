package com.eldercare.backend.care.api.dto;

import java.util.List;
import java.util.Optional;

/** Trưởng đoàn và người đi cùng hiện hành, kể cả sau chuyển trưởng đoàn. */
public record ResidentOngoingTripView(long tripId, Optional<Long> leaderId, List<Long> companionIds) {}
