package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.ProlongedPoorEating;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.WeightLossDetected;
import com.eldercare.backend.nutrition.api.events.ReviewDietNotice;
import com.eldercare.backend.nutrition.internal.review.service.ReviewNoticeService;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.support.TestUsers;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 011 T036 (US8, FR-058, FR-059, BR-M08-05, Q-307; quickstart Q45, Q46): ăn kém kéo dài (005) và sụt cân (007) mỗi sự kiện một
 * thông báo Nhẹ tới Bác sĩ kèm nguồn và chế độ ăn hiện hành; không tạo yêu cầu, không hạn, không nhắc lại; bản gán mới chọn
 * tham chiếu thông báo đã nhận.
 */
class BR_22_ReviewIT extends MenuTestBase {

    private static final String NOTICES = "/api/v1/nutrition/review-notices";

    @Autowired
    ReviewNoticeService review;

    @Autowired
    M08JobService jobs;

    private ProlongedPoorEating poorEating(long resident, long firstMealTask) {
        Instant now = time.now();
        return new ProlongedPoorEating(
                resident,
                List.of(firstMealTask, firstMealTask + 1, firstMealTask + 2),
                now,
                new CareEventContext("MEAL", firstMealTask, "PROLONGED_POOR_EATING", now, Optional.empty(), List.of()));
    }

    private WeightLossDetected weightLoss(long resident, long record) {
        return new WeightLossDetected(resident, record - 5, record, "WEIGHT_LOSS:" + record, time.now());
    }

    private List<ReviewDietNotice> notices() {
        return events.stream(ReviewDietNotice.class).toList();
    }

    private ResultActions listNotices(long resident) throws Exception {
        f.asDoctor();
        return f.send(get(NOTICES + "?residentId=" + resident), null);
    }

    @Test
    void BR_22_q45_poorEatingThenWeightLoss_eachNotifiesDoctorsOnce_noRequestNoReminder() throws Exception {
        long resident = f.resident("IN_RESIDENCE");
        long assignment = f.activeAssignment(resident, defaultDiet, "REGULAR", List.of(), time.now());
        long task = TestUsers.nextId();

        review.onProlongedPoorEating(poorEating(resident, task));
        review.onProlongedPoorEating(poorEating(resident, task));
        assertThat(notices()).singleElement().satisfies(n -> {
            assertThat(n.residentId()).isEqualTo(resident);
            assertThat(n.source()).isEqualTo(ReviewDietNotice.Source.PROLONGED_POOR_EATING);
            NotificationRequest request = n.notificationRequests().getFirst();
            assertThat(request.level()).contains(NotificationLevel.LOW);
            assertThat(request.recipientGroups()).containsExactly(new RecipientGroup.FacilityRole(Role.DOCTOR));
            assertThat(request.title().orElseThrow()).doesNotContain("kém").doesNotContain("sụt");
            assertThat(request.content()).anySatisfy(c -> {
                assertThat(c.type()).isEqualTo(InformationType.HEALTH);
                assertThat(c.content()).contains("ăn kém kéo dài").contains("Chế độ ăn hiện hành");
            });
        });

        long record = TestUsers.nextId();
        review.onWeightLossDetected(weightLoss(resident, record));
        review.onWeightLossDetected(weightLoss(resident, record));
        assertThat(notices()).hasSize(2);
        assertThat(notices().getLast().source()).isEqualTo(ReviewDietNotice.Source.WEIGHT_LOSS);

        listNotices(resident)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.source == 'PROLONGED_POOR_EATING')].sourceId")
                        .value(SlipTestBase.num(task)))
                .andExpect(jsonPath("$[?(@.source == 'WEIGHT_LOSS')].sourceId").value(SlipTestBase.num(record)))
                .andExpect(jsonPath("$[0].currentAssignmentId").value(SlipTestBase.num(assignment)));

        // Không yêu cầu, không hạn, không nhắc lại (Q-307): sau 30 ngày chạy việc định kỳ vẫn chỉ hai thông báo.
        Instant later = time.now().plus(Duration.ofDays(30));
        jobs.runMinuteTasks(later);
        jobs.runDailyTasks(
                later.atZone(com.eldercare.backend.common.time.BusinessTime.ZONE)
                        .toLocalDate(),
                later);
        assertThat(notices()).hasSize(2);
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind LIKE 'M08.%REVIEW%'", Integer.class))
                .isZero();
    }

    private long noticeOf(long resident) throws Exception {
        String body = listNotices(resident)
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(body, "$[0].id")).longValue();
    }

    private String reviewBody(long notice) {
        return "{\"dietId\":" + defaultDiet + ",\"texture\":\"REGULAR\",\"personalRestrictions\":[],"
                + "\"personalInstruction\":\"Ăn chậm\",\"basis\":\"REVIEW\",\"reviewNoticeId\":" + notice
                + ",\"effectiveFrom\":\"" + time.now().plusSeconds(3600) + "\"}";
    }
}
