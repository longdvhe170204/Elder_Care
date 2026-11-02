package com.eldercare.backend.admission.internal.contract.persistence;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ContractStatus;
import com.eldercare.backend.admission.internal.contract.domain.Contract;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent.ArrivalLine;
import com.eldercare.backend.admission.internal.contract.domain.ContractContent.ServiceLine;
import com.eldercare.backend.admission.internal.contract.domain.ContractVersion;
import com.eldercare.backend.admission.internal.contract.domain.ExpiringContract;
import com.eldercare.backend.admission.internal.contract.domain.ServiceKind;
import com.eldercare.backend.admission.internal.domain.ServiceAttributes;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.ResidenceType;
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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;
import tools.jackson.databind.node.ObjectNode;

/**
 * Hợp đồng và phiên bản hợp đồng ở bảng {@code contract}, {@code contract_version} (data-model mục 0, 5; R-15). Dòng {@code DRAFT}
 * của {@code contract_version} giữ dịch vụ, lịch đến dạng JSON và đơn giá đã ghi; dòng {@code CONTENT} là phiên bản nội dung, khóa
 * ngay khi thêm. Trigger khóa nội dung ngoài Nháp và chặn hai hợp đồng Hiệu lực chồng khoảng (DBR-06); lớp này chỉ ghi, không tự kiểm
 * lại các quy tắc đó.
 */
@Repository
public class ContractStore {

    private final JdbcTemplate jdbc;
    private final JsonMapper json;

    private final BusinessTime time;
    private final CatalogApi catalog;

