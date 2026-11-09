package com.eldercare.backend.health;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.health.internal.examination.service.ExaminationService;
import com.eldercare.backend.health.internal.examination.service.ExaminationService.ExaminationInput;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/** FR-072, FR-074: bản ghi khám lưu bất biến trên bảng chung, tra lại theo người cao tuổi, mới nhất trước. */
@Transactional
class ExaminationRecordIT extends VitalSignMeasurementTestBase {
    @Autowired
    ExaminationService service;

    @Autowired
    BusinessTime time;

    @Test
    void FR_072_externalRecordIsSavedAndListedNewestFirst() {
        when(measurementActor.accountId()).thenReturn(TestUsers.nextId());
        when(measurementActor.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        long resident = TestUsers.nextId();
        var now = time.now();
        service.record(
                Target.UNSPECIFIED,
                resident,
                new ExaminationInput(
                        "RESULT",
                        "Xét nghiệm máu bình thường",
                        false,
                        "Bệnh viện X",
                        null,
                        "BS Lan",
                        now.minusSeconds(7200),
                        List.of()));
        service.record(
                Target.UNSPECIFIED,
                resident,
                new ExaminationInput(
                        "DIAGNOSIS",
                        "Viêm phổi",
                        false,
                        "Bệnh viện X",
                        null,
                        "BS Lan",
                        now.minusSeconds(60),
                        List.of(5L)));

        var list = service.list(Target.UNSPECIFIED, resident);

        assertThat(list).extracting(ExaminationService.ExaminationView::type).containsExactly("DIAGNOSIS", "RESULT");
        assertThat(list.get(0).fileIds()).containsExactly(5L);
        assertThat(list.get(0).externalFacilityName()).isEqualTo("Bệnh viện X");
        assertThat(list.get(0).atFacility()).isFalse();
    }
}
