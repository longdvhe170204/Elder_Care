package com.eldercare.backend.admission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.admission.internal.waitinglist.domain.BedContact;
import com.eldercare.backend.admission.internal.waitinglist.domain.BedContactResult;
import com.eldercare.backend.admission.internal.waitinglist.persistence.BedContactStore;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.resident.api.dto.Gender;
import com.eldercare.backend.roombedallocation.api.BedAllocationCommandApi;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.JsonNode;

/**
 * 004 T021 (US2; FR-012, FR-013, FR-014, BR-M02-01, 02, 10, 20; Q-394): danh sách hồ sơ phù hợp với một giường Trống và lệnh liên hệ.
 * Dữ liệu phòng, giường là của 003, thay bằng mock vì {@code BedAllocationCommandApi} chưa có bản cài production (team-plan mục 8);
 * phần 004 chạy trên MySQL thật.
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_01_MatchingEntriesIT extends WaitingListTestBase {

    @MockitoSpyBean
    RoomPolicyApi rooms;

    @MockitoSpyBean
    BedStatusApi bedStatus;

    @MockitoBean
    BedAllocationCommandApi bedCommands;

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
    void BR_M02_10_matchingList_filtersByRoom_priorityFirst_thenApplicationDate() throws Exception {
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

    @Test
    void FR_010_sameDate_createdEarlierComesFirst() throws Exception {
        LocalDate date = today().minusDays(3);
        long first = entry(Gender.FEMALE, date, "REGULAR");
        long second = entry(Gender.FEMALE, date, "REGULAR");

        assertThat(matchingIds(second, first)).containsExactly(first, second);
    }

    @Test
    void BR_M02_20_contactedDeclinedExpiredOnThisBed_inCurrentVacancy_areDropped() throws Exception {
        LocalDate date = today().minusDays(9);
        long contacted = entry(Gender.FEMALE, date, "REGULAR");
        long declined = entry(Gender.FEMALE, date, "REGULAR");
        long expired = entry(Gender.FEMALE, date, "REGULAR");
        long untouched = entry(Gender.FEMALE, date, "REGULAR");
        long otherBed = entry(Gender.FEMALE, date, "REGULAR");
        long earlierVacancy = entry(Gender.FEMALE, date, "REGULAR");
        contact(contacted, bed, BedContactResult.CONTACTED, time.now());
        contact(declined, bed, BedContactResult.BED_DECLINED, time.now());
        contact(expired, bed, BedContactResult.EXPIRED, time.now());
        contact(otherBed, bed + 1, BedContactResult.BED_DECLINED, time.now());
        contact(earlierVacancy, bed, BedContactResult.EXPIRED, vacancyStart.minusSeconds(60));

        assertThat(matchingIds(contacted, declined, expired, untouched, otherBed, earlierVacancy))
                .containsExactly(untouched, otherBed, earlierVacancy);
    }

    @Test
    void FR_012_isolatedRoom_orBedWithFutureAllocation_hasNoMatch() throws Exception {
        long entry = entry(Gender.FEMALE, today().minusDays(2), "REGULAR");

        doReturn(room(true, false, false)).when(rooms).allocationProperties(bed);
        assertThat(matchingIds(entry)).isEmpty();

        doReturn(room(false, true, false)).when(rooms).allocationProperties(bed);
        assertThat(matchingIds(entry)).isEmpty();

        doReturn(room(false, false, true)).when(rooms).allocationProperties(bed);
        assertThat(matchingIds(entry)).isEmpty();
    }

    @Test
    void FR_014_contactingTheFirstEntry_holdsTheBed_withoutReason() throws Exception {
        long first = entry(Gender.FEMALE, oldest(), "REGULAR");
        priority(first, true);

        user.set(office.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + first + "/record-contacted")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedId\":" + bed + "}"))
                .andExpect(status().isOk());

        verify(bedCommands).holdForWaitingRecord(eq(first), anyLong(), eq(bed), any());
        assertThat(entry(first).get("status").asString()).isEqualTo("CONTACTED");
        assertThat(entry(first).get("heldBedId").asLong()).isEqualTo(bed);
    }

    @Test
    void FR_014_contactingOutOfOrder_requiresReason() throws Exception {
        long first = entry(Gender.FEMALE, today().minusDays(30), "REGULAR");
        long second = entry(Gender.FEMALE, today().minusDays(1), "REGULAR");
        priority(first, true);

        user.set(office.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + second + "/record-contacted")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedId\":" + bed + "}"))
                .andExpect(status().isUnprocessableContent());
        verify(bedCommands, never()).holdForWaitingRecord(eq(second), anyLong(), eq(bed), any());

        mvc.perform(post("/api/v1/waiting-list-entries/" + second + "/record-contacted")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedId\":" + bed
                                + ",\"outOfOrderReason\":\"Gia đình hồ sơ đầu chưa liên lạc được\"}"))
                .andExpect(status().isOk());
        assertThat(contacts.of(second)).singleElement().satisfies(event -> {
            assertThat(event.content().result()).isEqualTo(BedContactResult.CONTACTED);
            assertThat(event.content().outOfOrderReason()).contains("chưa liên lạc");
        });
    }

    @Test
    void FR_012_decliningTheBed_returnsToWaiting_keepsPriority_andDropsFromThisVacancy() throws Exception {
        long entry = entry(Gender.FEMALE, oldest(), "REGULAR");
        priority(entry, true);
        user.set(office.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + entry + "/record-contacted")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedId\":" + bed + "}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/v1/waiting-list-entries/" + entry + "/decline-bed")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"Phòng xa gia đình\"}"))
                .andExpect(status().isOk());

        verify(bedCommands).cancelHold(eq(entry), any());
        JsonNode view = entry(entry);
        assertThat(view.get("status").asString()).isEqualTo("WAITING");
        assertThat(view.get("priority").asBoolean()).isTrue();
        assertThat(view.has("heldBedId")).isFalse();
        assertThat(matchingIds(entry)).isEmpty();
    }

    @Test
    void BR_M02_02_bedCommandFailure_rollsBackTheContact() throws Exception {
        long entry = entry(Gender.FEMALE, oldest(), "REGULAR");
        priority(entry, true);
        doThrow(new BusinessException(org.springframework.http.HttpStatus.CONFLICT, "BED_NOT_AVAILABLE", "x"))
                .when(bedCommands)
                .holdForWaitingRecord(eq(entry), anyLong(), eq(bed), any());

        user.set(office.user());
        mvc.perform(post("/api/v1/waiting-list-entries/" + entry + "/record-contacted")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"bedId\":" + bed + "}"))
                .andExpect(status().isConflict());

        assertThat(entry(entry).get("status").asString()).isEqualTo("WAITING");
        assertThat(contacts.of(entry)).isEmpty();
    }
}
