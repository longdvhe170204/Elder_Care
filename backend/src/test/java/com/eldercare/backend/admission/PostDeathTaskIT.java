package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.admission.api.PostDeathTaskApi;
import com.eldercare.backend.admission.api.dto.PostDeathItemCode;
import com.eldercare.backend.admission.api.dto.PostDeathItemStatus;
import com.eldercare.backend.admission.api.events.M02Reminder;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T062 (US8; FR-071, FR-072, FR-073): danh sách việc sau qua đời. Trạng thái từng mục lấy từ module sở hữu (bản giả) tại
 * lúc xem, khi module sở hữu gọi {@code refresh} và khi job nhắc chạy; mọi mục xong thì hồ sơ lưu trú Đã đóng và dấu không gỡ
 * được; nhắc Hành chính theo chu kỳ CFG-M02-09 tới khi hoàn thành.
 */
@AutoConfigureMockMvc
@WithMockUser
class PostDeathTaskIT extends DeathTestBase {

    @Autowired
    private PostDeathTaskApi postDeathTasks;

    private long deadWithOpenBelongings() throws Exception {
        long residentId = inResidence();
        item(PostDeathItemCode.BELONGINGS).status(residentId, PostDeathItemStatus.NOT_COMPLETED);
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.NOT_COMPLETED);
        doctorRecordsDeath(residentId, null);
        return residentId;
    }

    private long recordIdOf(long residentId) {
        return jdbc.queryForObject(
                "SELECT id FROM residency_ending WHERE kind = 'DEATH' AND resident_id = ?", Long.class, residentId);
    }

    private List<M02Reminder> reminders(long recordId) {
        return events.stream(M02Reminder.class)
                .filter(r -> "POST_DEATH_TASKS".equals(r.type()) && r.subjectId() == recordId)
                .toList();
    }

    @Test
    void FR_071_theListShowsFiveItemsWithTheStatusAndBasisFromTheOwningModule() throws Exception {
        long residentId = deadWithOpenBelongings();

        JsonNode list = postDeathTasks(office, residentId);

        assertThat(list.get("items")).hasSize(5);
        assertThat(task(list, "BELONGINGS").get("status").asString()).isEqualTo("NOT_COMPLETED");
        assertThat(task(list, "BELONGINGS").get("basis").asString()).isEqualTo("fake:BELONGINGS");
        assertThat(task(list, "STORED_MEDICATION").get("status").asString()).isEqualTo("NOT_APPLICABLE");
        assertThat(list.has("closedAt")).isFalse();
    }
}
