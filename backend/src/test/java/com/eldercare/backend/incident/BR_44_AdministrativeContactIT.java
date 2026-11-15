package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.incident.api.dto.VisitTracePersonView;
import com.eldercare.backend.incident.api.dto.VisitTraceView;
import com.eldercare.backend.support.TestUsers;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * BR-M05-24, FR-062, Q-401: Hành chính chỉ thấy người thân cần liên hệ trực tiếp (người thân không có kênh nhận thông
 * báo, người đi cùng): họ tên, quan hệ, số điện thoại, nội dung cần báo; không thấy danh sách tiếp xúc hay sức khỏe.
 */
class BR_44_AdministrativeContactIT extends ContactTracingTestBase {
    private static final String TO_NOTIFY = "/api/v1/incidents/{id}/family-contacts-to-notify";

    @Test
    void BR_44_officeSeesOnlyFamilyWithoutChannelAndNothingElse() throws Exception {
        long listId = createList(nurse);
        long withChannel = TestUsers.nextId();
        long withoutChannel = TestUsers.nextId();
        Instant from = traceFrom.plus(Duration.ofDays(1));
        when(family.visitsInRange(eq(resident), any(), any()))
                .thenReturn(List.of(new VisitTraceView(
                        TestUsers.nextId(),
                        from,
                        Optional.of(from.plus(Duration.ofHours(1))),
                        List.of(
                                new VisitTracePersonView(
                                        Optional.of(withChannel),
                                        "Người thân A",
                                        Optional.of("Con gái"),
                                        Optional.of("0901000001"),
                                        true),
                                new VisitTracePersonView(
                                        Optional.of(withoutChannel),
                                        "Người thân B",
                                        Optional.of("Cháu"),
                                        Optional.of("0901000002"),
                                        false)))));
        post(nurse, CONTACTS, listId, contact("FAMILY_MEMBER", withChannel, "FAMILY_VISIT", from, discoveredAt, null))
                .andExpect(status().isCreated());
        post(
                        nurse,
                        CONTACTS,
                        listId,
                        contact("FAMILY_MEMBER", withoutChannel, "FAMILY_VISIT", from, discoveredAt, null)
                                .replace(
                                        "{",
                                        "{\"fullName\":\"Người thân B\",\"relationship\":\"Cháu\",\"phoneNumber\":\"0901000002\","))
                .andExpect(status().isCreated());
        post(nurse, CONFIRM, listId, "{}").andExpect(status().isOk());

        var office = users.staff(Role.OFFICE_STAFF);
        mvc.perform(as(office, get(TO_NOTIFY, incidentId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].fullName").value("Người thân B"))
                .andExpect(jsonPath("$[0].relationship").value("Cháu"))
                .andExpect(jsonPath("$[0].phoneNumber").value("0901000002"))
                .andExpect(jsonPath("$[0].message").isNotEmpty());
        mvc.perform(as(office, get(LIST, incidentId))).andExpect(status().isForbidden());
        mvc.perform(as(nurse, get(TO_NOTIFY, incidentId))).andExpect(status().isOk());
    }
}
