package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
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
import com.eldercare.backend.common.time.BusinessTime;
import com.eldercare.backend.incident.api.dto.RecordIncidentCommand;
import com.eldercare.backend.incident.api.events.IncidentRecorded;
import com.eldercare.backend.incident.api.spi.AlertStaffPort;
import com.eldercare.backend.incident.internal.alert.domain.IncidentLevel;
import com.eldercare.backend.incident.internal.alert.domain.IncidentTypeView;
import com.eldercare.backend.incident.internal.alert.service.IncidentCatalogService;
import com.eldercare.backend.incident.internal.governance.service.MonitoringLock;
import com.eldercare.backend.incident.internal.incident.domain.FallMonitoringSetting;
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
    private final com.eldercare.backend.care.api.ActivityRegistrationAutoCancelApi autoCancel =
            mock(com.eldercare.backend.care.api.ActivityRegistrationAutoCancelApi.class);
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
            fallMonitoring,
            autoCancel);

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
                        new FallMonitoringSetting.Amount(4, "HOURS"), new FallMonitoringSetting.Amount(72, "HOURS")));
        when(parameters.read(ParameterCode.CFG_M05_06)).thenReturn(fall);
        when(incidents.insert(any())).thenReturn(41L);
        when(incidents.openEmergency(RESIDENT)).thenReturn(Optional.empty());
    }

    @Test
    void FR_043b_belongingsIncidentDoesNotAskReassessmentButLowFallDoes() {
        service.record(command(RESIDENT, 3, "MEDIUM", null, false), user);
        verify(events).publishEvent(new IncidentRecorded(41L, RESIDENT, "BELONGINGS_LOST", "MEDIUM", false));

        service.record(command(RESIDENT, 1, "LOW", "Chỉ trượt nhẹ, không đau", false), user);
        verify(events).publishEvent(new IncidentRecorded(41L, RESIDENT, "FALL", "LOW", true));
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

    /** BR-M05-18: sự cố lây nhiễm nghi ngờ luôn gắn một người cao tuổi. */
    @Test
    void BR_44_suspectedInfectionRequiresResident() {
        assertThatThrownBy(() -> service.record(command(null, 5, "MEDIUM", null, false), user))
                .isInstanceOf(BusinessException.class);
        verify(incidents, never()).insert(any());
    }

    private void allow(PermissionBasis basis) {
        when(permissions.hasPermission(eq(user), any(), any())).thenReturn(PermissionResult.allowed(basis));
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
