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

    @Test
    void FR_051_notificationLevelsMatchTheApprovedMatrix() throws Exception {
        var at = java.time.Instant.parse("2026-10-25T00:00:00Z");
        var context = new com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext(
                at,
                1L,
                List.of(new com.eldercare.backend.common.notification.RecipientGroup.FacilityRole(
                        com.eldercare.backend.common.Role.NURSING_HOME_MANAGER)));
        var levels = java.util.Map.ofEntries(
                java.util.Map.entry("AllocationReminder", "LOW"),
                java.util.Map.entry("BedReadinessReminder", "LOW"),
                java.util.Map.entry("DoctorAssignedToAllocation", "LOW"),
                java.util.Map.entry("ReturnedToIsolatedRoom", "URGENT"),
                java.util.Map.entry("DifferentPriceBedTransfer", "MEDIUM"),
                java.util.Map.entry("AllocationNoLongerSuitable", "MEDIUM"),
                java.util.Map.entry("FutureAllocationPastStart", "MEDIUM"),
                java.util.Map.entry("FutureAllocationCancelledForDamagedBed", "MEDIUM"),
                java.util.Map.entry("ScheduledTransferWaitingForBed", "MEDIUM"),
                java.util.Map.entry("FormerOccupantListed", "MEDIUM"),
                java.util.Map.entry("DelayedScheduledWork", "MEDIUM"),
                java.util.Map.entry("DayCareOverCapacity", "MEDIUM"));
        for (var entry : levels.entrySet()) {
            var type = Class.forName("com.eldercare.backend.roombedallocation.api.events." + entry.getKey());
            var components = type.getRecordComponents();
            Object[] args = new Object[components.length];
            Class<?>[] types = new Class<?>[components.length];
            for (int n = 0; n < components.length; n++) {
                var field = components[n].getType();
                types[n] = field;
                if (field == long.class || field == Long.class) args[n] = 1L;
                else if (field == int.class) args[n] = 1;
                else if (field == String.class) args[n] = "TEST";
                else if (field == java.time.Instant.class) args[n] = at;
                else if (field == List.class) args[n] = List.of();
                else if (field == com.eldercare.backend.roombedallocation.api.dto.BedNotificationContext.class)
                    args[n] = context;
                else throw new IllegalStateException("Cần dữ liệu test cho " + field.getName());
            }
            var event = (HasNotificationRequests) type.getConstructor(types).newInstance(args);
            assertThat(event.notificationRequests())
                    .as(entry.getKey())
                    .isNotEmpty()
                    .allSatisfy(request -> assertThat(request.level())
                            .contains(com.eldercare.backend.common.notification.NotificationLevel.valueOf(
                                    entry.getValue())));
        }
    }
}
