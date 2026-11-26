package com.eldercare.backend.care.internal.careplan.domain;

import java.time.Instant;

/** Mốc phát sinh yêu cầu xem xét kế hoạch chăm sóc (FR-058, Q-448): nguồn, mã nguồn, mã tham chiếu, thời điểm. */
public record CarePlanReviewRaised(String source, long sourceId, String reference, Instant raisedAt) {}
