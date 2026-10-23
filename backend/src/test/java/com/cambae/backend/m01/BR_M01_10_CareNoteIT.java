package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Reason;
import com.cambae.backend.m01.api.AssessmentApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.AssessmentView;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T036 (US3; BR-M01-10, FR-038, research H-10): {@code AssessmentApi.currentCareNote} trả theo lô lưu ý chăm sóc của
 * lần Đã xác nhận gần nhất; lần mới hơn để trống thì trả rỗng, không lùi về lần cũ hơn; bản nháp và bản hủy không tính.
 */
class BR_M01_10_CareNoteIT extends AssessmentTestBase {

    @Autowired
    AssessmentApi api;

    private long admitted(String note) {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        confirmed(id, AssessmentType.INITIAL, CareLevel.BASIC, "Đầu vào", note);
        return id;
    }

    @Test
    void BR_M01_10_batchLookup_givesLatestConfirmedNote_perResident() {
        long withNote = admitted("Dễ ngã khi đứng dậy");
        long withoutNote = admitted(null);
        long none = newResident();

        Map<Long, Optional<String>> notes = api.currentCareNote(List.of(withNote, withoutNote, none));

        assertThat(notes).containsOnlyKeys(withNote, withoutNote, none);
        assertThat(notes.get(withNote)).contains("Dễ ngã khi đứng dậy");
        assertThat(notes.get(withoutNote)).isEmpty();
        assertThat(notes.get(none)).isEmpty();
    }

    @Test
    void FR_038_newerConfirmedWithoutNote_doesNotFallBackToOlderNote() {
        long resident = admitted("Cần người đỡ khi đi vệ sinh");
        setStatus(resident, "IN_RESIDENCE");
        assertThat(api.currentCareNote(List.of(resident)).get(resident)).contains("Cần người đỡ khi đi vệ sinh");

        confirmed(resident, AssessmentType.REASSESSMENT, CareLevel.BASIC, "Đã khá hơn", null);

        assertThat(api.currentCareNote(List.of(resident)).get(resident)).isEmpty();
    }

    @Test
    void FR_038_newerConfirmedWithNote_replacesOlderOne() {
        long resident = admitted("Cũ");
        setStatus(resident, "IN_RESIDENCE");

        confirmed(resident, AssessmentType.REASSESSMENT, CareLevel.BASIC, "Định kỳ", "Mới");

        assertThat(api.currentCareNote(List.of(resident)).get(resident)).contains("Mới");
    }

    @Test
    void BR_M01_10_draftOrDiscardedAssessment_isNotUsed() {
        long resident = admitted("Lưu ý đã xác nhận");
        setStatus(resident, "IN_RESIDENCE");
        AssessmentView draft =
                draftWithContent(resident, AssessmentType.REASSESSMENT, CareLevel.BASIC, "Nháp", "Chưa xác nhận");

        assertThat(api.currentCareNote(List.of(resident)).get(resident)).contains("Lưu ý đã xác nhận");

        assessments.discard(target(resident), draft.id(), new Reason("Bỏ"));
        assertThat(api.currentCareNote(List.of(resident)).get(resident)).contains("Lưu ý đã xác nhận");
    }

    @Test
    void FR_038_latestConfirmed_givesLevelAndNeedsForContractFeature() {
        long resident = admitted("x");

        var latest = api.latestConfirmed(resident);

        assertThat(latest).isPresent();
        assertThat(latest.get().careLevel()).isEqualTo(CareLevel.BASIC);
        assertThat(latest.get().residentId()).isEqualTo(resident);
        assertThat(api.latestConfirmed(newResident())).isEmpty();
    }
}
