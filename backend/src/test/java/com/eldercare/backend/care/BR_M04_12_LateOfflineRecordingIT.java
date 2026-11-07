package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.common.error.BusinessException;
import java.time.Duration;
import org.junit.jupiter.api.Test;

/**
 * 005 T034 (BR-M04-12, FR-035, FR-028a; research C-14; payload chủ dự án duyệt 2026-11-03; loại lý do theo thời điểm
 * thiết bị tra nhật ký ngừng hiệu lực, người dùng chọn 2026-11-06): ghi nhận đồng bộ sau mất kết nối dùng thời điểm thiết
 * bị làm "thời điểm ghi", gắn nhãn ghi muộn theo CFG-M04-06, giữ cờ mất quyền; hai loại bản ghi {@code M04.CARE_TASK_RESULT},
 * {@code M04.AD_HOC_RECORDING}.
 */
class BR_M04_12_LateOfflineRecordingIT extends CareOfflineTestBase {

    @Test
    void C_14_twoOfflineRecordTypesWithChecklistPermission() {
        assertThat(handler(RESULT).permissionKey())
                .isEqualTo(new PermissionKey(Function.TASK_CHECKLIST, Operation.CONFIRM));
        assertThat(handler(AD_HOC).permissionKey())
                .isEqualTo(new PermissionKey(Function.TASK_CHECKLIST, Operation.CREATE));
        assertThat(handler(RESULT)
                        .target(content(RESULT, resultBody("RECORD", "{\"numericValue\":200}", null)))
                        .floorAreaId())
                .isEqualTo(floor);
        assertThat(handler(RESULT).requiresOnline()).isFalse();
    }

    @Test
    void C_14_contentFollowsTheApprovedPayloadShape() {
        var h = handler(RESULT);
        assertThatThrownBy(() -> h.validate(content(RESULT, resultBody("RECORD", null, null))))
                .as("RECORD cần recording")
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> h.validate(content(RESULT, resultBody("NOT_PERFORM", null, null))))
                .as("NOT_PERFORM cần reasonTypeId")
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> h.validate(content(RESULT, resultBody("SKIP", null, null))))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void BR_M04_12_deviceTimeIsTheRecordingTimeForTheLateLabel() {
        var from = time.now().minus(Duration.ofHours(4));
        window(from, from.plus(Duration.ofMinutes(30)));
        var performed = from.plus(Duration.ofMinutes(10));
        var device = performed.plus(Duration.ofHours(2)).plus(Duration.ofMinutes(5));
        sync(
                RESULT,
                resultBody("RECORD", "{\"numericValue\":200,\"performedAt\":\"" + performed + "\"}", null),
                device,
                false);
        var row = jdbc.queryForMap(
                "SELECT status,result_late_recording,result_device_at IS NOT NULL AS device,result_permission_lost"
                        + " FROM care_task WHERE id=?",
                task);
        assertThat(row)
                .as("thực hiện trong khung nên Hoàn thành, không phải Hoàn thành trễ")
                .containsEntry("status", "COMPLETED")
                .containsEntry("result_late_recording", true)
                .containsEntry("device", 1L)
                .containsEntry("result_permission_lost", false);
    }

    @Test
    void BR_M04_12_recordWithinTheThresholdIsNotLateAndKeepsThePermissionLostFlag() {
        var from = time.now().minus(Duration.ofHours(2));
        window(from, from.plus(Duration.ofMinutes(30)));
        var performed = from.plus(Duration.ofMinutes(10));
        sync(
                RESULT,
                resultBody("RECORD", "{\"numericValue\":200,\"performedAt\":\"" + performed + "\"}", null),
                performed.plus(Duration.ofMinutes(30)),
                true);
        assertThat(jdbc.queryForMap(
                        "SELECT result_late_recording,result_permission_lost FROM care_task WHERE id=?", task))
                .containsEntry("result_late_recording", false)
                .containsEntry("result_permission_lost", true);
    }

    @Test
    void FR_028a_notPerformedOfflineAcceptsAReasonActiveAtTheDeviceTime() throws Exception {
        long reason = newReason();
        var device = time.now().minus(Duration.ofMinutes(5));
        deactivateReason(reason);
        sync(RESULT, resultBody("NOT_PERFORM", null, reason), device, false);
        assertThat(storedStatus())
                .as("lý do còn hiệu lực tại thời điểm thiết bị")
                .isEqualTo("NOT_PERFORMED");
    }

    @Test
    void FR_028a_notPerformedOfflineRejectsAReasonDeactivatedBeforeTheDeviceTime() throws Exception {
        long reason = newReason();
        deactivateReason(reason);
        var device = time.now().plus(Duration.ofSeconds(30));
        assertThatThrownBy(() -> sync(RESULT, resultBody("NOT_PERFORM", null, reason), device, false))
                .isInstanceOf(BusinessException.class);
        assertThat(storedStatus()).isEqualTo("DUE");
    }

    @Test
    void BR_M04_12_adHocRecordingOfflineUsesTheDeviceTime() {
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,"
                        + "'ACTIVE',?)",
                type,
                "Loại phát sinh ngoại tuyến " + type,
                "{\"group\":\"DRINKING_ASSISTANCE\",\"professional\":false,\"resultType\":\"ML\",\"defaultRole\":"
                        + "\"FLOOR_MANAGER\",\"defaultImportance\":\"NORMAL\",\"upperLimit\":500,\"billable\":false,"
                        + "\"certificates\":[]}");
        jdbc.update("UPDATE care_task SET status='COMPLETED' WHERE id=?", task);
        var performed = time.now().minus(Duration.ofHours(3));
        var device = time.now().minus(Duration.ofMinutes(10));
        long earlier = com.eldercare.backend.support.TestUsers.nextId();
        shifts.shift(new com.eldercare.backend.care.api.spi.ShiftStaffPort.ShiftView(
                earlier, floor, performed.minus(Duration.ofMinutes(30)), performed.plus(Duration.ofMinutes(30)), true));
        shifts.members(
                earlier,
                java.util.List.of(new com.eldercare.backend.care.api.spi.ShiftStaffPort.ShiftMemberView(
                        performer, com.eldercare.backend.common.Role.FLOOR_MANAGER)));
        shifts.onShift(performer, performed, true);
        shifts.qualified(
                performer,
                java.util.Set.of(),
                performed
                        .atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                        .toLocalDate(),
                true);
        String reference = sync(
                AD_HOC,
                "{\"residentId\":" + resident + ",\"taskTypeId\":" + type + ",\"performedAt\":\"" + performed
                        + "\",\"numericValue\":150,\"sameTypeTasksReviewed\":true}",
                device,
                false);
        assertThat(reference).isNotBlank();
        assertThat(jdbc.queryForMap(
                        "SELECT status,basis_type,result_late_recording FROM care_task WHERE id=?",
                        Long.parseLong(reference)))
                .containsEntry("status", "COMPLETED")
                .containsEntry("basis_type", "AD_HOC_RECORDING")
                .containsEntry("result_late_recording", true);
    }
}
