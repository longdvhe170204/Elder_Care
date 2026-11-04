package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.PermissionApi;
import com.eldercare.backend.administration.account.api.dto.PermissionBasis;
import com.eldercare.backend.administration.account.api.dto.PermissionResult;
import com.eldercare.backend.administration.auditlog.api.AuditLogApi;
import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.parameter.api.dto.ParameterCode;
import com.eldercare.backend.administration.parameter.api.dto.ParameterValue;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.error.BusinessException;
import com.eldercare.backend.common.notification.NotificationLevel;
import com.eldercare.backend.common.notification.Q354Case;
import com.eldercare.backend.common.notification.RecipientGroup;
import com.eldercare.backend.common.notification.Substitution;
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.api.events.ContactListRequested;
import com.eldercare.backend.incident.api.events.FallIncidentRecorded;
import com.eldercare.backend.incident.api.events.IncidentNotificationCreated;
import com.eldercare.backend.incident.api.events.IncidentRecorded;
import com.eldercare.backend.incident.api.events.InfectionSuspicionChanged;
import com.eldercare.backend.incident.api.events.PostFallFollowUpRequested;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.incident.internal.incident.domain.FallMonitoringSetting;
import com.eldercare.backend.incident.internal.incident.domain.NewIncident;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

/** Ghi nhận sự cố (FR-040 → FR-043 a, b; FR-047 a; FR-047b): lát 1 của T035. */
class FR_040_IncidentRecordTest {
    private static final Instant NOW = Instant.parse("2026-11-04T01:00:00Z");
    private static final long RESIDENT = 101L;
    private static final long FLOOR = 7L;

    private final IncidentStore incidents = mock(IncidentStore.class);
    private final IncidentCatalogService catalog = mock(IncidentCatalogService.class);
    private final PermissionApi permissions = mock(PermissionApi.class);
    private final AuditLogApi audit = mock(AuditLogApi.class);
    private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
    private final BusinessTime time = mock(BusinessTime.class);
    private final ParameterApi parameters = mock(ParameterApi.class);
    private final MonitoringLock monitoringLock = mock(MonitoringLock.class);
    private final AlertStaffPort staff = mock(AlertStaffPort.class);
    private final ResidentApi residents = mock(ResidentApi.class);
    private final ResidentLocationApi locations = mock(ResidentLocationApi.class);
    private final FallMonitoringStore fallMonitoring = mock(FallMonitoringStore.class);
    private final CurrentUser user = mock(CurrentUser.class);
    private final IncidentRecordService service = new IncidentRecordService(
            incidents,
            catalog,
            permissions,
            audit,
            events,
            time,
            parameters,
            monitoringLock,
            staff,
            residents,
            locations,
            fallMonitoring);

    @BeforeEach
    void defaults() {
        when(time.now()).thenReturn(NOW);
        when(user.accountId()).thenReturn(900L);
        when(user.roles()).thenReturn(Set.of(Role.NURSING_STAFF));
        allow(PermissionBasis.ROLE);
        when(catalog.incidentTypes())
                .thenReturn(List.of(
                        type(1, "FALL", IncidentLevel.CRITICAL, false),
                        type(2, "OTHER", IncidentLevel.MEDIUM, false),
                        type(3, "BELONGINGS_LOST", IncidentLevel.MEDIUM, false),
                        type(4, "FACILITY", IncidentLevel.LOW, true),
                        type(5, "SUSPECTED_INFECTION", IncidentLevel.MEDIUM, false)));
        when(locations.current(RESIDENT))
                .thenReturn(Optional.of(new LocationView(null, null, FLOOR, "Tầng 1", 3L, "P101", 9L, "G1")));
        when(staff.onDutyDoctors(NOW)).thenReturn(List.of(55L));
        var medium = mock(ParameterValue.class);
        when(medium.asDuration()).thenReturn(Duration.ofMinutes(30));
        when(parameters.read(ParameterCode.CFG_M05_01)).thenReturn(medium);
        var handoverLead = mock(ParameterValue.class);
        when(handoverLead.asDuration()).thenReturn(Duration.ofMinutes(30));
        when(parameters.read(ParameterCode.CFG_M09_04)).thenReturn(handoverLead);
        when(staff.shiftAt(FLOOR, NOW))
                .thenReturn(Optional.of(new com.eldercare.backend.incident.api.dto.AlertShiftView(
                        1L, FLOOR, NOW.minusSeconds(3600), NOW.plusSeconds(7200))));
        var fall = mock(ParameterValue.class);
        when(fall.asTable(FallMonitoringSetting.class))
                .thenReturn(new FallMonitoringSetting(
                        new FallMonitoringSetting.Amount(4, "HOURS"),
                        new FallMonitoringSetting.Amount(72, "HOURS")));
        when(parameters.read(ParameterCode.CFG_M05_06)).thenReturn(fall);
        when(incidents.insert(any())).thenReturn(41L);
        when(incidents.openEmergency(RESIDENT)).thenReturn(Optional.empty());
    }