    ContractStore(JdbcTemplate jdbc, JsonMapper json, BusinessTime time, CatalogApi catalog) {
        this.jdbc = jdbc;
        this.json = json;
        this.time = time;
        this.catalog = catalog;
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
        jdbc.update(
                "INSERT INTO contract_version (kind, contract_id, services, arrival_schedule, created_at)"
                        + " VALUES ('DRAFT', ?, ?, ?, ?)",
                id,
                servicesJson(content),
                scheduleJson(content),
                utc(now));
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
            jdbc.update(
                    "UPDATE contract_version SET services = ?, arrival_schedule = ? WHERE kind = 'DRAFT' AND contract_id = ?",
                    servicesJson(content),
                    scheduleJson(content),
                    id);
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

    public void markTerminated(long id, String reason, LocalDate on) {
        jdbc.update(
                "UPDATE contract SET status = 'TERMINATED', termination_reason = ?, terminated_on = ?, version ="
                        + " version + 1 WHERE id = ?",
                reason,
                on,
                id);
    }

    /** Ngày hợp đồng chuyển Chấm dứt (mốc dừng tính phí, FR-070, FR-075a); rỗng với hợp đồng cũ hoặc chưa Chấm dứt. */
    public Optional<LocalDate> terminatedOn(long id) {
        return jdbc
                .query(
                        "SELECT terminated_on FROM contract WHERE id = ?",
                        (rs, i) -> rs.getObject(1, LocalDate.class),
                        id)
                .stream()
                .filter(Objects::nonNull)
                .findFirst();
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

    /** Đơn giá ghi lúc Ghi nhận đã ký (FR-036), đặt một lần; khóa dòng Nháp. */
    public void setRecordedPrices(long contractId, Map<Long, BigDecimal> prices) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        prices.forEach((serviceId, price) -> node.put(String.valueOf(serviceId), price));
        jdbc.update(
                "UPDATE contract_version SET recorded_prices = ?, locked_at = ? WHERE kind = 'DRAFT' AND contract_id = ?",
                node.toString(),
                utc(time.now()),
                contractId);
    }

    public void insertVersion(
            long contractId, int sequenceNo, LocalDate effectiveFrom, JsonNode content, ContractContent summary) {
        jdbc.update(
                "INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from, content, residence_type,"
                        + " care_level, end_date, room_type_id, deposit_amount, created_at, locked_at) VALUES ('CONTENT', ?, ?,"
                        + " ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                contractId,
                sequenceNo,
                effectiveFrom,
                content.toString(),
                summary.residenceType().name(),
                summary.careLevel().name(),
                summary.endDate(),
                summary.roomTypeId(),
                summary.depositAmount(),
                utc(time.now()),
                utc(time.now()));
    }

    /**
     * Thêm phiên bản nội dung của một phụ lục đã áp dụng (FR-024, DBR-07) và trả mã; các cột đọc nhanh trích từ {@code
     * content}. Cùng ngày hiệu lực thì phiên bản có {@code sequenceNo} lớn hơn thắng khi tra theo ngày.
     */
    public long appendVersion(
            long contractId, int sequenceNo, LocalDate effectiveFrom, JsonNode content, long addendumId) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    PreparedStatement statement = connection.prepareStatement(
                            "INSERT INTO contract_version (kind, contract_id, sequence_no, effective_from,"
                                    + " addendum_id, content, residence_type, care_level, end_date, room_type_id,"
                                    + " deposit_amount, created_at, locked_at) VALUES ('CONTENT', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?,"
                                    + " ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, contractId);
                    statement.setInt(2, sequenceNo);
                    statement.setObject(3, effectiveFrom);
                    statement.setLong(4, addendumId);
                    statement.setString(5, content.toString());
                    statement.setString(6, content.get("residenceType").asString());
                    statement.setString(7, content.get("careLevel").asString());
                    statement.setObject(
                            8,
                            content.hasNonNull("endDate")
                                    ? LocalDate.parse(content.get("endDate").asString())
                                    : null);
                    statement.setObject(
                            9,
                            content.hasNonNull("roomTypeId")
                                    ? content.get("roomTypeId").asLong()
                                    : null);
                    statement.setObject(
                            10,
                            content.hasNonNull("depositAmount")
                                    ? content.get("depositAmount").decimalValue()
                                    : null);
                    statement.setObject(11, utc(time.now()));
                    statement.setObject(12, utc(time.now()));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Phụ lục gia hạn hoặc rút ngắn: đổi ngày kết thúc hiện hành và dấu quá hạn (FR-022, FR-029). */
    public void setCurrentEndDate(long id, LocalDate currentEndDate, LocalDate overdueSince) {
        jdbc.update(
                "UPDATE contract SET current_end_date = ?, overdue_since = ?, version = version + 1 WHERE id = ?",
                currentEndDate,
                overdueSince,
                id);
    }

    // ---- Đọc ------------------------------------------------------------------------------------------------------

    public Optional<Contract> find(long id) {
        return jdbc.query("SELECT * FROM contract WHERE id = ?", (rs, i) -> map(rs), id).stream()
                .findFirst()
                .map(this::withChildren);
    }

    /** Khóa dòng hợp đồng cho tới hết transaction (thứ tự khóa: sau theo người, research T-03). */
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

    public List<ExpiringContract> activeWithEndDate() {
        return jdbc.query(
                "SELECT c.id, c.resident_id, COALESCE((SELECT v.residence_type FROM contract_version v"
                        + " WHERE v.contract_id = c.id AND v.kind = 'CONTENT' ORDER BY v.sequence_no DESC LIMIT 1),"
                        + " c.residence_type)"
                        + " AS residence_type, c.current_end_date, c.overdue_since, c.manager_notified_overdue_at"
                        + " FROM contract c WHERE c.status = 'ACTIVE' AND c.current_end_date IS NOT NULL ORDER BY c.id",
                (rs, i) -> new ExpiringContract(
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
        return catalog.list(ServiceAttributes.KIND, null, false).stream().anyMatch(item -> item.id() == serviceId);
    }

    /** Loại, mức chăm sóc (chỉ dịch vụ chăm sóc, FR-025, Q-297) và trạng thái Hiệu lực của dịch vụ. */
    public Optional<ServiceKind> serviceKind(long serviceId) {
        return catalog.list(ServiceAttributes.KIND, null, false).stream()
                .filter(item -> item.id() == serviceId)
                .findFirst()
                .map(item -> new ServiceKind(
                        item.attributes().type().name(),
                        item.attributes().careLevelCode(),
                        item.status() == CatalogStatus.ACTIVE));
    }

    public Optional<ContractVersion> versionAt(long contractId, LocalDate date) {
        return jdbc
                .query(
                        "SELECT * FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ? AND effective_from <= ?"
                                + " ORDER BY effective_from DESC, sequence_no DESC LIMIT 1",
                        (rs, i) -> version(rs),
                        contractId,
                        date)
                .stream()
                .findFirst();
    }

    /** Phiên bản nội dung mới nhất (số thứ tự lớn nhất), nền cho phụ lục kế tiếp. */
    public Optional<ContractVersion> latestVersion(long contractId) {
        return jdbc
                .query(
                        "SELECT * FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ? ORDER BY sequence_no DESC"
                                + " LIMIT 1",
                        (rs, i) -> version(rs),
                        contractId)
                .stream()
                .findFirst();
    }

    public List<ContractVersion> versions(long contractId) {
        return jdbc.query(
                "SELECT * FROM contract_version WHERE kind = 'CONTENT' AND contract_id = ? ORDER BY sequence_no",
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

    private String servicesJson(ContractContent content) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (ServiceLine line : content.services()) {
            ObjectNode node = array.addObject();
            node.put("serviceId", line.serviceId());
            node.put("inPackage", line.inPackage());
            node.put("quantity", line.quantity());
            node.put("customUnitPrice", line.customUnitPrice());
        }
        return array.toString();
    }

    private String scheduleJson(ContractContent content) {
        ArrayNode array = JsonNodeFactory.instance.arrayNode();
        for (ArrivalLine line : content.arrivalSchedule()) {
            ObjectNode node = array.addObject();
            node.put("dayOfWeek", line.dayOfWeek());
            node.put("date", line.date() == null ? null : line.date().toString());
            node.put("arrivalTime", line.arrivalTime().toString());
            node.put("departureTime", line.departureTime().toString());
        }
        return array.toString();
    }

    private Contract withChildren(Contract contract) {
        Map<String, Object> draft = jdbc.queryForMap(
                "SELECT services, arrival_schedule FROM contract_version WHERE kind = 'DRAFT' AND contract_id = ?",
                contract.id());
        List<ServiceLine> services = new ArrayList<>();
        for (JsonNode node : json.readTree(String.valueOf(draft.get("services")))) {
            services.add(new ServiceLine(
                    node.get("serviceId").asLong(),
                    node.get("inPackage").asBoolean(),
                    node.get("quantity").decimalValue(),
                    node.hasNonNull("customUnitPrice")
                            ? node.get("customUnitPrice").decimalValue()
                            : null));
        }
        services.sort(Comparator.comparingLong(ServiceLine::serviceId));
        List<ArrivalLine> schedule = new ArrayList<>();
        for (JsonNode node : json.readTree(String.valueOf(draft.get("arrival_schedule")))) {
            schedule.add(new ArrivalLine(
                    node.hasNonNull("dayOfWeek") ? node.get("dayOfWeek").asInt() : null,
                    node.hasNonNull("date") ? LocalDate.parse(node.get("date").asString()) : null,
                    LocalTime.parse(node.get("arrivalTime").asString()),
                    LocalTime.parse(node.get("departureTime").asString())));
        }
        return contract.withContent(contract.content().withLines(services, schedule));
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
