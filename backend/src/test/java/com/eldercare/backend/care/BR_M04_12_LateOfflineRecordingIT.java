package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;

/**
 * 005 T034 (BR-M04-12, FR-035, FR-028a; research C-14; payload chủ dự án duyệt 2026-11-03; loại lý do theo thời điểm
 * thiết bị tra nhật ký ngừng hiệu lực, người dùng chọn 2026-11-06): ghi nhận đồng bộ sau mất kết nối dùng thời điểm thiết
 * bị làm "thời điểm ghi", gắn nhãn ghi muộn theo CFG-M04-06, giữ cờ mất quyền; hai loại bản ghi {@code M04.CARE_TASK_RESULT},
 * {@code M04.AD_HOC_RECORDING}.
 */
class BR_M04_12_LateOfflineRecordingIT extends CareOfflineTestBase {
    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.context.ApplicationContext context;

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

    /**
     * BR-M04-27: đồng bộ bản ghi cho công việc đã Hủy bị từ chối (giữ nội dung trên máy); công việc hệ thống đóng vì
     * trạng thái cuối thì bản ghi thực hiện trước lúc đó thành Ghi nhận phát sinh, sau lúc đó bị từ chối.
     */
    @Test
    void BR_M04_27_closedTaskSyncRejectedAndFinalStatusClosureBecomesAdHocOnlyBefore() {
        var now = time.now();
        jdbc.update(
                "INSERT INTO catalog_item(id,catalog_type,module,name,status,attributes) VALUES(?,'M04.TASK_TYPE','M04',?,'ACTIVE',?)",
                type,
                "Uống nước kiểm thử " + type,
                "{\"group\":\"OTHER\",\"professional\":false,\"resultType\":\"ML\",\"upperLimit\":500,\"defaultRole\":\"FLOOR_MANAGER\",\"defaultImportance\":\"NORMAL\",\"billable\":false,\"certificates\":[]}");
        long cancelled = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO care_task(id,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,status,data,created_by,created_at)"
                        + " SELECT ?,resident_id,task_type_id,basis_type,shift_id,floor_area_id,planned_at,window_from,window_to,importance,performer_role,performer_id,'CANCELLED',data,created_by,created_at FROM care_task WHERE id=?",
                cancelled,
                task);
        long original = task;
        task = cancelled;
        try {
            assertThatThrownBy(() -> sync(RESULT, recordAt(now.minusSeconds(120)), now, false))
                    .isInstanceOf(com.eldercare.backend.common.error.BusinessException.class)
                    .extracting(e -> ((com.eldercare.backend.common.error.BusinessException) e).errorCode())
                    .isEqualTo("CARE_TASK_CLOSED");
        } finally {
            task = original;
        }

        var finalAt = now.minusSeconds(60);
        residents.changeStatus(resident, ResidentStatus.DECEASED, finalAt);
        context.getBean("careStatusParticipant", StatusTransitionParticipant.class)
                .applyEffects(new TransitionResult(
                        resident,
                        ResidentStatus.IN_RESIDENCE,
                        ResidentStatus.DECEASED,
                        StatusCommand.RECORD_DEATH,
                        TestUsers.nextId(),
                        finalAt,
                        json.createObjectNode()));
        assertThat(jdbc.queryForObject("SELECT closure_rule FROM care_task WHERE id=?", String.class, task))
                .isEqualTo("FR_010_FINAL_STATE");

        shifts.onShift(performer, finalAt.minusSeconds(120), true);
        long adHoc = Long.parseLong(sync(RESULT, recordAt(finalAt.minusSeconds(120)), now, false));
        assertThat(adHoc).isNotEqualTo(task);
        assertThat(jdbc.queryForObject("SELECT basis_type FROM care_task WHERE id=?", String.class, adHoc))
                .isEqualTo("AD_HOC_RECORDING");
        assertThatThrownBy(() -> sync(RESULT, recordAt(finalAt.plusSeconds(30)), now, false))
                .extracting(e -> ((com.eldercare.backend.common.error.BusinessException) e).errorCode())
                .isEqualTo("AFTER_FINAL_STATUS");
    }

    private String recordAt(java.time.Instant performedAt) {
        return resultBody("RECORD", "{\"numericValue\":200,\"performedAt\":\"" + performedAt + "\"}", null);
    }
}
