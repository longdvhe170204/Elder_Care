package com.eldercare.backend.support.fake.billing;

import com.eldercare.backend.billing.internal.purchase.domain.PurchaseBalancePort;
import java.time.Instant;
import java.util.*;

/** 017 và 010 cùng chủ E; dùng lại bản giả này, không nạp vào ứng dụng thật. */
public class FakePurchaseBalance implements PurchaseBalancePort {
    private record Query(long residentId, long amount, Instant at) {}

    private final Map<Query, Boolean> results = new HashMap<>();

    public FakePurchaseBalance insufficient(long residentId, long amount, Instant at, boolean result) {
        results.put(new Query(residentId, amount, at), result);
        return this;
    }

    @Override
    public boolean insufficientBalance(long residentId, long amount, Instant at) {
        return results.getOrDefault(new Query(residentId, amount, at), false);
    }

    public void reset() {
        results.clear();
    }
}
