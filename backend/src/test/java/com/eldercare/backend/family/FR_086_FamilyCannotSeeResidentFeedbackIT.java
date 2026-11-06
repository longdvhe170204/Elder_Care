package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.family.api.events.FeedbackNotice;
import org.junit.jupiter.api.Test;

/**
 * 012 T064 (US9; FR-086, BR-M10-11; quickstart K5): phản hồi do người cao tuổi gửi, kể cả được ghi thay, không hiện với
 * người thân ở danh sách, chi tiết hay thông báo; người cao tuổi chỉ thấy phản hồi của chính mình.
 */
class FR_086_FamilyCannotSeeResidentFeedbackIT extends FeedbackTestBase {

    @Test
    void FR_086_familyListAndDetailHideResidentFeedback() throws Exception {
        long own = channelSubmitted(residentSession(a), "COMPLAINT", "CARE", null);
        long onBehalf = id(onBehalfSubmit(staff(nurse), a, "SUGGESTION", "CARE").andExpect(status().isCreated()));
        long familyOwn = submitted(family(mMember), null, "QUESTION", "CARE");

        for (long member : new long[] {mMember, nMember, rMember}) {
            assertThat(idsOf(getJsonAs(family(member), "/api/v1/feedback"))).doesNotContain(own, onBehalf);
            getAs(family(member), "/api/v1/feedback/" + own).andExpect(status().isNotFound());
        }
        assertThat(idsOf(getJsonAs(family(mMember), "/api/v1/feedback"))).contains(familyOwn);
        // Người cao tuổi chỉ thấy phản hồi của mình, không thấy phản hồi của người thân
        assertThat(idsOf(getJsonAs(residentSession(a), "/api/v1/resident-channel/feedback")))
                .contains(own, onBehalf)
                .doesNotContain(familyOwn);
        assertThat(idsOf(getJsonAs(residentSession(a), "/api/v1/feedback"))).doesNotContain(familyOwn);
        getAs(residentSession(a), "/api/v1/feedback/" + familyOwn).andExpect(status().isNotFound());
    }

    @Test
    void FR_086_noNoticeToFamily() throws Exception {
        long id = channelSubmitted(residentSession(a), "SUGGESTION", "CARE", null);
        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());

        assertThat(events.stream(FeedbackNotice.class)
                        .filter(n -> n.feedbackId() == id)
                        .map(FeedbackNotice::recipient))
                .noneMatch(r -> r instanceof RecipientGroup.FamilyMember
                        || r instanceof RecipientGroup.PrimaryContact
                        || r instanceof RecipientGroup.Representative
                        || r instanceof RecipientGroup.FamilyWithRight);
    }
}
