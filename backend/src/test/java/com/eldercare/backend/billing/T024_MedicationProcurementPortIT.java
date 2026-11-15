package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.billing.api.spi.ProcurementIntakePort;
import com.eldercare.backend.billing.internal.procurement.domain.ProcurementLineId;
import com.eldercare.backend.medication.api.spi.ProcurementMedicationPort;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 019 T024 (006 FR-054, BR-M07-24): bean thật của {@code medication.api.spi.ProcurementMedicationPort} do M11 cài trả dòng
 * thuốc đã ghi đã mua; dòng của yêu cầu chưa ghi đã mua thì rỗng. Không dùng bản giả {@code FakeProcurementMedicationPort}.
 */
@Transactional
class T024_MedicationProcurementPortIT extends MySqlIntegrationTestBase {

    @Autowired
    ProcurementMedicationPort port;

    @Autowired
    ProcurementIntakePort intakes;

    @Autowired
    JdbcTemplate jdbc;

    private long request(String status) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO procurement_request (id, department_id, created_by_staff_id, type, `lines`, estimated_total,"
                        + " reason, linked_sources, status, created_at, approved_amount, priority, needed_by)"
                        + " VALUES (?, 3, 77, 'MEDICATION', ?, 60000, 'Amlodipin sắp hết', '[]', ?, ?, 60000, 'NORMAL', CURDATE())",
                id,
                "[{\"name\":\"Amlodipin\",\"quantity\":30,\"estimatedUnitPrice\":2000,\"amount\":60000,\"residentId\":5,"
                        + "\"prescriptionId\":91,\"activeIngredient\":\"Amlodipin\",\"strength\":\"5 mg\","
                        + "\"dosageForm\":\"Viên nén\",\"unit\":\"viên\"}]",
                status,
                LocalDateTime.now());
        return id;
    }

    @Test
    void T024_realPortReturnsPurchasedMedicationLineOnly() {
        long purchased = request("PENDING_RECONCILIATION");
        var line =
                port.lineRecordedPurchased(ProcurementLineId.of(purchased, 0)).orElseThrow();
        assertThat(line.procurementRequestId()).isEqualTo(purchased);
        assertThat(line.residentId()).isEqualTo(5);
        assertThat(line.prescriptionId()).isEqualTo(91);
        assertThat(line.medicationName()).isEqualTo("Amlodipin");
        assertThat(line.creatorStaffId()).isEqualTo(77);

        long approved = request("APPROVED");
        assertThat(port.lineRecordedPurchased(ProcurementLineId.of(approved, 0)))
                .isEmpty();

        // Đã nhận (FR-008d): bean thật đọc kết quả tiếp nhận của M07; chưa có lần tiếp nhận nào thì rỗng
        assertThat(intakes.passedLineIds(purchased)).isEmpty();
    }
}
