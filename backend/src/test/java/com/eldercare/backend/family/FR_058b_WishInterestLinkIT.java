package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.administration.shared.api.CatalogApi;
import com.eldercare.backend.administration.shared.api.dto.NewCatalogItem;
import com.eldercare.backend.care.internal.activity.domain.ActivityKinds;
import com.eldercare.backend.support.TestUsers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * FR-058b, BR-M10-12, 001 FR-023g ("Đổi hợp đồng" api-012 A duyệt 2026-11-10): người phụ trách gắn mục sở thích mới vào
 * phản hồi Mong muốn bằng {@code link-opinion} loại {@code INTEREST}; 012 gọi {@code FeedbackPreferenceCommandApi} của 014
 * cùng transaction, mục sở thích có nguồn người cao tuổi và mã phản hồi, phản hồi được gắn mục đó. Nhóm đã có mục hiện hành
 * thì cần xác nhận thay (409).
 */
class FR_058b_WishInterestLinkIT extends FeedbackTestBase {

    @Autowired
    CatalogApi catalogs;

    @Autowired
    TransactionTemplate transaction;

    @Test
    void FR_058b_linkInterest_recordsPreferenceFromTheWish_andLinksIt() throws Exception {
        long feedback = channelSubmitted(residentSession(a), "WISH", "CARE", "OTHER");
        long group = transaction.execute(s -> catalogs.create(
                        ActivityKinds.INTEREST_GROUPS,
                        new NewCatalogItem<>(null, "Sở thích " + TestUsers.nextId(), null, 0, null))
                .id());
        String body = "{\"opinion\": {\"itemType\": \"INTEREST\", \"interestGroupId\": " + group
                + ", \"level\": \"LIKE\", \"description\": \"Thích nghe nhạc tiền chiến\"}}";

        feedbackCommand(staff(floorManager), feedback, "link-opinion", body).andExpect(status().isOk());

        Long preference = jdbc.queryForObject(
                "SELECT id FROM simple_record WHERE kind = 'M04.PREFERENCE' AND content ->> '$.feedbackId' = ?",
                Long.class,
                String.valueOf(feedback));
        assertThat(jdbc.queryForObject(
                        "SELECT content ->> '$.source.type' FROM simple_record WHERE id = ?", String.class, preference))
                .isEqualTo("RESIDENT");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event WHERE kind = 'M10.FEEDBACK_OPINION' AND entity_id = ?"
                                + " AND content ->> '$.itemType' = 'INTEREST' AND content ->> '$.itemId' = ?",
                        Integer.class,
                        feedback,
                        String.valueOf(preference)))
                .isEqualTo(1);

        feedbackCommand(staff(floorManager), feedback, "link-opinion", body).andExpect(status().isConflict());
    }
}
