package com.eldercare.backend.support.fake.medication;

import com.eldercare.backend.medication.api.spi.PurchaseAssistancePort;
import com.eldercare.backend.medication.api.spi.PurchaseAssistanceView;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Bản giả {@link PurchaseAssistancePort} (Module 010 cài): trả về kết quả mua thuốc hộ.
 */
public class FakePurchaseAssistancePort implements PurchaseAssistancePort {

    private final Map<String, PurchaseAssistanceView> purchases = new HashMap<>();

    public FakePurchaseAssistancePort recordPurchase(PurchaseAssistanceView purchase) {
        purchases.put(purchase.requestId() + ":" + purchase.residentId(), purchase);
        return this;
    }

    @Override
    public Optional<PurchaseAssistanceView> purchasedDetails(long requestId, long residentId) {
        return Optional.ofNullable(purchases.get(requestId + ":" + residentId));
    }
}
