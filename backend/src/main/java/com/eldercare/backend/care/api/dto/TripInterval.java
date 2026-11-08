package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/** 014/006: khoảng đi dự kiến, đầu đóng cuối mở. */
public record TripInterval(Instant from, Instant to) {}
