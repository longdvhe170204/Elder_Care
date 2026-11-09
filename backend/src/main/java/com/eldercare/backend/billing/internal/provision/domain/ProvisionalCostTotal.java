package com.eldercare.backend.billing.internal.provision.domain;

/** FR-038/038a: thành phần chung của tạm tính và tổng chưa chốt. */
public final class ProvisionalCostTotal {
    private ProvisionalCostTotal() {}

    public static boolean included(String origin, String status) {
        if (!java.util.Set.of("DRAFT", "CHECKED", "APPROVED").contains(status)) return false;
        return !(origin.equals("MANUAL") || origin.equals("PURCHASE_ASSISTANCE")) || status.equals("APPROVED");
    }
}
