package com.eldercare.backend.billing.balance.internal.document.domain;

/** Loại bảng chi phí in trên phiếu báo phí (glossary 23.3: thường, bổ sung, kỳ cuối). */
public enum StatementType {
    REGULAR("Bảng thường"),
    SUPPLEMENTARY("Bảng bổ sung"),
    FINAL_PERIOD("Bảng kỳ cuối");

    private final String label;

    StatementType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
