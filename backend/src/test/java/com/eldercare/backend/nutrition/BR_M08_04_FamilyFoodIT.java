package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.retention.api.RetentionKind;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.DataRef;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.nutrition.api.events.FamilyFoodNotice;
import com.eldercare.backend.nutrition.api.events.FamilyFoodNotice.Kind;
import com.eldercare.backend.nutrition.internal.familyfood.service.FamilyFoodService;
import com.eldercare.backend.nutrition.internal.familyfood.service.FamilyFoodService.Cause;
import com.eldercare.backend.nutrition.internal.schedule.service.M08JobService;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationshipsConfig;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.ResultActions;

/**
 * 011 T034 (US7, FR-052 → FR-057, BR-M08-04; quickstart Q42 → Q44): ghi nhận đồ ăn gia đình gán Không sử dụng / Cần xác nhận
 * / Được sử dụng theo đối chiếu; không có lệnh nào đưa Không sử dụng sang Được sử dụng; dị ứng mới và hạn dùng chuyển Được
 * sử dụng sang Không sử dụng; người gửi có tài khoản và quan hệ Hiệu lực nhận thông báo Nhẹ, lý do sức khỏe ở phần SỨC KHỎE.
 */
@Import(FakeFamilyRelationshipsConfig.class)
class BR_M08_04_FamilyFoodIT extends MenuTestBase {

    private static final String TYPE = "M08.FAMILY_FOOD";

    @Autowired
    FakeFamilyRelationships relatives;

    @Autowired
    FamilyFoodService familyFood;

    @Autowired
    M08JobService jobs;

    @Autowired
    TestClockControl clock;

    @Autowired
    List<RetentionProvider> retention;

    @AfterEach
    void resetClock() {
        clock.reset();
    }

    private String foodBody(String type, String ingredients, boolean unknown, Long sender, Instant expires) {
        return "{\"senderName\":\"Con gái\"," + (sender == null ? "" : "\"familyMemberId\":" + sender + ",")
                + "\"type\":\"" + type + "\",\"description\":\"Mô tả\",\"ingredients\":" + ingredients
                + ",\"ingredientsUnknown\":" + unknown + ",\"expiresAt\":\"" + expires + "\","
                + "\"storageMethod\":\"Tủ mát\"}";
    }

    private ResultActions record(long resident, String body) throws Exception {
        f.asDoctor();
        return f.send(post("/api/v1/residents/" + resident + "/family-foods"), body);
    }

    private long recorded(long resident, String body, String expectedStatus) throws Exception {
        String json = record(resident, body)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(expectedStatus))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    private ResultActions foodCommand(long food, String command, String body) throws Exception {
        f.asDoctor();
        return f.send(post("/api/v1/family-foods/" + food + "/" + command), body);
    }

    private String statusOf(long food) {
        return jdbc.queryForObject("SELECT status FROM simple_record WHERE id = ?", String.class, food);
    }

    private String contentOf(long food, String path) {
        return jdbc.queryForObject("SELECT content->>? FROM simple_record WHERE id = ?", String.class, path, food);
    }

    private List<FamilyFoodNotice> notices(Kind kind) {
        return events.stream(FamilyFoodNotice.class)
                .filter(n -> n.kind() == kind)
                .toList();
    }

    /** Gán chế độ ăn liên quan điều trị: đổi sang chế độ điều trị cần lý do chuyên môn (FR-013). */
    private void treat(long resident) throws Exception {
        f.propose(
                        resident,
                        NutritionCatalogFixture.assignmentBody(
                                f.treatmentDiet(List.of(), safe), "REGULAR", List.of(), time.now()))
                .andExpect(status().isCreated());
        f.apply(f.latestAssignment(resident), "Chuyển sang chế độ điều trị").andExpect(status().isOk());
    }

    private long relatedSender(long resident) {
        long sender = TestUsers.nextId();
        relatives.relate(sender, resident);
        return sender;
    }

