package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentProgressionStore;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Đề xuất 2026-11-23 (A duyệt qua Đức Anh), BR-M05-29, FR-047i: báo theo tầng xảy ra hoặc khu vực chung, gửi khi ngoài
 * ca, đổi tầng sau khi gửi, nhắn thêm, rút yêu cầu, "Tôi đến giúp" thành người tham gia xử lý, người gửi có quyền xử lý
 * đóng bằng một lệnh và không mang dấu "không ghi mốc tới nơi".
 */
class BR_47_AssistFloorAndSenderIT extends AssistTestBase {
    @Autowired
    IncidentProgressionStore progressions;

    @Test
    void chosenFloorNotifiesThatFloorNotTheShiftFloor() throws Exception {
        postNew(presser, ASSIST, "{\"floorAreaId\":" + otherFloor + "}").andExpect(status().isCreated());

        long alert = assistAlert(presser);
        assertThat(floorOf(alert)).isEqualTo(otherFloor);
        assertThat(leadFloors(assistCreatedGroups(presser))).containsExactly(otherFloor);
        assertThat(staffFloors(assistCreatedGroups(presser))).containsExactly(otherFloor);
    }

    @Test
    void commonAreaNotifiesShiftLeadsOfEveryFloorWithShift() throws Exception {
        postNew(presser, ASSIST, "{\"commonArea\":true}").andExpect(status().isCreated());

        assertThat(floorOf(assistAlert(presser))).isNull();
        assertThat(leadFloors(assistCreatedGroups(presser))).containsExactlyInAnyOrder(floor, otherFloor);
        assertThat(staffFloors(assistCreatedGroups(presser))).isEmpty();
    }

    @Test
    void floorWithoutShiftOrBothChoicesIsRejected() throws Exception {
        postNew(presser, ASSIST, "{\"floorAreaId\":" + TestUsers.nextId() + "}")
                .andExpect(status().isUnprocessableContent());
        postNew(presser, ASSIST, "{\"floorAreaId\":" + otherFloor + ",\"commonArea\":true}")
                .andExpect(status().isUnprocessableContent());
    }

    @Test
    void offShiftSenderCanSendAndIsMarked() throws Exception {
        when(staff.onShift(eq(presser.staffId()), any())).thenReturn(false);

        postNew(presser, ASSIST, "{}").andExpect(status().isCreated());

        long alert = assistAlert(presser);
        assertThat(floorOf(alert)).isNull();
        mvc.perform(as(presser, get("/api/v1/alerts/{id}", alert)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.senderOffShift").value(true))
                .andExpect(jsonPath("$.pressedByMe").value(true));
    }

    @Test
    void senderChangesFloorAndNewFloorIsNotified() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());
        long alert = assistAlert(presser);

        post(presser, ASSIST + "/{id}/floor", alert, "{\"floorAreaId\":" + otherFloor + "}")
                .andExpect(status().isNoContent());

        assertThat(floorOf(alert)).isEqualTo(otherFloor);
        assertThat(leadFloors(detailGroups())).contains(otherFloor);
        post(presser, ASSIST + "/{id}/floor", alert, "{\"floorAreaId\":" + otherFloor + "}")
                .andExpect(status().isUnprocessableContent());
        post(nurse, ASSIST + "/{id}/floor", alert, "{\"commonArea\":true}").andExpect(status().isForbidden());
    }

