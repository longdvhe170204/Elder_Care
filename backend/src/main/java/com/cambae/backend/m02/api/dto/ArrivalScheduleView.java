package com.cambae.backend.m02.api.dto;

import java.time.LocalTime;

/** Một dòng lịch đến bán trú của hợp đồng áp dụng tại ngày hỏi (FR-019). */
public record ArrivalScheduleView(long contractId, LocalTime arrivalTime, LocalTime departureTime) {}
