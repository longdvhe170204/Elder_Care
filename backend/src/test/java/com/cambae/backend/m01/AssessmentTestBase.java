package com.cambae.backend.m01;

import com.cambae.backend.common.web.VersionEtag;
import com.cambae.backend.m01.api.dto.CareLevel;
import com.cambae.backend.m01.internal.assessment.domain.AssessmentType;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.AssessmentView;
import com.cambae.backend.m01.internal.assessment.service.AssessmentService.DraftContent;
import com.cambae.backend.support.fake.m01.FakeCareLevelChangeRequests;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.event.ApplicationEvents;

/**
 * Nền chung cho test đánh giá của 001 US3 (T034 → T036): thêm vào {@link HealthTestBase} bản giả
 * {@link FakeCareLevelChangeRequests} (004 cài {@code CareLevelChangeRequestApi}, chưa có bản thật). Không import cả
 * {@code FakeM01Config} vì bản giả {@code ResidentApi} của nó sẽ thay bản thật.
 */
@Import(AssessmentTestBase.Fakes.class)
public abstract class AssessmentTestBase extends HealthTestBase {

    @TestConfiguration
    static class Fakes {

        @Bean
        @Primary
        FakeCareLevelChangeRequests fakeCareLevelChangeRequests() {
            return new FakeCareLevelChangeRequests();
        }
    }

    @Autowired
    protected AssessmentService assessments;

    @Autowired
    protected FakeCareLevelChangeRequests changeRequests;

    @Autowired
    protected ApplicationEvents events;

    /** Nhận xét, mức và nhu cầu hợp lệ để xác nhận. */
    protected static DraftContent full(CareLevel level, String comment, String careNote, Set<Long> careNeedIds) {
        return new DraftContent(null, comment, careNote, level, careNeedIds, null);
    }

    protected static String etag(AssessmentView view) {
        return VersionEtag.of(view.version());
    }

    /** Điều dưỡng lập, bác sĩ sửa thành nội dung đầy đủ; trả bản nháp đã sửa. */
    protected AssessmentView draftWithContent(
            long residentId, AssessmentType type, CareLevel level, String comment, String careNote) {
        assignNurse(nurse, residentId);
        user.set(nurse.user());
        AssessmentView draft = assessments.create(target(residentId), type, "Căn cứ thử");
        user.set(doctor.user());
        return assessments.saveDraft(
                target(residentId), draft.id(), etag(draft), full(level, comment, careNote, Set.of()));
    }

    /** Lần đánh giá đã xác nhận bởi bác sĩ; người dùng hiện tại là bác sĩ. */
    protected AssessmentView confirmed(
            long residentId, AssessmentType type, CareLevel level, String comment, String careNote) {
        AssessmentView draft = draftWithContent(residentId, type, level, comment, careNote);
        user.set(doctor.user());
        return assessments.confirm(target(residentId), draft.id(), etag(draft)).assessment();
    }

    protected long careNeedId() {
        jdbc.update("INSERT INTO care_need (name) VALUES (?)", "Nhu cầu " + UUID.randomUUID());
        return jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    protected String currentCareLevel(long residentId) {
        return jdbc.queryForObject("SELECT current_care_level FROM resident WHERE id = ?", String.class, residentId);
    }

    protected int historyRows(long residentId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM care_level_history WHERE resident_id = ?", Integer.class, residentId);
    }
}
