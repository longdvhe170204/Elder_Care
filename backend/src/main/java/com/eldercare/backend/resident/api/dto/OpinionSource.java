package com.eldercare.backend.resident.api.dto;

/** Nguồn của ý kiến dùng chung (FR-023g, BR-M01-14); {@code familyMemberId} chỉ có khi {@code FAMILY_MEMBER}. */
public record OpinionSource(OpinionSourceType type, Long familyMemberId) {}