    @Test
    void FR_043_mediumIncidentNotifiesNurseAndShiftLeadAndAsksReassessment() {
        var result = service.record(command(RESIDENT, 2, "MEDIUM", null, false), user);

        assertThat(result.incidentId()).isEqualTo(41L);
        assertThat(result.identificationOnly()).isFalse();
        var row = inserted();
        assertThat(row.level()).isEqualTo(IncidentLevel.MEDIUM);
        assertThat(row.floorAreaId()).isEqualTo(FLOOR);
        assertThat(row.acknowledgementDeadline()).isEqualTo(NOW.plus(Duration.ofMinutes(30)));
        verify(monitoringLock).lock(RESIDENT);
        verify(events).publishEvent(new IncidentRecorded(41L, RESIDENT, "OTHER", "MEDIUM", true));
        var notice = notice();
        assertThat(notice.notificationRequests()).hasSize(1);
        var request = notice.notificationRequests().getFirst();
        assertThat(request.level()).contains(NotificationLevel.MEDIUM);
        assertThat(request.recipientGroups())
                .containsExactlyInAnyOrder(
                        new RecipientGroup.AssignedNurse(RESIDENT, new Substitution.Default()),
                        new RecipientGroup.ShiftLead(FLOOR, new Substitution.Default()));
    }

    @Test
    void FR_043b_belongingsIncidentDoesNotAskReassessmentButLowFallDoes() {
        service.record(command(RESIDENT, 3, "MEDIUM", null, false), user);
        verify(events).publishEvent(new IncidentRecorded(41L, RESIDENT, "BELONGINGS_LOST", "MEDIUM", false));

        service.record(command(RESIDENT, 1, "LOW", "Chỉ trượt nhẹ, không đau", false), user);
        verify(events).publishEvent(new IncidentRecorded(41L, RESIDENT, "FALL", "LOW", true));
    }

    @Test
    void FR_042_lowerThanDefaultLevelRequiresReason() {
        assertThatThrownBy(() -> service.record(command(RESIDENT, 1, "MEDIUM", " ", false), user))
                .isInstanceOf(BusinessException.class);
        verify(incidents, never()).insert(any());
    }

    @Test
    void FR_041_openEmergencyIncidentBlocksUnlessCreateDespiteOpen() {
        when(incidents.openEmergency(RESIDENT)).thenReturn(Optional.of(30L));

        assertThatThrownBy(() -> service.record(command(RESIDENT, 2, "MEDIUM", null, false), user))
                .isInstanceOfSatisfying(BusinessException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("OPEN_EMERGENCY_INCIDENT_EXISTS");
                    assertThat(e.properties()).containsEntry("incidentId", 30L);
                });
        verify(incidents, never()).insert(any());

