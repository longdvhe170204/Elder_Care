package com.eldercare.backend.billing.internal.period.persistence;

import com.eldercare.backend.billing.internal.period.domain.CostJobCadence;
import java.time.Instant;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Chỉ đọc marker có tiền tố M11; ghi qua ReminderMarkers của A. */
@Repository
public class CostJobMarkers {
    private final JdbcTemplate jdbc;

    public CostJobMarkers(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public static String entityType(String task) {
        return "M11.JOB." + task;
    }

    public Optional<Instant> completed(String task, CostJobCadence cadence) {
        return Optional.ofNullable(jdbc.queryForObject(
                        "SELECT MAX(marker) FROM reminder_marker WHERE kind='M11.JOB_DONE' AND entity_type=? AND entity_id=?",
                        String.class,
                        entityType(task),
                        cadence.markerId()))
                .map(Instant::parse);
    }

    public Instant first(String task, CostJobCadence cadence) {
        return Instant.parse(jdbc.queryForObject(
                "SELECT JSON_UNQUOTE(JSON_EXTRACT(details,'$.slot')) FROM reminder_marker WHERE kind='M11.JOB_ANCHOR' AND entity_type=? AND entity_id=? AND marker='FIRST'",
                String.class,
                entityType(task),
                cadence.markerId()));
    }
}
