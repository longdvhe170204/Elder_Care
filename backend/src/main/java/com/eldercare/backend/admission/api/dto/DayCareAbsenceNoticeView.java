package com.eldercare.backend.admission.api.dto;

import java.time.Instant;

/** Một báo vắng bán trú; {@code sessionId} rỗng là báo cho cả ngày. */
public record DayCareAbsenceNoticeView(
        long noticeId, Long sessionId, Instant familyNotifiedAt, boolean withinNoticePeriod) {}
