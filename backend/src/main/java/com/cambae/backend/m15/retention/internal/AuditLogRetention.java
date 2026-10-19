package com.cambae.backend.m15.retention.internal;

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
 * ({@code resident_id}) tính hạn từ ngày kết thúc lưu trú, chưa có giao diện của 001 cung cấp nên chưa bao giờ hết hạn
 * (plan 000, D5). Bản tóm tắt đợt loại bỏ không bao giờ hết hạn (FR-051).
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

    @Override
    public List<Attached> attachedTo(DataRef record, Instant cutoff) {
        return jdbc.query(
                "SELECT id, occurred_at, resident_id IS NULL AND NOT exempt_from_purge AS purgeable FROM audit_log"
                        + " WHERE entity_type = ? AND entity_id = ?",
                (rs, i) -> {
                    Instant at =
                            rs.getObject("occurred_at", LocalDateTime.class).toInstant(ZoneOffset.UTC);
                    return new Attached(rs.getLong("id"), at, rs.getBoolean("purgeable") && at.isBefore(cutoff));
                },
                record.dataType(),
                String.valueOf(record.id()));
    }

    @Override
    public void delete(Collection<Long> ids) {
        database.jdbc().update("DELETE FROM audit_log WHERE id IN (" + Ids.inList(ids) + ")");
    }
}
