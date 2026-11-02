package com.eldercare.backend.admission.internal.servicecatalog.persistence;

import com.eldercare.backend.admission.api.dto.UnitPriceView;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Đọc phiên bản đơn giá hiệu lực tại ngày, đánh dấu đã tham chiếu cho {@code UnitPriceService} (research T-05; FR-035). */
@Repository
public class PriceVersionStore {

    private final JdbcTemplate jdbc;

    PriceVersionStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<UnitPriceView> effectiveAt(long serviceId, LocalDate date, boolean forUpdate) {
        return jdbc
                .query(
                        "SELECT id, service_id, unit_price, effective_from, effective_to FROM price_version"
                                + " WHERE service_id = ? AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?)"
                                + (forUpdate ? " FOR UPDATE" : ""),
                        (rs, row) -> new UnitPriceView(
                                rs.getLong("service_id"),
                                rs.getLong("id"),
                                rs.getBigDecimal("unit_price"),
                                rs.getObject("effective_from", LocalDate.class),
                                rs.getObject("effective_to", LocalDate.class)),
                        serviceId,
                        Date.valueOf(date),
                        Date.valueOf(date))
                .stream()
                .findFirst();
    }

    public void markReferenced(long priceVersionId) {
        jdbc.update("UPDATE price_version SET referenced = TRUE WHERE id = ?", priceVersionId);
    }
}
