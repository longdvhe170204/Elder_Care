package com.eldercare.backend.nutrition;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.MySqlIntegrationTestBase;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.SwitchableUserConfig;
import com.eldercare.backend.support.TestUsers;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/**
 * Nền chung cho IT thực đơn tuần của 011 Phase 5 (một context cho cả bốn lớp). Mỗi test chạy trong transaction và tự cô
 * lập khỏi dữ liệu sót trong schema test: các bữa, bản gán đang dùng của test khác bị tắt trong transaction (rollback cuối
 * test), chế độ ăn mặc định mới thay mặc định cũ trong cùng lệnh, tuần thực đơn chọn ngẫu nhiên ở tương lai.
 */
@AutoConfigureMockMvc
@WithMockUser
@Transactional
@RecordApplicationEvents
@Import(SwitchableUserConfig.class)
abstract class MenuTestBase extends MySqlIntegrationTestBase {

    static final String MENUS = "/api/v1/menus";

    @Autowired
    MockMvc mvc;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    BusinessTime time;

    @Autowired
    SwitchableUser user;

    @Autowired
    ApplicationEvents events;

    NutritionCatalogFixture f;
    long breakfast;
    long lunch;
    long dinner;
    /** Chế độ ăn mặc định của viện trong test; món an toàn {@link #safe}. */
    long defaultDiet;

    long safe;
    /** Món thường của chế độ mặc định, không thành phần. */
    long plain;

    @BeforeEach
    void setUpMenus() throws Exception {
        f = new NutritionCatalogFixture(mvc, jdbc, time, user);
        isolate();
        breakfast = meal("Sáng", true, false);
        lunch = meal("Trưa", true, true);
        dinner = meal("Tối", true, false);
        long base = f.baseDiet();
        safe = f.activeDish(List.of(), List.of(base));
        defaultDiet = f.diet(true, List.of(), safe);
        plain = f.activeDish(List.of(), List.of(defaultDiet));
    }

    /** Tắt bữa, bản gán của dữ liệu sót trong transaction của test (rollback cuối test). */
    void isolate() {
        jdbc.update(
                "UPDATE catalog_item SET status = 'INACTIVE' WHERE catalog_type = 'M08.MEAL' AND status = 'ACTIVE'");
        jdbc.update(
                "UPDATE diet_assignment SET status = 'DISCONTINUED', discontinued_at = NOW(6) WHERE status = 'ACTIVE'");
        jdbc.update("UPDATE diet_assignment SET status = 'CANCELLED', cancel_reason = 'test', cancelled_at = NOW(6)"
                + " WHERE status IN ('PROPOSED', 'PENDING_EFFECTIVE')");
    }

