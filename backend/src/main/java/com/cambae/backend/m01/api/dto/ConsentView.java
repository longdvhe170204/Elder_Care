package com.cambae.backend.m01.api.dto;

import java.util.Optional;
import java.util.Set;

/** Bản đồng ý chia sẻ dữ liệu Hiệu lực (009, 012). */
public record ConsentView(
        long id,
        long residentId,
        ConsenterType consenterType,
        Optional<Long> consenterFamilyMemberId,
        Set<Long> familyMemberIdsAllowedHealthView,
        boolean photoConsent,
        boolean notificationConsent) {}
