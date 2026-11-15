package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.events.ResidentCreated;
import com.eldercare.backend.resident.internal.profile.service.ResidentProfileService.CreateResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * 001 T014 (US1 #1; FR-001, FR-002, FR-003; DBR-23): tạo hồ sơ cấp mã duy nhất, trạng thái Đang tiếp nhận, bản ghi
 * lịch sử đầu tiên kèm người tạo và thời điểm, đúng một bản ghi nhật ký, phát {@code ResidentCreated}.
 */
class CreateProfileIT extends ResidentProfileTestBase {

    @Autowired
    ApplicationEvents events;

    @Test
    void US1_1_create_issuesUniqueCodeAndAdmissionStatus() {
        String name = uniqueName();

        CreateResult first = createAsOffice(noDocument(name));
        CreateResult second = createAsOffice(noDocument(uniqueName()));

        assertThat(first.profile().profileCode()).matches("NCT-\\d{4}-\\d{6,}");
        assertThat(first.profile().profileCode()).isNotEqualTo(second.profile().profileCode());
        assertThat(first.profile().id()).isPositive();
        assertThat(first.profile().status()).isEqualTo(ResidentStatus.ADMISSION_IN_PROGRESS);
        assertThat(first.profile().personalInfo().fullName()).isEqualTo(name);
        assertThat(first.profile().currentCareLevel()).isNull();
        assertThat(first.profile().previousProfileId()).isNull();
        assertThat(first.warnings()).isEmpty();
    }

    @Test
    void DBR_23_create_writesExactlyOneAuditEntryAndPublishesEvent() {
        CreateResult created = createAsOffice(noDocument(uniqueName()));
        long id = created.profile().id();

        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM audit_log WHERE entity_type = 'M01.RESIDENT' AND entity_id = ?"
                                + " AND action = 'RESIDENT_CREATED' AND resident_id = ? AND actor_id = ?",
                        Long.class,
                        String.valueOf(id),
                        id,
                        OFFICE.accountId()))
                .isEqualTo(1);
        assertThat(events.stream(ResidentCreated.class).map(ResidentCreated::residentId))
                .contains(id);
    }
}
