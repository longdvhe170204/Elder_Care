package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.nutrition.api.events.RetainedSampleNotice;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 011 T038 (US9, FR-060 → FR-062, BR-M08-15, SC-013; quickstart Q46, Q47; tên {@code BR_M08_15_LuuMauIT} trong tasks):
 * danh sách món cần lưu mẫu gồm món thay thế và món an toàn; giao phiếu bị chặn khi bữa chưa có bản ghi lưu mẫu; món không
 * lưu cần lý do và báo Bác sĩ, Quản lý viện; ghi thêm món sau phát sinh; quá CFG-M08-06 nhắc bếp một lần; hủy mẫu đặt một
 * lần; người ngoài không có quyền bị từ chối.
 */
class BR_M08_15_RetainedSampleIT extends SlipTestBase {

    static final String SAMPLES = "/api/v1/daily-meals/";

    long shrimp;
    long nut;
    long shrimpSoup;
    long nutDish;
    long substitute;

    /**
     * Thực đơn bữa sáng của ngày {@link #day}: canh tôm có món thay thế, món hạt không có món thay thế; các bữa khác món
     * {@link #plain}.
     */
    void allergyMenu() throws Exception {
        shrimp = f.allergen();
        nut = f.allergen();
        shrimpSoup = f.activeDish(List.of(shrimp), List.of(defaultDiet));
        nutDish = f.activeDish(List.of(nut), List.of(defaultDiet));
        substitute = f.activeDish(List.of(), List.of(defaultDiet));
        LocalDate week = day.minusDays(2);
        long menu = createMenu(week);
        List<String> rows = new ArrayList<>(fullWeek(week, defaultDiet, plain));
        rows.removeIf(r -> r.contains("\"date\":\"" + day + "\"") && r.contains("\"mealId\":" + breakfast + ","));
        rows.add(row(day, breakfast, defaultDiet, shrimpSoup, substitute));
        rows.add(row(day, breakfast, defaultDiet, nutDish));
        putContent(menu, rows).andExpect(status().isOk());
        submit(menu, codes(menu)).andExpect(status().isOk());
        publish(menu, List.of()).andExpect(status().isOk());
    }

    ResultActions sampleView(long dailyMeal) throws Exception {
        f.asKitchen();
        return send(get(SAMPLES + dailyMeal + "/retained-samples"), null);
    }

    ResultActions recordSample(long dailyMeal, String body) throws Exception {
        return send(post(SAMPLES + dailyMeal + "/retained-samples"), body);
    }

    ResultActions discardSample(long sample) throws Exception {
        return send(post("/api/v1/retained-samples/" + sample + "/discard"), null);
    }

