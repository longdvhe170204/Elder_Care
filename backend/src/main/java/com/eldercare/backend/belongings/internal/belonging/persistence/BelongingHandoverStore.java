package com.eldercare.backend.belongings.internal.belonging.persistence;

import com.eldercare.backend.belongings.api.dto.BelongingStatus;
import com.eldercare.backend.belongings.internal.belonging.domain.GiverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverRow;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverType;
import com.eldercare.backend.belongings.internal.belonging.domain.HandoverView;
import com.eldercare.backend.belongings.internal.belonging.domain.NewHandover;
import com.eldercare.backend.belongings.internal.belonging.domain.ReturnFacts;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** Thêm, đọc bản ghi bàn giao {@code belonging_handover} (nhóm 3: không sửa, không xóa; data-model mục 4). */
@Repository
public class BelongingHandoverStore {

    private static final String COLUMNS =
            "id, belonging_id, kind, target_status, giver_type, giver_name, recipient_type,"
                    + " recipient_name, occurred_at, quantity, cash_amount, condition_level_id, condition_description,"
                    + " storage_location_id, reason, photo_file_ids, incident_id, recorded_at";

    private final JdbcTemplate jdbc;

    BelongingHandoverStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public long insert(NewHandover h) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var ps = connection.prepareStatement(
                            "INSERT INTO belonging_handover (belonging_id, resident_id, kind, target_status, giver_type,"
                                    + " giver_id, giver_name, giver_id_document, recipient_type, recipient_id,"
                                    + " recipient_name, occurred_at, quantity, cash_amount, condition_level_id,"
                                    + " condition_description, storage_location_id, reason, holder_absent, valuable,"
                                    + " photo_file_ids, note, recorded_by, recorded_at, recipient_id_document_type,"
                                    + " recipient_id_document_number, recipient_basis, verification_result,"
                                    + " signature_file_id, no_signature_reason, condition_opinion,"
                                    + " recipient_confirmation_id, block_reason, inventory_check_id, approval_request_id,"
                                    + " receiving_unit)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, CAST(? AS JSON),"
                                    + " ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    int i = 1;
                    ps.setLong(i++, h.belongingId());
                    ps.setLong(i++, h.residentId());
                    ps.setString(i++, h.kind().name());
                    ps.setString(i++, h.targetStatus().name());
                    ps.setString(
                            i++, h.giverType() == null ? null : h.giverType().name());
                    ps.setObject(i++, h.giverId());
                    ps.setString(i++, h.giverName());
                    ps.setString(i++, h.giverIdDocument());
                    ps.setString(
                            i++,
                            h.recipientType() == null ? null : h.recipientType().name());
                    ps.setObject(i++, h.recipientId());
                    ps.setString(i++, h.recipientName());
                    ps.setObject(i++, utc(h.occurredAt()));
                    ps.setBigDecimal(i++, h.quantity());
                    ps.setBigDecimal(i++, h.cashAmount());
                    ps.setObject(i++, h.conditionLevelId());
                    ps.setString(i++, h.conditionDescription());
                    ps.setObject(i++, h.storageLocationId());
                    ps.setString(i++, h.reason());
                    ps.setBoolean(i++, h.holderAbsent());
                    ps.setBoolean(i++, h.valuable());
                    ps.setString(
                            i++,
                            h.photoFileIds().stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]")));
                    ps.setString(i++, h.note());
                    ps.setLong(i++, h.recordedBy());
                    ps.setObject(i++, utc(h.recordedAt()));
                    ReturnFacts facts = h.returnFacts();
                    ps.setString(i++, facts == null ? null : facts.documentType());
                    ps.setString(i++, facts == null ? null : facts.documentNumber());
                    ps.setString(
                            i++,
                            facts == null || facts.basis() == null
                                    ? null
                                    : facts.basis().name());
                    ps.setString(
                            i++,
                            facts == null || facts.verification() == null
                                    ? null
                                    : facts.verification().name());
                    ps.setObject(i++, facts == null ? null : facts.signatureFileId());
                    ps.setString(i++, facts == null ? null : facts.noSignatureReason());
                    ps.setString(i++, facts == null ? null : facts.conditionOpinion());
                    ps.setObject(i++, facts == null ? null : facts.recipientConfirmationId());
                    ps.setString(
                            i++,
                            facts == null || facts.blockReason() == null
                                    ? null
                                    : facts.blockReason().name());
                    ps.setObject(i++, h.inventoryCheckId());
                    ps.setObject(i++, h.approvalRequestId());
                    ps.setString(i, h.receivingUnit());
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Lịch sử của mọi đồ cùng lần tiếp nhận (gồm đồ tách; FR-012), theo thứ tự ghi. */
    public List<HandoverView> ofIntake(long intakeHandoverId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM belonging_handover WHERE kind <> 'RETURN_BLOCKED' AND belonging_id IN"
                        + " (SELECT id FROM belonging WHERE intake_handover_id = ?) ORDER BY id",
                BelongingHandoverStore::view,
                intakeHandoverId);
    }

