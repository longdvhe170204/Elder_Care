package com.cambae.backend.m02.internal.servicecatalog.service;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.UnitPriceApi;
import com.cambae.backend.m02.api.dto.BillingPeriod;
import com.cambae.backend.m02.api.dto.ServiceType;
import com.cambae.backend.m02.api.dto.ServiceView;
import com.cambae.backend.m02.api.dto.UnitPriceView;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Đơn giá và tra dịch vụ (research T-05; FR-034, FR-035, BR-M11-02, Q-297). Đơn giá hiệu lực tại ngày là phiên bản có
 * {@code effective_from <= ngày <= effective_to} (hoặc còn mở); không có thì trả rỗng để 010 ghi khoản "thiếu đơn giá"
 * (010 FR-012). {@code markReferenced} chạy trong transaction của người gọi và đặt cờ {@code referenced}, từ đó giá và
 * ngày bắt đầu của phiên bản không sửa được (trigger {@code trg_price_version_immutable}).
 *
 * <p>{@link #contractUnitPrice} chưa có nguồn: nội dung hợp đồng (bảng {@code contract_content_version}) thuộc story hợp
 * đồng, nên tới lúc đó hàm trả rỗng. Phí lưu trú theo loại phòng và thuốc nguồn viện chưa có ràng buộc duy nhất; hàm tra
 * trả dịch vụ Hiệu lực có mã nhỏ nhất.
 */
@Service
public class UnitPriceService implements UnitPriceApi {

    private static final String SERVICE_COLUMNS = "id, type, name, unit, billing_period";

    private static final RowMapper<ServiceView> SERVICE = (rs, row) -> new ServiceView(
            rs.getLong("id"),
            ServiceType.valueOf(rs.getString("type")),
            rs.getString("name"),
            rs.getString("unit"),
            BillingPeriod.valueOf(rs.getString("billing_period")));

    private final JdbcTemplate jdbc;

    UnitPriceService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(propagation = Propagation.SUPPORTS)
    public Optional<UnitPriceView> unitPriceAt(long serviceId, LocalDate date, boolean markReferenced) {
        if (markReferenced && !TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalTransactionStateException(
                    "Đánh dấu đã tham chiếu phải chạy trong transaction của người gọi (BR-M11-02)");
        }
        Optional<UnitPriceView> found = jdbc
                .query(
                        "SELECT id, service_id, unit_price, effective_from, effective_to FROM price_version"
                                + " WHERE service_id = ? AND effective_from <= ? AND (effective_to IS NULL OR effective_to >= ?)"
                                + (markReferenced ? " FOR UPDATE" : ""),
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
        if (markReferenced) {
            found.ifPresent(view ->
                    jdbc.update("UPDATE price_version SET referenced = TRUE WHERE id = ?", view.priceVersionId()));
        }
        return found;
    }

    @Override
    public Optional<UnitPriceView> contractUnitPrice(long residentId, long serviceId, LocalDate date) {
        return Optional.empty();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceView> careServiceOf(CareLevel level) {
        return activeService("type = 'CARE' AND active_care_level = ?", level.name());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceView> residencyFeeServiceOf(long roomTypeId) {
        return activeService("type = 'RESIDENCY_FEE' AND room_type_id = ?", roomTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ServiceView> facilityMedicationServiceOf(long medicationId) {
        return activeService("type = 'FACILITY_SUPPLIED_MEDICATION' AND medication_id = ?", medicationId);
    }

    private Optional<ServiceView> activeService(String condition, Object argument) {
        return jdbc
                .query(
                        "SELECT " + SERVICE_COLUMNS + " FROM service WHERE status = 'ACTIVE' AND " + condition
                                + " ORDER BY id LIMIT 1",
                        SERVICE,
                        argument)
                .stream()
                .findFirst();
    }
}
