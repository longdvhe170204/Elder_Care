package com.eldercare.backend.care.internal.careplan.domain;

import java.time.Instant;

/** Metadata người ghi và thời điểm lấy từ record_event, không nhận giá trị từ client. */
public record RiskOpinionEntry(RiskOpinion opinion, Long recordedBy, Instant at) {}
