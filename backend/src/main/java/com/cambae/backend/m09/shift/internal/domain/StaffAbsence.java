package com.cambae.backend.m09.shift.internal.domain;

import java.time.Instant;

/** Định danh vắng ca là id dòng thành viên; không đưa lý do riêng tư vào sự kiện. */
public record StaffAbsence(long id, long shiftId, long staffId, Long recordedBy, Instant at) {}
