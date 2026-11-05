package com.eldercare.backend.belongings;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 013 T007 (contracts mục 1.1; research K-15; NFR-08): provider của M12 cho bảng đã có ở Phase 2 ({@code belonging},
 * {@code belonging_handover}). Loại của xác nhận người nhận, đồng ý, phiếu kiểm kê thêm cùng bảng ở US1, US3, US5.
 */
class T007_M12ProvidersIT extends BelongingsTestBase {

    @Autowired
    List<FieldClassificationProvider> classifications;

    @Autowired
    List<RetentionProvider> retentions;

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<FilePermissionProvider> files;

    @Autowired
    TransactionTemplate tx;

    private long handover(long belongingId) {
        long residentId =
                jdbc.queryForObject("SELECT resident_id FROM belonging WHERE id = ?", Long.class, belongingId);
        jdbc.update(
                "INSERT INTO belonging_handover (belonging_id, resident_id, kind, target_status, occurred_at, quantity,"
                        + " condition_level_id, valuable, photo_file_ids, recorded_by, recorded_at)"
                        + " VALUES (?, ?, 'INTAKE', 'HELD', NOW(6), 1, ?, FALSE, JSON_ARRAY(), 1, NOW(6))",
                belongingId,
                residentId,
                catalogId("M12.CONDITION_LEVEL", "GOOD"));
        return jdbc.queryForObject(
                "SELECT MAX(id) FROM belonging_handover WHERE belonging_id = ?", Long.class, belongingId);
    }

    @Test
    void FR_031a_fieldClassification_hidesIdDocumentAndCashAmount() {
        var handover = classifications.stream()
                .filter(p -> p.entityType().equals("M12.BELONGING_HANDOVER"))
                .findFirst()
                .orElseThrow();
        assertThat(handover.fieldGroups())
                .containsEntry("giverIdDocument", InfoGroup.IDENTITY)
                .containsEntry("recipientIdDocumentNumber", InfoGroup.IDENTITY)
                .containsEntry("cashAmount", InfoGroup.FINANCE);
        assertThat(classifications).anyMatch(p -> p.entityType().equals("M12.BELONGING"));
    }

    @Test
    void FR_031a_fieldClassification_hidesRecipientIdDocumentOfConfirmation() {
        var confirmation = classifications.stream()
                .filter(p -> p.entityType().equals("M12.RECIPIENT_CONFIRMATION"))
                .findFirst()
                .orElseThrow();

        assertThat(confirmation.fieldGroups()).containsEntry("recipientIdDocument", InfoGroup.IDENTITY);
    }

    @Test
    void K_15_belongings_keptByResidentRecord_andDeletedWithTheirHandovers() {
        var retention = retentions.stream()
                .filter(p -> p.dataType().equals("M12.BELONGING"))
                .findFirst()
                .orElseThrow();
        assertThat(retention.retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);

        long resident = TestUsers.nextId();
        long original = heldBelonging(resident, null);
        long split = heldBelonging(resident, null);
        jdbc.update("UPDATE belonging SET original_belonging_id = ? WHERE id = ?", original, split);
        handover(original);
        handover(split);

        tx.executeWithoutResult(status -> retention.delete(List.of(original, split)));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM belonging WHERE resident_id = ?", Integer.class, resident))
                .isZero();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM belonging_handover WHERE resident_id = ?", Integer.class, resident))
                .isZero();
    }

    @Test
    void FR_029_scope_ofBelonging_isItsResident() {
        long resident = TestUsers.nextId();
        long belonging = heldBelonging(resident, null);
        var scope = scopes.stream()
                .filter(p -> p.entityType().equals("M12.BELONGING"))
                .findFirst()
                .orElseThrow();

        assertThat(scope.assignScope(belonging).residentId()).isEqualTo(resident);
    }

    @Test
    void NFR_08_photoProvider_isRegisteredForHandoverPhotos() {
        assertThat(files).anyMatch(p -> p.ownerType().equals("M12.BELONGING_PHOTO"));
    }

    @Test
    void NFR_08_returnSignatureProvider_isRegistered_andUnknownRecordIsDenied() {
        var signature = files.stream()
                .filter(p -> p.ownerType().equals("M12.RETURN_SIGNATURE"))
                .findFirst()
                .orElseThrow();

        assertThat(signature.canDownload(Long.MAX_VALUE)).isFalse();
    }
}
