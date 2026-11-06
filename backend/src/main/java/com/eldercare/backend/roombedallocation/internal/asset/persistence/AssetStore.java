package com.eldercare.backend.roombedallocation.internal.asset.persistence;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.dto.AssetStatus;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetInput;
import com.eldercare.backend.roombedallocation.internal.asset.domain.AssetRow;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/** 019 T017, T018: đọc, ghi {@code asset}; thời điểm theo {@link BusinessTime#ZONE}. */
@Repository
public class AssetStore {
    private static final String COLUMNS = "id, code, type_id, name, department_id, source, source_description,"
            + " procurement_request_id, acquisition_cost, in_service_on, maintenance_interval_days, maintenance_due_on,"
            + " license_plate, seat_count, vehicle_inspection_due_on, insurance_due_on, bed_id, status, location_type,"
            + " location_id, floor_area_id, maintenance_overdue, vehicle_inspection_overdue, insurance_overdue,"
            + " awaiting_resident_transfer, version";

    private final JdbcTemplate jdbc;

    public AssetStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean codeTaken(String code) {
        return Boolean.TRUE.equals(
                jdbc.queryForObject("SELECT EXISTS (SELECT 1 FROM asset WHERE code = ?)", Boolean.class, code));
    }

    /** Ghi tăng ở Sẵn sàng, vị trí "kho tài sản" (FR-013). */
    public long insert(AssetInput in, long createdBy, Instant at) {
        var keys = new GeneratedKeyHolder();
        jdbc.update(
                c -> {
                    PreparedStatement ps = c.prepareStatement(
                            "INSERT INTO asset (code, type_id, name, department_id, source, source_description,"
                                    + " procurement_request_id, acquisition_cost, in_service_on, maintenance_interval_days,"
                                    + " maintenance_due_on, license_plate, seat_count, vehicle_inspection_due_on,"
                                    + " insurance_due_on, status, location_type, created_by_staff_id, created_at)"
                                    + " VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'READY', 'ASSET_STORAGE', ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    ps.setString(1, in.code());
                    ps.setLong(2, in.typeId());
                    ps.setString(3, in.name());
                    ps.setLong(4, in.departmentId());
                    ps.setString(5, in.source());
                    ps.setString(6, in.sourceDescription());
                    ps.setObject(7, in.procurementRequestId());
                    ps.setObject(8, in.acquisitionCost());
                    int i = describe(ps, in, 9);
                    ps.setLong(i++, createdBy);
                    ps.setObject(i, LocalDateTime.ofInstant(at, BusinessTime.ZONE));
                    return ps;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** FR-011: sửa thông tin mô tả (nhóm 1); trạng thái, vị trí, bộ phận chỉ qua lệnh. */
    public void updateDescription(long id, AssetInput in) {
        jdbc.update(c -> {
            PreparedStatement ps = c.prepareStatement("UPDATE asset SET code = ?, type_id = ?, name = ?,"
                    + " source_description = ?, acquisition_cost = ?, in_service_on = ?, maintenance_interval_days = ?,"
                    + " maintenance_due_on = ?, license_plate = ?, seat_count = ?, vehicle_inspection_due_on = ?,"
                    + " insurance_due_on = ?, version = version + 1 WHERE id = ?");
            ps.setString(1, in.code());
            ps.setLong(2, in.typeId());
            ps.setString(3, in.name());
            ps.setString(4, in.sourceDescription());
            ps.setObject(5, in.acquisitionCost());
            int i = describe(ps, in, 6);
            ps.setLong(i, id);
            return ps;
        });
    }

    public void updateStatus(long id, AssetStatus status) {
        jdbc.update("UPDATE asset SET status = ?, version = version + 1 WHERE id = ?", status.name(), id);
    }

    public void updateLocation(long id, AssetStatus status, String type, Long locationId, Long floorAreaId) {
        jdbc.update(
                "UPDATE asset SET status = ?, location_type = ?, location_id = ?, floor_area_id = ?,"
                        + " version = version + 1 WHERE id = ?",
                status.name(),
                type,
                locationId,
                floorAreaId,
                id);
    }

    /** Kết quả bảo trì: trạng thái mới; Đạt thì gỡ dấu quá hạn bảo trì và đặt hạn kế tiếp (FR-014). */
    public void maintenanceResult(long id, AssetStatus status, LocalDate nextDue, boolean clearOverdue) {
        jdbc.update(
                "UPDATE asset SET status = ?, maintenance_due_on = IF(?, ?, maintenance_due_on),"
                        + " maintenance_overdue = IF(?, FALSE, maintenance_overdue), version = version + 1 WHERE id = ?",
                status.name(),
                nextDue != null,
                nextDue,
                clearOverdue,
                id);
    }

    /** FR-014: kết quả Đạt của lần đăng kiểm, bảo hiểm đặt hạn mới và gỡ dấu tương ứng. */
    public void renewDue(long id, AssetStatus status, String dueColumn, String flagColumn, LocalDate nextDue) {
        if (!Set.of("vehicle_inspection_due_on", "insurance_due_on").contains(dueColumn)
                || !Set.of("vehicle_inspection_overdue", "insurance_overdue").contains(flagColumn))
            throw new IllegalArgumentException(dueColumn + "," + flagColumn);
        jdbc.update(
                "UPDATE asset SET status = ?, " + dueColumn + " = ?, " + flagColumn + " = FALSE, version = version + 1"
                        + " WHERE id = ?",
                status.name(),
                nextDue,
                id);
    }

    /** BR-M03-17: gắn dấu quá hạn; {@code true} khi vừa gắn. */
    public boolean flagOverdue(long id, String flagColumn) {
        if (!Set.of("maintenance_overdue", "vehicle_inspection_overdue", "insurance_overdue")
                .contains(flagColumn)) throw new IllegalArgumentException(flagColumn);
        return jdbc.update(
                        "UPDATE asset SET " + flagColumn + " = TRUE, version = version + 1 WHERE id = ? AND NOT "
                                + flagColumn,
                        id)
                == 1;
    }

    /** Tài sản chưa thanh lý có ít nhất một hạn bảo trì, đăng kiểm, bảo hiểm (job W-09). */
    public List<AssetRow> withDueDates() {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM asset WHERE status <> 'DISPOSED' AND (maintenance_due_on IS NOT NULL"
                        + " OR vehicle_inspection_due_on IS NOT NULL OR insurance_due_on IS NOT NULL) ORDER BY id",
                this::row);
    }

    public Optional<Long> disposalRequest(long id) {
        return Optional.ofNullable(
                jdbc.queryForObject("SELECT disposal_request_id FROM asset WHERE id = ?", Long.class, id));
    }

    public void setDisposalRequest(long id, long requestId) {
        jdbc.update("UPDATE asset SET disposal_request_id = ?, version = version + 1 WHERE id = ?", requestId, id);
    }

    public void dispose(long id, Instant at) {
        jdbc.update(
                "UPDATE asset SET status = 'DISPOSED', disposed_at = ?, version = version + 1 WHERE id = ?",
                LocalDateTime.ofInstant(at, BusinessTime.ZONE),
                id);
    }

    /** W-09: tài sản Đã thanh lý trước {@code cutoff} (mốc tính hạn lưu giữ). */
    public List<long[]> disposedBefore(Instant cutoff, int batch) {
        return jdbc.query(
                "SELECT id, disposed_at FROM asset WHERE status = 'DISPOSED' AND disposed_at < ? ORDER BY disposed_at, id"
                        + " LIMIT ?",
                (rs, i) -> new long[] {
                    rs.getLong(1),
                    rs.getObject(2, LocalDateTime.class)
                            .atZone(BusinessTime.ZONE)
                            .toInstant()
                            .toEpochMilli()
                },
                LocalDateTime.ofInstant(cutoff, BusinessTime.ZONE),
                batch);
    }

    public void delete(JdbcTemplate retention, java.util.Collection<Long> ids) {
        if (ids.isEmpty()) return;
        retention.update(
                "DELETE FROM asset WHERE id IN (" + String.join(",", java.util.Collections.nCopies(ids.size(), "?"))
                        + ")",
                ids.toArray());
    }

    public void updateDepartment(long id, long departmentId) {
        jdbc.update("UPDATE asset SET department_id = ?, version = version + 1 WHERE id = ?", departmentId, id);
    }

    public Optional<AssetRow> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM asset WHERE id = ?", this::row, id).stream()
                .findFirst();
    }

    public Optional<AssetRow> lock(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM asset WHERE id = ? FOR UPDATE", this::row, id).stream()
                .findFirst();
    }

    public List<AssetRow> list(Long departmentId, Long typeId, AssetStatus status) {
        var sql = new StringBuilder("SELECT " + COLUMNS + " FROM asset WHERE 1 = 1");
        var args = new ArrayList<Object>();
        if (departmentId != null) {
            sql.append(" AND department_id = ?");
            args.add(departmentId);
        }
        if (typeId != null) {
            sql.append(" AND type_id = ?");
            args.add(typeId);
        }
        if (status != null) {
            sql.append(" AND status = ?");
            args.add(status.name());
        }
        sql.append(" ORDER BY code");
        return jdbc.query(sql.toString(), this::row, args.toArray());
    }

    private static int describe(PreparedStatement ps, AssetInput in, int i) throws SQLException {
        ps.setObject(i++, in.inServiceOn());
        ps.setObject(i++, in.maintenanceIntervalDays());
        ps.setObject(i++, in.maintenanceDueOn());
        ps.setString(i++, in.licensePlate());
        ps.setObject(i++, in.seatCount());
        ps.setObject(i++, in.vehicleInspectionDueOn());
        ps.setObject(i++, in.insuranceDueOn());
        return i;
    }

    private AssetRow row(ResultSet rs, int n) throws SQLException {
        return new AssetRow(
                rs.getLong("id"),
                rs.getString("code"),
                rs.getLong("type_id"),
                rs.getString("name"),
                rs.getLong("department_id"),
                rs.getString("source"),
                rs.getString("source_description"),
                rs.getObject("procurement_request_id", Long.class),
                rs.getObject("acquisition_cost", Long.class),
                rs.getObject("in_service_on", LocalDate.class),
                rs.getObject("maintenance_interval_days", Integer.class),
                rs.getObject("maintenance_due_on", LocalDate.class),
                rs.getString("license_plate"),
                rs.getObject("seat_count", Integer.class),
                rs.getObject("vehicle_inspection_due_on", LocalDate.class),
                rs.getObject("insurance_due_on", LocalDate.class),
                rs.getObject("bed_id", Long.class),
                AssetStatus.valueOf(rs.getString("status")),
                rs.getString("location_type"),
                rs.getObject("location_id", Long.class),
                rs.getObject("floor_area_id", Long.class),
                rs.getBoolean("maintenance_overdue"),
                rs.getBoolean("vehicle_inspection_overdue"),
                rs.getBoolean("insurance_overdue"),
                rs.getBoolean("awaiting_resident_transfer"),
                rs.getInt("version"));
    }
}
