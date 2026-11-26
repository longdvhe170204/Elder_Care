package com.eldercare.backend.care.internal.careplan.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

/**
 * Yêu cầu xem xét còn Đang mở (FR-058, Q-448): mốc phát sinh R còn mở khi chưa có phiên bản nào đã được duyệt
 * ({@code approvedAt} có giá trị, kể cả bản sau đó bị thay) lập sau R, và chưa có lần "Không cần thay đổi" từ R trở đi. Phiên
 * bản bị trả lại, bị bỏ trước khi duyệt hoặc lập trước R không đóng R.
 */
public final class CarePlanReviewRule {
    private CarePlanReviewRule() {}

    public static List<CarePlanReviewRaised> open(
            Collection<CarePlanReviewRaised> raised, Collection<CarePlan> plans, Collection<Instant> dismissals) {
        return raised.stream()
                .filter(r -> plans.stream()
                        .noneMatch(p -> p.approvedAt() != null && p.createdAt().isAfter(r.raisedAt())))
                .filter(r -> dismissals.stream().noneMatch(d -> !d.isBefore(r.raisedAt())))
                .toList();
    }
}
