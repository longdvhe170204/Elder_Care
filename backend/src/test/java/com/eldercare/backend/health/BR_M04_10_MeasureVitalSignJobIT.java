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
import com.eldercare.backend.health.api.dto.VitalSignLevel;
import com.eldercare.backend.health.api.events.VitalSignConfirmed;
import com.eldercare.backend.health.internal.measurement.persistence.VitalSignRecordStore;
import com.eldercare.backend.health.internal.measurement.service.OverdueRemeasurementTask;
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

/** BR-M04-10: việc đo chỉ số tự động quét và xác nhận các bản ghi quá hạn đo lại. */
@Transactional
@RecordApplicationEvents
class BR_M04_10_MeasureVitalSignJobIT extends VitalSignMeasurementTestBase {

    @Autowired
    VitalSignMeasurementService measurements;

    @Autowired
    VitalSignRecordStore records;

    @Autowired
    OverdueRemeasurementTask overdueTask;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    ApplicationEvents events;

    @Test
    void BR_M04_10_overdueRemeasurementTaskProcessesOverdueDraftsAutomatically() {
        long fixture = TestUsers.nextId();
        long indicatorId = catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SPO2_JOB_" + fixture,
                                "SpO2 đo lại job " + fixture,
                                null,
                                0,
                                Map.of("unit", "%", "vitalSign", true)))
                .id();
        VitalSignTestThresholds.seed(jdbc, indicatorId);
        CurrentUser nurse = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user();
        asMeasurementUser(nurse);

        var measuredAt = time.now().minus(Duration.ofMinutes(15));
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
        assertThat(deadline).isNotNull();

        // Chạy task trước hạn -> chưa xử lý
        int beforeDeadlineCount = overdueTask.run(deadline.minusSeconds(1), deadline.minusSeconds(1));
        assertThat(beforeDeadlineCount).isZero();

        // Chạy task tại hoặc sau hạn -> xử lý thành công
        int afterDeadlineCount = overdueTask.run(deadline.plusSeconds(5), deadline.plusSeconds(5));
        assertThat(afterDeadlineCount).isGreaterThanOrEqualTo(1);

        var confirmed = records.findById(draft.id()).orElseThrow();
        assertThat(confirmed.confirmationStatus()).isEqualTo(VitalSignConfirmationStatus.CONFIRMED);
        assertThat(confirmed.level()).isEqualTo(VitalSignLevel.DANGEROUS);
        assertThat(confirmed.labels()).contains(VitalSignLabel.NOT_REMEASURED);

        assertThat(events.stream(VitalSignConfirmed.class)
                        .filter(e -> e.recordId() == draft.id() && e.level() == VitalSignLevel.DANGEROUS))
                .hasSize(1);
    }
}
