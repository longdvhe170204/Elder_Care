package com.eldercare.backend.care.api;

import com.eldercare.backend.resident.api.dto.OpinionSource;

/** 012 gọi trong transaction ghi sở thích từ phản hồi (001 FR-023g, Q-363). */
public interface FeedbackPreferenceCommandApi {
    /** MANDATORY; trả mã mục sở thích, phát OpinionRecordedFromFeedback cùng transaction. */
    long recordFromFeedback(
            long residentId,
            long feedbackId,
            Long interestGroupId,
            String description,
            String level,
            OpinionSource source,
            boolean confirmReplacement);
}
