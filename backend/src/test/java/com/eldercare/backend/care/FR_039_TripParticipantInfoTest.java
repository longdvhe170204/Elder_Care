package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.FieldVisibilityApi;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedItem;
import com.eldercare.backend.care.api.dto.TripParticipantSources.CarriedMedication;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripDose;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripEmergencyCard;
import com.eldercare.backend.care.api.dto.TripParticipantSources.TripHealth;
import com.eldercare.backend.care.api.spi.TripHealthPort;
import com.eldercare.backend.care.api.spi.TripMedicationPreparationPort;
import com.eldercare.backend.care.internal.trip.domain.Trip;
import com.eldercare.backend.care.internal.trip.domain.TripDuties;
import com.eldercare.backend.care.internal.trip.domain.TripParticipant;
import com.eldercare.backend.care.internal.trip.domain.TripPlan;
import com.eldercare.backend.care.internal.trip.service.TripParticipantInfo;
import com.eldercare.backend.care.internal.trip.service.TripParticipantInfo.Availability;
import com.eldercare.backend.care.internal.trip.service.TripPreparationService.Detail;
import com.eldercare.backend.common.Role;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

/**
 * 014 FR-036, FR-038, FR-039 (TripDetail 6 phần, chủ dự án duyệt 2026-11-10): mỗi người tham gia có liều trong khoảng đi,
 * thuốc mang theo, sức khỏe đang mở, lưu ý chăm sóc, liên hệ chính, thẻ khẩn cấp; phần sức khỏe chỉ khi xem đủ nhóm sức
 * khỏe; số điện thoại liên hệ chỉ Trưởng tầng và trưởng đoàn; phân biệt không có dữ liệu, nguồn chưa kết nối, không quyền.
 */
class FR_039_TripParticipantInfoTest {

    private static final long TRIP = 7, RESIDENT = 21, LEADER = 5, COMPANION = 6;
    private static final Instant DEPART = Instant.parse("2026-11-12T01:00:00Z");
    private static final Instant RETURN = Instant.parse("2026-11-12T05:00:00Z");

    private final TripMedicationPreparationPort medication = mock(TripMedicationPreparationPort.class);
    private final TripHealthPort health = mock(TripHealthPort.class);
    private final FieldVisibilityApi visibility = mock(FieldVisibilityApi.class);
    private final CurrentUser user = mock(CurrentUser.class);

    private final Detail detail = new Detail(
            new Trip(TRIP, 3, 11L, new TripPlan("Chùa", DEPART, RETURN, "Xe viện", true), "SCHEDULED", 1),
            new TripDuties(LEADER, List.of(COMPANION)),
            List.of(new TripParticipant(91, RESIDENT, "ELIGIBLE", null)));

    {
        when(medication.dosesDuring(RESIDENT, DEPART, RETURN))
                .thenReturn(
                        List.of(new TripDose("10:2026-11-12T02:00:00Z", 10, "Amlodipin", "5 mg", DEPART, "UPCOMING")));
        when(medication.carried(RESIDENT, TRIP))
                .thenReturn(Optional.of(new CarriedMedication(
                        DEPART.minusSeconds(600),
                        "Trần Thu Hà",
                        List.of(new CarriedItem("Amlodipin", "5 mg", BigDecimal.ONE, "viên")),
                        null)));
        when(health.health(eq(RESIDENT), any())).thenReturn(new TripHealth(false, List.of(), List.of()));
        when(health.emergencyCard(eq(RESIDENT), any(), eq(true)))
                .thenReturn(new TripEmergencyCard(
                        false,
                        "Nguyễn Thị Lan",
                        LocalDate.of(1940, 1, 2),
                        "Phòng 203 / A",
                        "NOT_RECORDED",
                        false,
                        List.of("Penicillin"),
                        List.of("Amlodipin"),
                        List.of("Tăng huyết áp"),
                        "Đi chậm, cần người dìu",
                        "Nguyễn Văn Nam",
                        "0900000000",
                        List.of(),
                        DEPART));
    }

    private TripParticipantInfo service(boolean withHealthPort) {
        return new TripParticipantInfo(
                provider(medication), withHealthPort ? provider(health) : provider(null), visibility, user);
    }

    @SuppressWarnings("unchecked")
    private static <T> ObjectProvider<T> provider(T bean) {
        ObjectProvider<T> p = mock(ObjectProvider.class);
        when(p.getIfAvailable()).thenReturn(bean);
        return p;
    }

    private void viewer(Role role, long staffId, Visibility healthVisibility) {
        when(user.roles()).thenReturn(Set.of(role));
        when(user.staffId()).thenReturn(Optional.of(staffId));
        when(visibility.canView(eq(user), eq(InfoGroup.HEALTH), any())).thenReturn(healthVisibility);
    }

    @Test
    void FR_039_leaderWithFullHealth_seesAllSixPartsAndThePhone() {
        viewer(Role.NURSING_STAFF, LEADER, Visibility.FULL);
        var info = service(true).of(detail).get(RESIDENT);

        assertThat(info.medicationDoses().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(info.medicationDoses().data())
                .extracting(TripDose::medicationName)
                .containsExactly("Amlodipin");
        assertThat(info.carriedMedication().data().recipientName()).isEqualTo("Trần Thu Hà");
        assertThat(info.health().availability()).isEqualTo(Availability.AVAILABLE);
        assertThat(info.careNotes().data()).isEqualTo("Đi chậm, cần người dìu");
        assertThat(info.primaryContact().data().phoneNumber()).isEqualTo("0900000000");
        assertThat(info.emergencyCard().data().allergies()).containsExactly("Penicillin");
    }

    @Test
    void FR_039_companionWithoutHealth_getsIdentificationOnly_contactNameWithoutPhone() {
        viewer(Role.CAREGIVER, COMPANION, Visibility.NONE);
        var info = service(true).of(detail).get(RESIDENT);

        assertThat(info.medicationDoses().availability()).isEqualTo(Availability.NOT_PERMITTED);
        assertThat(info.medicationDoses().data()).isNull();
        assertThat(info.carriedMedication().availability()).isEqualTo(Availability.NOT_PERMITTED);
        assertThat(info.health().availability()).isEqualTo(Availability.NOT_PERMITTED);
        assertThat(info.careNotes().availability()).isEqualTo(Availability.NOT_PERMITTED);
        assertThat(info.primaryContact().data().fullName()).isEqualTo("Nguyễn Văn Nam");
        assertThat(info.primaryContact().data().phoneNumber()).isNull();
        var card = info.emergencyCard().data();
        assertThat(card.identificationOnly()).isTrue();
        assertThat(card.fullName()).isEqualTo("Nguyễn Thị Lan");
        assertThat(card.allergies()).isNull();
    }

    @Test
    void FR_039_floorManagerSeesPhone_missingSourceIsUnavailable_emptyIsNoData() {
        viewer(Role.FLOOR_MANAGER, 99, Visibility.FULL);
        when(medication.carried(anyLong(), anyLong())).thenReturn(Optional.empty());
        var info = service(false).of(detail).get(RESIDENT);

        assertThat(info.carriedMedication().availability()).isEqualTo(Availability.NO_DATA);
        assertThat(info.health().availability()).isEqualTo(Availability.SOURCE_UNAVAILABLE);
        assertThat(info.emergencyCard().availability()).isEqualTo(Availability.SOURCE_UNAVAILABLE);
        assertThat(info.primaryContact().availability()).isEqualTo(Availability.SOURCE_UNAVAILABLE);
    }
}
