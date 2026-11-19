package com.eldercare.backend.billing.internal.provision.service;

import com.eldercare.backend.billing.api.dto.*;
import com.eldercare.backend.billing.internal.item.persistence.CostViewStore;
import com.eldercare.backend.billing.internal.period.domain.CostStatementRecord;
import java.util.*;
import org.springframework.stereotype.Service;

@Service
public class FinalizedCostViews {
    private final CostViewStore views;

    public FinalizedCostViews(CostViewStore views) {
        this.views = views;
    }

    public CostStatementView statement(CostStatementRecord s) {
        if (!s.status().equals("FINALIZED")) throw new IllegalArgumentException("Bảng chưa chốt");
        return new CostStatementView(
                s.id(),
                s.month().toString(),
                s.residentId(),
                CostStatementType.valueOf(s.statementType()),
                s.finalPeriod(),
                s.throughDate(),
                s.status(),
                views.savedTotals(s.id()),
                s.total(),
                views.ofStatement(s.id()).stream()
                        .filter(i -> i.status().equals("FINALIZED"))
                        .map(this::item)
                        .toList());
    }

    private CostItemView item(CostItemView i) {
        // Không lộ điều chỉnh chưa chốt, lịch sử nội bộ hoặc căn cứ nguồn chứa thông tin sức khỏe.
        return new CostItemView(
                i.id(),
                i.costType(),
                i.origin(),
                i.billingSource(),
                i.costType() == CostType.MEDICATION ? "Thuốc " + Objects.toString(i.itemCode(), "") : i.description(),
                i.itemCode(),
                null,
                i.incurredOn(),
                i.quantity(),
                i.unit(),
                i.unitPrice(),
                Map.of(),
                i.factor(),
                i.amount(),
                i.flags(),
                i.status(),
                i.originalItemId(),
                i.originalPeriod(),
                List.of(),
                List.of(),
                List.of(),
                i.amount(),
                List.of("medicationName"));
    }
}
