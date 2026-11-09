package com.eldercare.backend.incident.internal.governance.persistence;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.SharedRowRetention;
import com.eldercare.backend.resident.api.ResidentApi;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Lưu giữ danh sách tiếp xúc ({@code simple_record} {@code M05.CONTACT_LIST} gắn sự cố; người trong danh sách là
 * {@code M05.CONTACT_PERSON} gắn danh sách) theo data-model 007 mục 14: theo sự cố, và khi người tiếp xúc là người cao
 * tuổi khác thì giữ tới khi cả chuỗi hết hạn. Hồ sơ của người tiếp xúc trỏ tới danh sách ({@link #referencing}) nên
 * không bị xóa trước; danh sách kéo sự cố vào cùng chuỗi ({@link #referencesTo}).
 */
public class ContactListRetention implements RetentionProvider {
    public static final String TYPE = "M05.CONTACT_LIST";
    private static final String PERSON = "M05.CONTACT_PERSON";

    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ObjectProvider<ResidentApi> residents;

    public ContactListRetention(JdbcTemplate jdbc, RetentionDatabase database, ObjectProvider<ResidentApi> residents) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    @Override
    public String dataType() {
        return TYPE;
    }

    @Override
    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return expiredCandidates(cutoff, null, batch);
    }

    @Override
    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        if (batch <= 0) return Stream.empty();
        Map<Long, Instant> expired = ExpiredResidents.before(residents, cutoff);
        Map<Long, Instant> lists = new HashMap<>();
        for (String in : ExpiredResidents.chunks(expired.keySet())) {
            jdbc.query(
                    "SELECT l.id, i.resident_id FROM simple_record l JOIN incident i ON i.id = l.entity_id"
                            + " WHERE l.kind = ? AND l.entity_type = 'M05.INCIDENT' AND i.resident_id IN (" + in + ")",
                    rs -> {
                        lists.put(rs.getLong(1), expired.get(rs.getLong(2)));
                    },
                    TYPE);
        }
        List<Candidate> candidates = new ArrayList<>();
        lists.forEach((listId, at) -> {
            Instant latest = at;
            for (Long person : jdbc.queryForList(
                    "SELECT resident_id FROM simple_record WHERE kind = ? AND entity_type = ? AND entity_id = ?"
                            + " AND resident_id IS NOT NULL",
                    Long.class,
                    PERSON,
                    TYPE,
                    listId)) {
                Instant personAt = expired.get(person);
                if (personAt == null) return;
                if (personAt.isAfter(latest)) latest = personAt;
            }
            candidates.add(new Candidate(listId, latest));
        });
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    /** Danh sách có người tiếp xúc là người cao tuổi {@code target} (FR-051a). */
    @Override
    public Set<DataRef> referencing(DataRef target) {
        if (!SharedRowRetention.RESIDENT.equals(target.dataType())) return Set.of();
        return jdbc
                .queryForList(
                        "SELECT DISTINCT entity_id FROM simple_record WHERE kind = ? AND entity_type = ?"
                                + " AND resident_id = ?",
                        Long.class,
                        PERSON,
                        TYPE,
                        target.id())
                .stream()
                .map(id -> new DataRef(TYPE, id))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<DataRef> referencesTo(long id) {
        return jdbc
                .queryForList(
                        "SELECT entity_id FROM simple_record WHERE kind = ? AND id = ? AND entity_type = 'M05.INCIDENT'",
                        Long.class,
                        TYPE,
                        id)
                .stream()
                .map(incident -> new DataRef(IncidentRetentionTypes.INCIDENT, incident))
                .collect(Collectors.toSet());
    }

    @Override
    public void delete(Collection<Long> ids) {
        if (ids.isEmpty()) return;
        String in = ids.stream().map(String::valueOf).collect(Collectors.joining(",", "(", ")"));
        database.jdbc().update("DELETE FROM simple_record WHERE kind = ? AND id IN " + in, TYPE);
    }
}
