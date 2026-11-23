package com.eldercare.backend.administration.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.approval.api.dto.ApprovalRequestView;
import com.eldercare.backend.administration.approval.api.events.ApproverReminderDue;
import com.eldercare.backend.administration.approval.api.events.RequestSubmitted;
import com.eldercare.backend.common.notification.NotificationLevel;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

/**
 * Mức thông báo của yêu cầu phê duyệt theo bảng mức của 009 (dòng 000; test luồng BF-14 2026-11-23): yêu cầu mới Nhẹ,
 * quá CFG-M15-05 Trung bình. Trước đây để trống, 009 gắn dấu thiếu mức.
 */
class ApprovalNoticeLevelTest {

    private static final Instant AT = Instant.parse("2027-02-21T09:05:00Z");

    private static ApprovalRequestView request() {
        ApprovalRequestView view = mock(ApprovalRequestView.class, Answers.RETURNS_DEEP_STUBS);
        when(view.id()).thenReturn(7L);
        when(view.displayCode()).thenReturn("YC-2027-000007");
        when(view.type()).thenReturn("M12.UNCLAIMED_BELONGING_DISPOSAL");
        return view;
    }

    @Test
    void submittedIsLow_reminderIsMedium() {
        var submitted = new RequestSubmitted(request(), List.of(1L), Optional.empty(), AT).notificationRequests();
        assertThat(submitted.getFirst().level()).contains(NotificationLevel.LOW);

        var reminder = new ApproverReminderDue(request(), List.of(1L), AT).notificationRequests();
        assertThat(reminder.getFirst().level()).contains(NotificationLevel.MEDIUM);
    }
}
