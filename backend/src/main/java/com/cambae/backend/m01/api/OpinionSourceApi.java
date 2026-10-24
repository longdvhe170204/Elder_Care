package com.cambae.backend.m01.api;

import com.cambae.backend.m01.api.dto.OpinionSource;

/** Kiểm nguồn ý kiến cho 004, 005, 012, 014 (FR-023g, BR-M01-14, research H-16). */
public interface OpinionSourceApi {

    /**
     * {@code FAMILY_MEMBER}: người thân phải có quan hệ Hiệu lực lúc gọi; loại khác: {@code familyMemberId} phải rỗng.
     * Sai thì 422 {@code INVALID_OPINION_SOURCE}.
     */
    void validate(long residentId, OpinionSource source);
}
