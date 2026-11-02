package com.eldercare.backend.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.ContentPart;
import com.eldercare.backend.common.notification.InformationType;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.NotificationRequest;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.SourceGroup;
import com.eldercare.backend.common.notification.SourceRecord;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.common.time.TestClockControl;
import com.eldercare.backend.notification.internal.delivery.service.SmsWindowCloser;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import com.eldercare.backend.support.fake.administration.FakeM15Config;
import com.eldercare.backend.support.fake.administration.FakeStaffProfiles;
import com.eldercare.backend.support.fake.notification.FakeSmsProvider;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * 009 US3 (T024, tên logic {@code FR_016b_GopTinNhanIT}; quickstart Q17): tin nhắn Trung bình đầu tiên gửi ngay và mở
 * khung CFG-M13-03; các tin trong khung dồn lại; hết khung gửi một tin tổng hợp chỉ có số thông báo, các tin dồn ghi Đã
 * gộp kèm tin tổng hợp, mở khung mới; khung không có tin dồn thì đóng. Khẩn cấp không gộp.
 */
@Import(FakeM15Config.class)
class FR_016b_SmsBatchingIT extends M13IntegrationTestBase {

    @Autowired
    BusinessTime time;

    @Autowired
    TestClockControl clock;

    @Autowired
    FakeStaffProfiles profiles;

    @Autowired
    FakeSmsProvider sms;

    @Autowired
    SmsWindowCloser closer;

    TestUsers users;
    TestUser staff;

    @BeforeEach
    void setUp() {
        users = new TestUsers(jdbc, time);
        staff = users.staff(Role.ACCOUNTANT);
        profiles.phone(staff.staffId(), "0902222001");
        jdbc.update("UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.enabled', CAST('true' AS JSON))"
                + " WHERE catalog_type = 'M13.NOTIFICATION_CHANNEL' AND code = 'SMS'");
        sms.reset();
    }

    @AfterEach
    void reset() {
        jdbc.update("UPDATE catalog_item SET attributes = JSON_SET(attributes, '$.enabled', CAST('false' AS JSON))"
                + " WHERE catalog_type = 'M13.NOTIFICATION_CHANNEL' AND code = 'SMS'");
        clock.reset();
        sms.reset();
    }

    private static Instant at(String local) {
        return OffsetDateTime.parse(local + "+07:00").toInstant();
    }

    private void send(String key, NotificationLevel level) {
        NotificationRequest r = new NotificationRequest(
                "M05",
                "TEST_EVENT",
                SourceGroup.OTHER,
                new SourceRecord("M05.TEST", key),
                key,
                Optional.of(level),
                Optional.empty(),
                Optional.empty(),
                List.of(new RecipientGroup.Staff(staff.staffId(), "X")),
                List.of(new ContentPart(InformationType.GENERAL, "Nội dung")),
                Optional.of("Rút gọn " + key),
                Optional.of("Tiêu đề"),
                Optional.empty(),
                false,
                time.now(),
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
        sourceCommand("009", new SampleSourceEvent(List.of(r)));
        awaitRequests(key, 1);
    }

    private List<String> sentContents() {
        return sms.sent().stream().map(FakeSmsProvider.SentSms::content).toList();
    }

    @Test
    void FR_016b_firstSentNow_restBatched_digestAtWindowEnd_newWindow() {
        String p = "gop-" + staff.staffId() + "-";
        clock.setTo(at("2030-03-04T10:00:00"));
        send(p + "1", NotificationLevel.MEDIUM);
        await().atMost(Duration.ofSeconds(10)).until(() -> sms.sent().size() == 1);
        clock.setTo(at("2030-03-04T10:01:00"));
        send(p + "2", NotificationLevel.MEDIUM);
        send(p + "3", NotificationLevel.MEDIUM);
        send(p + "urgent", NotificationLevel.URGENT);
        await().atMost(Duration.ofSeconds(10)).until(() -> sms.sent().size() == 2);
        assertThat(sentContents()).containsExactly("Rút gọn " + p + "1", "Rút gọn " + p + "urgent");

        clock.setTo(at("2030-03-04T10:04:59"));
        closer.run();
        assertThat(sms.sent()).hasSize(2);

        // Đồng hồ giả lập chạy tiếp sau setTo: khung mở lúc 10:00 cộng vài trăm mili giây
        clock.setTo(at("2030-03-04T10:05:02"));
        closer.run();
        await().atMost(Duration.ofSeconds(10)).until(() -> sms.sent().size() == 3);
        assertThat(sentContents().get(2)).contains("2 thông báo mới").doesNotContain("Rút gọn");
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM record_event a JOIN record_event r ON r.kind = 'M13.DELIVERY_RESULT' AND r.entity_type = 'M13.DELIVERY_ATTEMPT' AND r.entity_id = a.id"
                                + " JOIN notification n ON n.id = a.entity_id WHERE a.kind = 'M13.DELIVERY_ATTEMPT'"
                                + " AND a.entity_type = 'M13.NOTIFICATION' AND n.recipient_key = ? AND r.m13_result = 'BATCHED'"
                                + " AND JSON_VALUE(r.content, '$.digestAttemptId' RETURNING SIGNED) IS NOT NULL",
                        Integer.class,
                        "STAFF:" + staff.staffId()))
                .isEqualTo(2);

        // Khung mới mở lúc gửi tin tổng hợp: tin Trung bình kế tiếp dồn lại; hết khung không có tin dồn thì đóng
        clock.setTo(at("2030-03-04T10:10:04"));
        closer.run();
        assertThat(jdbc.queryForObject(
                        "SELECT COUNT(*) FROM simple_record WHERE kind = 'M13.SMS_BATCH_WINDOW'"
                                + " AND content->>'$.recipientKey' = ? AND status = 'OPEN'",
                        Integer.class,
                        "STAFF:" + staff.staffId()))
                .isZero();
        send(p + "4", NotificationLevel.MEDIUM);
        await().atMost(Duration.ofSeconds(10)).until(() -> sms.sent().size() == 4);
    }
}
