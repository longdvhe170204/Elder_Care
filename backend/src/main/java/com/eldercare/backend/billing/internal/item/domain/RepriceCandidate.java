package com.eldercare.backend.billing.internal.item.domain;

public record RepriceCandidate(long itemId, long residentId, long statementId) {}
