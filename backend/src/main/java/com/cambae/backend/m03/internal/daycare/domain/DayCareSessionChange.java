package com.cambae.backend.m03.internal.daycare.domain;

import java.util.List;

/** Kết quả lệnh cấu hình kèm cảnh báo, không hủy lịch đã nhận. */
public record DayCareSessionChange(DayCareSessionState session, List<SessionCapacityRow> overCapacitySessions) {}
