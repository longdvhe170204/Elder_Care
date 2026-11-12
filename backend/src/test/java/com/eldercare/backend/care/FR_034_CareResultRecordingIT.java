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

    /**
     * BR-M04-10, FR-034 dòng "Đo chỉ số" (chủ dự án chốt 2026-11-12): lần đo lại do người khác, giờ khác xác nhận thì
     * công việc giữ người thực hiện, thời điểm thực hiện của lần đo đầu (trạng thái, nhãn ghi muộn tính theo mốc đó); kết
     * quả trỏ bản ghi đã xác nhận của lần đo lại.
     */
    @Test
    void BR_M04_10_remeasuredByAnotherPersonLater_keepsFirstMeasurementPerformerAndTime() throws Exception {
        vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.AWAITING_REMEASUREMENT);
        record(measurementBody()).andExpect(status().isAccepted());
        long confirmedRecord = TestUsers.nextId();

        vitalSignTasks.confirmed(task, confirmedRecord, measuredAt.plusSeconds(300), TestUsers.nextId());

        mvc.perform(get("/api/v1/care-tasks/" + task))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.vitalSignRecordId").value(confirmedRecord))
                // MySQL làm tròn micro giây; mốc đo lại cách 5 phút nên so tới giây là đủ phân biệt
                .andExpect(jsonPath("$.result.performedAt")
                        .value(org.hamcrest.Matchers.startsWith(measuredAt
                                .truncatedTo(java.time.temporal.ChronoUnit.SECONDS)
                                .toString()
                                .replace("Z", ""))))
                .andExpect(jsonPath("$.result.lateRecording").value(false));
    }

    /**
     * FR-025, FR-034 (chủ dự án chốt 2026-11-12, ngoại lệ của FR-025 chỉ cho loại đo chỉ số): Ghi nhận phát sinh đo chỉ số
     * mà 007 trả chờ đo lại thì công việc phát sinh chưa đóng, mang dấu chờ xác nhận; 007 xác nhận thì Hoàn thành, thời
     * điểm thực hiện của lần đo đầu.
     */
    @Test
    void FR_025_adHocMeasurementAwaitingRemeasurement_staysOpenUntilConfirmed() throws Exception {
        long measuredType = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                measuredType,
                "Đo nhiệt độ " + measuredType,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"MEASURED_VALUE\",\"vitalSignType\":\"TEMPERATURE\","
                        + "\"defaultRole\":\"FLOOR_MANAGER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
        vitalSigns.resultForAnyTask(new com.eldercare.backend.care.api.spi.VitalSignPort.RecordingResult(
                com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.AWAITING_REMEASUREMENT,
                Optional.empty(),
                Optional.empty(),
                Optional.of(TestUsers.nextId())));
        Instant first = time.now().minusSeconds(60).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        shifts.onShift(performer, first, true);

        String body = mvc.perform(post("/api/v1/ad-hoc-care-recordings")
                        .header("Idempotency-Key", "ad-hoc-" + TestUsers.nextId())
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"residentId\":" + resident + ",\"taskTypeId\":" + measuredType
                                + ",\"numericValue\":39.6,\"performedAt\":\"" + first
                                + "\",\"sameTypeTasksReviewed\":true}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DUE"))
                .andExpect(jsonPath("$.awaitingRemeasurementConfirmation").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();
        long adHoc = tools.jackson.databind.json.JsonMapper.builder()
                .build()
                .readTree(body)
                .get("id")
                .asLong();

        long confirmedRecord = TestUsers.nextId();
        vitalSignTasks.confirmed(adHoc, confirmedRecord, first.plusSeconds(240), TestUsers.nextId());

        mvc.perform(get("/api/v1/care-tasks/" + adHoc))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.result.vitalSignRecordId").value(confirmedRecord))
                .andExpect(jsonPath("$.result.performedAt").value(first.toString()));
    }

    @Test
    void BR_M04_10_savedMeasurementClosesAndReferencesSourceRecord() throws Exception {
        vital(com.eldercare.backend.care.api.spi.VitalSignPort.RecordingStatus.SAVED);
        record(measurementBody())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.vitalSignRecordId").isNumber());
    }
}
