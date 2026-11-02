package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

class FR_010_MeasurementAuditIT extends VitalSignMeasurementTestBase {
    @Autowired
    VitalSignMeasurementService measurements;

    @Autowired
    VitalSignRecordStore records;

    @Autowired
    AuditLogApi audit;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    TransactionTemplate transactions;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    private long residentId;
    private long indicatorId;
    private CurrentUser nurse;

    @BeforeEach
    void fixture() {
        residentId = TestUsers.nextId();
        indicatorId = transactions.execute(status -> catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "AUDIT_" + residentId,
                                "Chỉ số nhật ký " + residentId,
                                null,
                                0,
                                Map.of("unit", "đơn vị", "vitalSign", false)))
                .id());
        nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user();
        asMeasurementUser(nurse);
    }

    private RecordVitalSignCommand command() {
        return new RecordVitalSignCommand(
                residentId,
                indicatorId,
                time.now().minusSeconds(1),
                nurse.accountId(),
                Map.of("VALUE", BigDecimal.TEN),
                false,
                null,
                null,
                false,
                null);
    }

    @Test
    void FR_010_recordingCommitsAuditWithTheRecorderAndPermissionBasis() {
        var saved = measurements.record(command(), nurse);
        var entries = audit.recorded(
                new AuditAction("VITAL_SIGN_RECORDED", false),
                "M06.VITAL_SIGN_RECORD",
                String.valueOf(saved.id()),
                nurse.accountId());
        assertThat(entries).hasSize(1);
        assertThat(entries.getFirst().details()).containsEntry("permissionBasis", "ROLE");
    }

    @Test
    void FR_010_recordAndAuditRollBackTogether() {
        AtomicLong id = new AtomicLong();
        assertThatThrownBy(() -> transactions.execute(status -> {
                    id.set(measurements.record(command(), nurse).id());
                    throw new IllegalStateException("rollback fixture");
                }))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("rollback fixture");
        assertThat(id.get()).isPositive();
        assertThat(records.findById(id.get())).isEmpty();
        assertThat(audit.recorded(
                        new AuditAction("VITAL_SIGN_RECORDED", false),
                        "M06.VITAL_SIGN_RECORD",
                        String.valueOf(id.get()),
                        nurse.accountId()))
                .isEmpty();
    }
}
