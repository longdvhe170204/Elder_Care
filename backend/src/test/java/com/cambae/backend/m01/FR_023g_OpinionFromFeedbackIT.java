package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import com.cambae.backend.m01.api.events.OpinionRecordedFromFeedback;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 001 T076 (quickstart Y1; FR-023g, Q-366; research H-17): mục ý kiến ghi từ phản hồi Mong muốn phát sự kiện chung
 * {@code OpinionRecordedFromFeedback} ở {@code m01.api.events}. 014 phát (test phát thay), 012 nghe: sự kiện mang đủ phản hồi,
 * mục, người cao tuổi và nguồn.
 */
@RecordApplicationEvents
class FR_023g_OpinionFromFeedbackIT extends MySqlIntegrationTestBase {

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    ApplicationEvents events;

    @Test
    void FR_023g_residentInterestFromFeedback_eventCarriesFeedbackAndItem() {
        var source = new OpinionSource(OpinionSourceType.RESIDENT, null);

        tx.executeWithoutResult(s ->
                publisher.publishEvent(new OpinionRecordedFromFeedback(900L, "M12", "INTEREST", 31L, 100L, source)));

        var published = events.stream(OpinionRecordedFromFeedback.class).toList();
        assertThat(published).singleElement().satisfies(e -> {
            assertThat(e.feedbackId()).isEqualTo(900L);
            assertThat(e.sourceModule()).isEqualTo("M12");
            assertThat(e.itemType()).isEqualTo("INTEREST");
            assertThat(e.itemId()).isEqualTo(31L);
            assertThat(e.residentId()).isEqualTo(100L);
            assertThat(e.source()).isEqualTo(source);
        });
    }

    @Test
    void FR_023g_familyMemberSource_keepsFamilyMemberId() {
        var source = new OpinionSource(OpinionSourceType.FAMILY_MEMBER, 41L);

        tx.executeWithoutResult(s ->
                publisher.publishEvent(new OpinionRecordedFromFeedback(901L, "M12", "INTEREST", 32L, 100L, source)));

        assertThat(events.stream(OpinionRecordedFromFeedback.class))
                .singleElement()
                .extracting(e -> e.source().familyMemberId())
                .isEqualTo(41L);
    }

    @Test
    void FR_023g_eventIsInNamedInterfaceOfM01Events() {
        assertThat(OpinionRecordedFromFeedback.class.getPackageName()).isEqualTo("com.cambae.backend.m01.api.events");
    }
}
