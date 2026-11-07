package com.eldercare.backend.staffing;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.shared.api.RecordEventApi;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.*;
import com.eldercare.backend.common.error.InvalidValueException;
import com.eldercare.backend.staffing.internal.domain.StaffingEventData;
import com.eldercare.backend.staffing.internal.domain.StaffingRecordData;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.TestUsers;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** T050: CHECK của bảng cũ chuyển sang record nội dung và được API thật kiểm khi ghi. */
@Transactional
class StaffingSharedConstraintsIT extends MySqlIntegrationTestBase {
    @Autowired
    RecordEventApi events;

    @Autowired
    SimpleRecordApi records;

    private static final Instant AT = Instant.parse("2026-01-01T00:00:00Z");

    static Stream<StaffingEventData.Ratio> invalidRatios() {
        var one = BigDecimal.ONE;
        var minus = BigDecimal.ONE.negate();
        return Stream.of(
                new StaffingEventData.Ratio(AT, "PUBLISHED", minus, 1, one, one, true, null),
                new StaffingEventData.Ratio(AT, "PUBLISHED", one, -1, one, one, true, null),
                new StaffingEventData.Ratio(AT, "PUBLISHED", one, 1, one, BigDecimal.ZERO, true, null),
                new StaffingEventData.Ratio(AT, "PUBLISHED", one, 0, one, one, true, null),
                new StaffingEventData.Ratio(AT, "PUBLISHED", one, 1, null, one, true, null),
                new StaffingEventData.Ratio(AT, "PUBLISHED", one, 1, minus, one, true, null));
    }

    @ParameterizedTest
    @MethodSource("invalidRatios")
    void FR_037_sharedRatioKeepsOriginalNumericChecks(StaffingEventData.Ratio value) {
        assertThatThrownBy(() -> events.add(
                        StaffingEventData.RATIO,
                        new EntityRef("M09.SHIFT", TestUsers.nextId()),
                        new NewRecordEvent<>(value, null, null, false, AT, null, AT, null, null)))
                .isInstanceOf(InvalidValueException.class);
    }

    @Test
    void FR_001c_importBatchStillRequiresAtLeastOneRow() {
        assertThatThrownBy(() -> records.create(
                        StaffingRecordData.IMPORT_BATCH,
                        new NewSimpleRecord<>(
                                null,
                                null,
                                "RECORDED",
                                new StaffingRecordData.ImportBatch("[Tệp kiểm tra]", 0, null),
                                null,
                                null,
                                null,
                                TestUsers.nextId(),
                                true)))
                .isInstanceOf(InvalidValueException.class);
    }
}
