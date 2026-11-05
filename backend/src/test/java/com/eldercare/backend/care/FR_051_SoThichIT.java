package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.eldercare.backend.administration.account.api.spi.AssignmentKind;
import com.eldercare.backend.administration.account.api.spi.AssignmentView;
import com.eldercare.backend.administration.account.api.spi.BedAllocationView;
import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.care.internal.activity.domain.ActivityTypeAttributes;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.resident.StatusTestBase;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.administration.FakeFamilyRelationships;
import com.eldercare.backend.support.fake.care.FakeM04Config;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 014 T021 (FR-051, FR-052; Q-363): sở thích theo nhóm và nguồn, mục hiện hành thứ hai cùng nhóm và nguồn cần xác nhận
 * thay, bỏ cần lý do; đăng ký hoạt động có nhóm "không thích" hiện cảnh báo nêu nguồn. GET sở thích chờ schema phản hồi.
 */
@AutoConfigureMockMvc
@WithMockUser
@Import(FakeM04Config.class)
class FR_051_SoThichIT extends StatusTestBase {
    @Autowired
    MockMvc mvc;

    @Autowired
    CatalogApi catalogs;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    FakeFamilyRelationships family;

    long floor;
    long music;
    long chess;
    TestUsers.TestUser manager;

    @BeforeEach
    void fixture() {
        floor = TestUsers.nextId();
        manager = new TestUsers(jdbc, time).staff(Role.FLOOR_MANAGER);
        assignments.assign(
                manager.staffId(),
                new AssignmentView(
                        AssignmentKind.FLOOR_MANAGER,
                        floor,
                        null,
                        null,
                        null,
                        time.now().minusSeconds(86400),
                        null));
        user.set(manager.user());
        music = group("Âm nhạc");
        chess = group("Cờ tướng");
    }

    long group(String name) {
        return tx.execute(s -> catalogs.create(
                        ActivityKinds.INTEREST_GROUPS,
                        new NewCatalogItem<Void>(null, name + " " + TestUsers.nextId(), null, 0, null))
                .id());
    }

    long resident() {
        long r = residentIn(ResidentStatus.IN_RESIDENCE);
        bedAllocations.allocate(
                r, new BedAllocationView(TestUsers.nextId(), TestUsers.nextId(), TestUsers.nextId(), floor));
        user.set(manager.user());
        return r;
    }

    MockHttpServletRequestBuilder write(MockHttpServletRequestBuilder request, String body) {
        return request.header("Idempotency-Key", "pref-" + TestUsers.nextId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body);
    }

    ResultActions prefer(long resident, long group, String level, String source, boolean confirm) throws Exception {
        return mvc.perform(write(
                post("/api/v1/residents/" + resident + "/preferences"),
                "{\"interestGroupId\":" + group + ",\"description\":\"Nghe nhạc trữ tình\",\"level\":\"" + level
                        + "\",\"source\":" + source + ",\"confirmReplacement\":" + confirm + "}"));
    }

    long id(ResultActions result) throws Exception {
        String location =
                result.andExpect(status().isCreated()).andReturn().getResponse().getHeader("Location");
        return Long.parseLong(location.substring(location.lastIndexOf('/') + 1));
    }

    String recordStatus(long id) {
        return jdbc.queryForObject(
                "SELECT status FROM simple_record WHERE kind='M04.PREFERENCE' AND id=?", String.class, id);
    }

    static final String RESIDENT = "{\"type\":\"RESIDENT\"}";

