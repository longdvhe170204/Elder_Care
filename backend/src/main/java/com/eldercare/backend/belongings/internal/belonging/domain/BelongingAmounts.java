package com.eldercare.backend.belongings.internal.belonging.domain;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Số lượng của các đồ cùng một lần tiếp nhận (FR-012, SC-001; plan mục "Kế hoạch triển khai R-15" điểm 12): đồ mang
 * số lượng của bản ghi đầu tiên của nó (Tiếp nhận, hoặc bản ghi của lệnh tách ra nó) trừ số lượng bản ghi đầu tiên của
 * các đồ tách trực tiếp từ nó. Nhờ vậy tổng các đồ luôn bằng số lượng Tiếp nhận (sau đính chính).
 */
public final class BelongingAmounts {

    private BelongingAmounts() {}

    /** Một đồ: {@code parentId} là đồ nó được tách ra; {@code base} số lượng bản ghi đầu tiên hiện hành. */
    public record Node(long id, Long parentId, BigDecimal base) {}

    public static Map<Long, BigDecimal> of(List<Node> nodes) {
        Map<Long, BigDecimal> amounts = new HashMap<>();
        for (Node node : nodes) {
            amounts.put(node.id(), node.base());
        }
        for (Node node : nodes) {
            if (node.parentId() != null && amounts.containsKey(node.parentId())) {
                amounts.merge(node.parentId(), node.base(), BigDecimal::subtract);
            }
        }
        return amounts;
    }
}
