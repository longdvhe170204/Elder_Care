package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m02.api.UnitPriceApi;
import com.cambae.backend.m02.api.dto.ServiceType;
import com.cambae.backend.m02.api.dto.UnitPriceView;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 004 T008 (research T-05; FR-034, FR-035, BR-M11-02, Q-297): đơn giá hiệu lực tại ngày, đánh dấu đã tham chiếu và tra
 * dịch vụ theo mức chăm sóc, loại phòng, thuốc nguồn viện.
 */
class UnitPriceServiceIT extends MySqlIntegrationTestBase {

    @Autowired
    UnitPriceApi prices;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    PlatformTransactionManager transactions;

    @BeforeEach
    void deactivateCareServices() {
        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE type = 'CARE'");
    }

    private long service(String type, String careLevel, Long roomTypeId, Long medicationId) {
        String name = "Dịch vụ " + UUID.randomUUID();
        jdbc.update(
                "INSERT INTO service (type, name, unit, billing_period, care_level_code, room_type_id, medication_id)"
                        + " VALUES (?, ?, 'tháng', 'MONTH', ?, ?, ?)",
                type,
                name,
                careLevel,
                roomTypeId,
                medicationId);
        return jdbc.queryForObject("SELECT id FROM service WHERE name = ?", Long.class, name);
    }

    private void version(long serviceId, int price, String from, String to) {
        jdbc.update(
                "INSERT INTO price_version (service_id, unit_price, effective_from, effective_to) VALUES (?, ?, ?, ?)",
                serviceId,
                price,
                from,
                to);
    }

    @Test
    void BR_M11_02_unitPriceAt_returnsVersionCoveringTheDate_bothEndsIncluded() {
        long id = service("MEAL", null, null, null);
        version(id, 100, "2030-01-01", "2030-06-30");
        version(id, 120, "2030-07-01", null);

        assertThat(prices.unitPriceAt(id, LocalDate.of(2030, 1, 1), false).map(UnitPriceView::unitPrice))
                .contains(new BigDecimal("100"));
        assertThat(prices.unitPriceAt(id, LocalDate.of(2030, 6, 30), false).map(UnitPriceView::unitPrice))
                .contains(new BigDecimal("100"));
        assertThat(prices.unitPriceAt(id, LocalDate.of(2030, 7, 1), false).map(UnitPriceView::unitPrice))
                .contains(new BigDecimal("120"));
        assertThat(prices.unitPriceAt(id, LocalDate.of(2040, 1, 1), false).map(UnitPriceView::unitPrice))
                .contains(new BigDecimal("120"));
    }

    @Test
    void FR_035_unitPriceAt_isEmptyWhenNoVersionCoversTheDate() {
        long id = service("MEAL", null, null, null);
        version(id, 100, "2030-03-01", "2030-03-31");
        version(id, 120, "2030-05-01", null);

        assertThat(prices.unitPriceAt(id, LocalDate.of(2030, 2, 28), false)).isEmpty();
        assertThat(prices.unitPriceAt(id, LocalDate.of(2030, 4, 15), false)).isEmpty();
        assertThat(prices.unitPriceAt(id + 100000, LocalDate.of(2030, 3, 10), false))
                .isEmpty();
    }

    @Test
    void BR_M11_02_markReferenced_setsFlagOnTheVersionUsed_otherwiseLeavesIt() {
        long id = service("MEAL", null, null, null);
        version(id, 100, "2030-01-01", "2030-06-30");
        version(id, 120, "2030-07-01", null);
        TransactionTemplate tx = new TransactionTemplate(transactions);

        tx.executeWithoutResult(status -> prices.unitPriceAt(id, LocalDate.of(2030, 2, 1), false));
        assertThat(referenced(id)).isEqualTo(0);

        UnitPriceView view = tx.execute(status -> prices.unitPriceAt(id, LocalDate.of(2030, 2, 1), true))
                .orElseThrow();
        assertThat(view.unitPrice()).isEqualByComparingTo("100");
        assertThat(referenced(id)).isEqualTo(1);
        assertThat(jdbc.queryForObject(
                        "SELECT referenced FROM price_version WHERE service_id = ? AND effective_from = '2030-01-01'",
                        Boolean.class,
                        id))
                .isTrue();
    }

    @Test
    void BR_M11_02_markReferenced_requiresTransaction() {
        long id = service("MEAL", null, null, null);
        version(id, 100, "2030-01-01", null);

        assertThatThrownBy(() -> prices.unitPriceAt(id, LocalDate.of(2030, 2, 1), true))
                .isInstanceOf(IllegalTransactionStateException.class);
        assertThat(referenced(id)).isEqualTo(0);
    }

    @Test
    void Q_297_careServiceOf_returnsTheActiveServiceOfTheLevel() {
        long basic = service("CARE", "BASIC", null, null);
        service("CARE", "SPECIAL", null, null);

        assertThat(prices.careServiceOf(CareLevel.BASIC).orElseThrow().id()).isEqualTo(basic);
        assertThat(prices.careServiceOf(CareLevel.BASIC).orElseThrow().type()).isEqualTo(ServiceType.CARE);
        assertThat(prices.careServiceOf(CareLevel.REGULAR)).isEmpty();

        jdbc.update("UPDATE service SET status = 'INACTIVE' WHERE id = ?", basic);
        assertThat(prices.careServiceOf(CareLevel.BASIC)).isEmpty();
    }

    @Test
    void FR_034_feeAndMedicationServices_areFoundByRoomTypeAndMedication() {
        long fee = service("RESIDENCY_FEE", null, 930001L, null);
        long medication = service("FACILITY_SUPPLIED_MEDICATION", null, null, 940001L);

        assertThat(prices.residencyFeeServiceOf(930001L).orElseThrow().id()).isEqualTo(fee);
        assertThat(prices.residencyFeeServiceOf(930002L)).isEmpty();
        assertThat(prices.facilityMedicationServiceOf(940001L).orElseThrow().id())
                .isEqualTo(medication);
        assertThat(prices.facilityMedicationServiceOf(940002L)).isEmpty();
    }

    private int referenced(long serviceId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM price_version WHERE service_id = ? AND referenced", Integer.class, serviceId);
    }
}
