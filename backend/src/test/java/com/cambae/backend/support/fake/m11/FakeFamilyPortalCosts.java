package com.cambae.backend.support.fake.m11;

import com.cambae.backend.m11.api.FamilyPortalCostApi;
import com.cambae.backend.m11.api.dto.*;
import com.cambae.backend.m15.account.api.CurrentUser;
import java.time.LocalDate;
import java.util.*;

/** Kết quả do test đặt trước; không thay kiểm quyền người thân hoặc tên thuốc của bản thật. */
public class FakeFamilyPortalCosts implements FamilyPortalCostApi {
    private record Range(long residentId, LocalDate from, LocalDate to) {}

    private record Family(long residentId, long familyMemberId) {}

    private final Map<Long, List<CostStatementView>> statements = new HashMap<>();
    private final Map<Range, ProvisionalCostSummaryView> summaries = new HashMap<>();
    private final Map<Family, List<PurchaseAssistanceView>> requests = new HashMap<>();

    public FakeFamilyPortalCosts statements(long residentId, List<CostStatementView> items) {
        statements.put(residentId, List.copyOf(items));
        return this;
    }

    public FakeFamilyPortalCosts provisional(
            long residentId, LocalDate from, LocalDate to, ProvisionalCostSummaryView summary) {
        summaries.put(new Range(residentId, from, to), summary);
        return this;
    }

    public FakeFamilyPortalCosts awaitingConsent(
            long residentId, long familyMemberId, List<PurchaseAssistanceView> items) {
        requests.put(new Family(residentId, familyMemberId), List.copyOf(items));
        return this;
    }

    @Override
    public List<CostStatementView> finalizedStatements(long residentId, CurrentUser user) {
        return statements.getOrDefault(residentId, List.of());
    }

    @Override
    public ProvisionalCostSummaryView provisional(long residentId, LocalDate from, LocalDate to, CurrentUser user) {
        var result = summaries.get(new Range(residentId, from, to));
        if (result == null) throw new IllegalStateException("Chưa đặt kết quả tạm tính trong bản giả 010");
        return result;
    }

    @Override
    public List<PurchaseAssistanceView> awaitingConsent(long residentId, long familyMemberId) {
        return requests.getOrDefault(new Family(residentId, familyMemberId), List.of());
    }

    public void reset() {
        statements.clear();
        summaries.clear();
        requests.clear();
    }
}
