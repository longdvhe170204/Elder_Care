package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import com.eldercare.backend.administration.account.api.dto.PermissionKey;
import com.eldercare.backend.resident.api.dto.*;
import org.junit.jupiter.api.Test;

/**
 * 005 T034 (BR-M04-12, FR-035, FR-028a; research C-14; payload chủ dự án duyệt 2026-11-03; loại lý do theo thời điểm
 * thiết bị tra nhật ký ngừng hiệu lực, người dùng chọn 2026-11-06): ghi nhận đồng bộ sau mất kết nối dùng thời điểm thiết
 * bị làm "thời điểm ghi", gắn nhãn ghi muộn theo CFG-M04-06, giữ cờ mất quyền; hai loại bản ghi {@code M04.CARE_TASK_RESULT},
 * {@code M04.AD_HOC_RECORDING}.
 */
class BR_20_LateOfflineRecordingIT extends CareOfflineTestBase {
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
}
