package com.eldercare.backend.admission.internal.change.persistence;

import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.EntityRef;
import com.eldercare.backend.administration.shared.api.dto.NewSimpleRecord;
import com.eldercare.backend.administration.shared.api.dto.RecordChange;
import com.eldercare.backend.administration.shared.api.dto.SimpleRecord;
import com.eldercare.backend.admission.internal.change.domain.Addendum;
import com.eldercare.backend.admission.internal.change.domain.ChangeAttribute;
import com.eldercare.backend.admission.internal.change.domain.ChangeType;
import com.eldercare.backend.common.time.BusinessTime;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phụ lục hợp đồng ở dòng {@code ADDENDUM} của {@code contract_version} và chỉ mục thuộc tính của yêu cầu thay đổi (data-model mục 0, 7;
 * R-15). Trigger chặn sửa nội dung phụ lục và ngày hiệu lực sớm hơn ngày bắt đầu hợp đồng; {@code unique_key} chặn hai yêu cầu chờ hiệu
 * lực cùng thuộc tính (FR-046). Lớp này chỉ ghi, không tự kiểm lại các quy tắc đó.
 */
@Repository
public class AddendumStore {

    /** Đối tượng cha của chỉ mục thuộc tính: yêu cầu phê duyệt. */
    private static final String REQUEST_ENTITY = "M02.RESIDENCY_CHANGE";

    private static final String SELECT = "SELECT * FROM contract_version WHERE kind = 'ADDENDUM'";
    private static final String NOT_PENDING = "NOT_PENDING";
    private static final String PENDING = "PENDING";
    /** Thuộc tính bị bỏ khi sửa Nháp (T043); không bị đọc lại. */
    private static final String REMOVED = "REMOVED";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;
    private final SimpleRecordApi records;
    private final BusinessTime time;

    AddendumStore(JdbcTemplate jdbc, JsonMapper json, SimpleRecordApi records, BusinessTime time) {
        this.jdbc = jdbc;
        this.json = json;
        this.records = records;
        this.time = time;
    }

