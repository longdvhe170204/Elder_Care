package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.*;
import com.eldercare.backend.care.api.spi.ShiftStaffPort.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.support.*;
import com.eldercare.backend.support.SwitchableUserConfig;
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
@Import({SwitchableUserConfig.class, FakeM15Config.class, FakeM01Config.class, FakeM04Config.class})
class FR_034_CareResultRecordingIT extends CareResultTestBase {

    @Autowired
    FakeVitalSigns vitalSigns;

    void resultValue(String type, String code) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.RESULT_VALUE','M04',?,'ACTIVE',?)",
                id,
                "Giá trị kiểm thử " + id,
                "{\"type\":\"" + type + "\",\"code\":\"" + code + "\"}");
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

    /** FR-036 (chủ dự án duyệt 2026-11-10): trạng thái công việc ở {@code currentStatus}, {@code status} là mã HTTP. */
    @Test
    void FR_036_closedTask_returnsHttpStatusAndCurrentStatusSeparately() throws Exception {
        jdbc.update("UPDATE care_task SET status='CANCELLED' WHERE id=?", task);
        record("{\"numericValue\":100,\"performedAt\":\"" + time.now() + "\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("CARE_TASK_CLOSED"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.currentStatus").value("CANCELLED"))
                .andExpect(jsonPath("$.rejectedContent.numericValue").value(100));
    }

    @Autowired
    com.eldercare.backend.care.api.VitalSignTaskApi vitalSignTasks;

    @Test
    void BR_24_savedMeasurementClosesAndReferencesSourceRecord() throws Exception {
        vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.SAVED);
        record(measurementBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.vitalSignRecordId").isNumber());
    }
}
