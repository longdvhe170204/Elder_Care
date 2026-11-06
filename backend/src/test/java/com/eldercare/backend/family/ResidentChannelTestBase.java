package com.eldercare.backend.family;

import static com.eldercare.backend.support.TestUsers.bearer;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.family.api.dto.ResidentChannelBelongingView;
import com.eldercare.backend.family.api.dto.ResidentChannelSessionView;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.fake.family.FakeResidentChannelActivities;
import com.eldercare.backend.support.fake.family.FakeResidentChannelBelongings;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

/**
 * Nền test kênh người cao tuổi (012 US9; FR-083, FR-084): A có tài khoản AC-16, một lượt thăm Đã duyệt buổi sáng ngày
 * {@link #day} do Hành chính đăng ký cho M kèm một người đi cùng; buổi hoạt động và đồ gửi qua bản giả port của 014, 013
 * (chưa có bản cài). Đồng hồ đặt 07:00 ngày {@link #day}.
 */
@Import(ResidentChannelTestBase.Ports.class)
abstract class ResidentChannelTestBase extends VisitTestBase {

    @TestConfiguration
    static class Ports {
        @Bean
        FakeResidentChannelActivities fakeResidentChannelActivities() {
            return new FakeResidentChannelActivities();
        }

        @Bean
        FakeResidentChannelBelongings fakeResidentChannelBelongings() {
            return new FakeResidentChannelBelongings();
        }
    }

    @Autowired
    FakeResidentChannelActivities activities;

    @Autowired
    FakeResidentChannelBelongings belongings;

    long residentAccount;
    long visitId;
    long sessionId;
    long belongingId;

    @BeforeEach
    void channelData() throws Exception {
        residentAccount = users.resident(a);
        visitId = id(
                register(office, mMember, MORNING_START, MORNING_END, "Lê Bé").andExpect(status().isCreated()));
        sessionId = TestUsers.nextId();
        activities.session(
                a,
                new ResidentChannelSessionView(
                        sessionId,
                        "Hát karaoke",
                        at(day, LocalTime.of(15, 0)),
                        at(day, LocalTime.of(16, 0)),
                        true,
                        false));
        belongingId = TestUsers.nextId();
        belongings.belonging(a, new ResidentChannelBelongingView(belongingId, "Đài radio", "HELD", Optional.empty()));
        moveTo(at(day, LocalTime.of(7, 0)));
    }

    String self() {
        return bearer(users.session(residentAccount));
    }
}
