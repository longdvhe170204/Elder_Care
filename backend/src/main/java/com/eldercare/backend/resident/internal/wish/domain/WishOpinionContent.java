package com.eldercare.backend.resident.internal.wish.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Nội dung dòng {@code record_event} loại {@code M01.WISH_OPINION} (data-model 001 mục 0.1): nguồn và nội dung của một ý
 * kiến khác. {@code legacy} của dòng chép từ bảng cũ bị bỏ qua khi đọc.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record WishOpinionContent(String sourceType, Long sourceFamilyMemberId, String content) {}
