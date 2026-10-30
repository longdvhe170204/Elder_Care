package com.eldercare.backend.administration.retention.internal.service;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.shared.api.RecordTypeAliases;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Lưu giữ bản đính chính (000 T072; FR-050, FR-051a): theo CFG-M15-04 tính từ lúc ghi, đi kèm bản gốc. Xóa đính chính
 * thì xóa luôn dòng kỹ thuật {@code correction_chain} của bản gốc khi không còn đính chính nào.
 */
@Component
class CorrectionRetention implements AttachedRetention {

    static final String TYPE = "M15.CORRECTION";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final RecordTypeAliases aliases;

    CorrectionRetention(JdbcTemplate jdbc, RetentionDatabase database, RecordTypeAliases aliases) {
        this.jdbc = jdbc;
        this.database = database;
        this.aliases = aliases;
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
    public List<Attached> attachedTo(DataRef record, RetentionKind recordKind, Instant cutoff) {
        // Gồm đính chính ghi với loại cũ trước khi gộp bảng (research R-15 mục 5)
        var types = aliases.withLegacy(record.dataType());
        return jdbc.query(
                "SELECT id, recorded_at FROM correction WHERE record_type IN (" + Ids.placeholders(types.size())
                        + ") AND original_record_id = ?",
                (rs, i) -> {
                    Instant at =
                            rs.getObject("recorded_at", LocalDateTime.class).toInstant(ZoneOffset.UTC);
                    return new Attached(rs.getLong("id"), at, at.isBefore(cutoff));
                },
                java.util.stream.Stream.concat(types.stream(), java.util.stream.Stream.of(record.id()))
                        .toArray());
    }

    @Override
    public void delete(Collection<Long> ids) {
        String in = Ids.inList(ids);
        List<Object[]> originals = database.jdbc()
                .query(
                        "SELECT DISTINCT record_type, original_record_id FROM correction WHERE id IN (" + in + ")",
                        (rs, i) -> new Object[] {rs.getString(1), rs.getLong(2)});
        database.jdbc().update("DELETE FROM correction WHERE id IN (" + in + ")");
        for (Object[] original : originals) {
            database.jdbc()
                    .update(
                            "DELETE FROM correction_chain WHERE record_type = ? AND original_record_id = ?"
                                    + " AND NOT EXISTS (SELECT 1 FROM correction c WHERE c.record_type = ?"
                                    + " AND c.original_record_id = ?)",
                            original[0],
                            original[1],
                            original[0],
                            original[1]);
        }
    }
}
