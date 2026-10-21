package com.cambae.backend.m03.api.dto;

import java.time.LocalTime;

/** Khung buổi dùng để 004 đếm lại lịch đăng ký (003 FR-040); chưa phải phản hồi REST. */
public record DayCareSession(long id, String name, LocalTime startsAt, LocalTime endsAt) {}
