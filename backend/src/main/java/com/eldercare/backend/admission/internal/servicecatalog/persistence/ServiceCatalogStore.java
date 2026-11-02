package com.eldercare.backend.admission.internal.servicecatalog.persistence;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.admission.api.dto.BillingPeriod;
import com.eldercare.backend.admission.api.dto.ServiceType;
import com.eldercare.backend.admission.internal.domain.ServiceAttributes;
import com.eldercare.backend.admission.internal.servicecatalog.domain.MismatchedContract;
import com.eldercare.backend.admission.internal.servicecatalog.domain.PriceVersion;
import com.eldercare.backend.admission.internal.servicecatalog.domain.ServiceRecord;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.resident.api.dto.CareLevel;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Repository;

/**
 * Dịch vụ ở {@code catalog_item} (loại {@code M02.SERVICE}) và {@code price_version} (data-model mục 0, 6; R-15). Hàm ghi chạy trong
 * transaction của lệnh; lệnh trên một dịch vụ tuần tự hóa bằng khóa dòng {@code catalog_item} của dịch vụ.
 */
@Repository
public class ServiceCatalogStore {

    private static final String VERSION_COLUMNS =
            "id, service_id, unit_price, effective_from, effective_to, referenced";

    private static final RowMapper<PriceVersion> VERSION = (rs, row) -> new PriceVersion(
            rs.getLong("id"),
            rs.getLong("service_id"),
            rs.getBigDecimal("unit_price"),
            rs.getObject("effective_from", LocalDate.class),
            rs.getObject("effective_to", LocalDate.class),
            rs.getBoolean("referenced"));

    private final JdbcTemplate jdbc;
    private final CatalogApi catalog;

    ServiceCatalogStore(JdbcTemplate jdbc, CatalogApi catalog) {
        this.jdbc = jdbc;
        this.catalog = catalog;
    }

    public List<ServiceRecord> list(ServiceType type) {
        return catalog.list(ServiceAttributes.KIND, null, false).stream()
                .sorted(Comparator.comparingLong(CatalogItem::id))
                .map(ServiceCatalogStore::record)
                .filter(service -> type == null || service.type() == type)
                .toList();
    }

    public Optional<ServiceRecord> find(long id) {
        return catalog.list(ServiceAttributes.KIND, null, false).stream()
                .filter(item -> item.id() == id)
                .findFirst()
                .map(ServiceCatalogStore::record);
    }

    /** Khóa dịch vụ tới hết transaction (tuần tự hóa sửa, ngừng hiệu lực và thêm phiên bản). */
    public Optional<ServiceRecord> lock(long id) {
        jdbc.queryForList("SELECT id FROM catalog_item WHERE id = ? AND catalog_type = 'M02.SERVICE' FOR UPDATE", id);
        return find(id);
    }

    public boolean activeCareServiceExists(CareLevel level) {
        return catalog.list(ServiceAttributes.KIND, null, true).stream()
                .anyMatch(item -> item.attributes().type() == ServiceType.CARE
                        && item.attributes().careLevelCode() == level);
    }

    /** Trùng tên đang Hiệu lực thì {@code DuplicateKeyException} như ràng buộc {@code active_name} (BR-M15-06). */
    public long insert(ServiceRecord service) {
        if (catalog.list(ServiceAttributes.KIND, null, true).stream()
                .anyMatch(item -> item.name().equals(service.name()))) {
            throw new DuplicateKeyException("Trùng tên dịch vụ Hiệu lực: " + service.name());
        }
        return catalog.create(
                        ServiceAttributes.KIND,
                        new NewCatalogItem<>(
                                null,
                                service.name(),
                                null,
                                0,
                                new ServiceAttributes(
                                        service.type(),
                                        service.unit(),
                                        service.billingPeriod(),
                                        service.careLevel(),
                                        service.roomTypeId(),
                                        service.medicationId())))
                .id();
    }

    /** Đổi tên, đơn vị tính, kỳ tính; 412 nếu {@code version} không còn là phiên bản hiện tại. */
    public void update(long id, long version, String name, String unit, BillingPeriod billingPeriod) {
        CatalogItem<ServiceAttributes> current = catalog.require(ServiceAttributes.KIND, id);
        ServiceAttributes a = current.attributes();
        catalog.update(
                ServiceAttributes.KIND,
                id,
                (int) version,
                name,
                current.sortOrder(),
                new ServiceAttributes(
                        a.type(), unit, billingPeriod, a.careLevelCode(), a.roomTypeId(), a.medicationId()));
    }

    public void deactivate(long id, long version) {
        catalog.deactivate(ServiceAttributes.KIND, id, (int) version);
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
        String path = "$.\"" + serviceId + "\"";
        return jdbc.query(
                "SELECT c.id, c.contract_number, CAST(JSON_EXTRACT(v.recorded_prices, ?) AS DECIMAL(15, 0)) AS price"
                        + " FROM contract_version v JOIN contract c ON c.id = v.contract_id"
                        + " WHERE v.kind = 'DRAFT' AND c.status = 'ACTIVE' AND JSON_CONTAINS_PATH(v.recorded_prices, 'one', ?)"
                        + " AND CAST(JSON_EXTRACT(v.recorded_prices, ?) AS DECIMAL(15, 0)) <> ? ORDER BY c.id",
                (rs, row) -> new MismatchedContract(
                        rs.getLong("id"), rs.getString("contract_number"), rs.getBigDecimal("price"), latest),
                path,
                path,
                path,
                latest);
    }

    private static ServiceRecord record(CatalogItem<ServiceAttributes> item) {
        ServiceAttributes a = item.attributes();
        return new ServiceRecord(
                item.id(),
                a.type(),
                item.name(),
                a.unit(),
                a.billingPeriod(),
                a.careLevelCode(),
                a.roomTypeId(),
                a.medicationId(),
                item.status() == CatalogStatus.ACTIVE,
                item.version());
    }
}