    @Test
    void FR_051_recordingKeepsRecorderAndTimeAndASecondEntryForTheSameGroupAndSourceNeedsConfirmation()
            throws Exception {
        long a = resident();
        long first = id(prefer(a, music, "LIKE", RESIDENT, false));
        assertThat(recordStatus(first)).isEqualTo("CURRENT");
        assertThat(jdbc.queryForObject("SELECT created_by FROM simple_record WHERE id=?", Long.class, first))
                .isEqualTo(manager.accountId());
        id(prefer(a, chess, "LIKE", RESIDENT, false));
        prefer(a, music, "DISLIKE", RESIDENT, false)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("PREFERENCE_REPLACEMENT_CONFIRMATION_REQUIRED"))
                .andExpect(jsonPath("$.unmetConditions[0].description")
                        .value(org.hamcrest.Matchers.containsString("Nghe nhạc trữ tình")));
        assertThat(recordStatus(first)).isEqualTo("CURRENT");
        long second = id(prefer(a, music, "DISLIKE", RESIDENT, true));
        assertThat(recordStatus(first)).isEqualTo("REPLACED");
        assertThat(recordStatus(second)).isEqualTo("CURRENT");
    }

    @Test
    void FR_051_residentAndFamilyOpinionsOnTheSameGroupAreSeparateCurrentEntries() throws Exception {
        long a = resident();
        long relative = TestUsers.nextId();
        family.relate(relative, a);
        long own = id(prefer(a, music, "LIKE", RESIDENT, false));
        long theirs = id(
                prefer(a, music, "DISLIKE", "{\"type\":\"FAMILY_MEMBER\",\"familyMemberId\":" + relative + "}", false));
        assertThat(recordStatus(own)).isEqualTo("CURRENT");
        assertThat(recordStatus(theirs)).isEqualTo("CURRENT");
        prefer(a, chess, "LIKE", "{\"type\":\"FAMILY_MEMBER\",\"familyMemberId\":" + TestUsers.nextId() + "}", false)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_OPINION_SOURCE"));
        prefer(a, chess, "MAYBE", RESIDENT, false).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void FR_051_removingNeedsAReasonAndKeepsTheHistory() throws Exception {
        long a = resident();
        long p = id(prefer(a, music, "LIKE", RESIDENT, false));
        mvc.perform(write(post("/api/v1/preferences/" + p + "/remove"), "{}"))
                .andExpect(status().isUnprocessableEntity());
        mvc.perform(write(post("/api/v1/preferences/" + p + "/remove"), "{\"reason\":\"Ghi nhầm người\"}"))
                .andExpect(status().isOk());
        assertThat(recordStatus(p)).isEqualTo("REMOVED");
        // FR-069, DBR-23: lệnh ghi nhật ký với người thực hiện, thời điểm, lý do.
        assertThat(jdbc.queryForMap(
                        "SELECT actor_id,reason,occurred_at FROM audit_log WHERE action=? AND entity_type=? AND entity_id=?"
                                + " ORDER BY id DESC LIMIT 1",
                        "PREFERENCE_REMOVED",
                        "M04.PREFERENCE",
                        Long.toString(p)))
                .containsEntry("actor_id", manager.accountId())
                .containsEntry("reason", "Ghi nhầm người")
                .containsKey("occurred_at");
        assertThat(jdbc.queryForObject(
                        "SELECT JSON_UNQUOTE(JSON_EXTRACT(content,'$.endReason')) FROM simple_record WHERE id=?",
                        String.class,
                        p))
                .isEqualTo("Ghi nhầm người");
        mvc.perform(write(post("/api/v1/preferences/" + p + "/remove"), "{\"reason\":\"Lần hai\"}"))
                .andExpect(status().isConflict());
        // Sau khi bỏ, ghi lại cùng nhóm và nguồn không cần xác nhận thay.
        id(prefer(a, music, "DISLIKE", RESIDENT, false));
    }

    @Test
    void FR_052_registrationWarnsAboutEachCurrentDislikeWithItsSourceEvenWhenTheResidentLikesIt() throws Exception {
        long a = resident();
        long relative = TestUsers.nextId();
        family.relate(relative, a);
        id(prefer(a, music, "LIKE", RESIDENT, false));
        id(prefer(a, music, "DISLIKE", "{\"type\":\"FAMILY_MEMBER\",\"familyMemberId\":" + relative + "}", false));
        long type = tx.execute(s -> catalogs.create(
                        ActivityKinds.ACTIVITY_TYPES,
                        new NewCatalogItem<>(
                                null,
                                "Văn nghệ " + TestUsers.nextId(),
                                null,
                                0,
                                new ActivityTypeAttributes(true, false, List.of(music))))
                .id());
        long activity = id(mvc.perform(write(
                post("/api/v1/activities"),
                "{\"name\":\"Hát cùng nhau\",\"typeId\":" + type + ",\"scope\":\"FLOOR\",\"floorAreaId\":" + floor
                        + ",\"maxParticipants\":10,\"groupActivity\":true}")));
        Instant start = time.now().plusSeconds(7200).truncatedTo(java.time.temporal.ChronoUnit.MICROS);
        long session = id(mvc.perform(write(
                post("/api/v1/activity-sessions"),
                "{\"activityId\":" + activity + ",\"startsAt\":\"" + start + "\",\"endsAt\":\""
                        + start.plusSeconds(3600) + "\"}")));
        mvc.perform(write(
                        post("/api/v1/activity-sessions/" + session + "/registrations"), "{\"residentId\":" + a + "}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("WARNING_ACKNOWLEDGEMENT_REQUIRED"))
                .andExpect(jsonPath("$.unmetConditions[?(@.code=='DISLIKED_INTEREST')]")
                        .value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.unmetConditions[?(@.code=='DISLIKED_INTEREST')].description")
                        .value(org.hamcrest.Matchers.hasItem(org.hamcrest.Matchers.containsString("người thân"))));
        mvc.perform(write(
                        post("/api/v1/activity-sessions/" + session + "/registrations"),
                        "{\"residentId\":" + a + ",\"acknowledgeWarnings\":true}"))
                .andExpect(status().isCreated());
    }
}
