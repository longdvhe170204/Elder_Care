package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.dto.Gender;
import com.cambae.backend.m01.api.dto.ResidenceType;
import com.cambae.backend.m02.internal.waitinglist.domain.WaitingListEntry;
import com.cambae.backend.m02.internal.waitinglist.service.MatchingEntryService;
import com.cambae.backend.m02.internal.waitinglist.service.MatchingEntryService.MatchCriteria;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import tools.jackson.databind.JsonNode;

/**
 * 004 T021 (US2; FR-012, FR-013, BR-M02-01, BR-M02-10): danh sách hồ sơ phù hợp với một giường Trống. Phần của 004: hồ sơ
 * Đang chờ thỏa loại lưu trú, mức, giới tính do phòng cho phép (003 truyền vào dạng {@link MatchCriteria}), chưa liên hệ,
 * từ chối hay hết hạn với chính giường này trong lượt trống hiện tại; sắp theo điểm giảm dần, ngày đăng ký, thời điểm tạo;
 * không chọn sẵn, không giới hạn số lượng. Nối với thuộc tính phòng, mốc lượt trống của 003 chờ 003 công bố (blocker).
 */
@AutoConfigureMockMvc
@WithMockUser
class BR_M02_01_MatchingEntriesIT extends WaitingListTestBase {

    private static final MatchCriteria FEMALE_ROOM = new MatchCriteria(
            Set.of(ResidenceType.LONG_TERM_RESIDENTIAL),
            Set.of(CareLevel.REGULAR, CareLevel.SPECIAL),
            Set.of(Gender.FEMALE));

    @Autowired
    MatchingEntryService matching;

    private long bedId = 880_000L;

    private long entryWith(Gender gender, LocalDate applicationDate, String careLevel, int score) throws Exception {
        long id = newEntry(gender, applicationDate, careLevel).get("id").asLong();
        jdbc.update("UPDATE waiting_list_entry SET current_score = ? WHERE id = ?", score, id);
        return id;
    }

    private List<Long> idsOf(List<WaitingListEntry> found, Long... mine) {
        Set<Long> wanted = Set.of(mine);
        return found.stream().map(WaitingListEntry::id).filter(wanted::contains).toList();
    }

    private long nextBed() {
        return ++bedId + System.nanoTime() % 100_000;
    }

    @Test
    void US2_4_matchingList_isFilteredAndOrderedByScoreThenRegistration() throws Exception {
        long c = entryWith(Gender.FEMALE, today().minusDays(15), "SPECIAL", 9);
        long e = entryWith(Gender.FEMALE, today().minusDays(20), "REGULAR", 9);
        long f = entryWith(Gender.MALE, today().minusDays(10), "SPECIAL", 12);
        long h = entryWith(Gender.FEMALE, today().minusDays(10), "BASIC", 11);
        long k = entryWith(Gender.FEMALE, today().minusDays(5), "REGULAR", 6);

        List<WaitingListEntry> found = matching.matching(nextBed(), Instant.EPOCH, FEMALE_ROOM);

        assertThat(idsOf(found, c, e, f, h, k))
                .as("F loại vì giới tính, H loại vì mức; E trước C vì hòa điểm và đăng ký sớm hơn")
                .containsExactly(e, c, k);
    }

    @Test
    void FR_012_sameScoreAndDate_createdEarlierComesFirst() throws Exception {
        LocalDate date = today().minusDays(3);
        long first = entryWith(Gender.FEMALE, date, "REGULAR", 8);
        long second = entryWith(Gender.FEMALE, date, "REGULAR", 8);

        List<WaitingListEntry> found = matching.matching(nextBed(), Instant.EPOCH, FEMALE_ROOM);

        assertThat(idsOf(found, second, first)).containsExactly(first, second);
    }

