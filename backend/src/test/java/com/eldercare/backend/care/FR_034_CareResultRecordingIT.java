package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.api.events.BillableCareResultRecorded;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.fake.administration.*;
import com.eldercare.backend.support.fake.care.*;
import com.eldercare.backend.support.fake.resident.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.*;
import org.springframework.test.web.servlet.*;
import org.springframework.transaction.annotation.Transactional;

/** T033: lệnh thật, MySQL và kiểm quyền thật; chỉ nguồn ngoài M04 dùng fixture của chủ SPI. */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import({FR_028a_ReasonCatalogHttpIT.Users.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class FR_034_CareResultRecordingIT extends CareResultTestBase {
    @Test
    void FR_038_unknownLegacyMlLimitCannotSilentlyBecomeUnlimited() throws Exception {
        jdbc.update(
                "UPDATE care_task SET data=JSON_REMOVE(data,'$.upperLimit','$.resultConfigurationCaptured') WHERE id=?",
                task);
        record("{\"numericValue\":900}").andExpect(status().isServiceUnavailable());
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Test
    void FR_038_explicitlyCapturedUnlimitedMlRemainsRecordable() throws Exception {
        jdbc.update(
                "UPDATE care_task SET data=JSON_SET(data,'$.upperLimit',NULL,'$.resultConfigurationCaptured',TRUE) WHERE id=?",
                task);
        record("{\"numericValue\":900}").andExpect(status().isOk());
    }

    @Test
    void BR_M04_10_pendingMeasurementCannotBeClosedByNotPerformed() throws Exception {
        vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.AWAITING_REMEASUREMENT);
        record(measurementBody()).andExpect(status().isAccepted());
        long reason = jdbc.queryForObject(
                "SELECT COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.legacy.id')) AS UNSIGNED),id) FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND status='ACTIVE' ORDER BY id LIMIT 1",
                Long.class);
        notPerform("{\"reasonTypeId\":" + reason + "}").andExpect(status().isConflict());
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Autowired
    FakeVitalSigns vitalSigns;

    @Test
    void FR_034_eatingMustUseOneOfTheFiveLevels() throws Exception {
        data("EATING", false);
        record("{\"codeValue\":\"UNKNOWN\"}").andExpect(status().isUnprocessableEntity());
        record("{\"codeValue\":\"ATE_ALL\"}").andExpect(status().isOk());
    }

    @Test
    void FR_034_movementMustUseOneOfTheThreeLevels() throws Exception {
        data("MOVEMENT", false);
        record("{\"codeValue\":\"UNKNOWN\"}").andExpect(status().isUnprocessableEntity());
        record("{\"codeValue\":\"ASSISTED\"}").andExpect(status().isOk());
    }

    void resultValue(String type, String code) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.RESULT_VALUE','M04',?,'ACTIVE',?)",
                id,
                "Giá trị kiểm thử " + id,
                "{\"type\":\"" + type + "\",\"code\":\"" + code + "\"}");
    }

    @Test
    void FR_034_sleepRequiresDurationAndAnActiveQuality() throws Exception {
        data("SLEEP", false);
        resultValue("SLEEP_QUALITY", "GOOD");
        record("{\"numericValue\":60,\"codeValue\":\"UNKNOWN\"}").andExpect(status().isUnprocessableEntity());
        record("{\"numericValue\":60,\"codeValue\":\"GOOD\"}").andExpect(status().isOk());
    }

    @Test
    void FR_034_moodRequiresAnActiveCatalogCode() throws Exception {
        data("MOOD", false);
        resultValue("MOOD", "CALM");
        record("{\"codeValue\":\"UNKNOWN\"}").andExpect(status().isUnprocessableEntity());
        record("{\"codeValue\":\"CALM\"}").andExpect(status().isOk());
    }

    @Test
    void FR_034_behaviorRequiresOccurrenceAndCatalogTypeWhenPresent() throws Exception {
        data("BEHAVIOR", false);
        resultValue("BEHAVIOR", "RESTLESS");
        record("{\"jsonValue\":{\"occurred\":true}}").andExpect(status().isUnprocessableEntity());
        record("{\"jsonValue\":{\"occurred\":true,\"behaviorCode\":\"RESTLESS\",\"description\":\"Mô tả kiểm thử\"}}")
                .andExpect(status().isOk());
    }

    Instant measuredAt;

    String measurementBody() {
        return "{\"numericValue\":37,\"performedAt\":\"" + measuredAt + "\"}";
    }

    void vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus result) {
        measuredAt = time.now();
        data("MEASURED_VALUE", false);
        jdbc.update("UPDATE care_task SET data=JSON_SET(data,'$.vitalSignType','TEMPERATURE') WHERE id=?", task);
        vitalSigns.result(
                new FakeVitalSigns.Measurement(
                        resident,
                        "TEMPERATURE",
                        new java.math.BigDecimal("37"),
                        measuredAt,
                        performer,
                        task,
                        Optional.empty()),
                new com.eldercare.backend.care.api.spi.VitalSignPort.RecordingResult(
                        result,
                        result == com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.SAVED
                                ? Optional.of(TestUsers.nextId())
                                : Optional.empty(),
                        Optional.of("Kiểm thử nguồn chỉ số"),
                        Optional.of(TestUsers.nextId())));
    }

    @Test
    void BR_M04_10_blockedMeasurementLeavesTaskOpen() throws Exception {
        vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.BLOCKED);
        record(measurementBody())
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VALUE_BLOCKED"));
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Test
    void BR_M04_10_awaitingRemeasurementDoesNotCloseOrCreateResult() throws Exception {
        vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.AWAITING_REMEASUREMENT);
        record(measurementBody()).andExpect(status().isAccepted());
        assertThat(storedStatus()).isEqualTo("DUE");
        assertThat(jdbc.queryForObject("SELECT awaiting_remeasurement FROM care_task WHERE id=?", Boolean.class, task))
                .isTrue();
    }

    @Test
    void BR_M04_10_savedMeasurementClosesAndReferencesSourceRecord() throws Exception {
        vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.SAVED);
        record(measurementBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.vitalSignRecordId").isNumber());
    }

    @Test
    void FR_034_positiveWholeMlCompletesAndStoresOneResult() throws Exception {
        record("{\"numericValue\":200}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.currentValue.numericValue").value(200));
        assertThat(storedStatus()).isEqualTo("COMPLETED");
    }

    @Test
    void FR_034_zeroMlIsRejectedWithoutClosing() throws Exception {
        record("{\"numericValue\":0}").andExpect(status().isUnprocessableEntity());
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Test
    void FR_034_fractionalMlIsRejected() throws Exception {
        record("{\"numericValue\":1.5}").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_034_aboveSnapshotLimitRequiresExplicitConfirmation() throws Exception {
        record("{\"numericValue\":501}")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("CONFIRMATION_REQUIRED"));
        record("{\"numericValue\":501,\"upperLimitConfirmed\":true}").andExpect(status().isOk());
    }

    @Test
    void FR_034_wrongValueShapeIsRejected() throws Exception {
        record("{\"codeValue\":\"ATE_ALL\"}").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_035_futurePerformedAtIsRejected() throws Exception {
        record("{\"numericValue\":200,\"performedAt\":\"" + time.now().plusSeconds(60) + "\"}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_035_beforeWindowIsRejected() throws Exception {
        record("{\"numericValue\":200,\"performedAt\":\"" + time.now().minusSeconds(901) + "\"}")
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_036_secondRecordingIsRejectedWithOriginalInput() throws Exception {
        record("{\"numericValue\":200}").andExpect(status().isOk());
        record("{\"numericValue\":300}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CARE_TASK_CLOSED"))
                .andExpect(jsonPath("$.rejectedContent.numericValue").value(300));
    }

    @Test
    void FR_028a_notPerformedRequiresReasonType() throws Exception {
        notPerform("{}").andExpect(status().isUnprocessableEntity());
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Test
    void FR_028a_inactiveReasonIsRejected() throws Exception {
        long reason = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.NON_PERFORMANCE_REASON_TYPE','M04',?,'INACTIVE','{\"fixed\":false}')",
                reason,
                "Kiểm thử " + reason);
        notPerform("{\"reasonTypeId\":" + reason + "}").andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_028a_activeReasonStoresOptionalNoteWithoutResult() throws Exception {
        long reason = jdbc.queryForObject(
                "SELECT COALESCE(CAST(JSON_UNQUOTE(JSON_EXTRACT(attributes,'$.legacy.id')) AS UNSIGNED),id) FROM catalog_item WHERE catalog_type='M04.NON_PERFORMANCE_REASON_TYPE' AND status='ACTIVE' ORDER BY id LIMIT 1",
                Long.class);
        notPerform("{\"reasonTypeId\":" + reason + ",\"note\":\"Ghi chú kiểm thử\"}")
                .andExpect(status().isOk());
        assertThat(storedStatus()).isEqualTo("NOT_PERFORMED");
        assertThat(jdbc.queryForObject("SELECT closure_reason FROM care_task WHERE id=?", String.class, task))
                .isEqualTo("Ghi chú kiểm thử");
    }

    @Test
    void FR_034_itemQuantityMayBeZero() throws Exception {
        data("ITEM_QUANTITY", false);
        record("{\"numericValue\":0}").andExpect(status().isOk());
    }

    @Test
    void FR_034_performedNeedsNoNumericResult() throws Exception {
        data("PERFORMED", false);
        record("{}").andExpect(status().isOk());
    }

    @Test
    void FR_051_billableRecordingWaitsForAgreedQuantityWithoutSaving() throws Exception {
        data("ITEM_QUANTITY", true);
        record("{\"numericValue\":2}").andExpect(status().isServiceUnavailable());
        assertThat(storedStatus()).isEqualTo("DUE");
        assertThat(events.stream(BillableCareResultRecorded.class)
                        .filter(e -> e.residentId() == resident)
                        .toList())
                .isEmpty();
    }
}
