package com.eldercare.backend.care.internal.wellbeing.domain;

import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.resident.api.dto.OpinionSource;
import java.time.Instant;

/**
 * 014 FR-051 (data-model {@code so_thich} → {@code simple_record} {@code M04.PREFERENCE}, research L-09; Q-363): một mục hiện
 * hành mỗi (người cao tuổi, nhóm sở thích, nguồn). Người ghi là {@code created_by}; người và lúc kết thúc, lý do bỏ ở nội
 * dung. Trạng thái {@code CURRENT}, {@code REPLACED}, {@code REMOVED}.
 */
public record Preference(
        long interestGroupId,
        String description,
        String level,
        OpinionSource source,
        Instant recordedAt,
        Instant endedAt,
        Long endedBy,
        String endReason,
        Long feedbackId) {
    public Preference(
            long interestGroupId,
            String description,
            String level,
            OpinionSource source,
            Instant recordedAt,
            Instant endedAt,
            Long endedBy,
            String endReason) {
        this(interestGroupId, description, level, source, recordedAt, endedAt, endedBy, endReason, null);
    }

    public static final SharedKind<Preference> KIND = SharedKind.of("M04.PREFERENCE", Preference.class);
    public static final String CURRENT = "CURRENT";
    public static final String REPLACED = "REPLACED";
    public static final String REMOVED = "REMOVED";
    public static final String LIKE = "LIKE";
    public static final String DISLIKE = "DISLIKE";

    /** Khóa mục hiện hành theo nhóm và nguồn (research L-09 {@code khoa_hien_hanh}). */
    public static String currentKey(long residentId, long interestGroupId, OpinionSource source) {
        return residentId + ":" + interestGroupId + ":" + source.type() + ":"
                + (source.familyMemberId() == null ? 0 : source.familyMemberId());
    }

    public Preference ended(Instant at, long by, String reason) {
        return new Preference(interestGroupId, description, level, source, recordedAt, at, by, reason, feedbackId);
    }
}
