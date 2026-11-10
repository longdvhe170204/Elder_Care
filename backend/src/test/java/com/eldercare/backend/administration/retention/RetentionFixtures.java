package com.eldercare.backend.administration.retention;

import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.dto.ApprovalStatus;
import com.eldercare.backend.administration.approval.api.dto.ExtendedTransitions;
import com.eldercare.backend.administration.approval.api.dto.ResultRef;
import com.eldercare.backend.administration.approval.api.spi.RequestTypeHandler;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.administration.sample.SampleOfficeRecordProvider;
import com.eldercare.backend.common.error.UnmetCondition;
import com.eldercare.backend.support.SwitchableUser;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

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
        final Map<RetentionProvider.DataRef, Set<RetentionProvider.DataRef>> crossReferences = new HashMap<>();
        final Map<String, String> sharedTypes = new HashMap<>();

        public void reference(RetentionProvider.DataRef from, RetentionProvider.DataRef to) {
            references.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        }

        /**
         * {@code from} trỏ tới {@code to} mà chỉ provider của {@code from} biết (module khác, 000 T112): job tìm qua
         * {@link RetentionProvider#referencing}, không qua {@code referencesTo} của {@code to}.
         */
        public void crossReference(RetentionProvider.DataRef from, RetentionProvider.DataRef to) {
            crossReferences.computeIfAbsent(to, k -> new HashSet<>()).add(from);
        }

        /** Dòng bảng dùng chung của loại {@code type} ghi với loại đối tượng {@code entityType} (005 T064). */
        public void sharedAs(String type, String entityType) {
            sharedTypes.put(type, entityType);
        }

        public void failOnDelete(long id) {
            failOnDelete.add(id);
        }

        public void clear() {
            references.clear();
            failOnDelete.clear();
            crossReferences.clear();
            sharedTypes.clear();
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
        public String sharedRowEntityType() {
            return control.sharedTypes.getOrDefault(type, type);
        }

        @Override
        public RetentionKind retentionKind() {
            return new RetentionKind.NotResidentLinked();
        }

        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
            return expiredCandidates(cutoff, null, batch);
        }

        /** Lấy tiếp sau mốc {@code (at, id)} của ứng viên cuối trang trước (000 T112). */
        @Override
        public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
            LocalDateTime afterAt = after == null
                    ? LocalDateTime.of(1000, 1, 1, 0, 0)
                    : LocalDateTime.ofInstant(after.at(), ZoneOffset.UTC);
            long afterId = after == null ? 0 : after.id();
            return jdbc
                    .query(
                            "SELECT id, occurred_at FROM " + table + " WHERE occurred_at < ?"
                                    + " AND (occurred_at > ? OR (occurred_at = ? AND id > ?)) ORDER BY occurred_at, id LIMIT ?",
                            (rs, i) -> new Candidate(
                                    rs.getLong(1),
                                    rs.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                            LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                            afterAt,
                            afterAt,
                            afterId,
                            batch)
                    .stream();
        }

        @Override
        public Set<DataRef> referencing(DataRef target) {
            return control.crossReferences.getOrDefault(target, Set.of()).stream()
                    .filter(from -> from.dataType().equals(type))
                    .collect(Collectors.toSet());
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

    /**
     * Loại yêu cầu mẫu có trạng thái mở rộng (000 T114, như 015): {@code WAITING} còn mở vì là nguồn của {@code AGREE},
     * {@code EXPIRED} là trạng thái cuối riêng của loại.
     */
    public static final String EXTENDED_REQUEST = "M15.SAMPLE_EXTENDED";

    @Bean
    RequestTypeHandler sampleExtendedRequestHandler() {
        return new RequestTypeHandler() {
            @Override
            public String typeCode() {
                return EXTENDED_REQUEST;
            }

            @Override
            public ExtendedTransitions extendedTransitions() {
                return new ExtendedTransitions(Map.of(
                        "WAIT",
                        new ExtendedTransitions.Transition(Set.of(ApprovalStatus.PENDING_APPROVAL), "WAITING"),
                        "AGREE",
                        new ExtendedTransitions.Transition(Set.of("WAITING"), ApprovalStatus.PENDING_APPROVAL),
                        "EXPIRE",
                        new ExtendedTransitions.Transition(
                                Set.of("WAITING", ApprovalStatus.PENDING_APPROVAL), "EXPIRED")));
            }

            @Override
            public void validateContent(JsonNode content) {}

            @Override
            public PermissionKey createPermission() {
                throw new UnsupportedOperationException();
            }

            @Override
            public PermissionKey approvePermission() {
                throw new UnsupportedOperationException();
            }

            @Override
            public Target scope(JsonNode content) {
                throw new UnsupportedOperationException();
            }

            @Override
            public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
                return List.of();
            }

            @Override
            public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
                throw new UnsupportedOperationException();
            }
        };
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
