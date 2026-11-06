package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.spi.InvalidPermissionDataException;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.support.TestUsers;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 019 T004 phần M03.ASSET (research W-08, W-09; module-interfaces mục 3): phạm vi theo tầng của vị trí, lưu giữ không gắn
 * hồ sơ tính từ lúc thanh lý, nguyên giá và chi phí bảo trì thuộc nhóm tài chính.
 */
class T004_AssetGovernanceIT extends AssetTestBase {

    @Autowired
    List<ScopeAssignmentProvider> scopes;

    @Autowired
    List<RetentionProvider> retention;

    @Autowired
    List<FieldClassificationProvider> fields;

    @Test
    void FR_034_assetScopeIsTheFloorOfItsLocation() throws Exception {
        long id = created(donated("Tặng"));
        command(id, "put-into-use", "{\"location\":{\"type\":\"ROOM\",\"id\":" + room + "}}");
        var provider = scopes.stream()
                .filter(p -> p.entityType().equals("M03.ASSET"))
                .findFirst()
                .orElseThrow();
        assertThat(provider.assignScope(id).floorAreaId()).isEqualTo(floor);
        assertThatThrownBy(() -> provider.assignScope(-TestUsers.nextId()))
                .isInstanceOf(InvalidPermissionDataException.class);
    }

    @Test
    void W_09_onlyDisposedAssetsExpire() throws Exception {
        var provider = retention.stream()
                .filter(p -> p.dataType().equals("M03.ASSET"))
                .findFirst()
                .orElseThrow();
        assertThat(provider.retentionKind()).isInstanceOf(RetentionKind.NotResidentLinked.class);
        long open = created(donated("Tặng"));
        long disposed = created(donated("Tặng"));
        jdbc.update("UPDATE asset SET status='DISPOSED', disposed_at=NOW() WHERE id=?", disposed);
        var ids = provider.expiredCandidates(Instant.now().plusSeconds(86_400L * 3650), 10_000)
                .map(RetentionProvider.Candidate::id)
                .toList();
        assertThat(ids).contains(disposed).doesNotContain(open);
    }

    @Test
    void NFR_08_acquisitionCostAndMaintenanceCostAreFinance() {
        assertThat(fields.stream()
                        .filter(p -> p.entityType().equals("M03.ASSET"))
                        .findFirst()
                        .orElseThrow()
                        .fieldGroups())
                .containsEntry("acquisitionCost", InfoGroup.FINANCE);
        assertThat(fields.stream()
                        .filter(p -> p.entityType().equals("M03.ASSET_MAINTENANCE"))
                        .findFirst()
                        .orElseThrow()
                        .fieldGroups())
                .containsEntry("cost", InfoGroup.FINANCE);
    }
}
