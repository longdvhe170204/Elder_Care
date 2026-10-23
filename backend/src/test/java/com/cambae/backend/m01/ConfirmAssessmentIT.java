package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.api.events.AssessmentChangedCareLevelOrNeeds;
import com.cambae.backend.m01.api.events.CareLevelChangeRequestNeeded;
import com.cambae.backend.m01.api.events.CareLevelChanged;
import com.cambae.backend.m01.api.events.InitialAssessmentConfirmed;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.AssessmentView;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.ConfirmResult;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * 001 T034 (US3; FR-033, FR-035, FR-036, FR-036a, FR-043; research H-10): tác động của lệnh Xác nhận theo loại đánh giá
 * và trạng thái người cao tuổi.
 */
class ConfirmAssessmentIT extends AssessmentTestBase {

    private long residentWithInitial(CareLevel level) {
        long id = newResident();
        setStatus(id, "ADMISSION_IN_PROGRESS");
        confirmed(id, AssessmentType.INITIAL, level, "Đầu vào", null);
        return id;
    }

    @Test
    void FR_033_initialConfirmed_publishesEventsForContractAndPlan() {
        long resident = newResident();
        setStatus(resident, "ADMISSION_IN_PROGRESS");

        AssessmentView done = confirmed(resident, AssessmentType.INITIAL, CareLevel.REGULAR, "Khỏe", null);

        assertThat(events.stream(InitialAssessmentConfirmed.class))
                .anyMatch(e -> e.residentId() == resident
                        && e.assessmentId() == done.id()
                        && e.careLevel() == CareLevel.REGULAR);
        assertThat(events.stream(CareLevelChanged.class))
                .anyMatch(e -> e.residentId() == resident
                        && e.previous().isEmpty()
                        && e.current() == CareLevel.REGULAR
                        && e.basisId() == done.id());
    }

    @Test
    void FR_035_reassessmentWhileAdmitting_replacesCurrentLevelImmediately() {
        long resident = residentWithInitial(CareLevel.BASIC);

        AssessmentView again = confirmed(resident, AssessmentType.REASSESSMENT, CareLevel.SPECIAL, "Nặng hơn", null);

        assertThat(currentCareLevel(resident)).isEqualTo("SPECIAL");
        assertThat(historyRows(resident)).isEqualTo(2);
        assertThat(events.stream(CareLevelChangeRequestNeeded.class)).noneMatch(e -> e.residentId() == resident);
        assertThat(again.type()).isEqualTo(AssessmentType.REASSESSMENT);
    }

    @Test
    void FR_036_reassessmentWhileResiding_withDifferentLevel_keepsCurrentLevelAndSupersedesOpenRequests() {
        long resident = residentWithInitial(CareLevel.BASIC);
        setStatus(resident, "IN_RESIDENCE");
        changeRequests.open(resident, 9001L).open(resident, 9002L);

        assignNurse(nurse, resident);
        user.set(nurse.user());
        AssessmentView draft = assessments.create(target(resident), AssessmentType.REASSESSMENT, "Định kỳ");
        user.set(doctor.user());
        AssessmentView ready = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(CareLevel.SPECIAL, "Yếu đi", null, Set.of()));
        ConfirmResult result = assessments.confirm(target(resident), draft.id(), etag(ready));

        assertThat(currentCareLevel(resident)).isEqualTo("BASIC");
        assertThat(historyRows(resident)).isEqualTo(1);
        assertThat(result.effects().careLevelChanged()).isFalse();
        assertThat(result.effects().supersededRequestIds()).containsExactlyInAnyOrder(9001L, 9002L);
        assertThat(result.effects().administratorNotified()).isTrue();
        assertThat(changeRequests.hasOpenRequest(resident)).isFalse();
        assertThat(events.stream(CareLevelChangeRequestNeeded.class))
                .anyMatch(e -> e.residentId() == resident
                        && e.assessmentId() == draft.id()
                        && e.newLevel() == CareLevel.SPECIAL);
        assertThat(events.stream(AssessmentChangedCareLevelOrNeeds.class))
                .anyMatch(e -> e.residentId() == resident
                        && e.previousLevel() == CareLevel.BASIC
                        && e.newLevel() == CareLevel.SPECIAL);
    }

    @Test
    void FR_036a_reassessmentWithSameLevel_stillSupersedesRequests_butDoesNotNotifyOffice() {
        long resident = residentWithInitial(CareLevel.REGULAR);
        setStatus(resident, "IN_RESIDENCE");
        changeRequests.open(resident, 9100L);

        assignNurse(nurse, resident);
        user.set(nurse.user());
        AssessmentView draft = assessments.create(target(resident), AssessmentType.REASSESSMENT, "Kiểm tra lại");
        user.set(doctor.user());
        AssessmentView ready = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(CareLevel.REGULAR, "Như cũ", null, Set.of()));
        ConfirmResult result = assessments.confirm(target(resident), draft.id(), etag(ready));

        assertThat(result.effects().supersededRequestIds()).containsExactly(9100L);
        assertThat(result.effects().administratorNotified()).isFalse();
        assertThat(events.stream(CareLevelChangeRequestNeeded.class)).noneMatch(e -> e.residentId() == resident);
        assertThat(changeRequests.hasOpenRequest(resident)).isFalse();
    }

    @Test
    void FR_043_onlyNeedsChanged_notifiesPlanOwnerButNotOffice() {
        long resident = residentWithInitial(CareLevel.REGULAR);
        setStatus(resident, "IN_RESIDENCE");
        long need = careNeedId();

        assignNurse(nurse, resident);
        user.set(nurse.user());
        AssessmentView draft = assessments.create(target(resident), AssessmentType.REASSESSMENT, "Thêm nhu cầu");
        user.set(doctor.user());
        AssessmentView ready = assessments.saveDraft(
                target(resident), draft.id(), etag(draft), full(CareLevel.REGULAR, "Cần phục hồi", null, Set.of(need)));
        ConfirmResult result = assessments.confirm(target(resident), draft.id(), etag(ready));

        assertThat(result.effects().administratorNotified()).isFalse();
        assertThat(result.effects().carePlanReviewNotified()).isTrue();
        assertThat(events.stream(AssessmentChangedCareLevelOrNeeds.class))
                .anyMatch(e -> e.residentId() == resident
                        && e.previousLevel() == CareLevel.REGULAR
                        && e.newLevel() == CareLevel.REGULAR
                        && e.newNeedIds().equals(Set.of(need)));
        assertThat(events.stream(CareLevelChangeRequestNeeded.class)).noneMatch(e -> e.residentId() == resident);
    }

    @Test
    void FR_043_nothingChanged_noPlanNotification() {
        long resident = residentWithInitial(CareLevel.REGULAR);
        setStatus(resident, "IN_RESIDENCE");

        AssessmentView again = draftWithContent(resident, AssessmentType.REASSESSMENT, CareLevel.REGULAR, "Giữ", null);
        ConfirmResult result = assessments.confirm(target(resident), again.id(), etag(again));

        assertThat(result.effects().carePlanReviewNotified()).isFalse();
        assertThat(events.stream(AssessmentChangedCareLevelOrNeeds.class)).noneMatch(e -> e.residentId() == resident);
    }
}
