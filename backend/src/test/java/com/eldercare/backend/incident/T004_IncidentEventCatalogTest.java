package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import org.junit.jupiter.api.Test;

/** Mục 1.12–1.13: kiểu công khai phải tồn tại để consumer dùng đúng một hợp đồng. */
class T004_IncidentEventCatalogTest {
    @Test
    void FR_083_allApprovedPublicEventsExistAndNotificationEventsImplementTheEnvelope() throws Exception {
        for (String name : new String[] {
            "AlertCreated",
            "AlertLevelRaised",
            "AlertEscalated",
            "MaximumEscalationReminder",
            "AlertRepeated",
            "AlertSourceCorrected",
            "AssigneeChanged",
            "IncidentNotificationCreated",
            "IncidentAssigned",
            "EmergencyIncidentActivated",
            "PostFallFollowUpRequested",
            "ContactListRequested",
            "ContactListReviewRequested",
            "ContactTemperatureCheckRequested",
            "ContactNotificationRequested",
            "ContactAdded",
            "CriticalFlagRecorded",
            "WishReconfirmationRequested",
            "TemporaryCriticalFlagConfirmationRequested",
            "TemporaryHomeLeaveRequested",
            "EffectiveImpactsRemain",
            "IsolationCreated",
            "IsolationLifted"
        }) {
            assertThat(Class.forName("com.eldercare.backend.incident.api.events." + name))
                    .isAssignableTo(HasNotificationRequests.class);
        }
        for (String name : new String[] {
            "IncidentRecorded",
            "FallIncidentRecorded",
            "ContactAdded",
            "CallReminderStopRequested",
            "WishReconfirmationCompleted",
            "PalliativeCareChoiceRecorded",
            "WeightLossDetected",
            "InfectionSuspicionChanged",
            "AlertRequested",
            "AlertSourceCloseRequested",
            "AlertProgressionRequested"
        }) {
            assertThat(Class.forName("com.eldercare.backend.incident.api.events." + name)
                            .isRecord())
                    .isTrue();
        }
    }
}
