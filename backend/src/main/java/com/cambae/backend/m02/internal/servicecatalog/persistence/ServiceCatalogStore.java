package com.cambae.backend.m02.internal.servicecatalog.persistence;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.api.dto.ServiceType;
import com.cambae.backend.m02.internal.servicecatalog.domain.MismatchedContract;
import com.cambae.backend.m02.internal.servicecatalog.domain.PriceVersion;
import com.cambae.backend.m02.internal.servicecatalog.domain.ServiceRecord;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Truy cập bảng {@code service} và {@code price_version} cho các lệnh của Quản lý viện (data-model mục 6). Hàm ghi chạy
 * trong transaction của lệnh; thêm phiên bản đơn giá dưới khóa dòng {@code service} do {@code EffectiveVersioning} giữ.
 */
@Repository
public class ServiceCatalogStore {

    private static final String COLUMNS =
            "id, type, name, unit, billing_period, care_level_code, room_type_id, medication_id, status, version";

    private static final String VERSION_COLUMNS =
            "id, service_id, unit_price, effective_from, effective_to, referenced";

    private static final RowMapper<ServiceRecord> SERVICE = (rs, row) -> new ServiceRecord(
            rs.getLong("id"),
            ServiceType.valueOf(rs.getString("type")),
            rs.getString("name"),
            rs.getString("unit"),
            BillingPeriod.valueOf(rs.getString("billing_period")),
            rs.getString("care_level_code") == null ? null : CareLevel.valueOf(rs.getString("care_level_code")),
            nullableLong(rs, "room_type_id"),
            nullableLong(rs, "medication_id"),
            "ACTIVE".equals(rs.getString("status")),
            rs.getLong("version"));

    private static final RowMapper<PriceVersion> VERSION = (rs, row) -> new PriceVersion(
            rs.getLong("id"),
            rs.getLong("service_id"),
            rs.getBigDecimal("unit_price"),
            rs.getObject("effective_from", LocalDate.class),
            rs.getObject("effective_to", LocalDate.class),
            rs.getBoolean("referenced"));

    private final JdbcTemplate jdbc;

    ServiceCatalogStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static Long nullableLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }

    public List<ServiceRecord> list(ServiceType type) {
        return type == null
                ? jdbc.query("SELECT " + COLUMNS + " FROM service ORDER BY id", SERVICE)
                : jdbc.query("SELECT " + COLUMNS + " FROM service WHERE type = ? ORDER BY id", SERVICE, type.name());
    }

    public Optional<ServiceRecord> find(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM service WHERE id = ?", SERVICE, id).stream()
                .findFirst();
    }

    /** Khóa dòng dịch vụ tới hết transaction (tuần tự hóa sửa, ngừng hiệu lực và thêm phiên bản). */
    public Optional<ServiceRecord> lock(long id) {
        return jdbc.query("SELECT " + COLUMNS + " FROM service WHERE id = ? FOR UPDATE", SERVICE, id).stream()
                .findFirst();
    }

    public boolean activeCareServiceExists(CareLevel level) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM service WHERE type = 'CARE' AND status = 'ACTIVE' AND care_level_code = ?)",
                Boolean.class,
                level.name()));
    }

    public long insert(ServiceRecord service) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO service (type, name, unit, billing_period, care_level_code, room_type_id,"
                                    + " medication_id) VALUES (?, ?, ?, ?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setString(1, service.type().name());
                    statement.setString(2, service.name());
                    statement.setString(3, service.unit());
                    statement.setString(4, service.billingPeriod().name());
                    statement.setString(
                            5,
                            service.careLevel() == null
                                    ? null
                                    : service.careLevel().name());
                    statement.setObject(6, service.roomTypeId());
                    statement.setObject(7, service.medicationId());
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    /** Đổi tên, đơn vị tính, kỳ tính; 0 nếu {@code version} không còn là phiên bản hiện tại. */
    public int update(long id, long version, String name, String unit, BillingPeriod billingPeriod) {
        return jdbc.update(
                "UPDATE service SET name = ?, unit = ?, billing_period = ?, version = version + 1"
                        + " WHERE id = ? AND version = ?",
                name,
                unit,
                billingPeriod.name(),
                id,
                version);
    }

    public int deactivate(long id, long version) {
        return jdbc.update(
                "UPDATE service SET status = 'INACTIVE', version = version + 1 WHERE id = ? AND version = ?",
                id,
                version);
    }

    public List<PriceVersion> versions(long serviceId) {
        return jdbc.query(
                "SELECT " + VERSION_COLUMNS + " FROM price_version WHERE service_id = ? ORDER BY effective_from",
                VERSION,
                serviceId);
    }

    public boolean hasReferencedVersion(long serviceId) {
        return Boolean.TRUE.equals(jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM price_version WHERE service_id = ? AND referenced)",
                Boolean.class,
                serviceId));
    }

    public long insertVersion(long serviceId, BigDecimal unitPrice, LocalDate from, LocalDate to) {
        GeneratedKeyHolder keys = new GeneratedKeyHolder();
        jdbc.update(
                connection -> {
                    var statement = connection.prepareStatement(
                            "INSERT INTO price_version (service_id, unit_price, effective_from, effective_to)"
                                    + " VALUES (?, ?, ?, ?)",
                            Statement.RETURN_GENERATED_KEYS);
                    statement.setLong(1, serviceId);
                    statement.setBigDecimal(2, unitPrice);
                    statement.setDate(3, Date.valueOf(from));
                    statement.setObject(4, to == null ? null : Date.valueOf(to));
                    return statement;
                },
                keys);
        return keys.getKey().longValue();
    }

    public Optional<PriceVersion> findVersion(long id) {
        return jdbc.query("SELECT " + VERSION_COLUMNS + " FROM price_version WHERE id = ?", VERSION, id).stream()
                .findFirst();
    }

    /** Hợp đồng Hiệu lực có dòng dịch vụ ghi đơn giá khác {@code latest} (FR-036). */
    public List<MismatchedContract> mismatchedContracts(long serviceId, BigDecimal latest) {
        return jdbc.query(
                "SELECT c.id, c.contract_number, cs.recorded_unit_price FROM contract_service cs"
                        + " JOIN contract c ON c.id = cs.contract_id"
                        + " WHERE cs.service_id = ? AND c.status = 'ACTIVE' AND cs.recorded_unit_price IS NOT NULL"
                        + " AND cs.recorded_unit_price <> ? ORDER BY c.id",
                (rs, row) -> new MismatchedContract(
                        rs.getLong("id"),
                        rs.getString("contract_number"),
                        rs.getBigDecimal("recorded_unit_price"),
                        latest),
                serviceId,
                latest);
    }
}