    /** Bữa Hiệu lực trong danh mục (lệnh danh mục bữa chờ Function của A, team-plan mục 8): ghi thẳng bảng chung. */
    long meal(String name, boolean residential, boolean dayCare) {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO catalog_item(id,module,catalog_type,code,name,status,sort_order,attributes,version)"
                        + " VALUES(?,'M08','M08.MEAL',?,?,'ACTIVE',?,CAST(? AS JSON),0)",
                id,
                "M08.MEAL." + id,
                name + " " + id,
                (int) (id % 1000),
                "{\"servesResidential\":" + residential + ",\"servesDayCare\":" + dayCare + "}");
        return id;
    }

    /** Thứ Hai của một tuần tương lai chưa ai dùng. */
    LocalDate futureWeek() {
        LocalDate next = time.today().with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        return next.plusWeeks(20 + TestUsers.nextId() % 2000);
    }

    LocalDate currentWeek() {
        return time.today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    ResultActions send(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request, String body)
            throws Exception {
        return f.send(request, body);
    }

    /** Bếp tạo thực đơn Nháp, trả id (header {@code Location}). */
    long createMenu(LocalDate weekStart) throws Exception {
        f.asKitchen();
        MvcResult result = send(post(MENUS), "{\"weekStart\":\"" + weekStart + "\"}")
                .andExpect(status().isCreated())
                .andReturn();
        String location = result.getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    String etag(long menu) throws Exception {
        return mvc.perform(get(MENUS + "/" + menu)).andReturn().getResponse().getHeader("ETag");
    }

    ResultActions putContent(long menu, List<String> rows) throws Exception {
        f.asKitchen();
        String etag = etag(menu);
        return mvc.perform(put(MENUS + "/" + menu)
                .header("Idempotency-Key", "m08-" + TestUsers.nextId())
                .header("If-Match", etag)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"dishes\":" + rows.stream().collect(Collectors.joining(",", "[", "]")) + "}"));
    }

    static String row(LocalDate date, long meal, long diet, long dish, Long... substitutes) {
        return "{\"date\":\"" + date + "\",\"mealId\":" + meal + ",\"dietId\":" + diet + ",\"dishId\":" + dish
                + ",\"substituteDishIds\":" + NutritionCatalogFixture.ids(List.of(substitutes)) + "}";
    }

    /** Mọi bữa của mọi ngày trong tuần với một món. */
    List<String> fullWeek(LocalDate weekStart, long diet, long dish) {
        List<String> rows = new ArrayList<>();
        for (int day = 0; day < 7; day++) {
            for (long meal : List.of(breakfast, lunch, dinner)) {
                rows.add(row(weekStart.plusDays(day), meal, diet, dish));
            }
        }
        return rows;
    }

    ResultActions command(long menu, String command, String body) throws Exception {
        return send(post(MENUS + "/" + menu + "/" + command), body);
    }

    ResultActions submit(long menu, List<String> confirmedWarnings) throws Exception {
        f.asKitchen();
        return command(
                menu,
                "submit",
                "{\"processingCheckConfirmed\":true,\"processingNote\":\"Đủ nguyên liệu\",\"confirmedWarnings\":"
                        + strings(confirmedWarnings) + "}");
    }

    /** Mã cảnh báo hiện có của thực đơn (lặp món, dị ứng), để xác nhận khi test không xét cảnh báo. */
    List<String> codes(long menu) throws Exception {
        f.asKitchen();
        String body = send(post(MENUS + "/" + menu + "/check"), "{}")
                .andReturn()
                .getResponse()
                .getContentAsString();
        return com.jayway.jsonpath.JsonPath.read(body, "$.warningCodes");
    }

    ResultActions publish(long menu, List<String> confirmedWarnings) throws Exception {
        f.asDoctor();
        return command(menu, "publish", "{\"confirmedWarnings\":" + strings(confirmedWarnings) + "}");
    }

    /** Thực đơn Công bố đủ độ phủ cho chế độ mặc định với món {@code dish}, trả id. */
    long publishedMenu(LocalDate weekStart, long dish) throws Exception {
        long menu = createMenu(weekStart);
        putContent(menu, fullWeek(weekStart, defaultDiet, dish)).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        publish(menu, List.of()).andExpect(status().isOk());
        return menu;
    }

    String menuStatus(long menu) {
        return jdbc.queryForObject("SELECT status FROM menu WHERE id = ?", String.class, menu);
    }

    /** Mục dị ứng Hiệu lực chọn từ danh mục cho người cao tuổi (bảng {@code health_item} của 001). */
    void allergy(long resident, long allergen) {
        jdbc.update(
                "INSERT INTO health_item (resident_id, type, allergen_id, severity_id, source_id, recorded_by,"
                        + " recorded_at, pending_nutrition_check) VALUES (?, 'ALLERGY', ?,"
                        + " (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.ALLERGY_SEVERITY'), (SELECT MIN(id) FROM catalog_item WHERE catalog_type = 'M01.INFORMATION_SOURCE'), 1,"
                        + " NOW(6), FALSE)",
                resident,
                allergen);
    }

    String residentName(long resident) {
        return jdbc.queryForObject("SELECT full_name FROM resident WHERE id = ?", String.class, resident);
    }

    static String strings(List<String> values) {
        return values.stream().map(v -> "\"" + v + "\"").collect(Collectors.joining(",", "[", "]"));
    }
}
