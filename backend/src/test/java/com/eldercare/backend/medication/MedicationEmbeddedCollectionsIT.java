package com.eldercare.backend.medication;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.medication.internal.family.domain.FamilyMedicationPrescription;
import com.eldercare.backend.medication.internal.family.persistence.FamilyMedicationPrescriptionRepository;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationDecision;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationItem;
import com.eldercare.backend.medication.internal.reconciliation.domain.ReconciliationItemSource;
import com.eldercare.backend.medication.internal.reconciliation.persistence.ReconciliationItemRepository;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** T069: bản ghi con mới được ghi vào JSON của bản ghi cha sau khi bỏ hai bảng con. */
class MedicationEmbeddedCollectionsIT extends M07IntegrationTestBase {

    @Autowired
    private ReconciliationItemRepository reconciliationItems;

    @Autowired
    private FamilyMedicationPrescriptionRepository familyBindings;

    @Test
    void T069_reconciliationLineIsStoredInParentJson() {
        long reconciliationId = TestUsers.nextId();
        long residentId = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO medication_reconciliation (id, resident_id, reason, reason_detail, status,"
                        + " created_by_id, created_at, `lines`, version)"
                        + " VALUES (?, ?, 'PROFESSIONAL_REQUEST', 'T069', 'OPEN', 1, UTC_TIMESTAMP(6), JSON_ARRAY(), 0)",
                reconciliationId,
                residentId);

        reconciliationItems.save(new ReconciliationItem(
                reconciliationId,
                1,
                ReconciliationItemSource.ADMISSION_CURRENT_MEDICATION,
                null,
                7001L,
                "Thuốc kiểm thử",
                ReconciliationDecision.CONTINUE,
                "Giữ thuốc",
                null,
                1L,
                Instant.parse("2026-10-31T01:00:00Z"),
                Instant.parse("2026-10-31T01:00:00Z")));

        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(`lines`->'$[0].reason')" + " FROM medication_reconciliation WHERE id = ?",
                        String.class,
                        reconciliationId))
                .isEqualTo("Giữ thuốc");
    }

    @Test
    void T069_familyPrescriptionBindingIsStoredInParentJson() {
        long batchId = TestUsers.nextId();
        long residentId = TestUsers.nextId();
        long prescriptionId = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO family_medication_batch (id, resident_id, initial_quantity, unit, has_prescription,"
                        + " file_ids, prescription_bindings, deliverer_type, deliverer_name, receiver_id, received_at,"
                        + " status, created_at, updated_at) VALUES (?, ?, 10, 'tablet', 1, JSON_ARRAY(), JSON_ARRAY(),"
                        + " 'FREE', 'Người giao', 1, UTC_TIMESTAMP(6), 'IN_USE', UTC_TIMESTAMP(6), UTC_TIMESTAMP(6))",
                batchId,
                residentId);

        familyBindings.save(
                new FamilyMedicationPrescription(batchId, prescriptionId, Instant.parse("2026-10-31T02:00:00Z")));

        assertThat(jdbc.queryForObject(
                        "SELECT CAST(JSON_UNQUOTE(prescription_bindings->'$[0].prescriptionId') AS UNSIGNED)"
                                + " FROM family_medication_batch WHERE id = ?",
                        Long.class,
                        batchId))
                .isEqualTo(prescriptionId);
    }
}
