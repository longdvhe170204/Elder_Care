package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.incident.api.dto.SosTripView;
import com.eldercare.backend.resident.api.dto.ResidentStatus;
import com.eldercare.backend.support.TestUsers;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

/**
 * FR-047e, US12 #6 (T069): Hoạt động bên ngoài thì thêm trưởng đoàn, người đi cùng qua {@code SosTripPort}; trạng thái
 * xét lúc nhận lệnh; không có chuyến thì chỉ nhóm FR-047a. Khi không có bean (014 chưa cài) {@code SosService} nhận
 * {@code Optional.empty()} như ca không có chuyến ở đây.
 */
class FR_047e_OffsiteSosIT extends SosTestBase {
    static final long TRIP = 7_001;

    @Test
    void FR_047e_offSiteAddsTripLeaderAndCompanions() throws Exception {
        long companion = TestUsers.nextId();
        when(residents.status(resident)).thenReturn(ResidentStatus.OFF_SITE_ACTIVITY);
        when(trips.ongoing(resident)).thenReturn(Optional.of(new SosTripView(TRIP, List.of(companion))));

        press("k1").andExpect(status().isCreated());

        assertThat(createdGroups())
                .contains(
                        new RecipientGroup.TripLeader(TRIP, new Substitution.Default()),
                        new RecipientGroup.Staff(companion, "SOS_TRIP_COMPANION"),
                        new RecipientGroup.AssignedNurse(resident, new Substitution.Default()),
                        new RecipientGroup.OnDutyDoctor(new Substitution.Default()));
    }

    @Test
    void FR_047e_statusCheckedWhenPressReceived() throws Exception {
        when(trips.ongoing(resident)).thenReturn(Optional.of(new SosTripView(TRIP, List.of(TestUsers.nextId()))));

        press("k1").andExpect(status().isCreated());

        assertThat(createdGroups())
                .isNotEmpty()
                .noneMatch(
                        group -> group instanceof RecipientGroup.TripLeader || group instanceof RecipientGroup.Staff);
    }

    @Test
    void FR_047e_noOngoingTripOnlyFr047aGroups() throws Exception {
        when(residents.status(resident)).thenReturn(ResidentStatus.OFF_SITE_ACTIVITY);
        when(trips.ongoing(resident)).thenReturn(Optional.empty());

        press("k1").andExpect(status().isCreated());

        assertThat(createdGroups())
                .contains(new RecipientGroup.AssignedNurse(resident, new Substitution.Default()))
                .noneMatch(group -> group instanceof RecipientGroup.TripLeader);
    }
}
