package com.cambae.backend.m02.internal.contract.persistence;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.api.dto.ContractStatus;
import com.cambae.backend.m02.internal.contract.domain.Contract;
import com.cambae.backend.m02.internal.contract.domain.ContractContent;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.AbsencePolicyLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ArrivalLine;
import com.cambae.backend.m02.internal.contract.domain.ContractContent.ServiceLine;
import com.cambae.backend.m02.internal.contract.domain.ContractVersion;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
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
 * Hợp đồng, ba bảng con và phiên bản nội dung ở các bảng {@code contract}, {@code contract_service}, {@code
 * contract_absence_policy}, {@code contract_arrival_schedule}, {@code contract_content_version} (data-model mục 5).
 * Trigger của cơ sở dữ liệu khóa nội dung ngoài Nháp và chặn hai hợp đồng Hiệu lực chồng khoảng (DBR-06); lớp này chỉ
 * ghi, không tự kiểm lại các quy tắc đó.
 */
@Repository
public class ContractStore {

    /** Hợp đồng Hiệu lực có ngày kết thúc hiện hành, đủ cho việc hằng ngày (không tải bảng con). */
    public record Expiring(
            long id,
            long residentId,
            ResidenceType residenceType,
            LocalDate currentEndDate,
            LocalDate overdueSince,
            Instant managerNotifiedOverdueAt) {}

