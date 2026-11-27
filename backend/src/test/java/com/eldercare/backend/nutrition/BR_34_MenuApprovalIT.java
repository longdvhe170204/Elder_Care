package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.nutrition.api.events.MenuPublished;
import com.eldercare.backend.nutrition.api.events.MenuReturned;
import com.eldercare.backend.nutrition.api.events.MenuSubmitted;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 011 T050 (US3, FR-023 → FR-030, Q-445; quickstart Q3, Q4): thực đơn chung; gửi duyệt không kiểm tra độ phủ; Quản lý viện
 * trả lại (lý do) hoặc công bố; bếp, Bác sĩ không công bố; món không Hiệu lực bị chặn.
 */
class BR_34_MenuApprovalIT extends MenuTestBase {

    @Test
    void FR_025_missingDinner_isNotBlocked_andManagerIsNotified() throws Exception {
        LocalDate week = futureWeek();
        long menu = createMenu(week);
        List<String> rows = new ArrayList<>();
        for (String row : fullWeek(week, defaultDiet, plain)) {
            if (!(row.contains(week.plusDays(4).toString()) && row.contains("\"mealId\":" + dinner + ","))) {
                rows.add(row);
            }
        }
        putContent(menu, rows).andExpect(status().isOk());

        submit(menu, List.of()).andExpect(status().isOk());

        assertThat(menuStatus(menu)).isEqualTo("PENDING_PUBLICATION");
        assertThat(events.stream(MenuSubmitted.class).filter(e -> e.menuId() == menu))
                .hasSize(1);
    }

    @Test
    void FR_029_onlyManagerPublishes_returnNeedsReason() throws Exception {
        LocalDate week = futureWeek();
        long menu = createMenu(week);
        putContent(menu, fullWeek(week, defaultDiet, plain)).andExpect(status().isOk());
        submit(menu, List.of()).andExpect(status().isOk());

        f.asKitchen();
        command(menu, "publish", "{}").andExpect(status().isForbidden());
        f.asDoctor();
        command(menu, "publish", "{}").andExpect(status().isForbidden());

        f.asManager();
        command(menu, "return", "{}").andExpect(status().is4xxClientError());
        command(menu, "return", "{\"reason\":\"Bữa tối nhiều đồ chiên\"}").andExpect(status().isOk());
        assertThat(menuStatus(menu)).isEqualTo("DRAFT");
        assertThat(events.stream(MenuReturned.class).filter(e -> e.menuId() == menu))
                .singleElement()
                .satisfies(e -> assertThat(e.reason()).isEqualTo("Bữa tối nhiều đồ chiên"));

        submit(menu, List.of()).andExpect(status().isOk());
        publish(menu, List.of()).andExpect(status().isOk());
        assertThat(menuStatus(menu)).isEqualTo("PUBLISHED");
        assertThat(events.stream(MenuPublished.class).filter(e -> e.menuId() == menu))
                .hasSize(1);

        f.asManager();
        mvc.perform(get(MENUS).param("status", "PUBLISHED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].id", hasItem((int) menu)));
    }

    @Test
    void FR_025_draftDish_blocksSubmit() throws Exception {
        LocalDate week = futureWeek();
        long draft = f.draftDish(List.of(), List.of());
        long menu = createMenu(week);
        putContent(menu, List.of(row(week, lunch, defaultDiet, draft))).andExpect(status().isConflict());
        putContent(menu, List.of(row(week, lunch, defaultDiet, plain))).andExpect(status().isOk());

        submit(menu, List.of()).andExpect(status().isOk());
        assertThat(menuStatus(menu)).isEqualTo("PENDING_PUBLICATION");
    }

    @Test
    void FR_030_publishedInStartedWeek_isApplied() throws Exception {
        LocalDate week = currentWeek();
        jdbc.update("UPDATE menu SET status = 'CANCELLED', cancel_reason = 'test' WHERE week_start = ?", week);
        long menu = createMenu(week);
        putContent(menu, List.of(row(time.today(), lunch, defaultDiet, plain))).andExpect(status().isOk());
        submit(menu, List.of()).andExpect(status().isOk());
        publish(menu, List.of()).andExpect(status().isOk());

        assertThat(menuStatus(menu)).isEqualTo(week.isAfter(time.today()) ? "PUBLISHED" : "APPLIED");
        f.asKitchen();
        send(post(MENUS), "{\"weekStart\":\"" + week + "\"}").andExpect(status().isConflict());
    }
}
