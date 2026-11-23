package com.eldercare.backend.care.internal.dailyschedule.domain;

import java.time.Instant;

/** 005 FR-055 (b): buổi hoạt động mở cho toàn viện trong ngày. */
public record FacilitySession(Instant at, String name) {}
