package com.eldercare.backend.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.common.file.FilePermissionProvider;
import java.time.Instant;
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

    @Test
    void W_09_onlyFinalRequestsExpireFromTheirLockTime() throws Exception {
        var provider = retention.stream()
                .filter(p -> p.dataType().equals("M11.PROCUREMENT_REQUEST"))
                .findFirst()
                .orElseThrow();
        assertThat(provider.retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        long open = approved(1_800_000);
        as(kitchenLead);
        long cancelled = created(ingredientBody(kitchen));
        command(cancelled, "cancel").andExpect(status().isOk());

        var future = Instant.now().plusSeconds(3_650L * 24 * 3600);
        var ids = provider.expiredCandidates(future, 10_000)
                .map(RetentionProvider.Candidate::id)
                .toList();
        assertThat(ids).contains(cancelled).doesNotContain(open);
        assertThat(provider.expiredCandidates(Instant.parse("2001-01-01T00:00:00Z"), 100))
                .isEmpty();
    }

    @Test
    void NFR_08_receiptDownloadFollowsRequestVisibility() throws Exception {
        long id = purchased(1_800_000, 1_900_000);
        var receipt = filePermissions.stream()
                .filter(p -> p.ownerType().equals("M11.PROCUREMENT_RECEIPT"))
                .findFirst()
                .orElseThrow();
        as(kitchenLead);
        assertThat(receipt.canDownload(id)).isTrue();
        as(accountant);
        assertThat(receipt.canDownload(id)).isTrue();
        as(manager);
        assertThat(receipt.canDownload(id)).isTrue();
        as(kitchenStaff);
        assertThat(receipt.canDownload(id)).isFalse();
        as(technicalStaff);
        assertThat(receipt.canDownload(id)).isFalse();
    }
}
