package com.eldercare.backend.billing.internal.provision.service;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.provision.domain.ProvisionalCostTotal;
import com.eldercare.backend.billing.internal.provision.persistence.CostProvisionStore;
import java.time.LocalDate;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CostSummaries {
    private final CostProvisionStore store;

    public CostSummaries(CostProvisionStore store) {
        this.store = store;
    }

    public ProvisionalCostSummaryView provisional(long resident, LocalDate from, LocalDate to) {
        var totals = new EnumMap<CostType, Long>(CostType.class);
        boolean missing = false;
        for (var item : store.provisional(resident)) {
            if (!ProvisionalCostTotal.included(item.origin(), item.status())) continue;
            if (from != null && item.date().isBefore(from)
                    || to != null && item.date().isAfter(to)) continue;
            if (item.amount() == null) missing = true;
            else totals.merge(item.type(), item.amount(), Math::addExact);
        }
        return new ProvisionalCostSummaryView(
                "tạm tính, chưa chốt",
                totals.entrySet().stream()
                        .map(e -> new CostTypeTotal(e.getKey(), e.getValue()))
                        .toList(),
                missing);
    }
}
