package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.care.api.spi.TaskSourcePort.SourceType;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.InformationSubject;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.incident.api.dto.VisitTracePersonView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import com.eldercare.backend.incident.api.events.ContactAdded;
import com.eldercare.backend.incident.api.events.ContactNotificationRequested;
import com.eldercare.backend.incident.api.events.ContactTemperatureCheckRequested;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.roombedallocation.api.spi.AllocationInfectionStatus;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * FR-061, FR-062, BR-M05-10, 11 (US8 #3, #8): danh sách do người có quyền lập, thêm, bỏ, xác nhận, loại; xác nhận gắn
 * dấu tiếp xúc, báo Người phụ trách ca lập công việc đo nhiệt độ (không tự tạo việc), báo nhân viên, người thân bằng nội
 * dung không danh tính và báo Quản lý viện; loại người gỡ dấu và báo 005 hủy công việc theo căn cứ đó.
 */
class FR_062_ContactNotificationIT extends ContactTracingTestBase {
    private static final long CONTACT_FLOOR = 7002L;

    @Autowired
    ApplicationEvents events;

    @Autowired
    AllocationInfectionStatus infections;

    long listId;
    long b;
    long f;
    long s;
    long n;
    Instant from;
    Instant to;

    @BeforeEach
    void draftList() throws Exception {
        listId = createList(nurse);
        b = TestUsers.nextId();
        f = TestUsers.nextId();
        s = TestUsers.nextId();
        n = TestUsers.nextId();
        from = traceFrom.plus(Duration.ofDays(1));
        to = discoveredAt;
        when(locations.current(b))
                .thenReturn(Optional.of(new LocationView(null, null, CONTACT_FLOOR, "Tầng 2", 202L, "P202", 3L, "G3")));
        when(family.visitsInRange(eq(resident), any(), any()))
                .thenReturn(List.of(new VisitTraceView(
                        TestUsers.nextId(),
                        from,
                        Optional.of(from.plus(Duration.ofHours(1))),
                        List.of(new VisitTracePersonView(
                                Optional.of(n),
                                "Người thân N",
                                Optional.of("Con gái"),
                                Optional.of("0901000002"),
                                true)))));
    }

    @Test
    void FR_062_confirmMarksContactsAndNotifiesWithoutIdentity() throws Exception {
        post(nurse, CONTACTS, listId, contact("RESIDENT", b, "SAME_ROOM", from, to, null))
                .andExpect(status().isCreated());
        post(nurse, CONTACTS, listId, contact("RESIDENT", f, "OTHER", from, to, null))
                .andExpect(status().isUnprocessableContent());
        post(nurse, CONTACTS, listId, contact("RESIDENT", f, "OTHER", from, to, "Ăn chung bàn"))
                .andExpect(status().isCreated());
        post(nurse, CONTACTS, listId, contact("STAFF", s, "CARE_STAFF", from, to, null))
                .andExpect(status().isCreated());
        post(nurse, CONTACTS, listId, contact("FAMILY_MEMBER", n, "FAMILY_VISIT", from, to, null))
                .andExpect(status().isCreated());
        post(nurse, CONTACTS, listId, contact("COMPANION", null, "FAMILY_VISIT", from, to, null))
                .andExpect(status().isCreated());
        post(nurse, CONTACTS, listId, contact("RESIDENT", b, "SAME_ROOM", from, to, null))
                .andExpect(status().isConflict());
        post(nurse, CONTACTS, listId, contact("RESIDENT", resident, "SAME_ROOM", from, to, null))
                .andExpect(status().isUnprocessableContent());
        long companion = jdbc.queryForObject(
                "SELECT MAX(id) FROM simple_record WHERE kind = 'M05.CONTACT_PERSON' AND entity_id = ?"
                        + " AND JSON_UNQUOTE(JSON_EXTRACT(content, '$.type')) = 'COMPANION'",
                Long.class,
                listId);
        post(nurse, CONTACTS + "/{contactId}/remove", listId, companion, "{}").andExpect(status().isOk());
        assertThat(events.stream(ContactTemperatureCheckRequested.class)).isEmpty();
        assertThat(infections.listedAt(b, discoveredAt.plusSeconds(1))).isFalse();

        post(nurse, CONFIRM, listId, "{}").andExpect(status().isOk());

        mvc.perform(as(nurse, get(LIST, incidentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.contacts.length()").value(4))
                .andExpect(jsonPath("$.noContacts").value(false));
        var now = time.now();
        assertThat(infections.listedAt(b, now)).isTrue();
        assertThat(infections.listedAt(f, now)).isTrue();
        var days = parameters
                .read(ParameterCode.CFG_M05_08)
                .raw()
                .get("duration")
                .get("value")
                .asLong();
        var temperature = events.stream(ContactTemperatureCheckRequested.class).toList();
        assertThat(temperature)
                .extracting(ContactTemperatureCheckRequested::residentId)
                .containsExactlyInAnyOrder(b, f);
        var forB = temperature.stream()
                .filter(e -> e.residentId() == b)
                .findFirst()
                .orElseThrow();
        assertThat(Duration.between(forB.from(), forB.to())).isEqualTo(Duration.ofDays(days));
        assertThat(forB.notificationRequests().getFirst().recipientGroups())
                .containsExactly(new RecipientGroup.ShiftLead(CONTACT_FLOOR, new Substitution.Default()));
        assertThat(events.stream(ContactAdded.class))
                .extracting(ContactAdded::residentId)
                .containsExactlyInAnyOrder(b, f);

        var notices = events.stream(ContactNotificationRequested.class)
                .flatMap(e -> e.notificationRequests().stream())
                .toList();
        var toStaff = notices.stream()
                .filter(r -> r.recipientGroups().contains(new RecipientGroup.Staff(s, "CONTACT_LIST")))
                .findFirst()
                .orElseThrow();
        var toFamily = notices.stream()
                .filter(r -> r.recipientGroups().contains(new RecipientGroup.FamilyMember(n, "CONTACT_LIST")))
                .findFirst()
                .orElseThrow();
        assertThat(toFamily.q354Case()).contains(Q354Case.CONTACT_LIST);
        assertThat(toFamily.informationSubject()).contains(InformationSubject.ABOUT_RESIDENT);
        for (var r : List.of(toStaff, toFamily)) {
            String text = r.content().getFirst().content();
            assertThat(text).contains("Có thể đã tiếp xúc tại Tầng 2").doesNotContain("P201", "[NCT kiểm thử]");
            assertThat(r.residentId()).isEmpty();
        }
        assertThat(notices)
                .anyMatch(
                        r -> r.recipientGroups().contains(new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER)));
        assertThat(notices)
                .noneMatch(r -> r.recipientGroups().stream()
                        .anyMatch(g -> g instanceof RecipientGroup.FamilyMember
                                && !g.equals(new RecipientGroup.FamilyMember(n, "CONTACT_LIST"))));
    }

    @Test
    void FR_061_supplementNeedsReasonAndExcludeRemovesMarkAndCancelsTasks() throws Exception {
        post(nurse, CONTACTS, listId, contact("RESIDENT", b, "SAME_ROOM", from, to, null))
                .andExpect(status().isCreated());
        long bRow = contactId(listId, "RESIDENT", b);
        post(nurse, CONTACTS + "/{contactId}/exclude", listId, bRow, "{\"reason\":\"Nhầm\"}")
                .andExpect(status().isConflict());
        post(doctor, CONFIRM, listId, "{}").andExpect(status().isOk());

        long c = TestUsers.nextId();
        post(nurse, CONTACTS, listId, contact("RESIDENT", c, "ACTIVITY", from, to, null))
                .andExpect(status().isUnprocessableContent());
        post(nurse, CONTACTS, listId, contact("RESIDENT", c, "ACTIVITY", from, to, "Tra cứu lại thấy cùng buổi"))
                .andExpect(status().isCreated());
        assertThat(infections.listedAt(c, time.now())).isTrue();
        assertThat(events.stream(ContactTemperatureCheckRequested.class))
                .extracting(ContactTemperatureCheckRequested::residentId)
                .containsExactlyInAnyOrder(b, c);

        post(nurse, CONTACTS + "/{contactId}/remove", listId, bRow, "{}").andExpect(status().isConflict());
        post(nurse, CONTACTS + "/{contactId}/exclude", listId, bRow, "{}").andExpect(status().isUnprocessableContent());
        verify(taskSources, never()).sourceCancelled(any(), eq(bRow), any());
        post(nurse, CONTACTS + "/{contactId}/exclude", listId, bRow, "{\"reason\":\"Không ở cùng phòng thực tế\"}")
                .andExpect(status().isOk());
        assertThat(infections.listedAt(b, time.now())).isFalse();
        verify(taskSources).sourceCancelled(eq(SourceType.CONTACT_MONITORING), eq(bRow), any());
        mvc.perform(as(nurse, get(LIST, incidentId)))
                .andExpect(jsonPath("$.contacts[?(@.subjectId == " + b + ")].excludedAt")
                        .isNotEmpty());
    }

    @Test
    void FR_061_confirmNeedsSomeoneOrNoContactsWithReason() throws Exception {
        post(nurse, CONFIRM, listId, "{}").andExpect(status().isConflict());
        post(nurse, CONFIRM, listId, "{\"noContacts\":true}").andExpect(status().isUnprocessableContent());
        post(nurse, CONFIRM, listId, "{\"noContacts\":true,\"reason\":\"Ở phòng đơn, không tham gia hoạt động\"}")
                .andExpect(status().isOk());
        mvc.perform(as(nurse, get(LIST, incidentId)))
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.noContacts").value(true));
        post(nurse, CONFIRM, listId, "{}").andExpect(status().isConflict());
    }

    @Test
    void FR_060_managerViewsOnlyAndFloorManagerCannotChangeList() throws Exception {
        var manager = users.staff(Role.NURSING_HOME_MANAGER);
        mvc.perform(as(manager, get(LIST, incidentId))).andExpect(status().isOk());
        post(manager, CONTACTS, listId, contact("RESIDENT", b, "SAME_ROOM", from, to, null))
                .andExpect(status().isForbidden());
        post(floorManager, CONTACTS, listId, contact("RESIDENT", b, "SAME_ROOM", from, to, null))
                .andExpect(status().isForbidden());
        post(manager, CONFIRM, listId, "{}").andExpect(status().isForbidden());
    }

    @Test
    void FR_061_closingInfectionEndsListButKeepsContactMark() throws Exception {
        post(nurse, CONTACTS, listId, contact("RESIDENT", b, "SAME_ROOM", from, to, null))
                .andExpect(status().isCreated());
        post(nurse, CONFIRM, listId, "{}").andExpect(status().isOk());
        jdbc.update(
                "UPDATE incident SET status = 'IN_PROGRESS', assignee_id = ? WHERE id = ?",
                nurse.accountId(),
                incidentId);

        post(
                        nurse,
                        "/api/v1/incidents/{id}/close",
                        incidentId,
                        "{\"resolution\":\"Đã loại trừ\",\"infectionOutcome\":\"RULED_OUT\"}")
                .andExpect(status().isOk());

        mvc.perform(as(nurse, get(LIST, incidentId)))
                .andExpect(jsonPath("$.status").value("ENDED"));
        assertThat(infections.listedAt(b, time.now())).isTrue();
        post(nurse, CONTACTS, listId, contact("RESIDENT", TestUsers.nextId(), "SAME_ROOM", from, to, "Bổ sung"))
                .andExpect(status().isConflict());
    }

    org.springframework.test.web.servlet.ResultActions post(
            com.eldercare.backend.support.TestUsers.TestUser as, String path, long id, long sub, String json)
            throws Exception {
        return mvc.perform(
                as(as, org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path, id, sub))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json));
    }
}
