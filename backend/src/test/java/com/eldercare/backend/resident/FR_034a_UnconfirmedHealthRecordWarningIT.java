package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.dto.CareLevel;
import com.eldercare.backend.resident.api.dto.HealthItemType;
import com.eldercare.backend.resident.internal.assessment.domain.AssessmentType;
import com.eldercare.backend.resident.internal.health.domain.InitialHealthRecordContent;
import com.eldercare.backend.resident.internal.health.service.HealthItemService;
import com.eldercare.backend.resident.internal.health.service.HealthItemService.RecordCommand;
import com.eldercare.backend.resident.internal.health.service.InitialHealthRecordService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T104 (Q-400, BR-M01-17): hồ sơ sức khỏe ban đầu chưa Bác sĩ xác nhận, hoặc còn mục tự khai chưa kiểm chứng, không chặn
 * Hoàn tất tiếp nhận; lệnh trả cảnh báo {@code UNCONFIRMED_HEALTH_RECORD}. Hồ sơ đã xác nhận và hết mục chưa kiểm chứng thì
 * không cảnh báo.
 */
class FR_034a_UnconfirmedHealthRecordWarningIT extends StatusTestBase {

    private static final String CODE = "UNCONFIRMED_HEALTH_RECORD";
    private static final InitialHealthRecordContent CONTENT =
            new InitialHealthRecordContent("O+", "Đi lại khó", null, null, null, null, "Thuốc A", null);

    @Autowired
    InitialHealthRecordService records;

    @Autowired
    HealthItemService items;

    private long admission() {
        long id = admissionResident();
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Nhận xét đầu vào", "Lưu ý");
        return id;
    }

    private void confirmRecord(long id) {
        user.set(doctor.user());
        var draft = records.saveDraft(target(id), null, CONTENT);
        records.confirm(target(id), "\"" + draft.version() + "\"");
    }

    private java.util.List<String> warningCodes(long id) {
        user.set(office.user());
        return statusService.completeAdmission(target(id), reason(), noDetails()).warnings().stream()
                .map(w -> w.code())
                .toList();
    }

    @Test
    void FR_034a_withoutAnyHealthRecord_completesWithWarning() {
        long id = admission();

        assertThat(warningCodes(id)).contains(CODE);
        assertThat(currentStatus(id).name()).isEqualTo("IN_RESIDENCE");
    }

    @Test
    void FR_034a_draftRecord_completesWithWarning() {
        long id = admission();
        user.set(doctor.user());
        records.saveDraft(target(id), null, CONTENT);

        assertThat(warningCodes(id)).contains(CODE);
    }

    @Test
    void FR_034a_confirmedRecordWithUnverifiedItem_completesWithWarning() {
        long id = admission();
        confirmRecord(id);
        user.set(office.user());
        items.record(
                target(id),
                new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen suyễn", null, sourceId(), false));

        assertThat(warningCodes(id)).contains(CODE);
    }

    @Test
    void FR_034a_confirmedRecordAndVerifiedItems_noWarning() {
        long id = admission();
        confirmRecord(id);
        user.set(doctor.user());
        items.record(
                target(id),
                new RecordCommand(HealthItemType.CHRONIC_CONDITION, null, "Hen suyễn", null, sourceId(), false));

        assertThat(warningCodes(id)).doesNotContain(CODE);
    }
}