    private void forbiddenType(String name) throws Exception {
        f.asDoctor();
        f.send(
                        post("/api/v1/nutrition/forbidden-food-types"),
                        "{\"name\":\"" + name + "\",\"description\":\"Chính sách viện\",\"status\":\"ACTIVE\"}")
                .andExpect(status().isCreated());
    }

    @Test
    void BR_M08_04_q42_allergenFood_isNotAllowed_senderWithRelationshipNotifiedInGeneralAndHealthParts()
            throws Exception {
        long peanut = f.allergen();
        long resident = f.resident("IN_RESIDENCE");
        allergy(resident, peanut);
        long sender = relatedSender(resident);
        Instant expires = time.now().plus(Duration.ofHours(6));

        long food = recorded(resident, foodBody("Bánh quy", "[" + peanut + "]", false, sender, expires), "NOT_ALLOWED");

        assertThat(contentOf(food, "$.systemReasons")).contains("ALLERGY");
        assertThat(notices(Kind.SENDER_NOT_ALLOWED)).singleElement().satisfies(n -> {
            assertThat(n.foodId()).isEqualTo(food);
            assertThat(n.senderFamilyMemberId()).isEqualTo(sender);
            NotificationRequest request = n.notificationRequests().getFirst();
            assertThat(request.level()).contains(NotificationLevel.LOW);
            assertThat(request.content())
                    .extracting(c -> c.type())
                    .containsExactly(InformationType.GENERAL, InformationType.HEALTH);
        });
    }

    @Test
    void BR_M08_04_q42_senderWithoutAccountOrRelationship_isNotNotified() throws Exception {
        long peanut = f.allergen();
        long resident = f.resident("IN_RESIDENCE");
        allergy(resident, peanut);
        Instant expires = time.now().plus(Duration.ofHours(6));

        recorded(resident, foodBody("Bánh quy", "[" + peanut + "]", false, null, expires), "NOT_ALLOWED");
        recorded(resident, foodBody("Bánh quy", "[" + peanut + "]", false, TestUsers.nextId(), expires), "NOT_ALLOWED");

        assertThat(notices(Kind.SENDER_NOT_ALLOWED)).isEmpty();
    }

