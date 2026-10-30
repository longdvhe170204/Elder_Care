package com.cambae.backend.m02.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;

/** Lịch đến có hiệu lực trong ngày, đã giải quy tắc thứ/ngày của hợp đồng; không qua đêm (004 FR-019). */
public record AttendanceScheduleView(LocalDate date, LocalTime arrivalTime, LocalTime departureTime) {}
