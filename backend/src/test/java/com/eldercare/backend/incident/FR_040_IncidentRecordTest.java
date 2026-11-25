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
import com.eldercare.backend.care.api.CarePlanReviewApi;
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
import com.eldercare.backend.incident.internal.incident.domain.IncidentState;
import com.eldercare.backend.incident.internal.incident.persistence.FallMonitoringStore;
import com.eldercare.backend.incident.internal.incident.persistence.IncidentStore;
import com.eldercare.backend.incident.internal.incident.service.IncidentRecordService;
import com.eldercare.backend.resident.api.ResidentApi;
import com.eldercare.backend.roombedallocation.api.ResidentLocationApi;
import com.eldercare.backend.roombedallocation.api.dto.LocationView;
import com.eldercare.backend.support.fake.care.FakeM04ApiFixture;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
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
    private final FakeM04ApiFixture care = new FakeM04ApiFixture();
    private final AtomicInteger reviewCalls = new AtomicInteger();
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
            autoCancel,
            care.api(CarePlanReviewApi.class));

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
        care.reply(CarePlanReviewApi.class, "fallIncidentRecorded", List.of(RESIDENT, 41L), () -> {
            reviewCalls.incrementAndGet();
            return null;
        });
    }

    /** BR-M04-20, 005 FR-011: lệnh ghi sự cố Ngã gọi 005 báo xem xét kế hoạch, sau khi đã ghi sự cố, khoảng theo dõi. */
    @Test
    void BR_M04_20_fallIncidentAsksCarePlanReviewInSameCommand() {
        service.record(command(RESIDENT, 1, "LOW", "Chỉ trượt nhẹ, không đau", false), user);

        assertThat(reviewCalls).hasValue(1);
        verify(fallMonitoring).start(eq(RESIDENT), eq(41L), any(), any(), any(), eq(900L));
    }

    /** 005 lỗi thì lỗi ra khỏi lệnh {@code @Transactional} của 007, nên sự cố, khoảng theo dõi đã ghi bị rollback. */
    @Test
    void BR_M04_20_carePlanReviewFailureFailsTheFallCommand() {
        care.reset();

        assertThatThrownBy(() -> service.record(command(RESIDENT, 1, "LOW", "Chỉ trượt nhẹ, không đau", false), user))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CarePlanReviewApi.fallIncidentRecorded");
        verify(incidents).insert(any());
    }

    @Test
    void BR_M04_20_nonFallIncidentDoesNotAskCarePlanReview() {
        care.reset();

        service.record(command(RESIDENT, 2, "MEDIUM", null, false), user);

        assertThat(reviewCalls).hasValue(0);
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

    /** BF-06: ghi nhận sau không có hạn tiếp nhận (không leo thang), không chạy quy trình khẩn cấp, chỉ báo thông tin. */
    @Test
    void BF_06_recordedLaterHasNoDeadlineNoEmergencyAndOnlyInformationNotice() {
        var informed = new com.eldercare.backend.incident.api.dto.FamilyNotice(
                "INFORMED", NOW.minusSeconds(600), "PHONE", "Đã gọi con gái");
        service.record(recordedLater(RESIDENT, 1, "CRITICAL", informed), user);

        var inserted = org.mockito.ArgumentCaptor.forClass(
                com.eldercare.backend.incident.internal.incident.domain.NewIncident.class);
        verify(incidents).insert(inserted.capture());
        assertThat(inserted.getValue().acknowledgementDeadline()).isNull();
        verify(incidents)
                .saveFamilyNotice(41L, true, "INFORMED", 900L, NOW.minusSeconds(600), "PHONE", "Đã gọi con gái");
        verify(events, never())
                .publishEvent(org.mockito.ArgumentMatchers.any(
                        com.eldercare.backend.incident.internal.incident.domain.EmergencyActivationRequested.class));
        verify(events)
                .publishEvent(org.mockito.ArgumentMatchers.argThat((Object e) ->
                        e instanceof com.eldercare.backend.incident.api.events.IncidentNotificationCreated n
                                && n.level().equals("LOW")
                                && n.eventKey().endsWith(":RECORDED_LATER")
                                && n.notificationRequests().size() == 1));
    }

    /** Ghi nhận sau mà chưa báo người nhà: gửi thêm thông báo thông tin cho người liên hệ chính. */
    @Test
    void BF_06_recordedLaterNotYetInformedAlsoNotifiesPrimaryContact() {
        var notYet = new com.eldercare.backend.incident.api.dto.FamilyNotice("NOT_YET", null, null, null);
        service.record(recordedLater(RESIDENT, 2, "MEDIUM", notYet), user);

        verify(incidents).saveFamilyNotice(41L, true, "NOT_YET", null, null, null, null);
        verify(events)
                .publishEvent(org.mockito.ArgumentMatchers.argThat((Object e) ->
                        e instanceof com.eldercare.backend.incident.api.events.IncidentNotificationCreated n
                                && n.notificationRequests().size() == 2));
    }

    @Test
    void BF_06_recordedLaterMustSayWhetherFamilyWasInformed() {
        assertThatThrownBy(() -> service.record(recordedLater(RESIDENT, 2, "MEDIUM", null), user))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        verify(incidents, never()).insert(any());
    }

    @Test
    void BF_06_informedNeedsAValidMethod() {
        var bad = new com.eldercare.backend.incident.api.dto.FamilyNotice("INFORMED", NOW, "EMAIL", null);
        assertThatThrownBy(() -> service.record(recordedLater(RESIDENT, 2, "MEDIUM", bad), user))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
    }

    /** Ghi thường (mọi nơi gọi cũ): vẫn có hạn tiếp nhận và quy trình khẩn cấp như trước, không ghi cột mới. */
    @Test
    void BF_06_normalRecordKeepsAcknowledgementDeadline() {
        service.record(command(RESIDENT, 2, "MEDIUM", null, false), user);

        var inserted = org.mockito.ArgumentCaptor.forClass(
                com.eldercare.backend.incident.internal.incident.domain.NewIncident.class);
        verify(incidents).insert(inserted.capture());
        assertThat(inserted.getValue().acknowledgementDeadline()).isNotNull();
        verify(incidents, never())
                .saveFamilyNotice(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyBoolean(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any(),
                        org.mockito.ArgumentMatchers.any());
    }

    /** BF-06: Bác sĩ, Điều dưỡng ghi sự cố đang xảy ra (từ web) thì tự tiếp nhận; lệnh cũ của module khác thì không. */
    @Test
    void BF_06_nurseRecordingFromWebAutoReceives_butLegacyCommandDoesNot() {
        when(incidents.lock(41L))
                .thenReturn(
                        Optional.of(new IncidentState(41L, RESIDENT, FLOOR, "NEW", IncidentLevel.MEDIUM, false, 0)));

        service.record(command(RESIDENT, 2, "MEDIUM", null, false), user);
        verify(incidents, never())
                .receive(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        any(),
                        org.mockito.ArgumentMatchers.anyInt());

        service.record(autoReceiving(RESIDENT, 2, "MEDIUM"), user);
        verify(incidents).receive(41L, 900L, NOW, 0);
    }

    @Test
    void BF_06_caregiverRecordingDoesNotAutoReceive() {
        when(user.roles()).thenReturn(Set.of(Role.CAREGIVER));

        service.record(autoReceiving(RESIDENT, 2, "MEDIUM"), user);

        verify(incidents, never())
                .receive(
                        org.mockito.ArgumentMatchers.anyLong(),
                        org.mockito.ArgumentMatchers.anyLong(),
                        any(),
                        org.mockito.ArgumentMatchers.anyInt());
    }

    /** BF-06: ghi nhận sau kèm đóng luôn: Đã đóng ngay với kết quả; chỉ Điều dưỡng, Trưởng tầng, Bác sĩ. */
    @Test
    void BF_06_recordedLaterCloseNowClosesImmediately_onlyForClosers() {
        var informed = new com.eldercare.backend.incident.api.dto.FamilyNotice(
                "INFORMED", NOW.minusSeconds(60), "PHONE", null);
        service.record(closeNow(RESIDENT, 2, "MEDIUM", informed, "Đã kiểm tra, không đau"), user);
        verify(incidents).closeRecordedLater(41L, "Đã kiểm tra, không đau", 900L, NOW);

        when(user.roles()).thenReturn(Set.of(Role.CAREGIVER));
        assertThatThrownBy(() -> service.record(closeNow(RESIDENT, 2, "MEDIUM", informed, "Đã làm"), user))
                .isInstanceOf(com.eldercare.backend.common.error.PermissionDeniedException.class);
    }

    @Test
    void BF_06_closeNowNeedsRecordedLaterAndResolution() {
        var informed = new com.eldercare.backend.incident.api.dto.FamilyNotice("INFORMED", NOW, "PHONE", null);
        assertThatThrownBy(() -> service.record(closeNow(RESIDENT, 2, "MEDIUM", informed, " "), user))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
        verify(incidents, never()).insert(any());
    }

    /** BF-06: giờ xảy ra/phát hiện không ở tương lai, xảy ra không sau phát hiện, mô tả tối đa 2000 ký tự. */
    @Test
    void BF_06_rejectsFutureOrReversedTimesAndLongDescription() {
        var invalid = com.eldercare.backend.common.error.InvalidValueException.class;
        assertThatThrownBy(() -> service.record(timed(NOW.plusSeconds(3600), NOW, "Mô tả"), user))
                .isInstanceOf(invalid);
        assertThatThrownBy(() -> service.record(timed(NOW.minusSeconds(60), NOW.plusSeconds(3600), "Mô tả"), user))
                .isInstanceOf(invalid);
        assertThatThrownBy(() -> service.record(timed(NOW, NOW.minusSeconds(600), "Mô tả"), user))
                .isInstanceOf(invalid);
        assertThatThrownBy(() -> service.record(timed(NOW.minusSeconds(60), NOW, "x".repeat(2001)), user))
                .isInstanceOf(invalid);
        verify(incidents, never()).insert(any());
    }

    @Test
    void BF_06_rejectsFamilyNoticeInTheFuture() {
        var later = new com.eldercare.backend.incident.api.dto.FamilyNotice(
                "INFORMED", NOW.plusSeconds(86400), "PHONE", null);
        assertThatThrownBy(() -> service.record(recordedLater(RESIDENT, 2, "MEDIUM", later), user))
                .isInstanceOf(com.eldercare.backend.common.error.InvalidValueException.class);
    }

    private static RecordIncidentCommand timed(
            java.time.Instant occurredAt, java.time.Instant detectedAt, String description) {
        return new RecordIncidentCommand(
                RESIDENT,
                2L,
                null,
                occurredAt,
                detectedAt,
                null,
                "Phòng 101",
                null,
                description,
                "MEDIUM",
                null,
                null,
                false,
                false,
                null,
                true,
                false,
                null);
    }

    private static RecordIncidentCommand autoReceiving(Long residentId, long typeId, String level) {
        return new RecordIncidentCommand(
                residentId,
                typeId,
                null,
                null,
                NOW,
                null,
                "Phòng 101",
                null,
                "Mô tả sự cố",
                level,
                null,
                null,
                false,
                false,
                null,
                true,
                false,
                null);
    }

    private static RecordIncidentCommand closeNow(
            Long residentId,
            long typeId,
            String level,
            com.eldercare.backend.incident.api.dto.FamilyNotice notice,
            String resolution) {
        return new RecordIncidentCommand(
                residentId,
                typeId,
                null,
                NOW.minusSeconds(3600),
                NOW,
                null,
                "Phòng 101",
                null,
                "Đã xử lý xong",
                level,
                null,
                null,
                false,
                true,
                notice,
                true,
                true,
                resolution);
    }

    private static RecordIncidentCommand recordedLater(
            Long residentId, long typeId, String level, com.eldercare.backend.incident.api.dto.FamilyNotice notice) {
        return new RecordIncidentCommand(
                residentId,
                typeId,
                null,
                NOW.minusSeconds(3600),
                NOW,
                null,
                "Phòng 101",
                null,
                "Đã xử lý xong, ghi nhận sau",
                level,
                null,
                null,
                false,
                true,
                notice);
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