        service.record(command(RESIDENT, 2, "MEDIUM", null, true), user);
        verify(incidents).insert(any());
    }

    @Test
    void FR_041_residentInFinalStatusCannotGetIncident() {
        when(residents.isFinalStatus(RESIDENT)).thenReturn(true);

        assertThatThrownBy(() -> service.record(command(RESIDENT, 2, "MEDIUM", null, false), user))
                .isInstanceOf(BusinessException.class);
        verify(incidents, never()).insert(any());
    }

    @Test
    void FR_047_criticalIncidentNotifiesTeamManagerAndPrimaryContactWithoutDeadline() {
        service.record(command(RESIDENT, 1, "CRITICAL", null, false), user);

        var row = inserted();
        assertThat(row.acknowledgementDeadline()).isNull();
        assertThat(row.noOnDutyDoctor()).isFalse();
        var requests = notice().notificationRequests();
        assertThat(requests).hasSize(2);
        var staffRequest = requests.get(0);
        assertThat(staffRequest.level()).contains(NotificationLevel.URGENT);
        assertThat(staffRequest.recipientGroups())
                .containsExactlyInAnyOrder(
                        new RecipientGroup.OnDutyDoctor(new Substitution.Default()),
                        new RecipientGroup.FloorManager(FLOOR, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER),
                        new RecipientGroup.AssignedNurse(RESIDENT, new Substitution.Default()),
                        new RecipientGroup.ShiftLead(FLOOR, new Substitution.Default()));
        var familyRequest = requests.get(1);
        assertThat(familyRequest.recipientGroups()).containsExactly(new RecipientGroup.PrimaryContact(RESIDENT));
        assertThat(familyRequest.q354Case()).contains(Q354Case.PRIMARY_CONTACT_EMERGENCY);
        assertThat(familyRequest.informationSubject()).isPresent();
    }

    @Test
    void FR_047b_noOnDutyDoctorNotifiesAllDoctorsAndManagerAndFlagsIncident() {
        when(staff.onDutyDoctors(NOW)).thenReturn(List.of());

        service.record(command(RESIDENT, 1, "CRITICAL", null, false), user);

        assertThat(inserted().noOnDutyDoctor()).isTrue();
        var groups = notice().notificationRequests().get(0).recipientGroups();
        assertThat(groups)
                .contains(
                        new RecipientGroup.FacilityRole(Role.DOCTOR),
                        new RecipientGroup.FacilityRole(Role.NURSING_HOME_MANAGER))
                .doesNotContain(new RecipientGroup.OnDutyDoctor(new Substitution.Default()));
    }

    @Test
    void FR_040_facilityIncidentUsesShiftLeadAndFloorManagerOfLocation() {
        service.record(command(null, 4, "MEDIUM", null, false), user);

        var groups = notice().notificationRequests().getFirst().recipientGroups();
        assertThat(groups)
                .containsExactlyInAnyOrder(
                        new RecipientGroup.ShiftLead(FLOOR, new Substitution.Default()),
                        new RecipientGroup.FloorManager(FLOOR, new Substitution.Default()));
        verify(events, never()).publishEvent(any(IncidentRecorded.class));
    }

    @Test
    void FR_040_residentRequiredExceptFacilityAndFacilityNeedsLocation() {
        assertThatThrownBy(() -> service.record(command(null, 2, "MEDIUM", null, false), user))
                .isInstanceOf(BusinessException.class);
        var noFloor = new RecordIncidentCommand(
                null, 4, null, null, NOW, null, null, null, "Vỡ ống nước", "LOW", null, null, false);
        assertThatThrownBy(() -> service.record(noFloor, user)).isInstanceOf(BusinessException.class);
        verify(incidents, never()).insert(any());
    }

    @Test
    void FR_041_outsideScopeRecorderOnlyGetsIdentification() {
        allow(PermissionBasis.OUT_OF_SCOPE);

        var result = service.record(command(RESIDENT, 2, "MEDIUM", null, false), user);

        assertThat(result.identificationOnly()).isTrue();
        assertThat(inserted().outOfScope()).isTrue();
    }

    @Test
    void FR_051_fallOpensMonitoringWindowFromAnchorAndAsksFollowUp() {
        // CHK013: ngã 02:00, ghi nhận 09:00, khoảng 4 giờ → mốc 05:00, kết thúc mốc + 72 giờ.
        var recordedAt = Instant.parse("2026-11-04T09:00:00Z");
        when(time.now()).thenReturn(recordedAt);
        when(staff.onDutyDoctors(recordedAt)).thenReturn(List.of(55L));
        var cmd = new RecordIncidentCommand(
                RESIDENT,
                1,
                "CARE",
                Instant.parse("2026-11-04T02:00:00Z"),
                recordedAt,
                null,
                "Phòng 101",
                null,
                "Ngã cạnh giường",
                "CRITICAL",
                null,
                null,
                false);

        service.record(cmd, user);

        var anchor = Instant.parse("2026-11-04T05:00:00Z");
        var end = anchor.plus(Duration.ofHours(72));
        verify(fallMonitoring).start(RESIDENT, 41L, anchor, end, Duration.ofHours(4), 900L);
        verify(events).publishEvent(new FallIncidentRecorded(RESIDENT, 41L));
        var followUp = published(PostFallFollowUpRequested.class);
        assertThat(followUp.startsAt()).isEqualTo(anchor);
        assertThat(followUp.endsAt()).isEqualTo(end);
        assertThat(followUp.measurementInterval()).isEqualTo(Duration.ofHours(4));
        assertThat(followUp.notificationRequests().getFirst().recipientGroups())
                .containsExactly(new RecipientGroup.ShiftLead(FLOOR, new Substitution.Default()));
    }

    @Test
    void FR_051_fallWithoutOccurrenceTimeAnchorsAtRecordingTime() {
        service.record(command(RESIDENT, 1, "CRITICAL", null, false), user);

        verify(fallMonitoring).start(RESIDENT, 41L, NOW, NOW.plus(Duration.ofHours(72)), Duration.ofHours(4), 900L);
    }

    @Test
    void FR_060_suspectedInfectionAsksNurseAndDoctorsForContactList() {
        service.record(command(RESIDENT, 5, "MEDIUM", null, false), user);

        var request = published(ContactListRequested.class);
        assertThat(request.incidentId()).isEqualTo(41L);
        assertThat(request.notificationRequests().getFirst().recipientGroups())
                .containsExactlyInAnyOrder(
                        new RecipientGroup.AssignedNurse(RESIDENT, new Substitution.Default()),
                        new RecipientGroup.FacilityRole(Role.DOCTOR));
        verify(events).publishEvent(new InfectionSuspicionChanged(RESIDENT, 41L, true, NOW));
        verify(fallMonitoring, never()).start(anyLong(), anyLong(), any(), any(), any(), anyLong());
    }

    private <T> T published(Class<T> type) {
        var captor = ArgumentCaptor.forClass(Object.class);
        verify(events, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        var found = captor.getAllValues().stream()
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
        assertThat(found).hasSize(1);
        return found.getFirst();
    }

    private void allow(PermissionBasis basis) {
        when(permissions.hasPermission(eq(user), any(), any())).thenReturn(PermissionResult.allowed(basis));
    }

    private NewIncident inserted() {
        var captor = ArgumentCaptor.forClass(NewIncident.class);
        verify(incidents).insert(captor.capture());
        return captor.getValue();
    }

    private IncidentNotificationCreated notice() {
        var captor = ArgumentCaptor.forClass(Object.class);
        verify(events, org.mockito.Mockito.atLeastOnce()).publishEvent(captor.capture());
        var notices = captor.getAllValues().stream()
                .filter(IncidentNotificationCreated.class::isInstance)
                .map(IncidentNotificationCreated.class::cast)
                .toList();
        assertThat(notices).hasSize(1);
        return notices.getFirst();
    }

    private static IncidentTypeView type(long id, String code, IncidentLevel level, boolean allowsNoResident) {
        return new IncidentTypeView(id, code, code, level, false, allowsNoResident, true, 0);
    }

    private static RecordIncidentCommand command(
            Long residentId, long typeId, String level, String lowerLevelReason, boolean createDespiteOpen) {
        return new RecordIncidentCommand(
                residentId,
                typeId,
                null,
                null,
                NOW,
                residentId == null ? FLOOR : null,
                "Phòng 101",
                null,
                "Mô tả sự cố",
                level,
                lowerLevelReason,
                null,
                createDespiteOpen);
    }
}