    @Test
    void senderMessageIsShownAndWithdrawClosesWithoutArrivalFlag() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());
        long alert = assistAlert(presser);

        post(presser, ASSIST + "/{id}/messages", alert, "{\"text\":\"  \"}")
                .andExpect(status().isUnprocessableContent());
        post(presser, ASSIST + "/{id}/messages", alert, "{\"text\":\"Cụ đã tỉnh\"}")
                .andExpect(status().isNoContent());
        mvc.perform(as(presser, get("/api/v1/alerts/{id}", alert)))
                .andExpect(jsonPath("$.senderMessages[0].text").value("Cụ đã tỉnh"))
                .andExpect(jsonPath("$.allowedCommands").value(org.hamcrest.Matchers.hasItem("withdraw")))
                .andExpect(jsonPath("$.allowedCommands")
                        .value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.hasItem("receive"))));

        post(presser, ASSIST + "/{id}/withdraw", alert, "{\"reason\":\"Bấm nhầm\"}")
                .andExpect(status().isNoContent());

        var row = jdbc.queryForMap(
                "SELECT status, resolution, skip_arrival_time, closed_by_id FROM alert WHERE id = ?", alert);
        assertThat(row.get("status")).isEqualTo("CLOSED");
        assertThat(row.get("resolution")).isEqualTo("Người gửi rút yêu cầu: Bấm nhầm");
        assertThat(row.get("skip_arrival_time")).isEqualTo(false);
        assertThat(((Number) row.get("closed_by_id")).longValue()).isEqualTo(presser.accountId());
    }

    @Test
    void withdrawAfterSomeoneReceivedIsRefused() throws Exception {
        pressAssist(presser).andExpect(status().isCreated());
        long alert = assistAlert(presser);
        post(floorManager, "/api/v1/alerts/{id}/receive", alert, "{}").andExpect(status().isOk());

        post(presser, ASSIST + "/{id}/withdraw", alert, "{}").andExpect(status().isConflict());
    }

    @Test
    void helperBecomesIncidentHandlerOnConversion() throws Exception {
        doReturn(Optional.of("Trưởng Tầng Lan")).when(accounts).fullName(floorManager.accountId());
        pressAssist(presser).andExpect(status().isCreated());
        long alert = assistAlert(presser);

        post(floorManager, "/api/v1/alerts/{id}/join-help", alert, "{}").andExpect(status().isOk());
        post(floorManager, "/api/v1/alerts/{id}/join-help", alert, "{}").andExpect(status().isConflict());
        post(presser, "/api/v1/alerts/{id}/join-help", alert, "{}").andExpect(status().isUnprocessableContent());
        mvc.perform(as(presser, get(ASSIST + "/current")))
                .andExpect(jsonPath("$.helperNames[0]").value("Trưởng Tầng Lan"))
                .andExpect(jsonPath("$.sentAt").exists());

        post(doctor, "/api/v1/alerts/{id}/receive", alert, "{}").andExpect(status().isOk());
        post(doctor, "/api/v1/alerts/{id}/start-handling", alert, "{}").andExpect(status().isOk());
        post(
                        doctor,
                        "/api/v1/alerts/{id}/convert-to-incident",
                        alert,
                        "{\"incidentTypeId\":" + typeId("FALL") + ",\"residentId\":" + resident
                                + ",\"location\":\"Hành lang tầng 2\"}")
                .andExpect(status().is2xxSuccessful());

        long incident = jdbc.queryForObject("SELECT incident_id FROM alert WHERE id = ?", Long.class, alert);
        assertThat(progressions.handlerAccountIds(incident)).contains(floorManager.accountId());
        assertThat(jdbc.queryForObject("SELECT location FROM incident WHERE id = ?", String.class, incident))
                .isEqualTo("Hành lang tầng 2");
    }

    @Test
    void senderWithHandlingRightClosesInOneStepWithoutArrivalFlag() throws Exception {
        assign(presser, AssignmentKind.SHIFT_MEMBER, floor, null);
        pressAssist(presser).andExpect(status().isCreated());
        long alert = assistAlert(presser);

        post(presser, "/api/v1/alerts/{id}/close", alert, "{\"resolution\":\"Cụ chóng mặt, đã nằm nghỉ\"}")
                .andExpect(status().is2xxSuccessful());

        var row = jdbc.queryForMap("SELECT status, receiver_id, skip_arrival_time FROM alert WHERE id = ?", alert);
        assertThat(row.get("status")).isEqualTo("CLOSED");
        assertThat(((Number) row.get("receiver_id")).longValue()).isEqualTo(presser.accountId());
        assertThat(row.get("skip_arrival_time")).isEqualTo(false);
    }

    private Long floorOf(long alert) {
        return jdbc.queryForObject("SELECT floor_area_id FROM alert WHERE id = ?", Long.class, alert);
    }

    private static List<Long> leadFloors(List<RecipientGroup> groups) {
        return groups.stream()
                .filter(RecipientGroup.ShiftLead.class::isInstance)
                .map(group -> ((RecipientGroup.ShiftLead) group).floorAreaId())
                .distinct()
                .toList();
    }

    private static List<Long> staffFloors(List<RecipientGroup> groups) {
        return groups.stream()
                .filter(RecipientGroup.OnShiftStaff.class::isInstance)
                .map(group -> ((RecipientGroup.OnShiftStaff) group).floorAreaId())
                .distinct()
                .toList();
    }
}
