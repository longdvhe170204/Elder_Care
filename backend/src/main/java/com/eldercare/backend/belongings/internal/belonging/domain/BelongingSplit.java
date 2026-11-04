package com.eldercare.backend.belongings.internal.belonging.domain;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Tách đồ khi bàn giao ({@code TachDo}; research K-04, FR-012, FR-013): phần còn lại giữ trạng thái cũ, phần bàn giao
 * mang trạng thái đích, phần đếm thiếu Báo thất lạc trong cùng lệnh. Tổng ba phần bằng số lượng hiện có (SC-001).
 */
public final class BelongingSplit {

    private BelongingSplit() {}

    /** Ba phần của đồ sau lệnh; đồ gốc giữ phần còn lại nếu có, nếu không thì phần bàn giao, nếu không nữa thì phần thiếu. */
    public record Plan(BigDecimal remaining, BigDecimal moved, BigDecimal missing) {

        /** Đồ gốc mang phần bàn giao (không có phần còn lại). */
        public boolean originalMoves() {
            return remaining.signum() == 0 && moved.signum() > 0;
        }

        /** Có hơn một phần khác 0 nên phải tạo đồ mới. */
        public boolean needsSplit() {
            int parts =
                    (remaining.signum() > 0 ? 1 : 0) + (moved.signum() > 0 ? 1 : 0) + (missing.signum() > 0 ? 1 : 0);
            return parts > 1;
        }
    }

    /**
     * @param handed số lượng bàn giao; {@code null} là toàn bộ
     * @param counted số lượng người nhận đếm được; {@code null} là bằng số lượng bàn giao
     * @throws IllegalArgumentException khi số lượng bàn giao không trong (0, hiện có] hoặc số đếm ngoài [0, bàn giao]
     */
    public static Plan plan(BigDecimal current, BigDecimal handed, BigDecimal counted) {
        Objects.requireNonNull(current, "current");
        BigDecimal h = handed == null ? current : handed;
        if (h.signum() <= 0 || h.compareTo(current) > 0) {
            throw new IllegalArgumentException("Số lượng bàn giao phải lớn hơn 0 và không quá số lượng hiện có");
        }
        BigDecimal c = counted == null ? h : counted;
        if (c.signum() < 0 || c.compareTo(h) > 0) {
            throw new IllegalArgumentException("Số lượng đếm được phải từ 0 tới số lượng bàn giao");
        }
        return new Plan(current.subtract(h), c, h.subtract(c));
    }
}
