package com.eldercare.backend.staffing.handover.internal.service;

import com.eldercare.backend.administration.parameter.api.*;
import com.eldercare.backend.administration.retention.api.*;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.staffing.shift.internal.persistence.ShiftStore;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** FR-047: giữ cả bàn giao tới khi mọi hồ sơ liên quan đã qua hạn; xóa con trước cha bằng tài khoản lưu giữ. */
@Component
public class HandoverRetention implements RetentionProvider {
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ObjectProvider<ResidentApi> residents;
    private final ParameterApi parameters;
    private final BusinessTime time;

    public HandoverRetention(
            JdbcTemplate jdbc,
            RetentionDatabase database,
            ObjectProvider<ResidentApi> residents,
            ParameterApi parameters,
            BusinessTime time) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
        this.parameters = parameters;
        this.time = time;
    }

    public String dataType() {
        return "M09.HANDOVER";
    }

    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        Instant unlinkedCutoff = time.now()
                .atZone(BusinessTime.ZONE)
                .minus(parameters.read(ParameterCode.CFG_M15_04).asPeriod())
                .toInstant();
        var candidates = jdbc.query(
                "SELECT h.id,h.created_at FROM shift_handover h JOIN work_shift s ON s.id=h.shift_id WHERE s.ends_at<=? ORDER BY h.created_at,h.id",
                (r, n) -> new Candidate(
                        r.getLong(1), r.getObject(2, LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                ShiftStore.sql(time.now()));
        return candidates.stream()
                .map(c -> {
                    var ids = jdbc.queryForList(
                            "SELECT resident_id FROM handover_item WHERE handover_id=? AND resident_id IS NOT NULL UNION SELECT resident_id FROM handover_assessment WHERE handover_id=? AND resident_id IS NOT NULL",
                            Long.class,
                            c.id(),
                            c.id());
                    if (ids.isEmpty()) return c.at().isBefore(unlinkedCutoff) ? c : null;
                    var api = residents.getIfAvailable();
                    if (api == null) return null;
                    Instant latest = null;
                    for (long id : ids) {
                        if (!api.isFinalStatus(id)) return null;
                        var at = api.finalStatusAt(id);
                        if (at.isEmpty() || !at.get().isBefore(cutoff)) return null;
                        if (latest == null || at.get().isAfter(latest)) latest = at.get();
                    }
                    return new Candidate(c.id(), latest);
                })
                .filter(Objects::nonNull)
                .limit(batch);
    }

    public Set<DataRef> referencesTo(long id) {
        return Set.of();
    }

    public void delete(Collection<Long> ids) {
        var db = database.jdbc();
        for (long id : ids) {
            db.update(
                    "DELETE r FROM handover_reply r JOIN handover_comment c ON c.id=r.comment_id WHERE c.handover_id=?",
                    id);
            db.update("DELETE FROM handover_comment WHERE handover_id=?", id);
            db.update("DELETE FROM handover_item WHERE handover_id=?", id);
            db.update("DELETE FROM handover_assessment WHERE handover_id=?", id);
            db.update("DELETE FROM shift_handover WHERE id=?", id);
        }
    }
}
