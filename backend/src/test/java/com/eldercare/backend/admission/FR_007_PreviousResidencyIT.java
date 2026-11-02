package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.resident.internal.profile.domain.IdDocumentType;
import com.eldercare.backend.resident.internal.profile.domain.PersonalInfo;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T025 (FR-007, Q-307, Q-394): người cao tuổi có hồ sơ trước ở Kết thúc lưu trú thì hồ sơ chờ hiện "từng lưu trú tại viện"
 * ({@code previouslyResided}) để Hành chính cân nhắc đánh dấu Ưu tiên; hệ thống không tự đánh dấu. Hồ sơ trước ở trạng thái khác
 * (Qua đời, Hủy tiếp nhận) không hiện.
 */
@AutoConfigureMockMvc
@WithMockUser
class FR_007_PreviousResidencyIT extends WaitingListTestBase {

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
    void FR_007_endedPreviousProfile_isShown_butPriorityIsNotSetAutomatically() throws Exception {
        long[] ids = returningResident("RESIDENCY_ENDED");

        JsonNode created = createEntry(application(ids[1], today(), "BASIC"));

        assertThat(created.get("previouslyResided").asBoolean()).isTrue();
        assertThat(created.get("priority").asBoolean())
                .as("Q-307: không tự đánh dấu")
                .isFalse();
    }

    @Test
    void FR_007_cancelledPreviousProfile_isNotShown() throws Exception {
        long[] ids = returningResident("ADMISSION_CANCELLED");

        assertThat(residentApi.previousProfileId(ids[1])).contains(ids[0]);
        assertThat(createEntry(application(ids[1], today(), "BASIC"))
                        .get("previouslyResided")
                        .asBoolean())
                .isFalse();
    }

    @Test
    void FR_007_residentWithoutPreviousProfile_isNotShown() throws Exception {
        JsonNode created = newEntry(Gender.MALE, today(), "BASIC");

        assertThat(created.get("previouslyResided").asBoolean()).isFalse();
        assertThat(residentApi.previousProfileId(created.get("residentId").asLong()))
                .isEmpty();
    }
}
