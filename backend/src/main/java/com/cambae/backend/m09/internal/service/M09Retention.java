package com.cambae.backend.m09.internal.service;

import com.cambae.backend.common.time.BusinessTime;
import com.cambae.backend.m15.account.api.AccountApi;
import com.cambae.backend.m15.account.api.ProfessionalActivityApi;
import com.cambae.backend.m15.retention.api.RetentionDatabase;
import com.cambae.backend.m15.retention.api.RetentionKind;
import com.cambae.backend.m15.retention.api.spi.RetentionProvider;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;

/** T009, R-11: provider cho bảng đã có; chỉ tài khoản lưu giữ thực hiện xoá, giữ các bản ghi còn dùng. */
public class M09Retention implements RetentionProvider {
    public enum Kind {
        STAFF_IMPORT_BATCH("M09.STAFF_IMPORT_BATCH", "staff_import_batch", "created_at"),
        STAFF("M09.STAFF", "staff_profile", "termination_applied_at"),
        CREDENTIAL("M09.CREDENTIAL", "credential", "created_at"),
        DEPARTMENT("M09.DEPARTMENT", "department", "created_at"),
        CREDENTIAL_TYPE("M09.CREDENTIAL_TYPE", "credential_type", "created_at"),
        PRACTICE_SCOPE("M09.PRACTICE_SCOPE", "practice_scope", "created_at"),
        STAFF_DEPARTMENT("M09.STAFF_DEPARTMENT", "staff_department", "ends_at"),
        DEPARTMENT_LEAD_HISTORY("M09.DEPARTMENT_LEAD_HISTORY", "department_lead_history", "ends_at"),
        SHIFT_TEMPLATE("M09.SHIFT_TEMPLATE", "shift_template", "created_at"),
        SCHEDULE("M09.SCHEDULE", "shift_schedule", "created_at"),
        SHIFT("M09.SHIFT", "work_shift", "ends_at"),
        SHIFT_STAFF("M09.SHIFT_STAFF", "shift_staff", ""),
        SHIFT_LEAD_HISTORY("M09.SHIFT_LEAD_HISTORY", "shift_lead_history", "ends_at"),
        SHIFT_PUBLICATION("M09.SHIFT_PUBLICATION", "shift_publication", "published_at"),
        FLOOR_MANAGER_ASSIGNMENT("M09.FLOOR_MANAGER_ASSIGNMENT", "floor_manager_assignment", "ends_at"),
        ASSIGNMENT("M09.ASSIGNMENT", "staff_assignment", "COALESCE(actual_end_at,ends_at)");
        final String type;
        final String table;
        final String at;

        Kind(String type, String table, String at) {
            this.type = type;
            this.table = table;
            this.at = at;
        }
    }

    private final Kind kind;
    private final JdbcTemplate jdbc;
    private final RetentionDatabase database;
    private final BusinessTime time;
    private final AccountApi accounts;
    private final ProfessionalActivityApi activities;

    public M09Retention(
            Kind kind,
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities) {
        this.kind = kind;
        this.jdbc = jdbc;
        this.database = database;
        this.time = time;
        this.accounts = accounts;
        this.activities = activities;
    }

    public String dataType() {
        return kind.type;
    }

    public RetentionKind retentionKind() {
        return new RetentionKind.NotResidentLinked();
    }

    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        if (kind == Kind.SHIFT_STAFF)
            return jdbc
                    .query(
                            "SELECT m.id,s.ends_at AS at FROM shift_staff m JOIN work_shift s ON s.id=m.shift_id WHERE s.status IN ('CLOSED','CANCELLED') AND s.ends_at<? ORDER BY s.ends_at,m.id LIMIT ?",
                            (r, i) -> new Candidate(
                                    r.getLong("id"),
                                    r.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                            LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC),
                            batch)
                    .stream();
        String extra = switch (kind) {
            case STAFF -> " AND status='RESIGNED'";
            case CREDENTIAL ->
                " AND (expiry_date < ? OR revoked_from <= ? OR EXISTS(SELECT 1 FROM credential replacement WHERE replacement.replaces_id=credential.id))";
            case DEPARTMENT, CREDENTIAL_TYPE, PRACTICE_SCOPE, SHIFT_TEMPLATE -> " AND status='INACTIVE'";
            case SHIFT -> " AND status IN ('CLOSED','CANCELLED')";
            case SCHEDULE ->
                " AND NOT EXISTS(SELECT 1 FROM work_shift s WHERE s.schedule_id=shift_schedule.id AND s.status NOT IN ('CLOSED','CANCELLED'))";
            case SHIFT_PUBLICATION ->
                " AND NOT EXISTS(SELECT 1 FROM work_shift s WHERE s.schedule_id=shift_publication.schedule_id AND s.status NOT IN ('CLOSED','CANCELLED'))";
            case FLOOR_MANAGER_ASSIGNMENT -> " AND status='ENDED'";
            case ASSIGNMENT ->
                " AND status IN ('ENDED','CANCELLED') AND NOT EXISTS(SELECT 1 FROM work_shift s WHERE s.id=staff_assignment.shift_id AND s.status NOT IN ('CLOSED','CANCELLED'))";
            case STAFF_IMPORT_BATCH, STAFF_DEPARTMENT, DEPARTMENT_LEAD_HISTORY, SHIFT_LEAD_HISTORY, SHIFT_STAFF -> "";
        };
        var args = new ArrayList<Object>();
        args.add(LocalDateTime.ofInstant(cutoff, ZoneOffset.UTC));
        if (kind == Kind.CREDENTIAL) {
            args.add(time.today());
            args.add(time.today());
        }
        args.add(batch);
        return jdbc
                .query(
                        "SELECT id," + kind.at + " AS at FROM " + kind.table + " WHERE " + kind.at + " < ?" + extra
                                + " ORDER BY " + kind.at + ",id LIMIT ?",
                        (r, i) -> new Candidate(
                                r.getLong("id"),
                                r.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        args.toArray())
                .stream()
                .filter(c -> {
                    if (kind == Kind.PRACTICE_SCOPE) return !activities.usesPracticeScope(c.id());
                    if (kind == Kind.CREDENTIAL_TYPE) {
                        String code = jdbc.queryForObject(
                                "SELECT code FROM credential_type WHERE id=?", String.class, c.id());
                        return !activities.usesCredentialType(code)
                                && !activities.usesCredentialType(Long.toString(c.id()));
                    }
                    return true;
                });
    }

