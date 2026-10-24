package com.cambae.backend.m03.internal.daycare.domain;

import java.time.LocalDate;

/** Kết quả đếm lịch nguồn 004 theo buổi/ngày, không sinh lịch mới. */
public record SessionCapacityRow(
        LocalDate date, long sessionId, String sessionName, int registrations, int capacity, int remaining) {}
