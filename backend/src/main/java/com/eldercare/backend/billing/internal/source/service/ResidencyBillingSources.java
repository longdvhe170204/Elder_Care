package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.billing.internal.source.domain.BillingSourceKey;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/** FR-005a: ngày lưu trú lấy khi tra cứu; không lưu bản sao vào chỉ mục sự kiện. */
@Service
public class ResidencyBillingSources {
    private final ResidencyDayApi residency;
    private final BillingSourceIndexStore index;

    public ResidencyBillingSources(ResidencyDayApi residency, BillingSourceIndexStore index) {
        this.residency = residency;
        this.index = index;
    }

    public record Source(long residentId, BillingSourceKey key, ResidencyDayView day, Long existingItemId) {}

    public List<Source> between(long residentId, LocalDate from, LocalDate to) {
        throw new UnsupportedOperationException("T011: chưa cài tra ngày lưu trú");
    }
}
