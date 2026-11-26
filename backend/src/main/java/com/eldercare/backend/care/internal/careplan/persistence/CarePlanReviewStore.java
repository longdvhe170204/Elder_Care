package com.eldercare.backend.care.internal.careplan.persistence;

import com.eldercare.backend.common.time.BusinessTime;
import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Yêu cầu xem xét kế hoạch chăm sóc {@code care_plan_review} (FR-058, Q-448). */
@Repository
public class CarePlanReviewStore {
    private final JdbcTemplate jdbc;

    public CarePlanReviewStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Mở yêu cầu; cùng người cao tuổi, nguồn và mã tham chiếu đã có thì bỏ qua. */
    public void open(long residentId, String source, long sourceId, String reference, Instant raisedAt) {
        jdbc.update(
                "INSERT IGNORE INTO care_plan_review (resident_id, source, source_id, reference, raised_at, status)"
                        + " VALUES (?, ?, ?, ?, ?, 'OPEN')",
                residentId,
                source,
                sourceId,
                reference,
                LocalDateTime.ofInstant(raisedAt, BusinessTime.ZONE));
    }
}
