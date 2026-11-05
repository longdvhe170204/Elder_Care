package com.eldercare.backend.care.web.request;

import java.util.List;

public record TripAssignmentWrite(Long leaderId, List<Long> companionIds) {}
