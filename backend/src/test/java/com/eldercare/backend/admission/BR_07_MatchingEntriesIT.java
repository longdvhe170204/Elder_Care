package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.internal.waitinglist.domain.BedContact;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContactResult;
import com.eldercare.backend.admission.internal.waitinglist.persistence.BedContactStore;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.roombedallocation.api.BedStatusApi;
import com.eldercare.backend.roombedallocation.api.RoomPolicyApi;
import com.eldercare.backend.roombedallocation.api.dto.GenderPolicy;
import com.eldercare.backend.roombedallocation.api.dto.RoomAllocationProperties;
import com.eldercare.backend.roombedallocation.api.spi.CareLevelCode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * 004 T021 (US2; FR-012, FR-013, FR-014, BR-M02-01, 02, 10, 20; Q-394): danh sách hồ sơ phù hợp với một giường Trống và lệnh liên hệ.
 * Thuộc tính phòng và thời điểm giường trống của 003 đặt bằng spy ({@code RoomPolicyApi}, {@code BedStatusApi}); phần 004 chạy trên
 * MySQL thật. Danh sách không gọi lệnh phân bổ nên không còn mock {@code BedAllocationCommandApi} (bản thật của 003 đã có, 2026-11-09).
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_07_MatchingEntriesIT extends WaitingListTestBase {

    @MockitoSpyBean
    RoomPolicyApi rooms;

    @MockitoSpyBean
    BedStatusApi bedStatus;

    @Autowired
    BedContactStore contacts;

    @Autowired
    TransactionTemplate transactions;

    private static final java.util.concurrent.atomic.AtomicLong OLDEST =
            new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis() / 1000 % 3000);

    private long bed;
    private Instant vacancyStart;

    @BeforeEach
    void emptyFemaleRoom() {
        bed = System.nanoTime() % 1_000_000_000L;
        vacancyStart = time.now().minusSeconds(3600);
        doReturn(Optional.of(vacancyStart)).when(bedStatus).emptyPeriodStarted(bed);
        doReturn(room(false, false, false)).when(rooms).allocationProperties(bed);
    }

    private static RoomAllocationProperties room(boolean isolated, boolean future, boolean awaiting) {
        return new RoomAllocationProperties(
                GenderPolicy.FEMALE,
                Set.of(CareLevelCode.FREQUENT, CareLevelCode.SPECIAL),
                true,
                false,
                isolated,
                future,
                awaiting);
    }

    /** Mỗi lần gọi một ngày cũ hơn mọi fixture: schema test giữ dữ liệu của lần chạy trước. */
    private LocalDate oldest() {
        return LocalDate.of(2000, 1, 1).minusDays(OLDEST.incrementAndGet());
    }

    private long entry(Gender gender, LocalDate applicationDate, String careLevel) throws Exception {
        return newEntry(gender, applicationDate, careLevel).get("id").asLong();
    }

    private void priority(long id, boolean value) throws Exception {
        user.set(office.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + id + "/mark-priority")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"priority\":" + value + ",\"reason\":\"Vừa ra viện, sống một mình\"}"))
                .andExpect(status().isOk());
    }

    private List<Long> matchingIds(Long... mine) throws Exception {
        user.set(office.user());
        JsonNode body = JSON.readTree(mvc.perform(get("/api/v1/beds/" + bed + "/matching-entries"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        Set<Long> wanted = Set.of(mine);
        List<Long> ids = new ArrayList<>();
        body.get("entries").forEach(node -> {
            if (wanted.contains(node.get("id").asLong())) {
                ids.add(node.get("id").asLong());
            }
        });
        return ids;
    }

    private void contact(long entryId, long bedId, BedContactResult result, Instant at) {
        transactions.executeWithoutResult(
                status -> contacts.add(entryId, new BedContact(bedId, result, null, null), null, at));
    }

    @Test
    void BR_07_matchingList_filtersByRoom_priorityFirst_thenApplicationDate() throws Exception {
        long c = entry(Gender.FEMALE, today().minusDays(15), "SPECIAL");
        long e = entry(Gender.FEMALE, today().minusDays(20), "REGULAR");
        long f = entry(Gender.MALE, today().minusDays(10), "SPECIAL");
        long h = entry(Gender.FEMALE, today().minusDays(10), "BASIC");
        long k = entry(Gender.FEMALE, today().minusDays(5), "REGULAR");
        priority(k, true);

        assertThat(matchingIds(c, e, f, h, k))
                .as("F loại vì giới tính, H loại vì mức; K trước vì Ưu tiên; E trước C vì đăng ký sớm hơn")
                .containsExactly(k, e, c);
    }
}
