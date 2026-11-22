package com.eldercare.backend.care.api.dto;

import java.time.Instant;

/**
 * Một mục "Lịch chăm sóc hằng ngày" (005 FR-055): {@code kind} là MEAL, ACTIVITY, ROUTINE hoặc CARE_TASK; {@code status}
 * chỉ có ở CARE_TASK: DONE, NOT_DONE, NOT_PERFORMED; {@code measuredValue} chỉ khi người gọi cho phép hiện giá trị đo.
 */
public record DailyScheduleEntry(Instant at, String kind, String name, String status, String measuredValue) {}
