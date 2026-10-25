package com.cambae.backend.m02.api.events;

import java.time.LocalDate;

/** Ngày nghỉ bán trú được khai báo hoặc hủy; 005, 010, 011 nghe (FR-060a). */
public record DayCareClosureDayChanged(LocalDate date, boolean active) {}
