package com.eldercare.backend.incident.internal.emergency.domain;

import java.util.Map;

/**
 * Thông tin hiện ở màn lệnh Chuyển viện (schema {@code TransferOutInfo} của api-007; FR-049, research S-25): nguyện vọng
 * khi điều trị, nguyện vọng cuối đời, người liên hệ chính; không gợi ý cơ sở tiếp nhận (Q-306).
 */
public record TransferOutInfo(
        Map<String, Object> treatmentWish,
        Map<String, Object> endOfLifeWish,
        EmergencyCard.Contact primaryContact,
        boolean mustAskFirst,
        boolean emergency) {}
