package com.eldercare.backend.billing.internal.purchase.persistence;

import com.eldercare.backend.administration.retention.api.*;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.billing.internal.purchase.domain.PurchaseDetails;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Instant;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Hồ sơ mua hộ theo mốc hồ sơ người cao tuổi; bảng chung/tệp do framework xóa cùng chuỗi. */
@Repository
public class PurchaseRetention implements RetentionProvider {
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final ResidentApi residents;

    public PurchaseRetention(JdbcTemplate jdbc, RetentionDatabase database, ResidentApi residents) {
        this.jdbc = jdbc;
        this.database = database;
        this.residents = residents;
    }

    public String dataType() {
        return PurchaseDetails.KIND.code();
    }

    public RetentionKind retentionKind() {
        return new RetentionKind.ResidentRecord();
    }

    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        return expiredCandidates(cutoff, null, batch);
    }

    public Stream<Candidate> expiredCandidates(Instant cutoff, Candidate after, int batch) {
        if (batch <= 0) return Stream.empty();
        var candidates = new ArrayList<Candidate>();
        for (long resident : residents.byStatus(
                Set.of(ResidentStatus.RESIDENCY_ENDED, ResidentStatus.DECEASED, ResidentStatus.ADMISSION_CANCELLED))) {
            residents
                    .finalStatusAt(resident)
                    .filter(at -> at.isBefore(cutoff))
                    .ifPresent(at -> jdbc.queryForList(
                                    "SELECT id FROM simple_record WHERE kind=? AND resident_id=?",
                                    Long.class,
                                    dataType(),
                                    resident)
                            .forEach(id -> candidates.add(new Candidate(id, at))));
        }
        return RetentionProvider.page(candidates.stream(), after, batch);
    }

    public Set<DataRef> referencesTo(long id) {
        var result = new LinkedHashSet<DataRef>();
        jdbc.queryForList("SELECT id FROM cost_item WHERE purchase_assistance_id=?", Long.class, id)
                .forEach(item -> result.add(new DataRef("M11.COST_ITEM", item)));
        return result;
    }

    public Set<DataRef> referencing(DataRef target) {
        if (!target.dataType().equals("M01.RESIDENT")) return Set.of();
        var result = new LinkedHashSet<DataRef>();
        jdbc.queryForList(
                        "SELECT id FROM simple_record WHERE kind=? AND resident_id=?",
                        Long.class,
                        dataType(),
                        target.id())
                .forEach(id -> result.add(new DataRef(dataType(), id)));
        return result;
    }

    public void delete(Collection<Long> ids) {
        for (long id : new LinkedHashSet<>(ids))
            database.jdbc().update("DELETE FROM simple_record WHERE kind=? AND id=?", dataType(), id);
    }
}
