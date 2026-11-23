package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.account.api.spi.BedAllocationApi;
import com.eldercare.backend.care.api.dto.CareEventContext;
import com.eldercare.backend.care.api.events.TaskPlanningNeeded;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.status.persistence.CareLockStore;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareAbsenceTasks;
import com.eldercare.backend.care.internal.task.service.CareTaskNotices;
import com.eldercare.backend.care.internal.task.service.CareTaskService;
import com.eldercare.backend.care.internal.task.service.SharedCareTasks;
import com.eldercare.backend.common.data.ReminderMarkers;
import com.eldercare.backend.common.notification.*;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.roombedallocation.api.events.BedTransferred;
import java.time.Instant;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/**
 * FR-020, FR-017, BF-01 bước 15 nối BF-02 bước 1: Trưởng tầng được báo khi có người cao tuổi vào tầng của mình, tiêu đề
 * nói rõ lý do cần lập công việc (không chứa thông tin sức khỏe, 009 FR-034).
 */
class FR_020_TaskPlanningNoticeTest {
    private static final Instant AT = Instant.parse("2026-11-23T03:00:00Z");

    @Test
    void FR_020_titleTellsFloorManagerWhyTasksAreNeeded() {
        assertThat(title("ADMISSION_COMPLETED")).isEqualTo("Có người cao tuổi mới vào tầng, cần lập công việc");
        assertThat(title("FLOOR_TRANSFER")).isEqualTo("Có người cao tuổi chuyển đến tầng, cần lập công việc");
        assertThat(title("RETURN")).isEqualTo("Người cao tuổi đã quay lại, cần lập công việc");
        assertThat(title("ABSENCE_RECALCULATED")).isEqualTo("Khoảng vắng đã đính chính, cần xem lại công việc");
        assertThat(title("DAY_CARE_ARRIVAL"))
                .isEqualTo("Người bán trú có mặt ngoài lịch, xem có cần công việc phát sinh");
    }

    @Test
    void FR_020_noticeKeepsResidentAndSourceForOpeningPlanningScreen() {
        var request = request("ADMISSION_COMPLETED");
        assertThat(request.eventType()).isEqualTo("TaskPlanningNeeded");
        assertThat(request.residentId()).contains(9001L);
        assertThat(request.sourceRecord()).isEqualTo(new SourceRecord("M04.RESIDENT", "9001"));
        assertThat(request.shortContent()).isEqualTo(request.title());
        assertThat(request.content())
                .extracting(ContentPart::content)
                .containsExactly(request.title().orElseThrow());
    }

    @Test
    void FR_020_transferToAnotherFloorNotifiesNewFloorManager() {
        var events = mock(ApplicationEventPublisher.class);
        shared(events).onBedTransferred(new BedTransferred(9001, 1, 2, 10, 20, AT));
        var captor = org.mockito.ArgumentCaptor.forClass(Object.class);
        verify(events, atLeastOnce()).publishEvent(captor.capture());
        var notices = captor.getAllValues().stream()
                .filter(TaskPlanningNeeded.class::isInstance)
                .map(TaskPlanningNeeded.class::cast)
                .toList();
        assertThat(notices).hasSize(1);
        var notice = notices.getFirst();
        assertThat(notice.residentId()).isEqualTo(9001);
        assertThat(notice.reason()).isEqualTo("FLOOR_TRANSFER");
        assertThat(notice.context().floorAreaId()).contains(20L);
        assertThat(notice.context().recipients())
                .containsExactly(new RecipientGroup.FloorManager(20, new Substitution.Default()));
    }

    @Test
    void FR_020_transferInsideSameFloorSendsNoPlanningNotice() {
        var events = mock(ApplicationEventPublisher.class);
        shared(events).onBedTransferred(new BedTransferred(9001, 1, 2, 10, 10, AT));
        verify(events, never()).publishEvent(any(TaskPlanningNeeded.class));
    }

    private static SharedCareTasks shared(ApplicationEventPublisher events) {
        var tasks = mock(CareTaskStore.class);
        when(tasks.openOfResident(anyLong())).thenReturn(List.of());
        var markers = mock(ReminderMarkers.class);
        when(markers.mark(anyString(), anyString(), anyLong(), anyString(), any()))
                .thenReturn(true);
        var commands = mock(CareTaskService.class);
        var locks = mock(CareLockStore.class);
        var time = mock(BusinessTime.class);
        when(time.now()).thenReturn(AT);
        var absence = new CareAbsenceTasks(tasks, commands, locks, mock(BedAllocationApi.class), events, time);
        return new SharedCareTasks(
                tasks,
                commands,
                mock(CareTaskNotices.class),
                locks,
                mock(CareCatalogStore.class),
                markers,
                time,
                absence);
    }

    private static String title(String reason) {
        return request(reason).title().orElseThrow();
    }

    private static NotificationRequest request(String reason) {
        var context = new CareEventContext(
                "M04.RESIDENT",
                9001,
                reason,
                AT,
                Optional.of(10L),
                List.of(new RecipientGroup.FloorManager(10, new Substitution.Default())));
        var requests = new TaskPlanningNeeded(9001, reason, AT, context).notificationRequests();
        assertThat(requests).hasSize(1);
        return requests.getFirst();
    }
}