    private static final String ROW_COLUMNS =
            "id, belonging_id, resident_id, kind, target_status, recipient_type, recipient_id, quantity, cash_amount,"
                    + " condition_level_id, storage_location_id, incident_id, recorded_by, occurred_at, recorded_at";

    public Optional<HandoverRow> find(long handoverId) {
        return jdbc
                .query(
                        "SELECT " + ROW_COLUMNS + " FROM belonging_handover WHERE id = ?",
                        BelongingHandoverStore::row,
                        handoverId)
                .stream()
                .findFirst();
    }

    /** Bản ghi của các đồ, theo thứ tự ghi (để tính lại đồ sau đính chính, research K-05). */
    public List<HandoverRow> ofBelongings(Collection<Long> belongingIds) {
        if (belongingIds.isEmpty()) {
            return List.of();
        }
        String marks = String.join(", ", Collections.nCopies(belongingIds.size(), "?"));
        return jdbc.query(
                "SELECT " + ROW_COLUMNS + " FROM belonging_handover WHERE kind <> 'RETURN_BLOCKED'"
                        + " AND belonging_id IN (" + marks + ") ORDER BY id",
                BelongingHandoverStore::row,
                belongingIds.toArray());
    }

    /**
     * Đặt sự cố cho bản ghi một lần (FR-021, FR-024); trigger chỉ cho đặt từ {@code NULL}. {@code false} khi bản ghi đã
     * có sự cố.
     */
    public boolean linkIncident(long handoverId, long incidentId) {
        return jdbc.update(
                        "UPDATE belonging_handover SET incident_id = ? WHERE id = ? AND incident_id IS NULL",
                        incidentId,
                        handoverId)
                == 1;
    }

    /** Sự cố của lần Báo thất lạc gần nhất của đồ (để Tìm thấy ghi diễn biến, FR-025); trống nếu không có. */
    public Optional<Long> lossIncidentOf(long belongingId) {
        return jdbc
                .query(
                        "SELECT incident_id FROM belonging_handover WHERE belonging_id = ? AND kind = 'REPORT_LOST'"
                                + " AND incident_id IS NOT NULL ORDER BY id DESC LIMIT 1",
                        (rs, i) -> rs.getLong(1),
                        belongingId)
                .stream()
                .findFirst();
    }

    private static HandoverRow row(ResultSet rs, int row) throws SQLException {
        String recipientType = rs.getString("recipient_type");
        return new HandoverRow(
                rs.getLong("id"),
                rs.getLong("belonging_id"),
                rs.getLong("resident_id"),
                HandoverType.valueOf(rs.getString("kind")),
                BelongingStatus.valueOf(rs.getString("target_status")),
                recipientType == null ? null : GiverType.valueOf(recipientType),
                rs.getObject("recipient_id", Long.class),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("cash_amount"),
                rs.getObject("condition_level_id", Long.class),
                rs.getObject("storage_location_id", Long.class),
                rs.getObject("incident_id", Long.class),
                rs.getLong("recorded_by"),
                instant(rs, "occurred_at"),
                instant(rs, "recorded_at"));
    }

    private static HandoverView view(ResultSet rs, int row) throws SQLException {
        String photos = rs.getString("photo_file_ids").replaceAll("[\\[\\]\\s]", "");
        return new HandoverView(
                rs.getLong("id"),
                rs.getLong("belonging_id"),
                rs.getString("kind"),
                rs.getString("target_status"),
                rs.getString("giver_type"),
                rs.getString("giver_name"),
                rs.getString("recipient_type"),
                rs.getString("recipient_name"),
                instant(rs, "occurred_at"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("cash_amount"),
                rs.getObject("condition_level_id", Long.class),
                rs.getString("condition_description"),
                rs.getObject("storage_location_id", Long.class),
                rs.getString("reason"),
                photos.isEmpty()
                        ? List.of()
                        : Arrays.stream(photos.split(",")).map(Long::valueOf).toList(),
                rs.getObject("incident_id", Long.class),
                instant(rs, "recorded_at"));
    }

    static LocalDateTime utc(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        LocalDateTime value = rs.getObject(column, LocalDateTime.class);
        return value == null ? null : value.toInstant(ZoneOffset.UTC);
    }
}
