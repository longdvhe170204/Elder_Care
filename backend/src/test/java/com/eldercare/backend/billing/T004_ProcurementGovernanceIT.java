package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.file.FilePermissionProvider;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 019 T004 phần M11 (research W-08, W-09; module-interfaces mục 3): phạm vi {@code M11.PROCUREMENT_REQUEST}, lưu giữ không
 * gắn hồ sơ tính từ lúc vào trạng thái cuối, quyền tải chứng từ {@code M11.PROCUREMENT_RECEIPT} theo quyền xem yêu cầu.
 */
class T004_ProcurementGovernanceIT extends ProcurementTestBase {

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    List<FilePermissionProvider> filePermissions;

    @Test
    void FR_034_requestScopeHasNoFloorOrResidentForIngredientRequests() throws Exception {
        long id = created(ingredientBody(kitchen));
        var target = scopes.stream()
                .filter(p -> p.entityType().equals("M11.PROCUREMENT_REQUEST"))
                .findFirst()
                .orElseThrow()
                .assignScope(id);
        assertThat(target.id()).isEqualTo(Long.toString(id));
        assertThat(target.residentId()).isNull();
        assertThat(target.floorAreaId()).isNull();
    }
}