    @Test
    void BR_M08_04_q42_forbiddenType_isNotAllowed_unknownIngredients_needsConfirmationThenApproved() throws Exception {
        forbiddenType("Đồ uống có cồn " + TestUsers.nextId());
        String forbidden = jdbc.queryForObject(
                "SELECT name FROM catalog_item WHERE catalog_type = 'M08.FORBIDDEN_FOOD_TYPE' ORDER BY id DESC LIMIT 1",
                String.class);
        long resident = f.resident("IN_RESIDENCE");
        long sender = relatedSender(resident);
        Instant expires = time.now().plus(Duration.ofHours(6));

        recorded(resident, foodBody(forbidden.toLowerCase(), "[]", false, sender, expires), "NOT_ALLOWED");
        long unknown = recorded(resident, foodBody("Hộp cơm", "[]", true, sender, expires), "NEEDS_CONFIRMATION");
        long apple = recorded(resident, foodBody("Trái cây", "[]", false, sender, expires), "ALLOWED");

        foodCommand(unknown, "approve-use", "{}").andExpect(status().isUnprocessableEntity());
        foodCommand(unknown, "approve-use", "{\"note\":\"Đã hỏi gia đình, cơm trắng và thịt\"}")
                .andExpect(status().isOk());

        assertThat(statusOf(unknown)).isEqualTo("ALLOWED");
        assertThat(contentOf(unknown, "$.confirmNote")).isEqualTo("Đã hỏi gia đình, cơm trắng và thịt");
        assertThat(contentOf(unknown, "$.confirmedBy")).isNotNull();
        assertThat(statusOf(apple)).isEqualTo("ALLOWED");
        f.asDoctor();
        f.send(get("/api/v1/residents/" + resident + "/family-foods"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
    }

    @Test
    void FR_054_residentSuspicions_otherAllergyTextureTreatmentDietUnrelatedSender() throws Exception {
        Instant expires = time.now().plus(Duration.ofHours(6));
        long otherAllergy = f.resident("IN_RESIDENCE");
        jdbc.update(
                "INSERT INTO health_item (resident_id, type, free_text, severity_id, source_id, recorded_by,"
                        + " recorded_at, pending_nutrition_check) VALUES (?, 'ALLERGY', 'Dị ứng lạ',"
                        + " (SELECT MIN(id) FROM allergy_severity), (SELECT MIN(id) FROM information_source), 1,"
                        + " NOW(6), FALSE)",
                otherAllergy);
        long soft = f.resident("IN_RESIDENCE");
        f.activeAssignment(soft, defaultDiet, "SOFT", List.of(), time.now());
        long treated = f.resident("IN_RESIDENCE");
        treat(treated);
        long plainResident = f.resident("IN_RESIDENCE");

        long a = recorded(
                otherAllergy,
                foodBody("Trái cây", "[]", false, relatedSender(otherAllergy), expires),
                "NEEDS_CONFIRMATION");
        long b = recorded(soft, foodBody("Táo", "[]", false, relatedSender(soft), expires), "NEEDS_CONFIRMATION");
        long c = recorded(
                treated,
                foodBody("Bánh mì nguyên cám", "[]", false, relatedSender(treated), expires),
                "NEEDS_CONFIRMATION");
        long d = recorded(plainResident, foodBody("Táo", "[]", false, null, expires), "NEEDS_CONFIRMATION");

        assertThat(contentOf(a, "$.systemReasons")).contains("RESIDENT_OTHER_ALLERGY");
        assertThat(contentOf(b, "$.systemReasons")).contains("TEXTURE_NOT_REGULAR");
        assertThat(contentOf(c, "$.systemReasons")).contains("TREATMENT_DIET");
        assertThat(contentOf(d, "$.systemReasons")).contains("SENDER_NOT_RELATED");
    }

    @Test
    void BR_M08_04_q44_noCommandMovesNotAllowedToAllowed_disposalEndsIt() throws Exception {
        long peanut = f.allergen();
        long resident = f.resident("IN_RESIDENCE");
        allergy(resident, peanut);
        long food = recorded(
                resident,
                foodBody("Bánh quy", "[" + peanut + "]", false, null, time.now().plus(Duration.ofHours(6))),
                "NOT_ALLOWED");

        foodCommand(food, "approve-use", "{\"note\":\"Cho dùng\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("NOT_ALLOWED_TO_USE"));
        foodCommand(food, "reject-use", "{\"reason\":\"Không cho\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        foodCommand(food, "end", "{\"method\":\"DISCARDED\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("STATE_CHANGED"));
        assertThat(statusOf(food)).isEqualTo("NOT_ALLOWED");

        foodCommand(food, "record-disposal", "{\"method\":\"CONSUMED\"}").andExpect(status().isUnprocessableEntity());
        foodCommand(food, "record-disposal", "{\"method\":\"RETURNED\"}").andExpect(status().isUnprocessableEntity());
        foodCommand(food, "record-disposal", "{\"method\":\"RETURNED\",\"returnedTo\":\"Con gái\"}")
                .andExpect(status().isOk());

        assertThat(statusOf(food)).isEqualTo("ENDED");
        assertThat(contentOf(food, "$.returnedTo")).isEqualTo("Con gái");
        foodCommand(food, "approve-use", "{\"note\":\"Cho dùng\"}").andExpect(status().isConflict());
        foodCommand(food, "record-disposal", "{\"method\":\"DISCARDED\"}").andExpect(status().isConflict());
    }

    @Test
    void BR_M08_04_rejectUseMovesNeedsConfirmationToNotAllowed_senderNotifiedHealthPartHoldsReason() throws Exception {
        long resident = f.resident("IN_RESIDENCE");
        long sender = relatedSender(resident);
        long food = recorded(
                resident,
                foodBody("Hộp cơm", "[]", true, sender, time.now().plus(Duration.ofHours(6))),
                "NEEDS_CONFIRMATION");

        foodCommand(food, "reject-use", "{}").andExpect(status().isUnprocessableEntity());
        foodCommand(food, "reject-use", "{\"reason\":\"Không rõ nguồn gốc, có thể nhiều muối\"}")
                .andExpect(status().isOk());

        assertThat(statusOf(food)).isEqualTo("NOT_ALLOWED");
        assertThat(contentOf(food, "$.rejectReason")).contains("Không rõ nguồn gốc");
        assertThat(notices(Kind.SENDER_NOT_ALLOWED)).singleElement().satisfies(n -> {
            NotificationRequest request = n.notificationRequests().getFirst();
            assertThat(request.content()).hasSize(2);
            assertThat(request.content().getFirst().content()).doesNotContain("nhiều muối");
            assertThat(request.content().getLast().type()).isEqualTo(InformationType.HEALTH);
            assertThat(request.content().getLast().content()).contains("nhiều muối");
        });
    }

    @Test
    void BR_M08_04_endOfAllowedFood_recordsMethod() throws Exception {
        long resident = f.resident("IN_RESIDENCE");
        long food = recorded(
                resident,
                foodBody(
                        "Trái cây",
                        "[]",
                        false,
                        relatedSender(resident),
                        time.now().plus(Duration.ofHours(6))),
                "ALLOWED");

        foodCommand(food, "end", "{}").andExpect(status().isUnprocessableEntity());
        foodCommand(food, "end", "{\"method\":\"CONSUMED\"}").andExpect(status().isOk());

        assertThat(statusOf(food)).isEqualTo("ENDED");
        assertThat(contentOf(food, "$.endMethod")).isEqualTo("CONSUMED");
    }

    @Test
    void FR_056_q43_newAllergy_movesAllowedToNotAllowed_nurseAndSenderNotifiedOnce() throws Exception {
        long peanut = f.allergen();
        long resident = f.resident("IN_RESIDENCE");
        long sender = relatedSender(resident);
        long food = recorded(
                resident,
                foodBody(
                        "Bánh quy",
                        "[" + peanut + "]",
                        false,
                        sender,
                        time.now().plus(Duration.ofHours(6))),
                "ALLOWED");
        long pending = recorded(
                resident,
                foodBody("Hộp cơm", "[]", true, sender, time.now().plus(Duration.ofHours(6))),
                "NEEDS_CONFIRMATION");

        allergy(resident, peanut);
        familyFood.recheck(resident, Cause.ALLERGY);
        familyFood.recheck(resident, Cause.ALLERGY);

        assertThat(statusOf(food)).isEqualTo("NOT_ALLOWED");
        assertThat(contentOf(food, "$.systemReasons")).contains("ALLERGY");
        assertThat(contentOf(food, "$.movedBecause")).isEqualTo("NEW_CONFLICT");
        assertThat(statusOf(pending)).isEqualTo("NEEDS_CONFIRMATION");
        assertThat(notices(Kind.NURSE_CONFLICT)).singleElement().satisfies(n -> {
            assertThat(n.residentId()).isEqualTo(resident);
            assertThat(n.notificationRequests().getFirst().level()).contains(NotificationLevel.MEDIUM);
        });
        assertThat(notices(Kind.SENDER_NOT_ALLOWED))
                .extracting(FamilyFoodNotice::foodId)
                .contains(food);
    }

    @Test
    void FR_055_expiredAllowedFood_movesToNotAllowedByMinuteJob_nurseNotifiedOnce() throws Exception {
        long resident = f.resident("IN_RESIDENCE");
        Instant expires = time.now().plus(Duration.ofHours(1));
        long food = recorded(resident, foodBody("Súp gà", "[]", false, relatedSender(resident), expires), "ALLOWED");

        jobs.runMinuteTasks(expires.minusSeconds(60));
        assertThat(statusOf(food)).isEqualTo("ALLOWED");

        jobs.runMinuteTasks(expires.plusSeconds(60));
        jobs.runMinuteTasks(expires.plusSeconds(120));

        assertThat(statusOf(food)).isEqualTo("NOT_ALLOWED");
        assertThat(contentOf(food, "$.systemReasons")).contains("EXPIRED");
        assertThat(contentOf(food, "$.movedBecause")).isEqualTo("EXPIRED");
        assertThat(notices(Kind.NURSE_EXPIRED))
                .filteredOn(n -> n.foodId() == food)
                .singleElement()
                .satisfies(n ->
                        assertThat(n.notificationRequests().getFirst().level()).contains(NotificationLevel.LOW));
    }

    @Test
    void FR_063_kitchenAndOutsiderRolesCannotUseFamilyFoodOrForbiddenTypes() throws Exception {
        long resident = f.resident("IN_RESIDENCE");
        f.asKitchen();
        f.send(post("/api/v1/residents/" + resident + "/family-foods"), foodBody("Táo", "[]", false, null, time.now()))
                .andExpect(status().isForbidden());
        f.send(get("/api/v1/residents/" + resident + "/family-foods"), null).andExpect(status().isForbidden());
        f.send(post("/api/v1/nutrition/forbidden-food-types"), "{\"name\":\"Rượu\",\"status\":\"ACTIVE\"}")
                .andExpect(status().isForbidden());
    }

    @Test
    void FR_007_forbiddenTypeCanBeListedAndStopped() throws Exception {
        String name = "Thực phẩm sống " + TestUsers.nextId();
        forbiddenType(name);

        f.asDoctor();
        f.send(get("/api/v1/nutrition/forbidden-food-types"), null)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + name + "')].status").value("ACTIVE"));

        f.send(post("/api/v1/nutrition/forbidden-food-types"), "{\"name\":\"" + name + "\",\"status\":\"INACTIVE\"}")
                .andExpect(status().isCreated());
        f.send(get("/api/v1/nutrition/forbidden-food-types"), null)
                .andExpect(jsonPath("$[?(@.name == '" + name + "')].status").value("INACTIVE"));

        long resident = f.resident("IN_RESIDENCE");
        recorded(
                resident,
                foodBody(name, "[]", false, relatedSender(resident), time.now().plus(Duration.ofHours(6))),
                "ALLOWED");
    }

    @Test
    void FR_051a_familyFoodKeptByResidentRecord_candidateOnlyAfterFinalStatus() throws Exception {
        long resident = f.resident("IN_RESIDENCE");
        long food = recorded(
                resident,
                foodBody("Táo", "[]", false, relatedSender(resident), time.now().plus(Duration.ofHours(6))),
                "ALLOWED");
        RetentionProvider provider = retention.stream()
                .filter(p -> TYPE.equals(p.dataType()))
                .findFirst()
                .orElseThrow();

        assertThat(provider.retentionKind()).isInstanceOf(RetentionKind.ResidentRecord.class);
        assertThat(provider.referencing(new DataRef("M01.RESIDENT", resident)))
                .containsExactly(new DataRef(TYPE, food));

        Instant cutoff = time.now().plus(Duration.ofDays(3650));
        assertThat(candidates(provider, cutoff)).doesNotContain(food);
        jdbc.update("UPDATE resident SET status = 'DECEASED', status_since = NOW(6) WHERE id = ?", resident);
        entityManager.clear();
        assertThat(candidates(provider, cutoff)).contains(food);
    }

    @Autowired
    jakarta.persistence.EntityManager entityManager;

    private static List<Long> candidates(RetentionProvider provider, Instant cutoff) {
        List<Long> ids = new ArrayList<>();
        Candidate after = null;
        while (true) {
            List<Candidate> page =
                    provider.expiredCandidates(cutoff, after, 500).toList();
            if (page.isEmpty()) {
                return ids;
            }
            page.forEach(c -> ids.add(c.id()));
            after = page.getLast();
        }
    }
}