    @Test
    void FR_012_entriesContactedDeclinedOrExpiredOnThisBed_areDroppedFromTheCurrentVacancy() throws Exception {
        long bed = nextBed();
        Instant vacancyStart = time.now().minusSeconds(3600);
        long contacted = entryWith(Gender.FEMALE, today().minusDays(9), "REGULAR", 20);
        long declined = entryWith(Gender.FEMALE, today().minusDays(9), "REGULAR", 19);
        long expired = entryWith(Gender.FEMALE, today().minusDays(9), "REGULAR", 18);
        long untouched = entryWith(Gender.FEMALE, today().minusDays(9), "REGULAR", 17);
        long otherBed = entryWith(Gender.FEMALE, today().minusDays(9), "REGULAR", 16);
        long earlierVacancy = entryWith(Gender.FEMALE, today().minusDays(9), "REGULAR", 15);
        contact(contacted, bed, "CONTACTED", time.now());
        contact(declined, bed, "BED_DECLINED", time.now());
        contact(expired, bed, "EXPIRED", time.now());
        contact(otherBed, bed + 1, "BED_DECLINED", time.now());
        contact(earlierVacancy, bed, "EXPIRED", time.now().minusSeconds(7200));

        List<WaitingListEntry> found = matching.matching(bed, vacancyStart, FEMALE_ROOM);

        assertThat(idsOf(found, contacted, declined, expired, untouched, otherBed, earlierVacancy))
                .as("chỉ loại hồ sơ đã có lần liên hệ, từ chối, hết hạn với chính giường này từ đầu lượt trống")
                .containsExactly(untouched, otherBed, earlierVacancy);
    }

    @Test
    void FR_012_onlyWaitingEntriesMatch_contactedAndClosedDoNot() throws Exception {
        long waiting = entryWith(Gender.FEMALE, today().minusDays(2), "REGULAR", 30);
        long holding = entryWith(Gender.FEMALE, today().minusDays(2), "REGULAR", 31);
        long closed = entryWith(Gender.FEMALE, today().minusDays(2), "REGULAR", 32);
        jdbc.update(
                "UPDATE waiting_list_entry SET status = 'CONTACTED', held_bed_id = ? WHERE id = ?", 880_900L, holding);
        jdbc.update("UPDATE waiting_list_entry SET status = 'WAITING_CANCELLED' WHERE id = ?", closed);

        List<WaitingListEntry> found = matching.matching(nextBed(), Instant.EPOCH, FEMALE_ROOM);

        assertThat(idsOf(found, waiting, holding, closed)).containsExactly(waiting);
    }

    @Test
    void FR_012_residenceTypeMustBeServedByTheRoom() throws Exception {
        long longTerm = entryWith(Gender.FEMALE, today().minusDays(2), "REGULAR", 40);
        long dayCare = entryWith(Gender.FEMALE, today().minusDays(2), "REGULAR", 41);
        jdbc.update(
                "UPDATE admission_application SET desired_residence_type = 'DAY_CARE' WHERE id ="
                        + " (SELECT application_id FROM waiting_list_entry WHERE id = ?)",
                dayCare);

        List<WaitingListEntry> found = matching.matching(nextBed(), Instant.EPOCH, FEMALE_ROOM);

        assertThat(idsOf(found, longTerm, dayCare)).containsExactly(longTerm);
    }

    @Test
    void FR_013_noMatch_isAnEmptyList_andNobodyIsPreselected() throws Exception {
        long entry = entryWith(Gender.FEMALE, today().minusDays(2), "REGULAR", 50);
        MatchCriteria nobody = new MatchCriteria(Set.of(ResidenceType.LONG_TERM_RESIDENTIAL), Set.of(), Set.of());

        assertThat(idsOf(matching.matching(nextBed(), Instant.EPOCH, nobody), entry))
                .isEmpty();
        assertThat(matching.matching(nextBed(), Instant.EPOCH, FEMALE_ROOM))
                .as("không giới hạn số hồ sơ")
                .isNotEmpty();
        JsonNode view = entry(entry);
        assertThat(view.has("selected")).isFalse();
    }

    private void contact(long entryId, long bedId, String result, Instant at) {
        jdbc.update(
                "INSERT INTO bed_contact (entry_id, bed_id, result, at, performed_by) VALUES (?, ?, ?, ?, ?)",
                entryId,
                bedId,
                result,
                java.time.LocalDateTime.ofInstant(at, java.time.ZoneOffset.UTC),
                office.accountId());
    }
}
