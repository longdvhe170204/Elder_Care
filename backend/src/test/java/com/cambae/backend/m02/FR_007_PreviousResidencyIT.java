package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.cambae.backend.m01.api.ResidentApi;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.internal.profile.domain.IdDocumentType;
import com.cambae.backend.m01.internal.profile.domain.PersonalInfo;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T025 (FR-007; contracts/api-004.yaml, {@code POST /waiting-list-entries}): người cao tuổi có hồ sơ trước ở Kết thúc
 * lưu trú thì tình huống "từng lưu trú tại viện" ({@code PREVIOUSLY_RESIDED}) được đánh dấu sẵn ở trạng thái chờ xác nhận
 * và chỉ tính điểm khi Hành chính xác nhận. Hồ sơ trước ở trạng thái khác (Qua đời, Hủy tiếp nhận) không được đánh dấu.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_007_PreviousResidencyIT extends WaitingListTestBase {

    private static final String CODE = "PREVIOUSLY_RESIDED";
    private static final AtomicLong CITIZEN_IDS = new AtomicLong(System.nanoTime() % 100_000_000_000L);

    @Autowired
    private ResidentApi residentApi;

    private long profileWithCitizenId(String name, String citizenId) {
        user.set(office.user());
        return profiles.create(
                        new PersonalInfo(
                                name,
                                LocalDate.of(1943, 2, 9),
                                Gender.FEMALE,
                                IdDocumentType.CITIZEN_ID,
                                citizenId,
                                null,
                                null,
                                null,
                                null,
                                null),
                        false)
                .profile()
                .id();
    }

    /** Người cao tuổi mới Đang tiếp nhận, cùng CCCD với một hồ sơ cũ đang ở {@code oldStatus}; trả {mã cũ, mã mới}. */
    private long[] returningResident(String oldStatus) {
        String citizenId = String.format("%012d", CITIZEN_IDS.incrementAndGet());
        long old = profileWithCitizenId("Trần Thị Cũ " + citizenId, citizenId);
        setStatus(old, oldStatus);
        long fresh = profileWithCitizenId("Trần Thị Mới " + citizenId, citizenId);
        setStatus(fresh, "ADMISSION_IN_PROGRESS");
        return new long[] {old, fresh};
    }

    @Test
    void FR_007_residentApi_exposesThePreviousProfile() {
        long[] ids = returningResident("RESIDENCY_ENDED");

        assertThat(residentApi.previousProfileId(ids[1])).contains(ids[0]);
        assertThat(residentApi.previousProfileId(ids[0])).isEmpty();
    }

    @Test
    void FR_007_endedPreviousProfile_marksTheCircumstanceAsPendingAndItDoesNotScoreYet() throws Exception {
        setScoreTable(SCORE_TABLE.replace("\"LIVING_ALONE\":2", "\"" + CODE + "\":4,\"LIVING_ALONE\":2"));
        long[] ids = returningResident("RESIDENCY_ENDED");
        long applicationId = application(ids[1], today(), "BASIC");

        JsonNode created = createEntry(applicationId);

        assertThat(created.get("circumstances")).hasSize(1);
        assertThat(created.get("circumstances").get(0).get("code").asString()).isEqualTo(CODE);
        assertThat(created.get("circumstances").get(0).get("status").asString()).isEqualTo("PENDING_CONFIRMATION");
        assertThat(created.get("score").asInt()).isZero();
    }

    @Test
    void FR_007_officeConfirms_thenTheCircumstanceScores_andCanBeDroppedWithAReason() throws Exception {
        setScoreTable(SCORE_TABLE.replace("\"LIVING_ALONE\":2", "\"" + CODE + "\":4,\"LIVING_ALONE\":2"));
        long[] ids = returningResident("RESIDENCY_ENDED");
        JsonNode created = createEntry(application(ids[1], today(), "BASIC"));
        long id = created.get("id").asLong();

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/update-circumstances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"circumstances\":[\"" + CODE + "\"],\"reason\":\"Hồ sơ cũ đúng người\"}"))
                .andExpect(status().isOk());
        JsonNode confirmed = entry(id);
        assertThat(confirmed.get("circumstances").get(0).get("status").asString())
                .isEqualTo("CONFIRMED");
        assertThat(confirmed.get("score").asInt()).isEqualTo(4);

        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/update-circumstances")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"circumstances\":[],\"reason\":\"Trùng CCCD nhưng là người khác\"}"))
                .andExpect(status().isOk());
        assertThat(entry(id).get("circumstances")).isEmpty();
        assertThat(entry(id).get("score").asInt()).isZero();
    }

    @Test
    void FR_007_cancelledPreviousProfile_isNotMarked() throws Exception {
        long[] ids = returningResident("ADMISSION_CANCELLED");

        assertThat(residentApi.previousProfileId(ids[1])).contains(ids[0]);
        JsonNode created = createEntry(application(ids[1], today(), "BASIC"));

        assertThat(created.get("circumstances")).isEmpty();
    }

    @Test
    void FR_007_residentWithoutPreviousProfile_isNotMarked() throws Exception {
        JsonNode created = newEntry(Gender.MALE, today(), "BASIC");

        assertThat(created.get("circumstances")).isEmpty();
        assertThat(residentApi.previousProfileId(created.get("residentId").asLong()))
                .isEmpty();
    }
}
