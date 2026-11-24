package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.care.api.SourceTaskReadApi;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.internal.alert.domain.HandoverItem;
import com.eldercare.backend.incident.internal.alert.persistence.HandoverItemStore;
import com.eldercare.backend.incident.internal.alert.service.AlertAssigneeTransfer;
import com.eldercare.backend.incident.internal.alert.service.DefaultAlertHandoverApi;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** FR-045b: nguồn bàn giao đánh dấu sự cố đang mở chưa có việc tiếp theo nào chưa đóng; cảnh báo không mang dấu. */
class FR_045b_HandoverNextStepTest {
    private static final Instant AT = Instant.parse("2026-11-23T10:00:00Z");

    private final HandoverItemStore items = mock(HandoverItemStore.class);
    private final SourceTaskReadApi tasks = mock(SourceTaskReadApi.class);

    private static HandoverItem item(String group, long id, String status) {
        return new HandoverItem(group, id, 101L, 7L, status, "MEDIUM", "Mô tả", AT, 5L, false, 0);
    }

    @Test
    void FR_045b_incidentWithoutOpenFollowUpIsMarked() {
        when(items.openByFloor(7))
                .thenReturn(List.of(
                        item("INCIDENT", 1, "MONITORING"),
                        item("INCIDENT", 2, "IN_PROGRESS"),
                        item("ALERT", 3, "IN_PROGRESS")));
        when(tasks.withOpenTask(any(), anyCollection())).thenReturn(Set.of(2L));
        var api = new DefaultAlertHandoverApi(
                items, mock(AlertAssigneeTransfer.class), mock(BusinessTime.class), null, tasks);

        var views = api.open(7, 1);

        assertThat(views)
                .filteredOn(view -> view.id() == 1)
                .singleElement()
                .satisfies(view -> assertThat(view.nextStepMissing()).isTrue());
        assertThat(views)
                .filteredOn(view -> view.id() == 2)
                .singleElement()
                .satisfies(view -> assertThat(view.nextStepMissing()).isFalse());
        assertThat(views)
                .filteredOn(view -> view.id() == 3)
                .singleElement()
                .satisfies(view -> assertThat(view.nextStepMissing()).isNull());
    }

    @Test
    void FR_045b_withoutTaskSourceNothingIsMarked() {
        when(items.openByFloor(7)).thenReturn(List.of(item("INCIDENT", 1, "MONITORING")));
        var api = new DefaultAlertHandoverApi(
                items, mock(AlertAssigneeTransfer.class), mock(BusinessTime.class), null, null);

        assertThat(api.open(7, 1))
                .singleElement()
                .satisfies(view -> assertThat(view.nextStepMissing()).isNull());
    }
}
