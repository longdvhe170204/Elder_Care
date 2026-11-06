package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.resident.api.dto.OpinionSource;
import com.eldercare.backend.resident.api.dto.OpinionSourceType;
import com.eldercare.backend.resident.api.events.OpinionRecordedFromFeedback;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 012 T063 (US9; FR-058b, FR-059, FR-066, BR-M10-12; quickstart K3, K4): loại Mong muốn chỉ người cao tuổi gửi, có chủ
 * đề quyết định người phụ trách; Trả lời bị chặn tới khi gắn mục ý kiến có nguồn người cao tuổi (mong muốn về khách thăm,
 * người đón của 012 qua {@code link-opinion}, sở thích của 014 qua sự kiện); không phải mong muốn thì đổi loại có lý do.
 */
class BR_M10_12_WishIT extends FeedbackTestBase {

    @Autowired
    ApplicationEventPublisher publisher;

    /** K3: Mong muốn về khách thăm: Hành chính phụ trách; chưa gắn mục thì 409; gắn mục rồi trả lời. */
    @Test
    void BR_M10_12_visitorWishNeedsLinkedOpinionBeforeReply() throws Exception {
        long id = channelSubmitted(residentSession(a), "WISH", "CARE", "VISITORS_PICKERS");

        assertThat(row(id).get("type")).isEqualTo("WISH");
        assertThat(row(id).get("wish_topic")).isEqualTo("VISITORS_PICKERS");
        assertThat(row(id).get("content_group")).isEqualTo("OTHER");
        assertThat(row(id).get("assignee_role")).isEqualTo("OFFICE_STAFF");
        assertThat(row(id).get("priority")).isEqualTo("NORMAL");

        feedbackCommand(office, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(office, id, "reply", reply())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("OPINION_NOT_LINKED"));

        long familyWish = wish("FAMILY_MEMBER", mMember);
        feedbackCommand(office, id, "link-opinion", opinion(familyWish)).andExpect(status().isUnprocessableContent());

        long residentWish = wish("RESIDENT", null);
        feedbackCommand(office, id, "link-opinion", opinion(residentWish)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M10.FEEDBACK_OPINION' AND entity_type = 'M10.FEEDBACK' AND entity_id = ?",
                        Integer.class,
                        id))
                .isEqualTo(1);
        // Gắn lại cùng mục không thêm dòng
        feedbackCommand(office, id, "link-opinion", opinion(residentWish)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M10.FEEDBACK_OPINION' AND entity_type = 'M10.FEEDBACK' AND entity_id = ?",
                        Integer.class,
                        id))
                .isEqualTo(1);

        feedbackCommand(office, id, "reply", reply())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REPLIED"));
    }

    /** Sở thích ghi ở 014 từ phản hồi Mong muốn: 012 nghe sự kiện, gắn mục khi đúng người và nguồn người cao tuổi. */
    @Test
    void BR_M10_12_interestFromActivityModuleIsLinked() throws Exception {
        long id = channelSubmitted(residentSession(a), "WISH", "CARE", "OTHER");
        assertThat(row(id).get("content_group")).isEqualTo("CARE");
        assertThat(row(id).get("assignee_account_id")).isEqualTo(floorManager.accountId());

        long other = resident(com.eldercare.backend.resident.api.dto.ResidentStatus.IN_RESIDENCE);
        publish(new OpinionRecordedFromFeedback(
                id, "M04", "INTEREST", TestUsers.nextId(), other, new OpinionSource(OpinionSourceType.RESIDENT, null)));
        publish(new OpinionRecordedFromFeedback(
                id,
                "M04",
                "INTEREST",
                TestUsers.nextId(),
                a,
                new OpinionSource(OpinionSourceType.FAMILY_MEMBER, mMember)));
        String tt = staff(floorManager);
        feedbackCommand(tt, id, "start-handling", "{}").andExpect(status().isOk());
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isConflict());

        publish(new OpinionRecordedFromFeedback(
                id, "M04", "INTEREST", TestUsers.nextId(), a, new OpinionSource(OpinionSourceType.RESIDENT, null)));
        feedbackCommand(tt, id, "reply", reply()).andExpect(status().isOk());
    }

    /** K4: không phải mong muốn thì đổi loại sang Hỏi đáp; thiếu lý do 422; mức và hạn giữ nguyên. */
    @Test
    void FR_066_changeTypeKeepsPriorityAndDue() throws Exception {
        long id = channelSubmitted(residentSession(a), "WISH", "CARE", "OTHER");
        Object due = row(id).get("due_at");
        String tt = staff(floorManager);

        feedbackCommand(tt, id, "change-type", "{\"newType\": \"QUESTION\"}")
                .andExpect(status().isUnprocessableContent());
        // Loại mới chỉ Góp ý hoặc Hỏi đáp (hợp đồng: enum của newType)
        feedbackCommand(tt, id, "change-type", "{\"newType\": \"COMPLAINT\", \"reason\": \"Là câu hỏi\"}")
                .andExpect(status().isBadRequest());
        feedbackCommand(tt, id, "change-type", "{\"newType\": \"QUESTION\", \"reason\": \"Là câu hỏi giờ ăn tối\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("QUESTION"));

        assertThat(row(id).get("type")).isEqualTo("QUESTION");
        assertThat(row(id).get("wish_topic")).isNull();
        assertThat(row(id).get("priority")).isEqualTo("NORMAL");
        assertThat(row(id).get("due_at")).isEqualTo(due);
        // Không còn là Mong muốn: đổi loại lần nữa bị chặn
        feedbackCommand(tt, id, "change-type", "{\"newType\": \"SUGGESTION\", \"reason\": \"Thử lại\"}")
                .andExpect(status().isConflict());
    }

    @Test
    void FR_058b_wishOnlyFromResidents() throws Exception {
        postAs(
                        family(mMember),
                        "/api/v1/feedback",
                        "{\"residentId\": " + a + ", \"type\": \"WISH\", \"group\": \"CARE\", \"wishTopic\": \"OTHER\","
                                + " \"content\": \"Muốn nghe nhạc\"}")
                .andExpect(status().isUnprocessableContent());
        channelSubmit(residentSession(a), "WISH", "CARE", null, "Thiếu chủ đề")
                .andExpect(status().isUnprocessableContent());
        channelSubmit(residentSession(a), "SUGGESTION", "CARE", "OTHER", "Chủ đề chỉ cho Mong muốn")
                .andExpect(status().isUnprocessableContent());
    }

    private long wish(String sourceType, Long familyMemberId) throws Exception {
        String source = familyMemberId == null
                ? "{\"type\": \"" + sourceType + "\"}"
                : "{\"type\": \"" + sourceType + "\", \"familyMemberId\": " + familyMemberId + "}";
        return id(postAs(
                        office,
                        "/api/v1/residents/" + a + "/visit-pickup-wishes",
                        "{\"fullName\": \"Nguyễn Văn X\", \"idDocumentNumber\": \"0790" + TestUsers.nextId()
                                + "\", \"type\": \"VISIT\", \"wanted\": false, \"source\": " + source + "}")
                .andExpect(status().isCreated()));
    }

    private static String opinion(long wishId) {
        return "{\"opinion\": {\"itemType\": \"VISIT_PICKUP_WISH\", \"itemId\": " + wishId + "}}";
    }

    private void publish(Object event) {
        tx.executeWithoutResult(s -> publisher.publishEvent(event));
    }
}
