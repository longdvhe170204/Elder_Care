package com.eldercare.backend.nutrition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider.Candidate;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.nutrition.api.events.FamilyFoodNotice;
import com.eldercare.backend.nutrition.api.events.FamilyFoodNotice.Kind;
import com.eldercare.backend.nutrition.internal.familyfood.service.FamilyFoodService;
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
        f.asManager();
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
