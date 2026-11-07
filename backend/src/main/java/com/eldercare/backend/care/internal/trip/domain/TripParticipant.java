package com.eldercare.backend.care.internal.trip.domain;

public record TripParticipant(long registrationId, long residentId, String status, String reason) {}
