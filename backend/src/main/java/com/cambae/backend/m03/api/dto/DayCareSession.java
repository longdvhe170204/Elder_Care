package com.cambae.backend.m03.api.dto;

import java.time.LocalTime;

/** Khung buổi bán trú để 004 đếm lịch đến theo FR-040. */
public record DayCareSession(long id, String name, LocalTime startTime, LocalTime endTime, boolean active) {}
