package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.notification.*;
import com.cambae.backend.m09.staff.internal.service.StaffNotifications;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class StaffNotificationsTest {
    @Test
    void FR_004_missingShiftLeadNotifiesBothFloorManagerAndFacilityManager() {
        var request = new StaffNotifications()
                .missingLeadInFloors(1L, java.util.List.of(7L), Instant.parse("2026-10-25T00:00:00Z"))
                .getFirst();
        assertThat(request.recipientGroups())
                .contains(
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER),
                        new RecipientGroup.FloorManager(7L, new Substitution.Default()));
        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
    }
}
