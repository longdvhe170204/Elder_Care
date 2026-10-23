package com.cambae.backend.m01.api.events;

import com.cambae.backend.m01.api.dto.OpinionSource;

/**
 * Module sở hữu phát khi lệnh ghi mục ý kiến có {@code feedbackId} (phản hồi loại Mong muốn); 012 nghe để gắn mục vào
 * phản hồi nếu phản hồi thuộc đúng người cao tuổi và nguồn là {@code RESIDENT} (FR-023g, Q-366; research H-17).
 */
public record OpinionRecordedFromFeedback(
        long feedbackId, String sourceModule, String itemType, long itemId, long residentId, OpinionSource source) {}
