package com.eldercare.backend.nutrition.internal.catalog.domain;

/**
 * Bảng chuyển trạng thái món ăn (data-model mục 3; bảng trạng thái món của spec). Hàm thuần: điều kiện nội dung (thành
 * phần, chế độ ăn phù hợp, chưa từng Hiệu lực, món an toàn) do service kiểm.
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

    public static Outcome apply(DishStatus from, boolean hasPendingRevision, DishCommand command) {
        return switch (command) {
            case SUBMIT -> from == DishStatus.DRAFT ? Outcome.to(DishStatus.PENDING_CONFIRMATION) : changed();
            case WITHDRAW, RETURN -> from == DishStatus.PENDING_CONFIRMATION ? Outcome.to(DishStatus.DRAFT) : changed();
            case CONFIRM -> from == DishStatus.PENDING_CONFIRMATION ? Outcome.to(DishStatus.ACTIVE) : changed();
            case CANCEL -> from == DishStatus.DRAFT ? Outcome.to(DishStatus.CANCELLED) : changed();
            case SUBMIT_REVISION -> {
                if (from != DishStatus.ACTIVE) {
                    yield changed();
                }
                yield hasPendingRevision ? Outcome.blocked("REVISION_EXISTS") : Outcome.to(DishStatus.ACTIVE);
            }
            case CONFIRM_REVISION, RETURN_REVISION, WITHDRAW_REVISION ->
                from == DishStatus.ACTIVE && hasPendingRevision ? Outcome.to(DishStatus.ACTIVE) : changed();
            case DEACTIVATE -> from == DishStatus.ACTIVE ? Outcome.to(DishStatus.INACTIVE) : changed();
        };
    }

    private static Outcome changed() {
        return Outcome.blocked("STATE_CHANGED");
    }
}