    public Set<DataRef> referencesTo(long id) {
        Set<DataRef> refs = new LinkedHashSet<>();
        switch (kind) {
            case STAFF_IMPORT_BATCH ->
                refs(refs, "M09.STAFF", "SELECT id FROM staff_profile WHERE import_batch_id=?", id);
            case STAFF -> {
                refs(refs, "M09.HANDOVER", "SELECT id FROM shift_handover WHERE handover_staff_id=?", id);
                refs(refs, "M09.CREDENTIAL", "SELECT id FROM credential WHERE staff_id=?", id);
                refs(refs, "M09.STAFF_DEPARTMENT", "SELECT id FROM staff_department WHERE staff_id=?", id);
                refs(
                        refs,
                        "M09.DEPARTMENT_LEAD_HISTORY",
                        "SELECT id FROM department_lead_history WHERE staff_id=?",
                        id);
                refs(refs, "M09.DEPARTMENT", "SELECT id FROM department WHERE lead_staff_id=?", id);
                refs(refs, "M09.SHIFT_STAFF", "SELECT id FROM shift_staff WHERE staff_id=?", id);
                refs(refs, "M09.SHIFT_LEAD_HISTORY", "SELECT id FROM shift_lead_history WHERE staff_id=?", id);
                refs(refs, "M09.SHIFT", "SELECT id FROM work_shift WHERE lead_staff_id=?", id);
                refs(refs, "M09.ASSIGNMENT", "SELECT id FROM staff_assignment WHERE staff_id=?", id);
                refs(
                        refs,
                        "M09.FLOOR_MANAGER_ASSIGNMENT",
                        "SELECT id FROM floor_manager_assignment WHERE staff_id=?",
                        id);
                accounts.byStaff(id).ifPresent(a -> refs.add(new DataRef("M15.ACCOUNT", a.accountId())));
            }
            case CREDENTIAL -> refs(refs, "M09.CREDENTIAL", "SELECT id FROM credential WHERE replaces_id=?", id);
            case DEPARTMENT -> {
                refs(refs, "M09.STAFF_DEPARTMENT", "SELECT id FROM staff_department WHERE department_id=?", id);
                refs(
                        refs,
                        "M09.DEPARTMENT_LEAD_HISTORY",
                        "SELECT id FROM department_lead_history WHERE department_id=?",
                        id);
            }
            case CREDENTIAL_TYPE ->
                refs(refs, "M09.CREDENTIAL", "SELECT id FROM credential WHERE credential_type_id=?", id);
            case PRACTICE_SCOPE ->
                refs(
                        refs,
                        "M09.CREDENTIAL",
                        "SELECT credential_id FROM credential_practice_scope WHERE practice_scope_id=?",
                        id);
            case SHIFT_TEMPLATE -> refs(refs, "M09.SHIFT", "SELECT id FROM work_shift WHERE template_id=?", id);
            case SCHEDULE -> {
                refs(refs, "M09.SHIFT", "SELECT id FROM work_shift WHERE schedule_id=?", id);
                refs(refs, "M09.SHIFT_PUBLICATION", "SELECT id FROM shift_publication WHERE schedule_id=?", id);
            }
            case SHIFT -> {
                jdbc.queryForList(
                                "SELECT id FROM shift_handover WHERE shift_id=? OR next_shift_id=?", Long.class, id, id)
                        .forEach(found -> refs.add(new DataRef("M09.HANDOVER", found)));
                refs(refs, "M09.SHIFT_STAFF", "SELECT id FROM shift_staff WHERE shift_id=?", id);
                refs(refs, "M09.SHIFT_LEAD_HISTORY", "SELECT id FROM shift_lead_history WHERE shift_id=?", id);
                refs(refs, "M09.ASSIGNMENT", "SELECT id FROM staff_assignment WHERE shift_id=?", id);
            }
            default -> {}
        }
        return refs;
    }

    private void refs(Set<DataRef> refs, String type, String query, long id) {
        jdbc.queryForList(query, Long.class, id).forEach(found -> refs.add(new DataRef(type, found)));
    }

    public void delete(Collection<Long> ids) {
        if (ids.isEmpty()) return;
        String in = "(" + String.join(",", Collections.nCopies(ids.size(), "?")) + ")";
        Object[] values = ids.toArray();
        var retention = database.jdbc();
        if (kind == Kind.CREDENTIAL)
            retention.update("DELETE FROM credential_practice_scope WHERE credential_id IN " + in, values);
        retention.update("DELETE FROM " + kind.table + " WHERE id IN " + in, values);
    }
}
