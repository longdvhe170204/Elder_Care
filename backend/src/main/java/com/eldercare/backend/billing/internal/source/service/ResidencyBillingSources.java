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
        var days = residency.daysBetween(residentId, from, to).stream()
                .sorted(java.util.Comparator.comparing(ResidencyDayView::date))
                .toList();
        if (days.isEmpty()) return List.of();
        var existing = index.effectiveItems(
                residentId,
                "RESIDENCY_DAY",
                days.stream().map(day -> residentId + ":" + day.date()).toList());
        return days.stream()
                .map(day -> {
                    var key = new BillingSourceKey(residentId + ":" + day.date(), "DAY");
                    return new Source(residentId, key, day, existing.get(key));
                })
                .toList();
    }
}
