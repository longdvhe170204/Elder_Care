package com.cambae.backend.m15.approval;

import static com.cambae.backend.m15.approval.ApprovalFixtures.MANAGER;
import static com.cambae.backend.m15.approval.ApprovalFixtures.MANAGER_2;
import static com.cambae.backend.m15.approval.ApprovalFixtures.OFFICE_B;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.Role;
import com.cambae.backend.m15.approval.api.ApproverReminderDue;
import com.cambae.backend.m15.approval.api.ManagerEscalationDue;
import com.cambae.backend.m15.approval.api.OverdueReminder.EscalationReason;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * US5 #4; FR-031a, BR-M15-09 (000 T087, T094; quickstart K6, K7; đồng hồ giả lập, mặc định CFG-M15-05 48 giờ,
 * CFG-M15-06 96 giờ): quá mốc nhắc thì nhắc người duyệt một lần; quá mốc báo thì báo Quản lý viện; không còn người
 * duyệt thì báo ngay ở mốc nhắc; người duyệt đều là Quản lý viện thì không báo; không bao giờ tự hủy.
 */
class PendingReminderIT extends ApprovalTestBase {

    private long submittedNow() throws Exception {
        return pending(time.today().plusDays(30));
    }

    private void advance(long id, Duration since) throws Exception {
        Instant submitted = Instant.parse(read(id).get("submittedAt").asString());
        clock.setTo(submitted.plus(since));
    }

    private long reminders(long id) {
        return events.stream(ApproverReminderDue.class)
                .filter(e -> e.request().id() == id)
                .count();
    }

    private long escalations(long id, EscalationReason reason) {
        return events.stream(ManagerEscalationDue.class)
                .filter(e -> e.request().id() == id && e.reason() == reason)
                .count();
    }

    @Test
    void US5_4_remindOnceAfterCfgM15_05_escalateAfterCfgM15_06_neverCancel() throws Exception {
        long id = submittedNow();

        advance(id, Duration.ofHours(47));
        runRemindJob();
        assertThat(reminders(id)).isZero();

        advance(id, Duration.ofHours(49));
        runRemindJob();
        runRemindJob();
        assertThat(reminders(id)).isEqualTo(1);
        assertThat(events.stream(ApproverReminderDue.class)
                        .filter(e -> e.request().id() == id))
                .singleElement()
                .satisfies(e -> assertThat(e.approvers())
                        .contains(MANAGER.accountId(), MANAGER_2.accountId(), OFFICE_B.accountId()));
        assertThat(lastAudit(id, "REQUEST_REMINDED", "JSON_UNQUOTE(JSON_EXTRACT(basis, '$.ruleCode'))"))
                .isEqualTo("CFG-M15-05");

        advance(id, Duration.ofHours(97));
        runRemindJob();
        runRemindJob();
        assertThat(escalations(id, EscalationReason.OVERDUE)).isEqualTo(1);
        assertThat(lastAudit(id, "REQUEST_ESCALATED", "JSON_UNQUOTE(JSON_EXTRACT(basis, '$.ruleCode'))"))
                .isEqualTo("CFG-M15-06");
        assertThat(read(id).get("status").asString()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void BR_M15_09_noApproverLeft_escalatesAtReminderMark() throws Exception {
        control.roleRestriction = Set.of(Role.DOCTOR);
        long id = submittedNow();

        advance(id, Duration.ofHours(49));
        runRemindJob();

        assertThat(reminders(id)).isZero();
        assertThat(escalations(id, EscalationReason.NO_APPROVER_LEFT)).isEqualTo(1);
        assertThat(read(id).get("status").asString()).isEqualTo("PENDING_APPROVAL");
    }

    @Test
    void FR_031a_approversAllManagers_noEscalationAtCfgM15_06() throws Exception {
        control.roleRestriction = Set.of(Role.NURSING_HOME_MANAGER);
        long id = submittedNow();

        advance(id, Duration.ofHours(97));
        runRemindJob();

        assertThat(reminders(id)).isEqualTo(1);
        assertThat(escalations(id, EscalationReason.OVERDUE)).isZero();
    }
}
