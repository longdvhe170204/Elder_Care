package com.eldercare.backend.nutrition;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.support.SwitchableUser;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/** Dựng dữ liệu danh mục 011 qua HTTP thật (quyền thật theo Phụ lục 27) cho test Phase 3. */
final class NutritionCatalogFixture {

    static final String BASE = "/api/v1/nutrition";

    private final MockMvc mvc;
    private final JdbcTemplate jdbc;
    private final SwitchableUser user;
    private final TestUsers users;
    final TestUsers.TestUser kitchen;
    final TestUsers.TestUser doctor;

    NutritionCatalogFixture(MockMvc mvc, JdbcTemplate jdbc, BusinessTime time, SwitchableUser user) {
        this.mvc = mvc;
        this.jdbc = jdbc;
        this.user = user;
        this.users = new TestUsers(jdbc, time);
        this.kitchen = users.staff(Role.KITCHEN_STAFF);
        this.doctor = users.staff(Role.DOCTOR);
    }

    void asKitchen() {
        user.set(kitchen.user());
    }

    void asDoctor() {
        user.set(doctor.user());
    }

    ResultActions send(MockHttpServletRequestBuilder request, String body) throws Exception {
        return mvc.perform(request.header("Idempotency-Key", "m08-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body == null ? "{}" : body));
    }

    /** Một dị nguyên nhóm thực phẩm mới (bảng {@code allergen} của 001, V001_03). */
    long allergen() {
        long id = TestUsers.nextId();
        jdbc.update(
                "INSERT INTO allergen(id,name,allergen_group,status) VALUES(?,?,'FOOD','ACTIVE')",
                id,
                "Thành phần " + id);
        return id;
    }

    static String dishBody(String name, List<Long> ingredients, boolean noCatalog, List<Long> diets) {
        return "{\"name\":\"" + name + "\",\"note\":\"\",\"ingredients\":" + ids(ingredients)
                + ",\"noCatalogIngredient\":" + noCatalog + ",\"suitableDietIds\":" + ids(diets)
                + ",\"texture\":\"REGULAR\"}";
    }

    static String ids(List<Long> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    }

    long dishId(String name) {
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M08.DISH' AND name=?", Long.class, name);
    }

    long dietId(String name) {
        return jdbc.queryForObject(
                "SELECT id FROM catalog_item WHERE catalog_type='M08.DIET' AND name=?", Long.class, name);
    }

    String dishStatus(long id) {
        return jdbc.queryForObject("SELECT attributes->>'$.status' FROM catalog_item WHERE id=?", String.class, id);
    }

    /** Món Nháp do bếp tạo, trả id. */
    long draftDish(List<Long> ingredients, List<Long> diets) throws Exception {
        asKitchen();
        String name = "Món " + TestUsers.nextId();
        send(post(BASE + "/dishes"), dishBody(name, ingredients, ingredients.isEmpty(), diets))
                .andExpect(status().isCreated());
        return dishId(name);
    }

    /** Món Hiệu lực: bếp tạo, gửi, Bác sĩ xác nhận. */
    long activeDish(List<Long> ingredients, List<Long> diets) throws Exception {
        long id = draftDish(ingredients, diets);
        send(post(BASE + "/dishes/" + id + "/submit"), "{}").andExpect(status().isOk());
        asDoctor();
        send(post(BASE + "/dishes/" + id + "/confirm"), "{}").andExpect(status().isOk());
        return id;
    }

    static String dietBody(String name, boolean treatment, boolean isDefault, List<Long> restrictions, long safeDish) {
        return "{\"name\":\"" + name + "\",\"description\":\"\",\"treatmentRelated\":" + treatment + ",\"isDefault\":"
                + isDefault + ",\"restrictions\":" + ids(restrictions) + ",\"safeDishId\":" + safeDish + "}";
    }

    /** Chế độ ăn Hiệu lực do Bác sĩ tạo, trả id. */
    long diet(boolean isDefault, List<Long> restrictions, long safeDish) throws Exception {
        asDoctor();
        String name = "Chế độ " + TestUsers.nextId();
        send(post(BASE + "/diets"), dietBody(name, false, isDefault, restrictions, safeDish))
                .andExpect(status().isCreated());
        return dietId(name);
    }

    /**
     * Chế độ ăn nền không hạn chế cho món tham chiếu: món an toàn là một món Hiệu lực bất kỳ; chưa có món Hiệu lực nào
     * thì để trống (ngoại lệ chế độ ăn đầu tiên, chủ dự án duyệt 2026-11-06).
     */
    long baseDiet() throws Exception {
        List<Long> active = jdbc.queryForList(
                "SELECT id FROM catalog_item WHERE catalog_type='M08.DISH' AND attributes->>'$.status'='ACTIVE' LIMIT 1",
                Long.class);
        asDoctor();
        String name = "Chế độ nền " + TestUsers.nextId();
        String safe = active.isEmpty() ? "null" : String.valueOf(active.getFirst());
        send(
                        post(BASE + "/diets"),
                        "{\"name\":\"" + name
                                + "\",\"description\":\"\",\"treatmentRelated\":false,\"isDefault\":false,"
                                + "\"restrictions\":[],\"safeDishId\":" + safe + "}")
                .andExpect(status().isCreated());
        return dietId(name);
    }

    int activeDefaultDiets() {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM catalog_item WHERE catalog_type='M08.DIET' AND status='ACTIVE'"
                        + " AND attributes->>'$.isDefault'='true'",
                Integer.class);
    }
}
