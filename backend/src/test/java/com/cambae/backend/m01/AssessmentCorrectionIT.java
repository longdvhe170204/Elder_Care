package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Reason;
import com.cambae.backend.common.error.BusinessException;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.m01.api.AssessmentApi;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentCorrectedLevel;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.AssessmentView;
import com.cambae.backend.m15.correction.api.CorrectionApi;
import com.cambae.backend.m15.correction.api.CorrectionResult;
import com.cambae.backend.m15.correction.api.CorrectionType;
import com.cambae.backend.m15.correction.api.CreationMethod;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 001 T036 (US3; FR-032, FR-038, FR-049a; research H-10): đính chính lần đánh giá Đã xác nhận qua 000 (loại
 * {@code M01.ASSESSMENT}, người ghi gốc là bác sĩ xác nhận). Lưu ý hiển thị theo bản đính chính; đính chính không tự đổi
 * mức đã áp dụng; chỉ khi mức trong bản đính chính khác mức gốc của lần Đã xác nhận gần nhất thì phát
 * {@link AssessmentCorrectedLevel} để thêm lần báo đánh giá lại {@code DINH_CHINH_DANH_GIA} (bảng ở T062).
 */
class AssessmentCorrectionIT extends AssessmentTestBase {

    @Autowired
    CorrectionApi corrections;

    @Autowired
    AssessmentApi api;

    private long admitted(CareLevel level, String note) {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        confirmed(id, AssessmentType.INITIAL, level, "Đầu vào", note);
        return id;
    }

    private long latestId(long resident) {
        return api.latestConfirmed(resident).orElseThrow().id();
    }

    private CorrectionResult correct(long assessmentId, Map<String, Object> content) {
        user.set(doctor.user());
        return corrections.create(
                "M01.ASSESSMENT", assessmentId, CorrectionType.AMEND_CONTENT, content, new Reason("Ghi sai"));
    }

    @Test
    void FR_032_correctingNote_changesDisplayedNote_andDoesNotNotifyReassessment() {
        long resident = admitted(CareLevel.REGULAR, "Lưu ý cũ");
        long id = latestId(resident);

        CorrectionResult result = correct(id, Map.of("careNote", "Lưu ý đúng"));

        assertThat(result)
                .isInstanceOfSatisfying(
                        CorrectionResult.Saved.class,
                        saved -> assertThat(saved.correction().creationMethod()).isEqualTo(CreationMethod.A));
        assertThat(api.currentCareNote(List.of(resident)).get(resident)).contains("Lưu ý đúng");
        user.set(doctor.user());
        AssessmentView view = assessments.get(target(resident), id);
        assertThat(view.corrected()).isTrue();
        assertThat(view.careNote()).isEqualTo("Lưu ý đúng");
        assertThat(events.stream(AssessmentCorrectedLevel.class)).noneMatch(e -> e.residentId() == resident);
    }

    @Test
    void FR_032_sharedFormWithBlankCommentAndLevel_keepsThem_butBlankNoteClearsIt() {
        long resident = admitted(CareLevel.REGULAR, "Lưu ý cũ");
        long id = latestId(resident);

        correct(id, Map.of("comment", "", "careNote", "", "careLevel", ""));

        user.set(doctor.user());
        AssessmentView view = assessments.get(target(resident), id);
        assertThat(view.comment()).isEqualTo("Đầu vào");
        assertThat(view.careLevel()).isEqualTo(CareLevel.REGULAR);
        assertThat(view.careNote()).isNull();
        assertThat(events.stream(AssessmentCorrectedLevel.class)).noneMatch(e -> e.residentId() == resident);
    }

    @Test
    void FR_032_correctingLevelOfLatest_keepsAppliedLevel_butNotifiesDoctorToReassess() {
        long resident = admitted(CareLevel.REGULAR, null);
        long id = latestId(resident);

        correct(id, Map.of("careLevel", "SPECIAL"));

        assertThat(currentCareLevel(resident)).isEqualTo("REGULAR");
        assertThat(historyRows(resident)).isEqualTo(1);
        assertThat(events.stream(AssessmentCorrectedLevel.class))
                .anyMatch(e -> e.residentId() == resident && e.assessmentId() == id);
    }

    @Test
    void FR_032_correctingToSameLevel_doesNotNotify() {
        long resident = admitted(CareLevel.REGULAR, null);

        correct(latestId(resident), Map.of("careLevel", "REGULAR"));

        assertThat(events.stream(AssessmentCorrectedLevel.class)).noneMatch(e -> e.residentId() == resident);
    }

    @Test
    void FR_032_correctingAnOlderAssessment_doesNotNotify() {
        long resident = admitted(CareLevel.BASIC, null);
        long oldest = latestId(resident);
        setStatus(resident, "IN_RESIDENCE");
        confirmed(resident, AssessmentType.REASSESSMENT, CareLevel.BASIC, "Định kỳ", null);

        correct(oldest, Map.of("careLevel", "SPECIAL"));

        assertThat(events.stream(AssessmentCorrectedLevel.class)).noneMatch(e -> e.residentId() == resident);
    }

    @Test
    void FR_049a_correctionOfDraftOrUnknownField_isRejected() {
        long resident = newResident();
        setStatus(resident, "ADMISSION_IN_PROGRESS");
        AssessmentView draft = draftWithContent(resident, AssessmentType.INITIAL, CareLevel.BASIC, "Nháp", null);

        assertThatThrownBy(() -> correct(draft.id(), Map.of("careNote", "x")))
                .isInstanceOfSatisfying(
                        BusinessException.class, e -> assertThat(e.errorCode()).isEqualTo("RECORD_NOT_CONFIRMED"));
        long other = admitted(CareLevel.BASIC, null);
        assertThatThrownBy(() -> correct(latestId(other), Map.of("tenTruongLa", "x")))
                .isInstanceOf(InvalidValueException.class);
    }
}
