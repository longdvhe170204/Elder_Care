package com.cambae.backend.m01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

import com.cambae.backend.m01.api.dto.OpinionSource;
import com.cambae.backend.m01.api.dto.OpinionSourceType;
import com.cambae.backend.m01.api.events.OpinionRecordedFromFeedback;
import com.cambae.backend.support.MySqlIntegrationTestBase;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 001 T076 (quickstart Y1; FR-023g, Q-366; research H-17): mục ý kiến ghi từ phản hồi Mong muốn phát sự kiện chung
 * {@code OpinionRecordedFromFeedback} ở {@code m01.api.events}. 014 (module M04, do C làm) phát, 012 nghe. Hợp đồng
 * chốt bộ giá trị: {@code sourceModule = "M04"}, {@code itemType = "INTEREST"} (mục sở thích {@code so_thich} của 014;
 * {@code loai_muc SO_THICH} của 012); các giá trị khác bị từ chối. Test kiểm cả đường giao qua kho sự kiện của Modulith:
 * người nghe (đứng thay 012) nhận sự kiện sau khi giao dịch của người phát hoàn tất, sự kiện lưu được và đọc lại
 * được từ kho, bản ghi giao hoàn tất.
 */
@Import(FR_023g_OpinionFromFeedbackIT.Listener.class)
class FR_023g_OpinionFromFeedbackIT extends MySqlIntegrationTestBase {

    /** Người nghe đứng thay 012: ghi lại sự kiện nhận được. */
    static class Received {
        final List<OpinionRecordedFromFeedback> events = new CopyOnWriteArrayList<>();
    }

    @TestConfiguration
    static class Listener {

        @Bean
        Received received() {
            return new Received();
        }

        @Bean
        Handler handler(Received received) {
            return new Handler(received);
        }
    }

    static class Handler {
        private final Received received;

        Handler(Received received) {
            this.received = received;
        }

        @ApplicationModuleListener
        void on(OpinionRecordedFromFeedback event) {
            received.events.add(event);
        }
    }

    @Autowired
    ApplicationEventPublisher publisher;

    @Autowired
    TransactionTemplate tx;

    @Autowired
    Received received;

    @Autowired
    JdbcTemplate jdbc;

    private static OpinionRecordedFromFeedback interest(long feedbackId, long itemId, OpinionSource source) {
        return new OpinionRecordedFromFeedback(
                feedbackId,
                OpinionRecordedFromFeedback.SOURCE_ACTIVITY_MODULE,
                OpinionRecordedFromFeedback.ITEM_INTEREST,
                itemId,
                100L,
                source);
    }

    private List<OpinionRecordedFromFeedback> receivedFor(long feedbackId) {
        return received.events.stream()
                .filter(e -> e.feedbackId() == feedbackId)
                .toList();
    }

    @Test
    void FR_023g_residentInterestFromFeedback_isDeliveredAfterCommit_withFeedbackItemResidentAndSource() {
        var source = new OpinionSource(OpinionSourceType.RESIDENT, null);

        tx.executeWithoutResult(s -> {
            publisher.publishEvent(interest(900L, 31L, source));
            // chưa commit thì chưa giao cho người nghe
            assertThat(receivedFor(900L)).isEmpty();
        });

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(receivedFor(900L))
                        .singleElement()
                        .satisfies(e -> {
                            assertThat(e.sourceModule()).isEqualTo("M04");
                            assertThat(e.itemType()).isEqualTo("INTEREST");
                            assertThat(e.itemId()).isEqualTo(31L);
                            assertThat(e.residentId()).isEqualTo(100L);
                            assertThat(e.source()).isEqualTo(source);
                        }));
    }

    @Test
    void FR_023g_familyMemberSource_keepsFamilyMemberId_throughTheEventStore() {
        var source = new OpinionSource(OpinionSourceType.FAMILY_MEMBER, 41L);

        tx.executeWithoutResult(s -> publisher.publishEvent(interest(901L, 32L, source)));

        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(receivedFor(901L))
                        .singleElement()
                        .extracting(e -> e.source().familyMemberId())
                        .isEqualTo(41L));
        // sự kiện đã lưu vào kho, đọc lại từ JSON đúng giá trị, và bản ghi giao đã hoàn tất
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> assertThat(jdbc.queryForObject(
                                "SELECT COUNT(*) FROM EVENT_PUBLICATION WHERE SERIALIZED_EVENT LIKE ? AND COMPLETION_DATE IS"
                                        + " NOT NULL",
                                Integer.class,
                                "%\"feedbackId\":901%"))
                        .isEqualTo(1));
    }

    @Test
    void FR_023g_onlyTheAgreedModuleAndItemType_areAccepted() {
        var source = new OpinionSource(OpinionSourceType.RESIDENT, null);

        assertThatThrownBy(() -> new OpinionRecordedFromFeedback(1L, "M12", "INTEREST", 1L, 1L, source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("sourceModule");
        assertThatThrownBy(() -> new OpinionRecordedFromFeedback(1L, "M04", "SO_THICH", 1L, 1L, source))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("itemType");
        assertThatThrownBy(() -> new OpinionRecordedFromFeedback(1L, null, "INTEREST", 1L, 1L, source))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void FR_023g_idsMustBePositive_andSourceIsRequired() {
        var source = new OpinionSource(OpinionSourceType.RESIDENT, null);

        assertThatThrownBy(() -> interest(0L, 1L, source)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> interest(1L, 0L, source)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new OpinionRecordedFromFeedback(1L, "M04", "INTEREST", 1L, 0L, source))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> interest(1L, 1L, null)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void FR_023g_eventIsInNamedInterfaceOfM01Events() {
        assertThat(OpinionRecordedFromFeedback.class.getPackageName()).isEqualTo("com.cambae.backend.m01.api.events");
    }
}
