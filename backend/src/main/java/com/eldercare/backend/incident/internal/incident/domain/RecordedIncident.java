package com.eldercare.backend.incident.internal.incident.domain;

/** Kết quả ghi nhận: người ghi ngoài phạm vi chỉ nhận thông tin nhận dạng (FR-041, FR-048). */
public record RecordedIncident(long incidentId, boolean identificationOnly, boolean noOnDutyDoctor) {}
