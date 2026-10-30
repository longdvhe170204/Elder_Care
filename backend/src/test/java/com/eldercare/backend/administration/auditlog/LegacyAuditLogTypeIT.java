package com.eldercare.backend.administration.auditlog;

import static com.eldercare.backend.administration.auditlog.AuditLogFixtures.MANAGER;
import static com.eldercare.backend.administration.sample.SampleLegacyRecordTypes.LEGACY_TYPE;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.auditlog.api.dto.AuditAction;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogEntry;
import com.eldercare.backend.administration.auditlog.internal.service.AuditLogQueryService;
import com.eldercare.backend.administration.sample.SampleFloorRecordProvider;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Nhật ký ghi với loại đối tượng cũ trước khi gộp bảng được tra theo loại mới (research R-15 mục 5, 000 T108 phần 2):
 * {@code SampleLegacyRecordTypes} ánh xạ {@code M15.SAMPLE_RECORD_V0} sang {@code M15.SAMPLE_RECORD}.
 */
@Import(AuditLogFixtures.class)
class LegacyAuditLogTypeIT extends MySqlIntegrationTestBase {

    @Autowired
    AuditLogApi auditLog;

    @Autowired
    AuditLogQueryService query;

    @Autowired
    SwitchableUser user;

    @Test
    void logOfLegacyType_isFoundByCurrentType_andShownAsCurrentType() {
        long id = TestUsers.nextId();
        user.set(MANAGER);
        auditLog.writeOutsideTransaction(AuditLogEntry.builder()
                .action(new AuditAction("SAMPLE_LEGACY", false))
                .entity(LEGACY_TYPE, id)
                .build());

        var page = query.search(
                new AuditLogQueryService.Filter(
                        SampleFloorRecordProvider.TYPE, String.valueOf(id), null, null, null, null, null),
                null);

        assertThat(page.items()).extracting(e -> e.action()).containsExactly("SAMPLE_LEGACY");
        assertThat(page.items()).extracting(e -> e.entity().type()).containsOnly(SampleFloorRecordProvider.TYPE);
    }
}
