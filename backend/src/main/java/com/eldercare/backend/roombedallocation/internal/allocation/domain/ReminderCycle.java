package com.eldercare.backend.roombedallocation.internal.allocation.domain;

import java.time.Instant;

/** T066 (R-15): chu kỳ dấu nhắc đang mở trên {@code reminder_marker}: số chu kỳ, lúc bắt đầu, lần nhắc cuối, số lần. */
public record ReminderCycle(int cycle, Instant started, Instant last, int count) {}