    private static final String INSERT_SERVICE =
            "INSERT INTO contract_service (contract_id, service_id, in_package, quantity, custom_unit_price)"
                    + " VALUES (?, ?, ?, ?, ?)";
    private static final String INSERT_POLICY = "INSERT INTO contract_absence_policy (contract_id, residence_type,"
            + " absence_type, from_day, to_day, coefficient, hold_bed) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String INSERT_ARRIVAL = "INSERT INTO contract_arrival_schedule (contract_id, day_of_week,"
            + " `date`, arrival_time, departure_time) VALUES (?, ?, ?, ?, ?)";

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    ContractStore(JdbcTemplate jdbc, JsonMapper json) {
        this.jdbc = jdbc;
        this.json = json;
    }

    // ---- Ghi ------------------------------------------------------------------------------------------------------

    /** Thêm hợp đồng Nháp cùng bảng con, đặt số hợp đồng {@code HD-<năm>-<id 6 chữ số>}; trả mã hợp đồng. */
    public long insert(long residentId, ContractContent content, int year, long createdBy, Instant now) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO contract (resident_id, residence_type, start_date, end_date, care_level,"
                                    + " room_type_id, bed_id, residency_fee_billing_period, deposit_amount,"
                                    + " no_deposit_required, bed_hold_policy, termination_condition, created_by,"
                                    + " created_at) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    int i = 0;
                    statement.setLong(++i, residentId);
                    i = contentParameters(statement, content, i);
                    statement.setLong(++i, createdBy);
                    statement.setObject(++i, utc(now));
                    return statement;
                },
                keys);
        long id = keys.getKey().longValue();
        jdbc.update("UPDATE contract SET contract_number = ? WHERE id = ?", "HD-%d-%06d".formatted(year, id), id);
        insertChildren(id, content);
        return id;
    }

    /**
     * Sửa nội dung Nháp theo phiên bản; {@code termsApprovalRequestId}, {@code contentHash} là giá trị sau sửa (bản duyệt
     * điều khoản còn hiệu lực hay không). Trả 0 khi phiên bản đã đổi.
     */
    public int updateContent(
            long id, int version, ContractContent content, Long termsApprovalRequestId, String contentHash) {
        int updated = jdbc.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(
                    "UPDATE contract SET residence_type = ?, start_date = ?, end_date = ?, care_level = ?,"
                            + " room_type_id = ?, bed_id = ?, residency_fee_billing_period = ?,"
                            + " deposit_amount = ?, no_deposit_required = ?, bed_hold_policy = ?,"
                            + " termination_condition = ?, terms_approval_request_id = ?, content_hash = ?,"
                            + " version = version + 1 WHERE id = ? AND version = ?");
            int i = contentParameters(statement, content, 0);
            statement.setObject(++i, termsApprovalRequestId);
            statement.setObject(++i, contentHash);
            statement.setLong(++i, id);
            statement.setInt(++i, version);
            return statement;
        });
        if (updated == 1) {
            jdbc.update("DELETE FROM contract_service WHERE contract_id = ?", id);
            jdbc.update("DELETE FROM contract_absence_policy WHERE contract_id = ?", id);
            jdbc.update("DELETE FROM contract_arrival_schedule WHERE contract_id = ?", id);
            insertChildren(id, content);
        }
        return updated;
    }

    /** Chuyển trạng thái từ {@code from} sang {@code to}; trả 0 khi hợp đồng không còn ở {@code from}. */
    public int transition(long id, ContractStatus from, ContractStatus to) {
        return jdbc.update(
                "UPDATE contract SET status = ?, version = version + 1 WHERE id = ? AND status = ?",
                to.name(),
                id,
                from.name());
    }

    public void markSigned(
            long id, LocalDate signedDate, long signingRepresentativeId, long scanFileId, LocalDate currentEndDate) {
        jdbc.update(
                "UPDATE contract SET status = 'ACTIVE', signed_date = ?, signing_representative_id = ?,"
                        + " scan_file_id = ?, current_end_date = ?, version = version + 1 WHERE id = ?",
                signedDate,
                signingRepresentativeId,
                scanFileId,
                currentEndDate,
                id);
    }

    public void markTerminated(long id, String reason) {
        jdbc.update(
                "UPDATE contract SET status = 'TERMINATED', termination_reason = ?, version = version + 1 WHERE id ="
                        + " ?",
                reason,
                id);
    }

    /** Khóa nội dung chờ duyệt điều khoản khác chuẩn cho bản băm {@code hash} (FR-025a). */
    public void awaitTerms(long id, long requestId, String hash) {
        jdbc.update(
                "UPDATE contract SET awaiting_terms_approval = TRUE, terms_approval_request_id = ?, content_hash = ?,"
                        + " version = version + 1 WHERE id = ?",
                requestId,
                hash,
                id);
    }

    /** Yêu cầu được duyệt: mở khóa, giữ yêu cầu và bản băm làm căn cứ cho Gửi ký. */
    public void termsApproved(long id) {
        jdbc.update("UPDATE contract SET awaiting_terms_approval = FALSE, version = version + 1 WHERE id = ?", id);
    }

    /** Yêu cầu bị từ chối hoặc hủy: mở khóa và bỏ yêu cầu; trả 0 khi hợp đồng đã gắn yêu cầu khác. */
    public int releaseTerms(long id, long requestId) {
        return jdbc.update(
                "UPDATE contract SET awaiting_terms_approval = FALSE, terms_approval_request_id = NULL,"
                        + " content_hash = NULL, version = version + 1 WHERE id = ? AND terms_approval_request_id = ?"
                        + " AND awaiting_terms_approval",
                id,
                requestId);
    }

    public void setOverdueSince(long id, LocalDate overdueSince) {
        jdbc.update("UPDATE contract SET overdue_since = ?, version = version + 1 WHERE id = ?", overdueSince, id);
    }

    public void markManagerNotified(long id, Instant at) {
        jdbc.update("UPDATE contract SET manager_notified_overdue_at = ? WHERE id = ?", utc(at), id);
    }

    public void setRecordedUnitPrice(long contractId, long serviceId, BigDecimal price) {
        jdbc.update(
                "UPDATE contract_service SET recorded_unit_price = ? WHERE contract_id = ? AND service_id = ?",
                price,
                contractId,
                serviceId);
    }

    public void insertVersion(
            long contractId, int sequenceNo, LocalDate effectiveFrom, JsonNode content, ContractContent summary) {
        jdbc.update(
                "INSERT INTO contract_content_version (contract_id, sequence_no, effective_from, content,"
                        + " residence_type, care_level, end_date, room_type_id, deposit_amount) VALUES (?, ?, ?, ?, ?, ?,"
                        + " ?, ?, ?)",
                contractId,
                sequenceNo,
                effectiveFrom,
                content.toString(),
                summary.residenceType().name(),
                summary.careLevel().name(),
                summary.endDate(),
                summary.roomTypeId(),
                summary.depositAmount());
    }

    // ---- Đọc ------------------------------------------------------------------------------------------------------

    public Optional<Contract> find(long id) {
        return jdbc.query("SELECT * FROM contract WHERE id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst()
                .map(this::withChildren);
    }

    /** Khóa dòng hợp đồng cho tới hết transaction (thứ tự khóa: sau {@code resident_residency}, research T-03). */
    public Optional<Contract> lock(long id) {
        return jdbc.query("SELECT * FROM contract WHERE id = ? FOR UPDATE", (rs, i) -> map(rs), id).stream()
                .findFirst()
                .map(this::withChildren);
    }

    /** Hợp đồng theo người, trạng thái, dấu quá hạn; mã giảm dần. Các tham số {@code null} là không lọc. */
    public List<Contract> list(Long residentId, Set<ContractStatus> statuses, Boolean overdue) {
        StringBuilder sql = new StringBuilder("SELECT * FROM contract WHERE 1 = 1");
        List<Object> args = new ArrayList<>();
        if (residentId != null) {
            sql.append(" AND resident_id = ?");
            args.add(residentId);
        }
        if (statuses != null && !statuses.isEmpty()) {
            sql.append(" AND status IN (")
                    .append(statuses.stream().map(s -> "?").collect(Collectors.joining(", ")))
                    .append(")");
            statuses.forEach(s -> args.add(s.name()));
        }
        if (overdue != null) {
            sql.append(overdue ? " AND overdue_since IS NOT NULL" : " AND overdue_since IS NULL");
        }
        sql.append(" ORDER BY id DESC");
        return jdbc.query(sql.toString(), (rs, i) -> map(rs), args.toArray()).stream()
                .map(this::withChildren)
                .toList();
    }

    /** Hợp đồng của người theo thứ tự ngày bắt đầu, id; không lọc trạng thái. */
    public List<Contract> ofResident(long residentId) {
        return jdbc
                .query(
                        "SELECT * FROM contract WHERE resident_id = ? ORDER BY start_date, id",
                        (rs, i) -> map(rs),
                        residentId)
                .stream()
                .map(this::withChildren)
                .toList();
    }

    public List<Expiring> activeWithEndDate() {
        return jdbc.query(
                "SELECT id, resident_id, residence_type, current_end_date, overdue_since, manager_notified_overdue_at"
                        + " FROM contract WHERE status = 'ACTIVE' AND current_end_date IS NOT NULL ORDER BY id",
                (rs, i) -> new Expiring(
                        rs.getLong("id"),
                        rs.getLong("resident_id"),
                        ResidenceType.valueOf(rs.getString("residence_type")),
                        rs.getObject("current_end_date", LocalDate.class),
                        rs.getObject("overdue_since", LocalDate.class),
                        instant(rs, "manager_notified_overdue_at")));
    }

    /**
     * Hợp đồng Hiệu lực khác của người giao khoảng [start, end] gồm cả hai đầu; {@code end} rỗng là vô hạn. Đọc hiện hành
     * ({@code FOR SHARE}) vì transaction có thể đã mở snapshot trước khi chờ khóa người cao tuổi.
     */
    public boolean overlapsActive(long residentId, long excludeId, LocalDate start, LocalDate end) {
        Integer found = jdbc.queryForObject(
                "SELECT COUNT(*) FROM contract c WHERE c.resident_id = ? AND c.id <> ? AND c.status = 'ACTIVE'"
                        + " AND c.start_date <= COALESCE(?, '9999-12-31')"
                        + " AND ? <= COALESCE(c.current_end_date, c.end_date, '9999-12-31') FOR SHARE",
                Integer.class,
                residentId,
                excludeId,
                end,
                start);
        return found != null && found > 0;
    }

    public boolean serviceExists(long serviceId) {
        Integer found = jdbc.queryForObject("SELECT COUNT(*) FROM service WHERE id = ?", Integer.class, serviceId);
        return found != null && found > 0;
    }

    /** Loại, mức chăm sóc (chỉ dịch vụ chăm sóc, FR-025, Q-297) và trạng thái Hiệu lực của dịch vụ. */
    public Optional<ServiceKind> serviceKind(long serviceId) {
        return jdbc
                .query(
                        "SELECT type, care_level_code, status FROM service WHERE id = ?",
                        (rs, i) -> new ServiceKind(
                                rs.getString("type"),
                                rs.getString("care_level_code") == null
                                        ? null
                                        : CareLevel.valueOf(rs.getString("care_level_code")),
                                "ACTIVE".equals(rs.getString("status"))),
                        serviceId)
                .stream()
                .findFirst();
    }

    public record ServiceKind(String type, CareLevel careLevel, boolean active) {}

    public Optional<ContractVersion> versionAt(long contractId, LocalDate date) {
        return jdbc
                .query(
                        "SELECT * FROM contract_content_version WHERE contract_id = ? AND effective_from <= ? ORDER BY"
                                + " effective_from DESC LIMIT 1",
                        (rs, i) -> version(rs),
                        contractId,
                        date)
                .stream()
                .findFirst();
    }

    public List<ContractVersion> versions(long contractId) {
        return jdbc.query(
                "SELECT * FROM contract_content_version WHERE contract_id = ? ORDER BY sequence_no",
                (rs, i) -> version(rs),
                contractId);
    }

    // ---- Chi tiết -------------------------------------------------------------------------------------------------

    private int contentParameters(PreparedStatement statement, ContractContent content, int index) throws SQLException {
        int i = index;
        statement.setString(++i, content.residenceType().name());
        statement.setObject(++i, content.startDate());
        statement.setObject(++i, content.endDate());
        statement.setString(++i, content.careLevel().name());
        statement.setObject(++i, content.roomTypeId());
        statement.setObject(++i, content.bedId());
        statement.setString(++i, content.residencyFeeBillingPeriod().name());
        statement.setObject(++i, content.depositAmount());
        statement.setBoolean(++i, content.noDepositRequired());
        statement.setString(++i, content.bedHoldPolicy());
        statement.setString(++i, content.terminationCondition());
        return i;
    }

    private void insertChildren(long id, ContractContent content) {
        for (ServiceLine line : content.services()) {
            jdbc.update(
                    INSERT_SERVICE, id, line.serviceId(), line.inPackage(), line.quantity(), line.customUnitPrice());
        }
        for (AbsencePolicyLine line : content.absencePolicyOverrides()) {
            jdbc.update(
                    INSERT_POLICY,
                    id,
                    line.residenceType().name(),
                    line.absenceType(),
                    line.fromDay(),
                    line.toDay(),
                    line.coefficient(),
                    line.holdBed());
        }
        for (ArrivalLine line : content.arrivalSchedule()) {
            jdbc.update(INSERT_ARRIVAL, id, line.dayOfWeek(), line.date(), line.arrivalTime(), line.departureTime());
        }
    }

    private Contract withChildren(Contract contract) {
        long id = contract.id();
        List<ServiceLine> services = jdbc.query(
                "SELECT service_id, in_package, quantity, custom_unit_price FROM contract_service WHERE contract_id = ?"
                        + " ORDER BY service_id",
                (rs, i) -> new ServiceLine(
                        rs.getLong("service_id"),
                        rs.getBoolean("in_package"),
                        rs.getBigDecimal("quantity"),
                        rs.getBigDecimal("custom_unit_price")),
                id);
        List<AbsencePolicyLine> policies = jdbc.query(
                "SELECT residence_type, absence_type, from_day, to_day, coefficient, hold_bed FROM"
                        + " contract_absence_policy WHERE contract_id = ? ORDER BY id",
                (rs, i) -> new AbsencePolicyLine(
                        ResidenceType.valueOf(rs.getString("residence_type")),
                        rs.getString("absence_type"),
                        rs.getObject("from_day", Integer.class),
                        rs.getObject("to_day", Integer.class),
                        rs.getString("coefficient"),
                        rs.getString("hold_bed")),
                id);
        List<ArrivalLine> schedule = jdbc.query(
                "SELECT day_of_week, `date`, arrival_time, departure_time FROM contract_arrival_schedule WHERE"
                        + " contract_id = ? ORDER BY id",
                (rs, i) -> new ArrivalLine(
                        rs.getObject("day_of_week", Integer.class),
                        rs.getObject("date", LocalDate.class),
                        rs.getObject("arrival_time", LocalTime.class),
                        rs.getObject("departure_time", LocalTime.class)),
                id);
        return contract.withContent(contract.content().withLines(services, policies, schedule));
    }

    private static Contract map(ResultSet rs) throws SQLException {
        ContractContent content = new ContractContent(
                ResidenceType.valueOf(rs.getString("residence_type")),
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                CareLevel.valueOf(rs.getString("care_level")),
                rs.getObject("room_type_id", Long.class),
                rs.getObject("bed_id", Long.class),
                BillingPeriod.valueOf(rs.getString("residency_fee_billing_period")),
                List.of(),
                rs.getBoolean("no_deposit_required"),
                rs.getBigDecimal("deposit_amount"),
                rs.getString("bed_hold_policy"),
                List.of(),
                List.of(),
                rs.getString("termination_condition"));
        return new Contract(
                rs.getLong("id"),
                rs.getString("contract_number"),
                rs.getLong("resident_id"),
                content,
                rs.getObject("current_end_date", LocalDate.class),
                rs.getObject("signing_representative_id", Long.class),
                rs.getObject("signed_date", LocalDate.class),
                rs.getObject("scan_file_id", Long.class),
                rs.getString("content_hash"),
                rs.getBoolean("awaiting_terms_approval"),
                rs.getObject("terms_approval_request_id", Long.class),
                ContractStatus.valueOf(rs.getString("status")),
                rs.getString("termination_reason"),
                rs.getObject("overdue_since", LocalDate.class),
                instant(rs, "manager_notified_overdue_at"),
                rs.getInt("version"),
                rs.getLong("created_by"),
                instant(rs, "created_at"));
    }

    private ContractVersion version(ResultSet rs) throws SQLException {
        return new ContractVersion(
                rs.getLong("id"),
                rs.getLong("contract_id"),
                rs.getInt("sequence_no"),
                rs.getObject("effective_from", LocalDate.class),
                rs.getObject("addendum_id", Long.class),
                json.readTree(rs.getString("content")));
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }

    private static LocalDateTime utc(Instant at) {
        return at == null ? null : LocalDateTime.ofInstant(at, ZoneOffset.UTC);
    }
}
