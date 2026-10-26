package com.cambae.backend.m02.internal.change.persistence;

import com.cambae.backend.m02.internal.change.domain.Addendum;
import com.cambae.backend.m02.internal.change.domain.ChangeType;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Phụ lục hợp đồng và chỉ mục thuộc tính của yêu cầu thay đổi ở các bảng {@code contract_addendum}, {@code
 * change_request_attribute} (data-model mục 7). Trigger của cơ sở dữ liệu chặn sửa nội dung phụ lục và ngày hiệu lực sớm
 * hơn ngày bắt đầu hợp đồng; {@code UNIQUE(lock_key)} chặn hai yêu cầu chờ hiệu lực cùng thuộc tính (FR-046). Lớp này chỉ
 * ghi, không tự kiểm lại các quy tắc đó.
 */
@Repository
public class AddendumStore {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    AddendumStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ---- Phụ lục --------------------------------------------------------------------------------------------------

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
                            "INSERT INTO contract_addendum (contract_id, approval_request_id, change_types,"
                                    + " content_before, content_after, effective_date, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?)",
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
        return jdbc
                .query(
                        "SELECT * FROM contract_addendum WHERE approval_request_id = ?",
                        (rs, i) -> map(rs),
                        approvalRequestId)
                .stream()
                .findFirst();
    }

    /** Khóa dòng phụ lục tới hết transaction. */
    public Optional<Addendum> lockByRequest(long approvalRequestId) {
        return jdbc
                .query(
                        "SELECT * FROM contract_addendum WHERE approval_request_id = ? FOR UPDATE",
                        (rs, i) -> map(rs),
                        approvalRequestId)
                .stream()
                .findFirst();
    }

    /** Phụ lục của hợp đồng theo ngày hiệu lực, mã. */
    public List<Addendum> ofContract(long contractId) {
        return jdbc.query(
                "SELECT * FROM contract_addendum WHERE contract_id = ? ORDER BY effective_date, id",
                (rs, i) -> map(rs),
                contractId);
    }

    public void markApplied(long id, long contentVersionId) {
        jdbc.update(
                "UPDATE contract_addendum SET status = 'APPLIED', content_version_id = ? WHERE id = ?",
                contentVersionId,
                id);
    }

    public void markNotApplied(long id) {
        jdbc.update("UPDATE contract_addendum SET status = 'NOT_APPLIED' WHERE id = ?", id);
    }

    // ---- Chỉ mục thuộc tính (FR-046) ------------------------------------------------------------------------------

    /** Ghi nhận yêu cầu thay đổi các thuộc tính này của người cao tuổi; chưa chờ hiệu lực. Chạy lại không tạo trùng. */
    public void addAttributes(long approvalRequestId, long contractId, long residentId, Set<ChangeType> types) {
        for (ChangeType type : types) {
            jdbc.update(
                    "INSERT IGNORE INTO change_request_attribute (approval_request_id, contract_id, resident_id,"
                            + " attribute) VALUES (?, ?, ?, ?)",
                    approvalRequestId,
                    contractId,
                    residentId,
                    type.name());
        }
    }

    /** Đặt cờ chờ hiệu lực của mọi thuộc tính của yêu cầu; đặt TRUE vi phạm UNIQUE nếu yêu cầu khác đang giữ thuộc tính. */
    public void setPending(long approvalRequestId, boolean pending) {
        jdbc.update(
                "UPDATE change_request_attribute SET pending_effective = ? WHERE approval_request_id = ?",
                pending,
                approvalRequestId);
    }

    /** Thuộc tính mà yêu cầu khác của người này đang giữ ở trạng thái Đã duyệt chờ hiệu lực. */
    public Set<ChangeType> pendingConflicts(long residentId, Set<ChangeType> types, long excludeRequestId) {
        Set<ChangeType> conflicts = EnumSet.noneOf(ChangeType.class);
        jdbc.query(
                "SELECT attribute FROM change_request_attribute WHERE resident_id = ? AND pending_effective AND"
                        + " approval_request_id <> ?",
                rs -> {
                    ChangeType type = ChangeType.valueOf(rs.getString("attribute"));
                    if (types.contains(type)) {
                        conflicts.add(type);
                    }
                },
                residentId,
                excludeRequestId);
        return conflicts;
    }

    /** Mã yêu cầu đã ghi nhận cho thuộc tính của người cao tuổi, mới nhất trước (trạng thái xét ở yêu cầu phê duyệt). */
    public List<Long> requestsOf(long residentId, ChangeType attribute) {
        return jdbc.queryForList(
                "SELECT approval_request_id FROM change_request_attribute WHERE resident_id = ? AND attribute = ?"
                        + " ORDER BY approval_request_id DESC",
                Long.class,
                residentId,
                attribute.name());
    }

    /** Mã mọi yêu cầu đã ghi nhận của người cao tuổi, mới nhất trước. */
    public List<Long> requestsOf(long residentId) {
        return jdbc.queryForList(
                "SELECT DISTINCT approval_request_id FROM change_request_attribute WHERE resident_id = ? ORDER BY"
                        + " approval_request_id DESC",
                Long.class,
                residentId);
    }

    /** Hợp đồng mà yêu cầu thay đổi, nếu đã được ghi nhận. */
    public Optional<Long> contractOf(long approvalRequestId) {
        return jdbc
                .queryForList(
                        "SELECT DISTINCT contract_id FROM change_request_attribute WHERE approval_request_id = ?",
                        Long.class,
                        approvalRequestId)
                .stream()
                .findFirst();
    }

    // ---- Chi tiết -------------------------------------------------------------------------------------------------

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
                Addendum.Status.valueOf(rs.getString("status")),
                rs.getObject("content_version_id", Long.class),
                created.toInstant(ZoneOffset.UTC));
    }

    private static String setValue(Set<ChangeType> types) {
        return new ArrayList<>(types).stream().map(ChangeType::name).collect(Collectors.joining(","));
    }
}
