package com.cambae.backend.m15.retention;

import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import com.cambae.backend.m15.sample.SampleFloorRecordProvider;
import com.cambae.backend.m15.sample.SampleOfficeRecordProvider;
import com.cambae.backend.support.SwitchableUser;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Provider lưu trữ cho hai bảng mẫu nhóm 3 (000 T066): mốc tính hạn là {@code occurred_at}, không gắn hồ sơ
 * (CFG-M15-04). Test đặt tham chiếu giữa bản ghi (FR-051a) và giả lập dừng giữa chừng khi xóa một bản ghi (BR-M15-13).
 */
@TestConfiguration
public class RetentionFixtures {

    /** Tham chiếu và lỗi giả lập do test đặt. */
    public static class Control {
        final Map<RetentionProvider.DataRef, Set<RetentionProvider.DataRef>> references = new HashMap<>();
        final Set<Long> failOnDelete = new HashSet<>();

        public void reference(RetentionProvider.DataRef from, RetentionProvider.DataRef to) {
            references.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        }

        public void failOnDelete(long id) {
            failOnDelete.add(id);
        }

        public void clear() {
            references.clear();
            failOnDelete.clear();
        }
    }

    static class SampleTableRetention implements RetentionProvider {

        private final String type;
        private final String table;
        private final JdbcTemplate jdbc;
        private final RetentionDatabase database;
        private final Control control;

        SampleTableRetention(
                String type, String table, JdbcTemplate jdbc, RetentionDatabase database, Control control) {
            this.type = type;
            this.table = table;
            this.jdbc = jdbc;
            this.database = database;
            this.control = control;
        }

        @Override
        public String dataType() {
            return type;
        }

        @Override
        public RetentionKind retentionKind() {
            return new RetentionKind.NotResidentLinked();
        }

        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
            return jdbc
                    .query(
                            "SELECT id, occurred_at FROM " + table + " WHERE occurred_at < ? ORDER BY id LIMIT ?",
                            (rs, i) -> new Candidate(
                                    rs.getLong(1),
                                    rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                            LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                            batch)
                    .stream();
        }

        @Override
        public Set<DataRef> referencesTo(long id) {
            return control.references.getOrDefault(new DataRef(type, id), Set.of());
        }

        @Override
        public void delete(Collection<Long> ids) {
            if (ids.stream().anyMatch(control.failOnDelete::contains)) {
                throw new IllegalStateException("Giả lập dừng giữa chừng khi xóa " + ids);
            }
            String in = ids.stream().map(String::valueOf).collect(Collectors.joining(","));
            database.jdbc().update("DELETE FROM " + table + " WHERE id IN (" + in + ")");
        }
    }

    @Bean
    SwitchableUser currentUser() {
        return new SwitchableUser();
    }

    @Bean
    Control retentionControl() {
        return new Control();
    }

    @Bean
    RetentionProvider sampleFloorRecordRetention(JdbcTemplate jdbc, RetentionDatabase database, Control control) {
        return new SampleTableRetention(SampleFloorRecordProvider.TYPE, "sample_floor_record", jdbc, database, control);
    }

    @Bean
    RetentionProvider sampleOfficeRecordRetention(JdbcTemplate jdbc, RetentionDatabase database, Control control) {
        return new SampleTableRetention(
                SampleOfficeRecordProvider.TYPE, "sample_office_record", jdbc, database, control);
    }
}
