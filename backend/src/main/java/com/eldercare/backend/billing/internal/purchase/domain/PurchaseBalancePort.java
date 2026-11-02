package com.eldercare.backend.billing.internal.purchase.domain;

import java.time.Instant;

/** P-12/T004: 017 cài, purchase của 010 gọi; không có bean thì không đặt dấu số dư không đủ. */
public interface PurchaseBalancePort {
    boolean insufficientBalance(long residentId, long amount, Instant at);
}
