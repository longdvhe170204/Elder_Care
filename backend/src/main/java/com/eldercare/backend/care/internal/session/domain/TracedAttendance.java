package com.eldercare.backend.care.internal.session.domain;

/** Bản gốc một dòng điểm danh cho truy vết; {@code value} là kết quả điểm danh buổi hoặc loại điểm danh chuyến. */
public record TracedAttendance(long id, long sessionId, long residentId, String value) {}
