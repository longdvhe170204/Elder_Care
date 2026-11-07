package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * 005 T034 (BR-M04-27, FR-035, FR-036, CHK044, CHK056; research C-14): đồng bộ bản ghi cho công việc đã được đóng hoặc
 * Hủy thì từ chối, báo ai đã đóng và giữ nội dung trên máy (002 trả REJECTED kèm lời nhắn); công việc bị hệ thống đóng vì
 * người cao tuổi chuyển trạng thái cuối: thời điểm thực hiện trước lúc đó thì ghi thành Ghi nhận phát sinh trỏ công việc
 * gốc, sau lúc đó thì từ chối.
 */
class BR_M04_27_ClosedTaskOfflineSyncIT extends CareOfflineTestBase {

    String recordAt(java.time.Instant performed) {
        return resultBody("RECORD", "{\"numericValue\":180,\"performedAt\":\"" + performed + "\"}", null);
    }

    @Test
    void FR_036_taskClosedOnlineBeforeSyncIsRejectedWithWhoClosedIt() throws Exception {
        record("{\"numericValue\":200}").andExpect(status().isOk());
        var device = time.now().minus(Duration.ofMinutes(2));
        assertThatThrownBy(() -> sync(RESULT, recordAt(device.minusSeconds(30)), device, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CARE_TASK_CLOSED")
                .hasMessageContaining(Long.toString(user.accountId()));
        assertThat(jdbc.queryForObject(
                        "SELECT CAST(result_numeric_value AS SIGNED) FROM care_task WHERE id=?", Long.class, task))
                .as("giữ kết quả lần đầu")
                .isEqualTo(200L);
    }

    @Test
    void CHK044_cancelledTaskIsRejected() {
        jdbc.update("UPDATE care_task SET status='CANCELLED',closed_at=? WHERE id=?", local(time.now()), task);
        var device = time.now().minus(Duration.ofMinutes(2));
        assertThatThrownBy(() -> sync(RESULT, recordAt(device.minusSeconds(30)), device, false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("CARE_TASK_CLOSED");
        assertThat(storedStatus()).isEqualTo("CANCELLED");
    }

    void closedOnFinalState(java.time.Instant at) {
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,"
                        + "'ACTIVE',?)",
                type,
                "Uống nước " + type,
                "{\"group\":\"DRINKING_ASSISTANCE\",\"professional\":false,\"resultType\":\"ML\",\"defaultRole\":"
                        + "\"FLOOR_MANAGER\",\"defaultImportance\":\"NORMAL\",\"upperLimit\":500,\"billable\":false,"
                        + "\"certificates\":[]}");
        residents.changeStatus(resident, ResidentStatus.DECEASED, at);
        jdbc.update(
                "UPDATE care_task SET status='NOT_PERFORMED',closed_at=?,closure_rule='FR_010_FINAL_STATE',"
                        + "closure_reason='trạng thái cuối: DECEASED' WHERE id=?",
                local(at),
                task);
    }

    @Test
    void CHK056_recordPerformedBeforeTheFinalStateBecomesAnAdHocRecordingOfTheOriginalTask() {
        var finalAt = time.now().minus(Duration.ofMinutes(5));
        closedOnFinalState(finalAt);
        var performed = finalAt.minus(Duration.ofMinutes(5));
        shifts.onShift(performer, performed, true);
        String reference = sync(RESULT, recordAt(performed), finalAt.plus(Duration.ofMinutes(1)), false);
        long adHoc = Long.parseLong(reference);
        assertThat(adHoc).isNotEqualTo(task);
        assertThat(jdbc.queryForMap(
                        "SELECT status,basis_type,original_task_id,resident_id FROM care_task WHERE id=?", adHoc))
                .containsEntry("status", "COMPLETED")
                .containsEntry("basis_type", "AD_HOC_RECORDING")
                .containsEntry("original_task_id", task)
                .containsEntry("resident_id", resident);
        assertThat(storedStatus()).as("công việc gốc giữ nguyên").isEqualTo("NOT_PERFORMED");
    }

    @Test
    void CHK056_recordPerformedAfterTheFinalStateIsRejected() {
        var finalAt = time.now().minus(Duration.ofMinutes(10));
        closedOnFinalState(finalAt);
        var performed = finalAt.plus(Duration.ofMinutes(2));
        assertThatThrownBy(() -> sync(RESULT, recordAt(performed), performed.plusSeconds(30), false))
                .isInstanceOf(BusinessException.class);
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM care_task WHERE original_task_id=?", Integer.class, task))
                .isZero();
    }
}