    long sampleIdOf(long dailyMeal) {
        return jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M08.RETAINED_SAMPLE' AND unique_key = ?",
                Long.class,
                "M08.RETAINED_SAMPLE:" + dailyMeal);
    }

    static String dish(long id, boolean retained, String reason) {
        return "{\"dishId\":" + id + ",\"retained\":" + retained
                + (reason == null ? "" : ",\"notRetainedReason\":\"" + reason + "\"") + "}";
    }

    static String sampleBody(String location, String... dishes) {
        return "{" + (location == null ? "" : "\"storageLocation\":\"" + location + "\",") + "\"dishes\":["
                + String.join(",", dishes) + "]}";
    }

    @Test
    void BR_M08_15_listIncludesSubstituteAndSafeDish_deliverBlockedUntilSampleRecorded() throws Exception {
        allergyMenu();
        long a = residentIn(room2);
        allergy(a, shrimp);
        long b = residentIn(room2);
        allergy(b, nut);
        residentIn(room2);
        clock.setTo(at(LocalTime.of(4, 30)));
        close(dailyMeal(breakfast)).andExpect(status().isCreated());
        long slip = slipId(dailyMeal(breakfast), floor2);

        sampleView(dailyMeal(breakfast))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dishes[*].dishId")
                        .value(containsInAnyOrder(num(shrimpSoup), num(nutDish), num(substitute), num(safe))))
                .andExpect(jsonPath("$.dishes[*].recorded").value(containsInAnyOrder(false, false, false, false)));

        f.asKitchen();
        slipCommand(slip, "deliver", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code").value(hasItem("SAMPLE_NOT_RETAINED")));
        for (long r : List.of(a, b)) {
            send(post("/api/v1/special-portions/" + specialOf(r) + "/mark-prepared"), "{}")
                    .andExpect(status().isOk());
        }
        slipCommand(slip, "mark-prepared", "{}").andExpect(status().isOk());
        slipCommand(slip, "deliver", "{}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.unmetConditions[*].code").value(containsInAnyOrder("SAMPLE_NOT_RETAINED")));

        f.asDoctor();
        recordSample(
                        dailyMeal(breakfast),
                        sampleBody(
                                "Tủ lạnh bếp",
                                dish(shrimpSoup, true, null),
                                dish(nutDish, true, null),
                                dish(substitute, true, null),
                                dish(safe, true, null)))
                .andExpect(status().isForbidden());

        f.asKitchen();
        recordSample(dailyMeal(breakfast), sampleBody("Tủ lạnh bếp", dish(shrimpSoup, true, null)))
                .andExpect(status().isUnprocessableEntity());
        recordSample(
                        dailyMeal(breakfast),
                        sampleBody(
                                "Tủ lạnh bếp",
                                dish(shrimpSoup, true, null),
                                dish(nutDish, true, null),
                                dish(substitute, false, null),
                                dish(safe, true, null)))
                .andExpect(status().isUnprocessableEntity());
        assertThat(events.stream(RetainedSampleNotice.class)).isEmpty();

        recordSample(
                        dailyMeal(breakfast),
                        sampleBody(
                                "Tủ lạnh bếp",
                                dish(shrimpSoup, true, null),
                                dish(nutDish, true, null),
                                dish(substitute, false, "hết hộp lưu mẫu"),
                                dish(safe, true, null)))
                .andExpect(status().isCreated());
        recordSample(dailyMeal(breakfast), sampleBody(null, dish(plain, true, null)))
                .andExpect(status().isCreated());

        slipCommand(slip, "deliver", "{}").andExpect(status().isOk());
        assertThat(slipStatus(slip)).isEqualTo("DELIVERED");

        sampleView(dailyMeal(breakfast))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sample.storageLocation").value("Tủ lạnh bếp"))
                .andExpect(jsonPath("$.dishes[?(@.dishId == " + num(substitute) + ")].retained")
                        .value(hasItem(false)))
                .andExpect(jsonPath("$.dishes[?(@.dishId == " + num(substitute) + ")].notRetainedReason")
                        .value(hasItem("hết hộp lưu mẫu")))
                .andExpect(jsonPath("$.dishes[?(@.dishId == " + num(plain) + ")].addedLater")
                        .value(hasItem(true)));
        assertThat(events.stream(RetainedSampleNotice.class)).singleElement().satisfies(n -> {
            var request = n.notificationRequests().getFirst();
            assertThat(request.level()).contains(NotificationLevel.LOW);
            assertThat(request.recipientGroups())
                    .contains(
                            new RecipientGroup.FacilityRole(Role.DOCTOR),
                            new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER));
        });
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind = 'M08.RETAINED_SAMPLE' AND unique_key = ?",
                        Integer.class,
                        "M08.RETAINED_SAMPLE:" + dailyMeal(breakfast)))
                .isEqualTo(1);
    }

    List<RetainedSampleNotice> reminders(long sample) {
        return events.stream(RetainedSampleNotice.class)
                .filter(n -> n.sampleId() == sample && n.kind() == RetainedSampleNotice.Kind.DISCARD_DUE)
                .toList();
    }
}
