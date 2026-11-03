package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.api.dto.RecordVitalSignCommand;
import com.eldercare.backend.health.api.dto.VitalSignConfirmationStatus;
import com.eldercare.backend.health.api.dto.VitalSignLabel;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.measurement.service.VitalSignMeasurementService;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

/** BR-M06-04: hết hạn đo lại thì xác nhận bản gốc một lần, giữ dữ liệu đo và phát cảnh báo. */
@Transactional
@RecordApplicationEvents
class BR_M06_04_OverdueRemeasurementIT extends com.eldercare.backend.health.VitalSignMeasurementTestBase {
    @Autowired
    VitalSignMeasurementService measurements;

    @Autowired
    VitalSignRecordStore records;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ApplicationEvents events;

    @Test
    void BR_M06_04_overdueDraftIsConfirmedOnceAndPublishesDangerousEvent() {
        long fixture = TestUsers.nextId();
        long indicatorId = catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SPO2_OVERDUE_" + fixture,
                                "SpO2 quá hạn " + fixture,
                                null,
                                0,
                                Map.of("unit", "%", "vitalSign", true)))
                .id();
        VitalSignTestThresholds.seed(jdbc, indicatorId);
        CurrentUser nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user();
        asMeasurementUser(nurse);
        var measuredAt = time.now().minus(Duration.ofMinutes(20));
        var draft = measurements.record(
                new RecordVitalSignCommand(
                        fixture,
                        indicatorId,
                        measuredAt,
                        nurse.accountId(),
                        Map.of("VALUE", new BigDecimal("88.00")),
                        false,
                        null,
                        null,
                        false,
                        null),
                nurse);
        assertThat(draft.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.AWAITING_REMEASUREMENT);
        var deadline = records.findById(draft.id()).orElseThrow().remeasurementDeadline();

        assertThat(measurements.confirmOverdue(draft.id(), deadline.minusSeconds(1)))
                .isFalse();
        assertThat(measurements.confirmOverdue(draft.id(), deadline)).isTrue();
        assertThat(measurements.confirmOverdue(draft.id(), deadline.plusSeconds(1)))
                .isFalse();

        var confirmed = records.findById(draft.id()).orElseThrow();
        assertThat(confirmed.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.CONFIRMED);
        assertThat(confirmed.labels()).contains(VitalSignLabel.NOT_REMEASURED);
        assertThat(confirmed.lockedAt()).isEqualTo(confirmed.confirmedAt());
        assertThat(events.stream(VitalSignConfirmed.class).filter(e -> e.recordId() == draft.id()))
                .hasSize(1);
    }
}
