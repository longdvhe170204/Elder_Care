package com.eldercare.backend.family;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.family.api.events.VisitCancelledNotice;
import com.eldercare.backend.family.api.events.VisitIsolationNotice;
import com.eldercare.backend.incident.api.dto.IsolationZoneView;
import com.eldercare.backend.incident.api.events.IsolationCreated;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.resident.api.dto.StatusCommand;
import com.eldercare.backend.resident.api.events.ResidentStatusChanged;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;

/**
 * 012 T022 ({@code FR_037_KhoanhVungHuyThamIT}; Q24, Q25; FR-036, FR-037; T009 ghi chú hủy lượt thăm khi kết thúc quan
 * hệ): khoanh vùng làm lượt Đã duyệt chưa hết khung chuyển Hủy (người đăng ký nhận Trung bình), lượt Đã vào giữ nguyên
 * và Hành chính được báo; gỡ vùng không khôi phục. Người cao tuổi Tạm vắng thì lượt Hủy, người đăng ký nhận Nhẹ. Kết
 * thúc quan hệ hủy lượt chưa đến của người đó.
 */
class FR_037_IsolationCancelsVisitIT extends VisitTestBase {

    @Autowired
    ApplicationEventPublisher publisher;

    @Test
    void Q24_isolation_cancelsApprovedVisit_keepsCheckedIn_andNotifies() throws Exception {
        long approved = id(
                register(family(mMember), null, AFTERNOON_START, AFTERNOON_END).andExpect(status().isCreated()));
        long inside =
                id(register(family(nMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        moveTo(at(day, LocalTime.of(9, 15)));
        visitCommand(office, inside, "check-in", "{\"actualVisitors\": []}").andExpect(status().isOk());

        IsolationZoneView zone = new IsolationZoneView(901, Set.of(), Set.of(), time.now(), null);
        isolation.resident(a, List.of(zone));
        tx.executeWithoutResult(s -> publisher.publishEvent(
                new IsolationCreated(901, List.of(), List.of(), "ZONE:901", time.now(), List.of())));

        assertThat(visitStatus(approved)).isEqualTo("CANCELLED");
        assertThat(visitStatus(inside)).isEqualTo("CHECKED_IN");
        assertThat(events.stream(VisitCancelledNotice.class).filter(e -> e.visitId() == approved))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.cause()).isEqualTo("ISOLATION_ACTIVE");
                    assertThat(e.familyMemberId()).isEqualTo(mMember);
                    assertThat(e.notificationRequests())
                            .singleElement()
                            .satisfies(r -> assertThat(r.level()).contains(NotificationLevel.MEDIUM));
                });
        assertThat(events.stream(VisitIsolationNotice.class).filter(e -> e.zoneId() == 901))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.cancelledVisitIds()).contains(approved);
                    assertThat(e.checkedInVisitIds()).contains(inside);
                });
        JsonNodeAssert.cancellation(getJson("/api/v1/visits?residentId=" + a + "&date=" + day), approved);

        isolation.resident(a, List.of());
        assertThat(visitStatus(approved)).isEqualTo("CANCELLED");
    }

    @Test
    void Q25_temporaryAbsence_cancelsVisitsInTheAbsence_withLowNotice() throws Exception {
        long visit =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        jdbc.update("UPDATE resident SET status = 'TEMPORARILY_ABSENT' WHERE id = ?", a);
        tx.executeWithoutResult(s -> publisher.publishEvent(new ResidentStatusChanged(
                a,
                ResidentStatus.IN_RESIDENCE,
                ResidentStatus.TEMPORARILY_ABSENT,
                StatusCommand.GRANT_TEMPORARY_ABSENCE,
                1,
                time.now())));

        assertThat(visitStatus(visit)).isEqualTo("CANCELLED");
        assertThat(events.stream(VisitCancelledNotice.class).filter(e -> e.visitId() == visit))
                .singleElement()
                .satisfies(e -> {
                    assertThat(e.cause()).isEqualTo("RESIDENT_NOT_PRESENT");
                    assertThat(e.notificationRequests())
                            .singleElement()
                            .satisfies(r -> assertThat(r.level()).contains(NotificationLevel.LOW));
                });
    }

    @Test
    void FR_075_finalStatus_cancelsFutureVisits() throws Exception {
        long visit =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        jdbc.update("UPDATE resident SET status = 'DECEASED' WHERE id = ?", a);
        tx.executeWithoutResult(s -> publisher.publishEvent(new ResidentStatusChanged(
                a, ResidentStatus.IN_RESIDENCE, ResidentStatus.DECEASED, StatusCommand.RECORD_DEATH, 1, time.now())));

        assertThat(visitStatus(visit)).isEqualTo("CANCELLED");
    }

    @Test
    void FR_006_endingRelationship_cancelsUpcomingVisitsOfThatPerson() throws Exception {
        long visit =
                id(register(family(nMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));
        long other =
                id(register(family(mMember), null, MORNING_START, MORNING_END).andExpect(status().isCreated()));

        command(n, "end", reason("Gia đình đề nghị")).andExpect(status().isOk());

        assertThat(visitStatus(visit)).isEqualTo("CANCELLED");
        assertThat(visitStatus(other)).isEqualTo("APPROVED");
    }

    /** Đọc lý do hủy trong danh sách lượt thăm. */
    static final class JsonNodeAssert {
        static void cancellation(tools.jackson.databind.JsonNode visits, long visitId) {
            for (tools.jackson.databind.JsonNode v : visits) {
                if (v.get("id").asLong() == visitId) {
                    assertThat(v.get("status").asString()).isEqualTo("CANCELLED");
                    assertThat(v.get("cancellationReason").asString()).isEqualTo("Khu đang khoanh vùng");
                    return;
                }
            }
            throw new AssertionError("Không thấy lượt " + visitId);
        }
    }
}
