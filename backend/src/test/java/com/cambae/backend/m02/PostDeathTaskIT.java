package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.m02.api.PostDeathTaskApi;
import com.cambae.backend.m02.api.dto.PostDeathItemCode;
import com.cambae.backend.m02.api.dto.PostDeathItemStatus;
import com.cambae.backend.m02.api.dto.PostDeathTaskView;
import com.cambae.backend.m02.api.events.M02Reminder;
import com.cambae.backend.support.TestUsers.TestUser;
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
        return jdbc.queryForObject("SELECT id FROM death_record WHERE resident_id = ?", Long.class, residentId);
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

    @Test
    void FR_071_openingTheListRecalculates_andWritesTheChangedResultBack() throws Exception {
        long residentId = deadWithOpenBelongings();
        item(PostDeathItemCode.BELONGINGS).status(residentId, PostDeathItemStatus.COMPLETED);

        JsonNode list = postDeathTasks(office, residentId);

        assertThat(task(list, "BELONGINGS").get("status").asString()).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject(
                        "SELECT status FROM post_death_task WHERE death_record_id = ? AND item_code = 'BELONGINGS'",
                        String.class,
                        recordIdOf(residentId)))
                .isEqualTo("COMPLETED");
        assertThat(list.has("closedAt")).as("còn mục quyết toán").isFalse();
    }

    @Test
    void FR_072_whenEveryItemIsDone_theResidencyIsClosed_andTheMarkIsNeverRemoved() throws Exception {
        long residentId = deadWithOpenBelongings();
        item(PostDeathItemCode.BELONGINGS).status(residentId, PostDeathItemStatus.COMPLETED);
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.COMPLETED);

        JsonNode closed = postDeathTasks(office, residentId);

        assertThat(closed.has("closedAt")).isTrue();
        assertThat(residencyClosed(residentId)).isTrue();

        item(PostDeathItemCode.BELONGINGS).status(residentId, PostDeathItemStatus.NOT_COMPLETED);
        JsonNode later = postDeathTasks(office, residentId);

        assertThat(later.get("closedAt")).as("dấu không gỡ được").isEqualTo(closed.get("closedAt"));
        assertThat(task(later, "BELONGINGS").get("status").asString())
                .as("đồ gửi tìm thấy sau khi đóng không mở lại mục (013 FR-026a)")
                .isEqualTo("COMPLETED");
        assertThat(residencyClosed(residentId)).isTrue();
    }

    @Test
    void FR_071_theOwningModuleCanRefreshWithoutAnyoneOpeningTheScreen() throws Exception {
        long residentId = deadWithOpenBelongings();
        item(PostDeathItemCode.BELONGINGS).status(residentId, PostDeathItemStatus.COMPLETED);
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.NOT_APPLICABLE);

        postDeathTasks.refresh(residentId);

        assertThat(residencyClosed(residentId)).isTrue();
        PostDeathTaskView view = postDeathTasks.view(residentId);
        assertThat(view.closedAt()).isNotNull();
        assertThat(view.items()).hasSize(5);
    }

    @Test
    void FR_072_theJobRemindsAdministrativeStaffEachDay_untilEveryItemIsDone() throws Exception {
        long residentId = deadWithOpenBelongings();
        long recordId = recordIdOf(residentId);

        runJobOn(d0.plusDays(1));
        assertThat(reminders(recordId)).hasSize(1);

        runJobOn(d0.plusDays(2));
        assertThat(reminders(recordId)).as("chu kỳ CFG-M02-09 mặc định 1 ngày").hasSize(2);

        item(PostDeathItemCode.BELONGINGS).status(residentId, PostDeathItemStatus.COMPLETED);
        item(PostDeathItemCode.SETTLEMENT).status(residentId, PostDeathItemStatus.COMPLETED);
        runJobOn(d0.plusDays(3));

        assertThat(reminders(recordId))
                .as("job tính lại, đã xong nên không nhắc")
                .hasSize(2);
        assertThat(residencyClosed(residentId)).isTrue();
    }

    @Test
    void FR_072_aClosedListIsNotRemindedAndAnOpenOneNotOnTheSameDayTwice() throws Exception {
        long residentId = deadWithOpenBelongings();
        long recordId = recordIdOf(residentId);

        runJobOn(d0.plusDays(1));
        dailyRunner.catchUp();

        assertThat(reminders(recordId)).hasSize(1);
    }

    @Test
    void FR_071_permission_accountantAndOfficeSee_aCaregiverDoesNot() throws Exception {
        long residentId = deadWithOpenBelongings();
        TestUser accountant = users.staff(Role.ACCOUNTANT);
        TestUser caregiver = users.staff(Role.CAREGIVER);

        getAs(accountant, "/api/v1/residents/" + residentId + "/post-death-tasks")
                .andExpect(status().isOk());
        getAs(caregiver, "/api/v1/residents/" + residentId + "/post-death-tasks")
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_071_aResidentWhoHasNotDied_hasNoList() throws Exception {
        long residentId = inResidence();

        getAs(office, "/api/v1/residents/" + residentId + "/post-death-tasks").andExpect(status().isNotFound());
    }
}
