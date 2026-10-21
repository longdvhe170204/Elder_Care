package com.cambae.backend.m03.api.dto;

import java.time.LocalDate;

/** Một buổi vào một ngày; khóa kết quả đếm đăng ký bán trú (003 FR-039, FR-040). */
public record SessionDate(LocalDate date, long sessionId) {}
