package com.eldercare.backend.staffing.internal.persistence;

import static com.eldercare.backend.staffing.internal.domain.StaffingCatalogData.*;
import static com.eldercare.backend.staffing.internal.domain.StaffingHistoryData.*;
import static com.eldercare.backend.staffing.internal.domain.StaffingRecordData.*;

import com.eldercare.backend.administration.account.api.AccountApi;
import com.eldercare.backend.administration.account.api.ProfessionalActivityApi;
import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.shared.api.*;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.error.NotFoundException;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.staffing.internal.domain.StaffingEventData;
import com.eldercare.backend.staffing.internal.domain.StaffingHistoryData.LeadDetails;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;

/** T050: đọc sự kiện M09 bằng SQL có giới hạn và API; chỉ xóa trên kết nối lưu giữ, giữ ID logic của audit. */
public class M09Retention implements RetentionProvider {
    public enum Kind {
        STAFFING_RATIO_CALCULATION("M09.STAFFING_RATIO_CALCULATION", "staffing_ratio_calculation", "calculated_at"),
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
        FLOOR_MANAGER_ASSIGNMENT("M09.FLOOR_MANAGER_ASSIGNMENT", "staff_assignment", "ends_at"),
        ASSIGNMENT("M09.ASSIGNMENT", "staff_assignment", "COALESCE(actual_end_at,ends_at)");
        final String type, table, at;

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
    private final CatalogApi catalogs;
    private final StatusHistoryApi histories;
    private final RecordEventApi events;
    private final SimpleRecordApi records;

    public M09Retention(
            Kind kind,
            JdbcTemplate jdbc,
            RetentionDatabase database,
            BusinessTime time,
            AccountApi accounts,
            ProfessionalActivityApi activities,
            CatalogApi catalogs,
            StatusHistoryApi histories,
            RecordEventApi events,
            SimpleRecordApi records) {
        this.kind = kind;
        this.jdbc = jdbc;
        this.database = database;
        this.time = time;
        this.accounts = accounts;
        this.activities = activities;
        this.catalogs = catalogs;
        this.histories = histories;
        this.events = events;
        this.records = records;
    }

    public String dataType() {
        return kind.type;
    }

    public RetentionKind retentionKind() {
        return new RetentionKind.NotResidentLinked();
    }

    public Stream<Candidate> expiredCandidates(Instant cutoff, int batch) {
        Stream<Candidate> stream;
        if (isCatalog())
            stream = catalogRows().stream()
                    .filter(c -> c.status() == CatalogStatus.INACTIVE)
                    .filter(c -> catalogAt(c) != null)
                    .map(c -> new Candidate(catalogId(c), catalogAt(c)))
                    .filter(c -> kind != Kind.PRACTICE_SCOPE || !activities.usesPracticeScope(c.id()))
                    .filter(c -> kind != Kind.CREDENTIAL_TYPE
                            || catalogRows().stream()
                                    .filter(r -> catalogId(r) == c.id())
                                    .noneMatch(r -> activities.usesCredentialType(catalogCode(r))
                                            || activities.usesCredentialType(Long.toString(c.id()))));
        else if (isHistory())
            stream = historyRows(kind == Kind.DEPARTMENT_LEAD_HISTORY).stream()
                    .map(Map.Entry::getValue)
                    .filter(h -> h.endsAt() != null)
                    .map(h -> new Candidate(historyId(h, kind == Kind.DEPARTMENT_LEAD_HISTORY), h.endsAt()));
        else if (kind == Kind.STAFF_IMPORT_BATCH)
            stream = records.listByStatus(IMPORT_BATCH, Set.of("RECORDED")).stream()
                    .map(r -> new Candidate(
                            Legacy.logicalId(r.id(), r.content().legacy(), "staff_import_batch"), r.createdAt()));
        else if (isEvent()) stream = eventCandidates(cutoff, batch);
        else stream = privateCandidates(cutoff);
        return stream.filter(c -> c.at().isBefore(cutoff))
                .sorted(Comparator.comparing(Candidate::at).thenComparingLong(Candidate::id))
                .limit(batch);
    }

