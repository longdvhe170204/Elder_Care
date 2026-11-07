package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.nutrition.api.events.ResidentWithoutDietAssignment;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.dto.TransitionResult;
import com.eldercare.backend.resident.api.spi.StatusTransitionParticipant;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 011 T012 (US1, FR-014, FR-015; module-interfaces mục 1.1; quickstart Q5): bean tham gia lệnh trạng thái của 001.
 * Trạng thái cuối (Kết thúc lưu trú, Qua đời, Hủy tiếp nhận): bản Hiệu lực → Ngừng, Đề xuất/Chờ hiệu lực → Đã hủy, lý
 * do theo sự kiện nguồn, không thông báo. Tạm vắng không đổi bản gán. Hoàn tất tiếp nhận khi chưa có bản gán thì báo Bác
 * sĩ mức Nhẹ.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import(FR_015_FinalStatusIT.Users.class)
class FR_015_FinalStatusIT extends MySqlIntegrationTestBase {

    @TestConfiguration
    static class Users {
        @Bean
        @Primary
        SwitchableUser nutritionStatusUser() {
            return new SwitchableUser();
        }
    }

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    List<StatusTransitionParticipant> participants;

    @Autowired
    ApplicationEvents events;

    NutritionCatalogFixture f;
    long regular;
    long resident;

    @BeforeEach
    void setUp() throws Exception {
        f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        long base = f.baseDiet();
        long safe = f.activeDish(List.of(), List.of(base));
        regular = f.diet(false, List.of(), safe);
        resident = f.resident("IN_RESIDENCE");
    }

    private StatusTransitionParticipant nutrition() {
        return participants.stream()
                .filter(p -> p.getClass().getName().startsWith("com.eldercare.backend.nutrition."))
                .findFirst()
                .orElseThrow();
    }

    private void transition(StatusCommand command, ResidentStatus from, ResidentStatus to) {
        f.asDoctor();
        nutrition().applyEffects(new TransitionResult(resident, from, to, command, 0L, time.now(), null));
    }

    @Test
    void FR_015_death_discontinuesActive_cancelsPending() throws Exception {
        long active = f.activeAssignment(resident, regular, "REGULAR", List.of(), time.now());
        long other = f.diet(false, List.of(), f.activeDish(List.of(), List.of(regular)));
        f.propose(
                resident,
                NutritionCatalogFixture.assignmentBody(
                        other, "SOFT", List.of(), time.now().plus(3, ChronoUnit.DAYS)));
        long pending = f.latestAssignment(resident);
        f.apply(pending, null);
        assertThat(f.assignmentStatus(pending)).isEqualTo("PENDING_EFFECTIVE");

        transition(StatusCommand.RECORD_DEATH, ResidentStatus.IN_RESIDENCE, ResidentStatus.DECEASED);

        assertThat(f.assignmentStatus(active)).isEqualTo("DISCONTINUED");
        assertThat(f.assignmentStatus(pending)).isEqualTo("CANCELLED");
        assertThat(jdbc.queryForObject("SELECT cancel_reason FROM diet_assignment WHERE id = ?", String.class, pending))
                .isNotBlank();
    }

    @Test
    void FR_015_endResidency_cancelsProposal() throws Exception {
        f.propose(resident, NutritionCatalogFixture.assignmentBody(regular, "REGULAR", List.of(), time.now()));
        long proposal = f.latestAssignment(resident);

        transition(StatusCommand.END_RESIDENCY, ResidentStatus.IN_RESIDENCE, ResidentStatus.RESIDENCY_ENDED);

        assertThat(f.assignmentStatus(proposal)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_015_temporaryAbsence_doesNotChangeAssignment() throws Exception {
        long active = f.activeAssignment(resident, regular, "REGULAR", List.of(), time.now());

        assertThat(nutrition().interestedCommands()).doesNotContain(StatusCommand.GRANT_TEMPORARY_ABSENCE);
        transition(
                StatusCommand.GRANT_TEMPORARY_ABSENCE, ResidentStatus.IN_RESIDENCE, ResidentStatus.TEMPORARILY_ABSENT);

        assertThat(f.assignmentStatus(active)).isEqualTo("ACTIVE");
    }

    @Test
    void FR_014_admissionCompletedWithoutAssignment_notifiesDoctor() {
        transition(StatusCommand.COMPLETE_ADMISSION, ResidentStatus.ADMISSION_IN_PROGRESS, ResidentStatus.IN_RESIDENCE);

        var notice = events.stream(ResidentWithoutDietAssignment.class)
                .filter(e -> e.residentId() == resident)
                .findFirst()
                .orElseThrow();
        var request = notice.notificationRequests().getFirst();
        assertThat(request.level()).contains(NotificationLevel.LOW);
        assertThat(request.recipientGroups()).containsExactly(new RecipientGroup.FacilityRole(Role.DOCTOR));
        assertThat(request.residentId()).contains(resident);
    }

    @Test
    void FR_014_admissionCompletedWithAssignment_noNotice() throws Exception {
        f.activeAssignment(resident, regular, "REGULAR", List.of(), time.now());

        transition(StatusCommand.COMPLETE_ADMISSION, ResidentStatus.ADMISSION_IN_PROGRESS, ResidentStatus.IN_RESIDENCE);

        assertThat(events.stream(ResidentWithoutDietAssignment.class).filter(e -> e.residentId() == resident))
                .isEmpty();
    }
}
