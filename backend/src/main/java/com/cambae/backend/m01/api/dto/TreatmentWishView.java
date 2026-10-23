package com.cambae.backend.m01.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** Phiếu nguyện vọng khi điều trị Hiệu lực cho 007: thẻ khẩn cấp, lệnh Chuyển viện, phiếu tóm tắt (FR-023). */
public record TreatmentWishView(
        long id,
        long residentId,
        List<PreferredHospital> preferredHospitals,
        Optional<HealthInsurance> healthInsurance,
        boolean askFamilyFirst,
        List<NotifyContact> notifyContacts,
        TreatmentEscort accompaniedBy,
        String note,
        Instant at) {}