    private Stream<Candidate> privateCandidates(Instant cutoff) {
        if (kind == Kind.SHIFT_STAFF)
            return jdbc
                    .query(
                            "SELECT m.id,s.ends_at AS at FROM shift_staff m JOIN work_shift s ON s.id=m.shift_id WHERE s.status IN ('CLOSED','CANCELLED') AND s.ends_at<?",
                            (r, i) -> new Candidate(
                                    r.getLong("id"),
                                    r.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                            sql(cutoff))
                    .stream();
        String extra = switch (kind) {
            case STAFF -> " AND status='RESIGNED'";
            case CREDENTIAL ->
                " AND (expiry_date < ? OR revoked_from <= ? OR EXISTS(SELECT 1 FROM credential replacement WHERE replacement.replaces_id=credential.id))";
            case SHIFT -> " AND status IN ('CLOSED','CANCELLED')";
            case SCHEDULE ->
                " AND NOT EXISTS(SELECT 1 FROM work_shift s WHERE s.schedule_id=shift_schedule.id AND s.status NOT IN ('CLOSED','CANCELLED'))";
            case FLOOR_MANAGER_ASSIGNMENT -> " AND kind='FLOOR_MANAGER' AND status='ENDED'";
            case ASSIGNMENT ->
                " AND kind='CARE' AND status IN ('ENDED','CANCELLED') AND NOT EXISTS(SELECT 1 FROM work_shift s WHERE s.id=staff_assignment.shift_id AND s.status NOT IN ('CLOSED','CANCELLED'))";
            default -> "";
        };
        var args = new ArrayList<Object>();
        args.add(sql(cutoff));
        if (kind == Kind.CREDENTIAL) {
            args.add(time.today());
            args.add(time.today());
        }
        String id = kind == Kind.FLOOR_MANAGER_ASSIGNMENT ? "COALESCE(legacy_floor_manager_id,id)" : "id";
        return jdbc
                .query(
                        "SELECT " + id + " AS logical_id," + kind.at + " AS at FROM " + kind.table + " WHERE " + kind.at
                                + "<?" + extra,
                        (r, i) -> new Candidate(
                                r.getLong("logical_id"),
                                r.getObject("at", LocalDateTime.class).toInstant(ZoneOffset.UTC)),
                        args.toArray())
                .stream();
    }

    public Set<DataRef> referencesTo(long id) {
        var refs = new LinkedHashSet<DataRef>();
        switch (kind) {
            case STAFF_IMPORT_BATCH ->
                refs(refs, "M09.STAFF", "SELECT id FROM staff_profile WHERE import_batch_id=?", importPhysical(id));
            case STAFF -> {
                refs(refs, "M09.HANDOVER", "SELECT id FROM shift_handover WHERE handover_staff_id=?", id);
                refs(refs, "M09.CREDENTIAL", "SELECT id FROM credential WHERE staff_id=?", id);
                refs(refs, "M09.STAFF_DEPARTMENT", "SELECT id FROM staff_department WHERE staff_id=?", id);
                for (var h : historyRows(true))
                    if (Objects.equals(h.getValue().valueId(), id))
                        refs.add(new DataRef("M09.DEPARTMENT_LEAD_HISTORY", historyId(h.getValue(), true)));
                for (var d : catalogs.list(DEPARTMENT, null, false))
                    if (Objects.equals(d.attributes().leadStaffId(), id))
                        refs.add(new DataRef(
                                "M09.DEPARTMENT",
                                Legacy.logicalId(d.id(), d.attributes().legacy(), "department")));
                refs(refs, "M09.SHIFT_STAFF", "SELECT id FROM shift_staff WHERE staff_id=?", id);
                for (var h : historyRows(false))
                    if (Objects.equals(h.getValue().valueId(), id))
                        refs.add(new DataRef("M09.SHIFT_LEAD_HISTORY", historyId(h.getValue(), false)));
                refs(refs, "M09.SHIFT", "SELECT id FROM work_shift WHERE lead_staff_id=?", id);
                refs(refs, "M09.ASSIGNMENT", "SELECT id FROM staff_assignment WHERE kind='CARE' AND staff_id=?", id);
                refs(
                        refs,
                        "M09.FLOOR_MANAGER_ASSIGNMENT",
                        "SELECT COALESCE(legacy_floor_manager_id,id) FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND staff_id=?",
                        id);
                accounts.byStaff(id).ifPresent(a -> refs.add(new DataRef("M15.ACCOUNT", a.accountId())));
            }
            case CREDENTIAL -> refs(refs, "M09.CREDENTIAL", "SELECT id FROM credential WHERE replaces_id=?", id);
            case DEPARTMENT -> {
                refs(
                        refs,
                        "M09.STAFF_DEPARTMENT",
                        "SELECT id FROM staff_department WHERE department_id=?",
                        catalogPhysical(id));
                for (var h : histories.list(DEPARTMENT_LEAD, id))
                    refs.add(new DataRef("M09.DEPARTMENT_LEAD_HISTORY", historyId(h, true)));
            }
            case CREDENTIAL_TYPE ->
                refs(
                        refs,
                        "M09.CREDENTIAL",
                        "SELECT id FROM credential WHERE credential_type_id=?",
                        catalogPhysical(id));
            case PRACTICE_SCOPE ->
                refs(
                        refs,
                        "M09.CREDENTIAL",
                        "SELECT id FROM credential WHERE JSON_CONTAINS(practice_scope_ids,CAST(? AS JSON))",
                        catalogPhysical(id));
            case SHIFT_TEMPLATE ->
                refs(refs, "M09.SHIFT", "SELECT id FROM work_shift WHERE template_id=?", catalogPhysical(id));
            case SCHEDULE -> {
                refs(refs, "M09.SHIFT", "SELECT id FROM work_shift WHERE schedule_id=?", id);
                for (var e : events.list(StaffingEventData.PUBLICATION, new EntityRef("M09.SCHEDULE", id)))
                    refs.add(new DataRef("M09.SHIFT_PUBLICATION", eventId(e)));
            }
            case SHIFT -> {
                for (var e : events.list(StaffingEventData.RATIO, new EntityRef("M09.SHIFT", id)))
                    refs.add(new DataRef("M09.STAFFING_RATIO_CALCULATION", eventId(e)));
                jdbc.queryForList(
                                "SELECT id FROM shift_handover WHERE shift_id=? OR next_shift_id=?", Long.class, id, id)
                        .forEach(found -> refs.add(new DataRef("M09.HANDOVER", found)));
                refs(refs, "M09.SHIFT_STAFF", "SELECT id FROM shift_staff WHERE shift_id=?", id);
                for (var h : histories.list(SHIFT_LEAD, id))
                    refs.add(new DataRef("M09.SHIFT_LEAD_HISTORY", historyId(h, false)));
                refs(refs, "M09.ASSIGNMENT", "SELECT id FROM staff_assignment WHERE kind='CARE' AND shift_id=?", id);
            }
            default -> {}
        }
        return refs;
    }

    private void refs(Set<DataRef> refs, String type, String statement, long id) {
        jdbc.queryForList(statement, Long.class, id).forEach(found -> refs.add(new DataRef(type, found)));
    }

    public void delete(Collection<Long> ids) {
        var retention = database.jdbc();
        for (long id : ids) {
            if (isCatalog())
                catalogRows().stream()
                        .filter(r -> catalogId(r) == id)
                        .findFirst()
                        .ifPresent(r -> retention.update(
                                "DELETE FROM catalog_item WHERE catalog_type=? AND id=?", kind.type, r.id()));
            else if (isHistory())
                historyRows(kind == Kind.DEPARTMENT_LEAD_HISTORY).stream()
                        .map(Map.Entry::getValue)
                        .filter(r -> historyId(r, kind == Kind.DEPARTMENT_LEAD_HISTORY) == id)
                        .findFirst()
                        .ifPresent(r -> retention.update(
                                "DELETE FROM status_history WHERE entity_type=? AND attribute='LEAD_STAFF' AND id=?",
                                kind == Kind.DEPARTMENT_LEAD_HISTORY ? "M09.DEPARTMENT" : "M09.SHIFT",
                                r.id()));
            else if (isEvent())
                eventById(id)
                        .ifPresent(r ->
                                retention.update("DELETE FROM record_event WHERE kind=? AND id=?", kind.type, r.id()));
            else if (kind == Kind.STAFF_IMPORT_BATCH)
                records.listByStatus(IMPORT_BATCH, Set.of("RECORDED")).stream()
                        .filter(r -> Legacy.logicalId(r.id(), r.content().legacy(), "staff_import_batch") == id)
                        .findFirst()
                        .ifPresent(r -> retention.update(
                                "DELETE FROM simple_record WHERE kind='M09.STAFF_IMPORT_BATCH' AND id=?", r.id()));
            else if (kind == Kind.FLOOR_MANAGER_ASSIGNMENT)
                retention.update(
                        "DELETE FROM staff_assignment WHERE kind='FLOOR_MANAGER' AND COALESCE(legacy_floor_manager_id,id)=?",
                        id);
            else if (kind == Kind.ASSIGNMENT)
                retention.update("DELETE FROM staff_assignment WHERE kind='CARE' AND id=?", id);
            else retention.update("DELETE FROM " + kind.table + " WHERE id=?", id);
        }
    }

    private boolean isCatalog() {
        return Set.of(Kind.DEPARTMENT, Kind.CREDENTIAL_TYPE, Kind.PRACTICE_SCOPE, Kind.SHIFT_TEMPLATE)
                .contains(kind);
    }

    private boolean isHistory() {
        return kind == Kind.DEPARTMENT_LEAD_HISTORY || kind == Kind.SHIFT_LEAD_HISTORY;
    }

    private boolean isEvent() {
        return kind == Kind.SHIFT_PUBLICATION || kind == Kind.STAFFING_RATIO_CALCULATION;
    }

    private List<? extends CatalogItem<?>> catalogRows() {
        return switch (kind) {
            case DEPARTMENT -> catalogs.list(DEPARTMENT, null, false);
            case CREDENTIAL_TYPE -> catalogs.list(CREDENTIAL_TYPE, null, false);
            case PRACTICE_SCOPE -> catalogs.list(PRACTICE_SCOPE, null, false);
            case SHIFT_TEMPLATE -> catalogs.list(SHIFT_TEMPLATE, null, false);
            default -> List.of();
        };
    }

    private Legacy catalogLegacy(CatalogItem<?> r) {
        return switch (r.attributes()) {
            case DepartmentAttributes a -> a.legacy();
            case CredentialTypeAttributes a -> a.legacy();
            case PracticeScopeAttributes a -> a.legacy();
            case ShiftTemplateAttributes a -> a.legacy();
            default -> throw new IllegalStateException("Loại danh mục không thuộc M09");
        };
    }

    private Instant catalogAt(CatalogItem<?> r) {
        return switch (r.attributes()) {
            case DepartmentAttributes a -> a.createdAt();
            case CredentialTypeAttributes a -> a.createdAt();
            case PracticeScopeAttributes a -> a.createdAt();
            case ShiftTemplateAttributes a -> a.createdAt();
            default -> null;
        };
    }

    private long catalogId(CatalogItem<?> r) {
        return Legacy.logicalId(r.id(), catalogLegacy(r), kind.table);
    }

    private long catalogPhysical(long logical) {
        return catalogRows().stream()
                .filter(r -> catalogId(r) == logical)
                .mapToLong(CatalogItem::id)
                .findFirst()
                .orElseThrow();
    }

    private String catalogCode(CatalogItem<?> r) {
        var a = (CredentialTypeAttributes) r.attributes();
        return a.legacyCode() == null ? r.code() : a.legacyCode();
    }

    private List<Map.Entry<Long, HistoryRow<LeadDetails>>> historyRows(boolean department) {
        List<Long> parents = department
                ? catalogs.list(DEPARTMENT, null, false).stream()
                        .map(r -> Legacy.logicalId(r.id(), r.attributes().legacy(), "department"))
                        .toList()
                : jdbc.queryForList("SELECT id FROM work_shift ORDER BY id", Long.class);
        var track = department ? DEPARTMENT_LEAD : SHIFT_LEAD;
        var result = new ArrayList<Map.Entry<Long, HistoryRow<LeadDetails>>>();
        for (long parent : parents) for (var row : histories.list(track, parent)) result.add(Map.entry(parent, row));
        return result;
    }

    private long historyId(HistoryRow<LeadDetails> r, boolean department) {
        return Legacy.logicalId(
                r.id(),
                r.details() == null ? null : r.details().legacy(),
                department ? "department_lead_history" : "shift_lead_history");
    }

    private Stream<Candidate> eventCandidates(Instant cutoff, int batch) {
        boolean publication = kind == Kind.SHIFT_PUBLICATION;
        SharedKind<?> type = publication ? StaffingEventData.PUBLICATION : StaffingEventData.RATIO;
        String activeParent = publication
                ? "SELECT 1 FROM work_shift s WHERE s.schedule_id=e.entity_id AND s.status NOT IN ('CLOSED','CANCELLED')"
                : "SELECT 1 FROM work_shift s WHERE s.id=e.entity_id AND s.status NOT IN ('CLOSED','CANCELLED')";
        // R-15 cho phép SQL chỉ đọc dòng M09: tìm cả cha đã xóa, lọc ca còn hoạt động trước LIMIT.
        var ids = jdbc.queryForList(
                "SELECT e.id FROM record_event e WHERE e.module='M09' AND e.kind=? AND e.entity_type=? AND e.occurred_at<? AND NOT EXISTS ("
                        + activeParent + ") ORDER BY e.occurred_at,e.id LIMIT ?",
                Long.class,
                kind.type,
                publication ? "M09.SCHEDULE" : "M09.SHIFT",
                sql(cutoff),
                batch);
        // Nội dung và định danh legacy vẫn đọc bằng API hiện có của A, không tự giải mã JSON.
        return ids.stream()
                .map(id -> events.require(type, id))
                .map(event -> new Candidate(eventId(event), event.occurredAt()));
    }

    private Optional<? extends RecordEvent<?>> eventById(long id) {
        return kind == Kind.SHIFT_PUBLICATION
                ? eventById(StaffingEventData.PUBLICATION, id)
                : eventById(StaffingEventData.RATIO, id);
    }

    private <T> Optional<RecordEvent<T>> eventById(SharedKind<T> type, long logicalId) {
        var legacy = events.findByUniqueKey(type, kind.type + ":legacy:" + logicalId);
        if (legacy.isPresent()) return legacy;
        try {
            var row = events.require(type, logicalId);
            return eventId(row) == logicalId ? Optional.of(row) : Optional.empty();
        } catch (NotFoundException absent) {
            // Khung lưu giữ có thể đã xóa dòng này cùng parent trong cùng chuỗi.
            return Optional.empty();
        }
    }

    private long eventId(RecordEvent<?> r) {
        Legacy legacy;
        String table;
        if (r.content() instanceof StaffingEventData.Publication p) {
            legacy = p.legacy();
            table = "shift_publication";
        } else if (r.content() instanceof StaffingEventData.Ratio p) {
            legacy = p.legacy();
            table = "staffing_ratio_calculation";
        } else throw new IllegalStateException("Loại sự kiện không thuộc provider M09");
        return Legacy.logicalId(r.id(), legacy, table);
    }

    private long importPhysical(long logical) {
        var r = records.findByUniqueKey(IMPORT_BATCH, "M09.STAFF_IMPORT_BATCH:legacy:" + logical)
                .orElseGet(() -> records.require(IMPORT_BATCH, logical));
        return r.id();
    }

    private static LocalDateTime sql(Instant at) {
        return LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
