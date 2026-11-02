package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.*;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.care.api.VitalSignPort;
import com.eldercare.backend.care.api.VitalSignPort.RecordResult;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * BR-M04-10: ChiSoPort.ghi trả DA_LUU / BI_CHAN / CHO_DO_LAI (S-06, FR-034).
 */
@Transactional
class BR_M04_10_VitalSignPortIT extends com.eldercare.backend.health.VitalSignMeasurementTestBase {

    private static final Instant MEASURED_AT = Instant.parse("2026-11-02T08:00:00Z");

    @Autowired
    VitalSignPort vitalSignPort;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @MockitoSpyBean
    ParameterApi parameters;

    private long indicatorId;
    private CurrentUser nurseUser;

    @BeforeEach
    void setup() {
        doReturn(new ParameterValue(
                        ParameterCode.CFG_M06_03,
                        ParameterKind.TABLE,
                        JsonMapper.builder()
                                .build()
                                .readTree("{\"SPO2_PORT_TEST\":{\"VALUE\":{\"min\":0,\"max\":100}}}")))
                .when(parameters)
                .read(ParameterCode.CFG_M06_03);
        indicatorId = catalogs.create(
                        SharedKind.of("M06.VITAL_SIGN", Map.class).notDeletable(),
                        new NewCatalogItem<>(
                                "SPO2_PORT_TEST", "SpO2 Port", null, 0, Map.of("unit", "%", "vitalSign", true)))
                .id();
        com.eldercare.backend.health.VitalSignTestThresholds.seed(jdbc, indicatorId);
        nurseUser = new TestUsers(jdbc, time).staff(Role.NURSING_STAFF).user();
        asMeasurementUser(nurseUser);
    }

    @Test
    void BR_M04_10_normalValueReturnsSaved() {
        // giá trị bình thường (không có ngưỡng mặc định → NO_THRESHOLD → CONFIRMED)
        var result = vitalSignPort.record(
                TestUsers.nextId(),
                indicatorId,
                MEASURED_AT,
                1001L,
                Map.of("VALUE", new BigDecimal("96.00")),
                nurseUser);

        assertThat(result).isEqualTo(RecordResult.SAVED);
    }

    @Test
    void BR_M04_10_blockedValueOutsidePhysicalRange() {
        // SpO2 > 100 bị chặn bởi CFG-M06-03
        var result = vitalSignPort.record(
                TestUsers.nextId(),
                indicatorId,
                MEASURED_AT,
                1001L,
                Map.of("VALUE", new BigDecimal("101.00")),
                nurseUser);

        assertThat(result).isEqualTo(RecordResult.BLOCKED);
    }

    @Test
    void BR_M04_10_dangerousValueWithoutImmediateActionReturnsAwaitingRemeasurement() {
        // SpO2 = 88 (nguy hiểm theo ngưỡng mặc định) → CHO_DO_LAI
        var result = vitalSignPort.record(
                TestUsers.nextId(),
                indicatorId,
                MEASURED_AT,
                1001L,
                Map.of("VALUE", new BigDecimal("88.00")),
                nurseUser);

        assertThat(result).isEqualTo(RecordResult.AWAITING_REMEASUREMENT);
    }
}
