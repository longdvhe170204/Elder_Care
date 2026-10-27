package com.cambae.backend.m15.retention.internal.service;

import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ nhật ký (000 T072; FR-050): theo CFG-M15-04, đi kèm bản ghi của đối tượng. Dòng gắn hồ sơ người cao tuổi
 * ({@code resident_id}) chỉ hết hạn cùng bản ghi tính hạn theo hồ sơ (plan 000, D5); đi kèm bản ghi không gắn hồ sơ thì
 * giữ. Bản tóm tắt đợt loại bỏ không bao giờ hết hạn (FR-051).
 */
@Component
class AuditLogRetention implements AttachedRetention {

    static final String TYPE = "M15.AUDIT_LOG";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;

    AuditLogRetention(JdbcTemplate jdbc, RetentionDatabase database) {
        this.jdbc = jdbc;
        this.database = database;
    }

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.NotResidentLinked();
    }

    /**
     * Dòng nhật ký hết hạn khi qua CFG-M15-04 và không nằm trong diện miễn; dòng gắn hồ sơ còn cần hồ sơ đã hết hạn (FR-050),
     * tức bản ghi đi kèm tính hạn theo hồ sơ ({@code ResidentRecord}) và đã hết hạn.
     */
    @Override
    public List<Attached> attachedTo(DataRef record, RetentionKind recordKind, Instant cutoff) {
        boolean residentExpired = recordKind instanceof RetentionKind.ResidentRecord;
        return jdbc.query(
                "SELECT id, occurred_at, (resident_id IS NULL OR ?) AND NOT exempt_from_purge AS purgeable FROM audit_log"
                        + " WHERE entity_type = ? AND entity_id = ?",
                (rs, i) -> {
                    Instant at =
                            rs.getObject("occurred_at", LocalDateTime.class).toInstant(ZoneOffset.UTC);
                    return new Attached(rs.getLong("id"), at, rs.getBoolean("purgeable") && at.isBefore(cutoff));
                },
                residentExpired,
                record.dataType(),
                String.valueOf(record.id()));
    }

    @Override
    public void delete(Collection<Long> ids) {
        database.jdbc().update("DELETE FROM audit_log WHERE id IN (" + Ids.inList(ids) + ")");
    }
}
