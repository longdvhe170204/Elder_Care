package com.eldercare.backend.incident.api.events;

import java.time.Instant;

/** Đã chọn chăm sóc giảm nhẹ để 005 điều chỉnh kế hoạch (FR-050d). */
public record PalliativeCareChoiceRecorded(long residentId, String eventKey, Instant occurredAt) {}
