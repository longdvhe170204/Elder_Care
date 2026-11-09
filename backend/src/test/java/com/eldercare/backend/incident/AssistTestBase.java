package com.eldercare.backend.incident;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.incident.api.events.AlertCreated;
import com.eldercare.backend.incident.internal.alert.domain.AlertNoticeRequested;
import com.eldercare.backend.support.TestUsers;
import com.eldercare.backend.support.TestUsers.TestUser;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/**
 * Nền cho IT nút hỗ trợ khẩn cấp và gọi 115 (US13; T097, T098): dùng chung context với IT nút SOS. Người bấm C là Điều
 * dưỡng đang trong ca ở {@link #floor}; {@link #otherFloor} cũng đang có ca; ca của 008 là bản giả Mockito.
 */
abstract class AssistTestBase extends SosTestBase {
    static final String ASSIST = "/api/v1/emergency-assist";

    TestUser presser;
    long otherFloor;

    @BeforeEach
    void prepareAssist() {
        presser = users.staff(Role.NURSING_STAFF);
        otherFloor = TestUsers.nextId();
        onShift(presser, floor);
        when(staff.floorsWithShift(any())).thenReturn(List.of(floor, otherFloor));
    }

    void onShift(TestUser user, Long floorAreaId) {
        when(staff.onShift(eq(user.staffId()), any())).thenReturn(true);
        when(staff.floorDuringShift(eq(user.staffId()), any())).thenReturn(Optional.ofNullable(floorAreaId));
    }

    ResultActions pressAssist(TestUser as) throws Exception {
        return mvc.perform(as(as, MockMvcRequestBuilders.post(ASSIST)));
    }

    ResultActions details(TestUser as, long alertId, String json) throws Exception {
        return post(as, ASSIST + "/{id}/details", alertId, json);
    }

    long assistAlert(TestUser as) {
        return jdbc.queryForObject(
                "SELECT id FROM alert WHERE type = 'STAFF_ASSIST_REQUEST' AND requester_id = ?",
                Long.class,
                as.accountId());
    }

    List<RecipientGroup> assistCreatedGroups(TestUser as) {
        String lock = "STAFF_ASSIST_REQUEST:" + as.accountId();
        return events.stream(AlertCreated.class)
                .filter(event -> lock.equals(event.eventKey()))
                .flatMap(event -> event.notificationRequests().stream())
                .flatMap(request -> request.recipientGroups().stream())
                .toList();
    }

    List<RecipientGroup> detailGroups() {
        return events.stream(AlertNoticeRequested.class)
                .flatMap(event -> event.notificationRequests().stream())
                .flatMap(request -> request.recipientGroups().stream())
                .toList();
    }
}
