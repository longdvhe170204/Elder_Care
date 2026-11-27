package com.eldercare.backend.nutrition.internal.catalog.domain;

/**
 * Bảng chuyển trạng thái món ăn (spec 011 mục A, Q-445). Hàm thuần: điều kiện "chưa từng Hiệu lực" của lệnh hủy do service
 * kiểm.
 */
public final class DishTransitions {

    private DishTransitions() {}

    /** Kết quả: trạng thái kế tiếp, hoặc mã lỗi 409 khi bị chặn. */
    public record Outcome(DishStatus next, String errorCode) {
        public static Outcome to(DishStatus next) {
            return new Outcome(next, null);
        }

        public static Outcome blocked(String errorCode) {
            return new Outcome(null, errorCode);
        }

        public boolean allowed() {
            return next != null;
        }
    }

    public static Outcome apply(DishStatus from, DishCommand command) {
        return switch (command) {
            case ACTIVATE -> from == DishStatus.DRAFT ? Outcome.to(DishStatus.ACTIVE) : changed();
            case CANCEL -> from == DishStatus.DRAFT ? Outcome.to(DishStatus.CANCELLED) : changed();
            case DEACTIVATE -> from == DishStatus.ACTIVE ? Outcome.to(DishStatus.INACTIVE) : changed();
        };
    }

    private static Outcome changed() {
        return Outcome.blocked("STATE_CHANGED");
    }
}