    /** Thêm phụ lục ở Chờ hiệu lực; trả mã phụ lục. */
    public long insert(
            long contractId,
            long approvalRequestId,
            Set<ChangeType> changeTypes,
            JsonNode before,
            JsonNode after,
            LocalDate effectiveDate,
            Instant now) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO contract_version (kind, contract_id, approval_request_id, change_types,"
                                    + " content_before, content_after, effective_date, addendum_status, created_at)"
                                    + " VALUES ('ADDENDUM', ?, ?, ?, ?, ?, ?, 'PENDING_EFFECTIVE', ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, contractId);
                    statement.setLong(2, approvalRequestId);
                    statement.setString(3, setValue(changeTypes));
                    statement.setString(4, before.toString());
                    statement.setString(5, after.toString());
                    statement.setObject(6, effectiveDate);
                    statement.setObject(7, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<Addendum> findByRequest(long approvalRequestId) {
        return jdbc.query(SELECT + " AND approval_request_id = ?", (rs, i) -> map(rs), approvalRequestId).stream()
                .findFirst();
    }

    /** Khóa dòng phụ lục tới hết transaction. */
    public Optional<Addendum> lockByRequest(long approvalRequestId) {
        return jdbc
                .query(SELECT + " AND approval_request_id = ? FOR UPDATE", (rs, i) -> map(rs), approvalRequestId)
                .stream()
                .findFirst();
    }

    /** Phụ lục của hợp đồng theo ngày hiệu lực, mã. */
    public List<Addendum> ofContract(long contractId) {
        return jdbc.query(SELECT + " AND contract_id = ? ORDER BY effective_date, id", (rs, i) -> map(rs), contractId);
    }

    public void markApplied(long id, long contentVersionId) {
        jdbc.update(
                "UPDATE contract_version SET addendum_status = 'APPLIED', content_version_id = ?, locked_at = ? WHERE id = ?",
                contentVersionId,
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC),
                id);
    }

    public void markNotApplied(long id) {
        jdbc.update(
                "UPDATE contract_version SET addendum_status = 'NOT_APPLIED', locked_at = ? WHERE id = ?",
                LocalDateTime.ofInstant(time.now(), ZoneOffset.UTC),
                id);
    }

    // ---- Chỉ mục thuộc tính (FR-046) ------------------------------------------------------------------------------

    /** Ghi nhận yêu cầu thay đổi các thuộc tính này của người cao tuổi; chưa chờ hiệu lực. Chạy lại không tạo trùng. */
    public void addAttributes(
            long approvalRequestId, long contractId, long residentId, Set<ChangeType> types, long actorId) {
        Set<ChangeType> existing = attributesOf(approvalRequestId).stream()
                .map(row -> row.content().attribute())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(ChangeType.class)));
        for (ChangeType type : types) {
            if (!existing.contains(type)) {
                records.create(
                        ChangeAttribute.KIND,
                        new NewSimpleRecord<>(
                                residentId,
                                new EntityRef(REQUEST_ENTITY, approvalRequestId),
                                NOT_PENDING,
                                new ChangeAttribute(approvalRequestId, contractId, type),
                                null,
                                null,
                                null,
                                actorId,
                                false));
            }
        }
    }

    /**
     * Sửa Nháp (T043): thuộc tính bỏ đi hoặc của hợp đồng cũ chuyển {@code REMOVED}, thuộc tính mới thêm dòng. Yêu cầu
     * còn Nháp nên chưa dòng nào chờ hiệu lực.
     */
    public void replaceAttributes(
            long approvalRequestId, long contractId, long residentId, Set<ChangeType> types, long actorId) {
        for (SimpleRecord<ChangeAttribute> row : attributesOf(approvalRequestId)) {
            if (!types.contains(row.content().attribute()) || row.content().contractId() != contractId) {
                records.change(
                        ChangeAttribute.KIND,
                        row.id(),
                        row.version(),
                        new RecordChange<>(REMOVED, row.content(), null, null, false));
            }
        }
        addAttributes(approvalRequestId, contractId, residentId, types, actorId);
    }

    /** Đặt cờ chờ hiệu lực của mọi thuộc tính của yêu cầu; đặt TRUE vi phạm UNIQUE nếu yêu cầu khác đang giữ thuộc tính. */
    public void setPending(long approvalRequestId, boolean pending) {
        for (SimpleRecord<ChangeAttribute> row : attributesOf(approvalRequestId)) {
            records.change(
                    ChangeAttribute.KIND,
                    row.id(),
                    row.version(),
                    new RecordChange<>(
                            pending ? PENDING : NOT_PENDING,
                            row.content(),
                            null,
                            pending ? uniqueKey(row.residentId(), row.content().attribute()) : null,
                            false));
        }
    }

    /** Thuộc tính mà yêu cầu khác của người này đang giữ ở trạng thái Đã duyệt chờ hiệu lực. */
    public Set<ChangeType> pendingConflicts(long residentId, Set<ChangeType> types, long excludeRequestId) {
        Set<ChangeType> conflicts = EnumSet.noneOf(ChangeType.class);
        records.listByResident(ChangeAttribute.KIND, residentId, Set.of(PENDING)).stream()
                .map(SimpleRecord::content)
                .filter(content -> content.approvalRequestId() != excludeRequestId)
                .map(ChangeAttribute::attribute)
                .filter(types::contains)
                .forEach(conflicts::add);
        return conflicts;
    }

    /** Mã yêu cầu đã ghi nhận cho thuộc tính của người cao tuổi, mới nhất trước (trạng thái xét ở yêu cầu phê duyệt). */
    public List<Long> requestsOf(long residentId, ChangeType attribute) {
        return records.listByResident(ChangeAttribute.KIND, residentId, Set.of(NOT_PENDING, PENDING)).stream()
                .map(SimpleRecord::content)
                .filter(content -> content.attribute() == attribute)
                .map(ChangeAttribute::approvalRequestId)
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** Mã mọi yêu cầu đã ghi nhận của người cao tuổi, mới nhất trước. */
    public List<Long> requestsOf(long residentId) {
        return records.listByResident(ChangeAttribute.KIND, residentId, Set.of(NOT_PENDING, PENDING)).stream()
                .map(row -> row.content().approvalRequestId())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** Mã mọi yêu cầu đã ghi nhận, mới nhất trước. */
    public List<Long> allRequests() {
        return records.listByStatus(ChangeAttribute.KIND, Set.of(NOT_PENDING, PENDING)).stream()
                .map(row -> row.content().approvalRequestId())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /** Hợp đồng mà yêu cầu thay đổi, nếu đã được ghi nhận. */
    public Optional<Long> contractOf(long approvalRequestId) {
        return attributesOf(approvalRequestId).stream()
                .map(row -> row.content().contractId())
                .filter(Objects::nonNull)
                .findFirst();
    }

    private List<SimpleRecord<ChangeAttribute>> attributesOf(long approvalRequestId) {
        return records.listByParent(ChangeAttribute.KIND, new EntityRef(REQUEST_ENTITY, approvalRequestId)).stream()
                .filter(row -> !REMOVED.equals(row.status()))
                .toList();
    }

    private static String uniqueKey(Long residentId, ChangeType attribute) {
        return "M02.CHANGE_ATTRIBUTE:" + residentId + ":" + attribute.name();
    }

    private Addendum map(ResultSet rs) throws SQLException {
        Set<ChangeType> types = EnumSet.noneOf(ChangeType.class);
        for (String value : rs.getString("change_types").split(",")) {
            types.add(ChangeType.valueOf(value));
        }
        LocalDateTime created = rs.getObject("created_at", LocalDateTime.class);
        return new Addendum(
                rs.getLong("id"),
                rs.getLong("contract_id"),
                rs.getLong("approval_request_id"),
                types,
                json.readTree(rs.getString("content_before")),
                json.readTree(rs.getString("content_after")),
                rs.getObject("effective_date", LocalDate.class),
                Addendum.Status.valueOf(rs.getString("addendum_status")),
                rs.getObject("content_version_id", Long.class),
                created.toInstant(ZoneOffset.UTC));
    }

    private static String setValue(Set<ChangeType> types) {
        return new ArrayList<>(types).stream().map(ChangeType::name).collect(Collectors.joining(","));
    }
}
