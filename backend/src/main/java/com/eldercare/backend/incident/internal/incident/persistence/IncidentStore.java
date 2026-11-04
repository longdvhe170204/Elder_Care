package com.eldercare.backend.incident.internal.incident.persistence;

import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.domain.SuspectedIncident;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Nguồn sự cố đã ghi nhận, bao gồm dấu nghi nhiễm tính tại thời điểm truy vấn. */
@Repository
public class IncidentStore {
    private final JdbcTemplate jdbc;

    public IncidentStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean suspectedAt(long residentId, Instant at) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("""
                SELECT EXISTS (
                    SELECT 1 FROM incident
                    WHERE resident_id = ? AND type_code = 'SUSPECTED_INFECTION'
                      AND created_at <= ? AND status <> 'CANCELLED'
                      AND (closed_at IS NULL OR ? < closed_at)
                )
                """, Boolean.class, residentId, Timestamp.from(at), Timestamp.from(at)));
    }

    public Set<Long> lockOpenSuspected(Set<Long> ids) {
        if (ids.isEmpty()) return Set.of();
        String placeholders = String.join(",", Collections.nCopies(ids.size(), "?"));
        return new HashSet<>(jdbc.queryForList("""
                SELECT id FROM incident WHERE id IN (%s)
                  AND type_code = 'SUSPECTED_INFECTION'
                  AND status IN ('NEW','IN_PROGRESS','MONITORING')
                ORDER BY id FOR UPDATE
                """.formatted(placeholders), Long.class, ids.toArray()));
    }

    public Optional<SuspectedIncident> lockSuspected(long id) {
        return jdbc.query(
                """
                SELECT id,resident_id,discovered_at,status FROM incident
                WHERE id=? AND type_code='SUSPECTED_INFECTION' FOR UPDATE
                """,
                rs -> rs.next()
                        ? Optional.of(new SuspectedIncident(
                                rs.getLong("id"),
                                rs.getLong("resident_id"),
                                rs.getTimestamp("discovered_at").toInstant(),
                                rs.getString("status")))
                        : Optional.empty(),
                id);
    }

    public Optional<IncidentState> lock(long id) {
        return jdbc.query(
                """
                SELECT id,resident_id,floor_area_id,status,level,was_emergency,version
                FROM incident WHERE id=? FOR UPDATE
                """,
                rs -> rs.next()
                        ? Optional.of(new IncidentState(
                                rs.getLong("id"),
                                (Long) rs.getObject("resident_id"),
                                (Long) rs.getObject("floor_area_id"),
                                rs.getString("status"),
                                IncidentLevel.valueOf(rs.getString("level")),
                                rs.getBoolean("was_emergency"),
                                rs.getInt("version")))
                        : Optional.empty(),
                id);
    }

    public void changeLevel(long id, IncidentLevel level, boolean wasEmergency, int expectedVersion) {
        int changed = jdbc.update("""
                UPDATE incident SET level=?, was_emergency=?, version=version+1
                WHERE id=? AND version=?
                """, level.name(), wasEmergency, id, expectedVersion);
        if (changed != 1) throw new IllegalStateException("Sự cố đã thay đổi đồng thời");
    }
}
