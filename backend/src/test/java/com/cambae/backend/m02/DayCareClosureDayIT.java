package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.common.Role;
import com.cambae.backend.m02.api.events.DayCareClosureDayChanged;
import java.sql.SQLException;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.JsonNode;

/**
 * 004 T050 (FR-060a; research T-08): Hành chính khai báo ngày khu bán trú nghỉ (ngày, lý do); mỗi ngày tối đa một khai báo
 * Hiệu lực; ngày đã qua là khai báo muộn; hủy rồi khai báo lại được; 005, 010, 011 được báo qua sự kiện. Khai báo và hủy
 * không trả nội dung (hợp đồng api-004); đọc lại qua {@code GET /day-care-closure-days}.
 */
@AutoConfigureMockMvc
@WithMockUser
class DayCareClosureDayIT extends AbsenceTestBase {

    @BeforeEach
    void clean() throws SQLException {
        WaitingListTestBase.admin("UPDATE day_care_closure_day SET status = 'CANCELLED'");
    }

    @AfterEach
    void cleanAfter() throws SQLException {
        WaitingListTestBase.admin("UPDATE day_care_closure_day SET status = 'CANCELLED'");
    }

    private ResultActions declare(LocalDate date, String reason) throws Exception {
        user.set(office.user());
        return mvc.perform(post("/api/v1/day-care-closure-days")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(reason == null ? "{\"date\":\"" + date + "\"}" : """
                        {"date":"%s","reason":"%s"}""".formatted(date, reason)));
    }

    /** Các khai báo trong đúng ngày {@code date}, đọc qua {@code GET}. */
    private JsonNode declarationsOn(LocalDate date) throws Exception {
        user.set(office.user());
        return JSON.readTree(mvc.perform(get("/api/v1/day-care-closure-days")
                        .param("from", date.toString())
                        .param("to", date.toString()))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
    }

    @Test
    void FR_060a_declaringAFutureDay_isActive_notLate_andPublishesTheEvent() throws Exception {
        LocalDate day = d0.plusDays(20);

        declare(day, "Sửa chữa khu bán trú").andExpect(status().isCreated());

        JsonNode listed = declarationsOn(day);
        assertThat(listed).hasSize(1);
        assertThat(listed.get(0).get("status").asString()).isEqualTo("ACTIVE");
        assertThat(listed.get(0).get("lateDeclaration").asBoolean()).isFalse();
        assertThat(listed.get(0).get("reason").asString()).isEqualTo("Sửa chữa khu bán trú");
        assertThat(events.stream(DayCareClosureDayChanged.class)
                        .filter(e -> e.date().equals(day)))
                .extracting(DayCareClosureDayChanged::active)
                .containsExactly(true);
    }

    @Test
    void FR_060a_aDayThatHasPassed_isALateDeclaration() throws Exception {
        LocalDate day = d0.minusDays(2);

        declare(day, "Quên khai báo").andExpect(status().isCreated());

        assertThat(declarationsOn(day).get(0).get("lateDeclaration").asBoolean())
                .isTrue();
    }

    @Test
    void FR_060a_oneActiveDeclarationPerDay_andTheReasonIsRequired() throws Exception {
        LocalDate day = d0.plusDays(21);
        declare(day, "Lý do một").andExpect(status().isCreated());

        declare(day, "Lý do hai")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DAY_ALREADY_DECLARED"));
        declare(d0.plusDays(22), null).andExpect(status().is4xxClientError());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM day_care_closure_day WHERE `date` = ? AND status = 'ACTIVE'",
                        Integer.class,
                        day))
                .isEqualTo(1);
    }

    @Test
    void FR_060a_cancelling_thenDeclaringAgain_isAllowed_andACancelledDayCannotBeCancelledTwice() throws Exception {
        LocalDate day = d0.plusDays(23);
        declare(day, "Nghỉ lễ").andExpect(status().isCreated());
        long id = declarationsOn(day).get(0).get("id").asLong();

        user.set(office.user());
        mvc.perform(post("/api/v1/day-care-closure-days/" + id + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Mở lại\"}"))
                .andExpect(status().isOk());
        assertThat(declarationsOn(day).get(0).get("status").asString()).isEqualTo("CANCELLED");
        mvc.perform(post("/api/v1/day-care-closure-days/" + id + "/cancel")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Hủy lần hai\"}"))
                .andExpect(status().isConflict());

        declare(day, "Nghỉ lại").andExpect(status().isCreated());
        assertThat(events.stream(DayCareClosureDayChanged.class)
                        .filter(e -> e.date().equals(day)))
                .extracting(DayCareClosureDayChanged::active)
                .containsExactly(true, false, true);
    }

    @Test
    void FR_060a_listFiltersByRange_andOnlyOfficeStaffDeclare() throws Exception {
        LocalDate inside = d0.plusDays(30);
        declare(inside, "Trong khoảng").andExpect(status().isCreated());
        declare(d0.plusDays(60), "Ngoài khoảng").andExpect(status().isCreated());

        user.set(office.user());
        mvc.perform(get("/api/v1/day-care-closure-days")
                        .param("from", d0.plusDays(29).toString())
                        .param("to", d0.plusDays(31).toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].date").value(inside.toString()));

        user.set(users.staff(Role.NURSING_STAFF).user());
        mvc.perform(post("/api/v1/day-care-closure-days")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"date\":\"" + d0.plusDays(31) + "\",\"reason\":\"Không có quyền\"}"))
                .andExpect(status().isForbidden());
    }
}
