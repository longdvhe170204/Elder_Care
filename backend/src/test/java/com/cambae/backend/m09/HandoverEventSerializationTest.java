package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.*;
import com.cambae.backend.m09.api.events.HandoverPrepared;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import tools.jackson.databind.json.JsonMapper;

/** Sự kiện lưu bởi Modulith phải đọc lại được sau khởi động, kể cả nhóm nhận lồng nhau. */
class HandoverEventSerializationTest {
    @ParameterizedTest
    @MethodSource("recipients")
    void FR_057_persistedNotificationRetainsRecipientTypeOnRestart(RecipientGroup group) {
        var at = Instant.parse("2035-04-01T08:00:00Z");
        var request = new NotificationRequest(
                "008",
                "HANDOVER_PREPARED",
                SourceGroup.OTHER,
                new SourceRecord("M09.HANDOVER", "1"),
                "test",
                Optional.of(NotificationLevel.MEDIUM),
                Optional.empty(),
                Optional.of(2L),
                List.of(group),
                List.of(new ContentPart(InformationType.GENERAL, "Cần xác nhận bàn giao")),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                false,
                at,
                Map.of(),
                false,
                Optional.empty(),
                Optional.empty());
        var original = new HandoverPrepared(1, at, List.of(request));
        var json = JsonMapper.builder().findAndAddModules().build();
        assertThat(json.readValue(json.writeValueAsString(original), HandoverPrepared.class))
                .isEqualTo(original);
    }

    static Stream<RecipientGroup> recipients() {
        var fallback = new Substitution.Default();
        return Stream.of(
                new RecipientGroup.Staff(1, "SHIFT_LEAD"),
                new RecipientGroup.FloorManager(2, fallback),
                new RecipientGroup.AssignedNurse(1, fallback),
                new RecipientGroup.AssignedStaff(
                        Optional.of(1L), Optional.empty(), Optional.of(Role.NURSING_STAFF), fallback),
                new RecipientGroup.OnShiftStaff(2, Set.of(Role.NURSING_STAFF), Set.of(), fallback),
                new RecipientGroup.ShiftLead(2, fallback),
                new RecipientGroup.OnDutyDoctor(fallback),
                new RecipientGroup.TripLeader(1, fallback),
                new RecipientGroup.DepartmentLead(1, fallback),
                new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER),
                new RecipientGroup.Approvers("M09.TEST", List.of(1L)),
                new RecipientGroup.FamilyMember(1, "TEST"),
                new RecipientGroup.PrimaryContact(1),
                new RecipientGroup.Representative(1),
                new RecipientGroup.FamilyWithRight(1, FamilyRight.VIEW_HEALTH),
                new RecipientGroup.Resident(1),
                new RecipientGroup.FloorManager(2, new Substitution.NoSubstitute()),
                new RecipientGroup.FloorManager(2, new Substitution.Designated(new RecipientGroup.Staff(1, "TEST"))));
    }
}
