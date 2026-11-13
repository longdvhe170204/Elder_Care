package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import java.util.List;
import org.junit.jupiter.api.Test;

/** T005: đủ sự kiện hợp đồng; chỉ các sự kiện được giao cho 009 mới tạo yêu cầu thông báo. */
class FR_024_BedEventsTest {
    @Test
    void FR_040_dayCareNotificationKeyIsBoundedAndIndependentOfSessionOrder() {
        var recipients = List.<com.eldercare.backend.common.notification.RecipientGroup>of(
                new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(
                        com.eldercare.backend.common.Role.NURSING_HOME_MANAGER));
        var context = new com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext(
                java.time.Instant.parse("2026-10-23T01:00:00Z"), null, recipients);
        var sessions = java.util.stream.LongStream.rangeClosed(1, 20)
                .mapToObj(id -> new com.eldercare.backend.roombedallocation.api.dto.SessionDate(
                        java.time.LocalDate.of(2026, 10, 23), id))
                .toList();
        var first = new com.eldercare.backend.roombedallocation.api.events.DayCareOverCapacity(sessions, context)
                .notificationRequests()
                .getFirst();
        var reversed = new com.eldercare.backend.roombedallocation.api.events.DayCareOverCapacity(
                        sessions.reversed(), context)
                .notificationRequests()
                .getFirst();
        assertThat(first.eventKey().length()).isLessThanOrEqualTo(191);
        assertThat(first.eventKey()).isEqualTo(reversed.eventKey());
    }

    @Test
    void FR_024_isolationReturnCarriesUrgencyRecipientsAndStableReplayKey() throws Exception {
        String contextName = "com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext";
        assertThat(getClass().getClassLoader().getResource(contextName.replace('.', '/') + ".class"))
                .isNotNull();
        Class<?> contextType = Class.forName(contextName);
        java.time.Instant at = java.time.Instant.parse("2026-10-23T01:00:00Z");
        var fallback = new com.eldercare.backend.common.notification.Substitution.Default();
        var recipients = List.of(
                new com.eldercare.backend.common.notification.RecipientGroup.OnDutyDoctor(fallback),
                new com.eldercare.backend.common.notification.RecipientGroup.FloorManager(1, fallback));
        Object context = contextType
                .getConstructor(java.time.Instant.class, Long.class, List.class)
                .newInstance(at, 1L, recipients);
        HasNotificationRequests event = (HasNotificationRequests)
                Class.forName("com.eldercare.backend.roombedallocation.api.events.ReturnedToIsolatedRoom")
                        .getConstructor(long.class, long.class, long.class, contextType)
                        .newInstance(2L, 3L, 4L, context);
        var request = event.notificationRequests().getFirst();
        assertThat(request.level()).contains(com.eldercare.backend.common.notification.NotificationLevel.URGENT);
        assertThat(request.recipientGroups()).containsExactlyElementsOf(recipients);
        assertThat(request.sourceModule()).isEqualTo("M03");
        assertThat(request.residentId()).contains(3L);
        assertThat(request.eventKey())
                .isEqualTo(event.notificationRequests().getFirst().eventKey());
    }

    @Test
    void FR_024_eventDeclarationsMatchTheModuleContract() throws Exception {
        List<String> notices = List.of(
                "DifferentPriceBedTransfer",
                "DoctorAssignedToAllocation",
                "ReturnedToIsolatedRoom",
                "AllocationNoLongerSuitable",
                "FutureAllocationPastStart",
                "FutureAllocationCancelledForDamagedBed",
                "ScheduledTransferWaitingForBed",
                "AllocationReminder",
                "BedReadinessReminder",
                "FormerOccupantListed",
                "DelayedScheduledWork",
                "DayCareOverCapacity");
        List<String> plain = List.of(
                "BedBecameEmpty",
                "BedAllocationStarted",
                "BedAllocationClosed",
                "BedTransferred",
                "AbsentResidentHoldExpired",
                "RoomIsolated",
                "RoomIsolationLifted",
                "BedAllocationCorrected");
        for (String name :
                java.util.stream.Stream.concat(notices.stream(), plain.stream()).toList()) {
            String full = "com.eldercare.backend.roombedallocation.api.events." + name;
            assertThat(getClass().getClassLoader().getResource(full.replace('.', '/') + ".class"))
                    .as(name)
                    .isNotNull();
            Class<?> event = Class.forName(full);
            assertThat(event.isRecord()).isTrue();
            assertThat(HasNotificationRequests.class.isAssignableFrom(event))
                    .as(name)
                    .isEqualTo(notices.contains(name));
        }
    }
}
