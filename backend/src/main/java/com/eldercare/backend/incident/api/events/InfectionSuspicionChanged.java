package com.eldercare.backend.incident.api.events;

import java.time.Instant;

/** Sự kiện/yêu cầu công khai theo hợp đồng 007 mục 1.12–1.13; không trực tiếp gửi thông báo. */
public record InfectionSuspicionChanged(long residentId, long incidentId, boolean assigned, Instant at) {}
