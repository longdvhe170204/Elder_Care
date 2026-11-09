package com.eldercare.backend.billing.internal.source.service;

import com.eldercare.backend.admission.api.ResidencyDayApi;
import com.eldercare.backend.admission.api.dto.ResidencyDayView;
import com.eldercare.backend.billing.internal.source.domain.BillingSourceKey;
import com.eldercare.backend.billing.internal.source.persistence.BillingSourceIndexStore;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** FR-005a: ngày lưu trú lấy khi tra cứu; không lưu bản sao vào chỉ mục sự kiện. */
@Service
public class ResidencyBillingSources {
    static final String SOURCE_TYPE = "RESIDENCY_DAY";
    static final String DAY_UNIT = "DAY";

    private final ResidencyDayApi residency;
    private final BillingSourceIndexStore index;

    public ResidencyBillingSources(ResidencyDayApi residency, BillingSourceIndexStore index) {
        this.residency = residency;
        this.index = index;
    }

    public record Source(long residentId, BillingSourceKey key, ResidencyDayView day, Long existingItemId) {}

    /**
     * Ngày lưu trú trong [from, to] theo thứ tự ngày, đọc lại từ 004 mỗi lần (không giữ bản sao), kèm khoản phí hiện hành
     * nếu đã lập. Lịch rỗng thì không tra chỉ mục.
     */
    public List<Source> between(long residentId, LocalDate from, LocalDate to) {
        List<ResidencyDayView> days = residency.daysBetween(residentId, from, to).stream()
                .sorted(Comparator.comparing(ResidencyDayView::date))
                .toList();
        if (days.isEmpty()) {
            return List.of();
        }
        List<BillingSourceKey> keys = days.stream()
                .map(d -> new BillingSourceKey(residentId + ":" + d.date(), DAY_UNIT))
                .toList();
        Map<BillingSourceKey, Long> existing = index.effectiveItems(
                residentId,
                SOURCE_TYPE,
                keys.stream().map(BillingSourceKey::sourceId).toList());
        List<Source> sources = new ArrayList<>(days.size());
        for (int i = 0; i < days.size(); i++) {
            sources.add(new Source(residentId, keys.get(i), days.get(i), existing.get(keys.get(i))));
        }
        return sources;
    }
}
