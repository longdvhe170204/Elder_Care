package com.eldercare.backend.care;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.eldercare.backend.administration.parameter.api.ParameterApi;
import com.eldercare.backend.administration.shared.api.dto.CatalogItem;
import com.eldercare.backend.care.api.DayCareAttendanceApi;
import com.eldercare.backend.care.api.dto.*;
import com.eldercare.backend.care.api.spi.*;
import com.eldercare.backend.care.api.spi.TaskSourcePort.*;
import com.eldercare.backend.care.internal.careplan.service.CarePlanQueries;
import com.eldercare.backend.care.internal.catalog.domain.*;
import com.eldercare.backend.care.internal.catalog.persistence.CareCatalogStore;
import com.eldercare.backend.care.internal.task.domain.*;
import com.eldercare.backend.care.internal.task.persistence.CareTaskStore;
import com.eldercare.backend.care.internal.task.service.CareTaskValidator;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.data.CatalogStatus;
import com.eldercare.backend.resident.api.*;
import com.eldercare.backend.resident.api.dto.*;
import com.eldercare.backend.resident.api.spi.ContractApi;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.ObjectProvider;

/** T018: kiểm kết quả nghiệp vụ của validator, không kiểm số lần gọi mock. */
class FR_017_CareTaskValidatorTest {
    final Instant now = Instant.parse("2026-11-01T01:00:00Z"), at = now.plusSeconds(1800);
    final ShiftStaffPort.ShiftView shift = new ShiftStaffPort.ShiftView(5, 3, now, now.plusSeconds(14400), true);
    CareCatalogStore catalogs;
    ResidentApi residents;
    ShiftStaffPort staff;
    TaskSourcePort source;
    CarePlanQueries plans;
    CareTaskStore tasks;
    CareTaskValidator validator;
    ObjectProvider<ContractApi> contractProvider;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void fixture() {
        catalogs = mock(CareCatalogStore.class);
        residents = mock(ResidentApi.class);
        staff = mock(ShiftStaffPort.class);
        source = mock(TaskSourcePort.class);
        plans = mock(CarePlanQueries.class);
        tasks = mock(CareTaskStore.class);
        var staffing = (ObjectProvider<ShiftStaffPort>) mock(ObjectProvider.class);
        when(staffing.getIfAvailable()).thenReturn(staff);
        var attendance = (ObjectProvider<DayCareAttendanceApi>) mock(ObjectProvider.class);
        var contracts = mock(ContractApi.class);
        contractProvider = (ObjectProvider<ContractApi>) mock(ObjectProvider.class);
        when(contractProvider.getIfAvailable()).thenReturn(contracts);
        var statuses = mock(ResidentStatusApi.class);
        var type = new TaskTypeAttributes(
                "OTHER", false, "PERFORMED", Role.CAREGIVER, "NORMAL", 15, 15, false, null, null, null, Set.of(), null);
        when(catalogs.require(CareCatalogKinds.TASK_TYPES, 9))
                .thenReturn(new CatalogItem<>(
                        9, null, "Loại kiểm thử", null, 0, CatalogStatus.ACTIVE, type, null, null, 0));
        when(residents.status(77)).thenReturn(ResidentStatus.IN_RESIDENCE);
        when(contracts.currentResidenceType(77, LocalDate.of(2026, 11, 1)))
                .thenReturn(Optional.of(ResidenceType.LONG_TERM_RESIDENTIAL));
        when(staff.assignments(77, 5))
                .thenReturn(List.of(new ShiftStaffPort.CareAssignmentView(22, Role.CAREGIVER, true)));
        when(staff.onShift(eq(22L), any())).thenReturn(true);
        when(staff.qualified(eq(22L), anySet(), any())).thenReturn(true);
        when(source.managedTypes()).thenReturn(Set.of(SourceType.MEAL, SourceType.MEASUREMENT));
        when(source.validate(any(), anyLong(), anyLong(), any()))
                .thenReturn(new SourceValidation(true, true, true, false));
        validator = new CareTaskValidator(
                catalogs,
                residents,
                contractProvider,
                attendance,
                staffing,
                List.of(source),
                plans,
                tasks,
                mock(ParameterApi.class),
                statuses);
    }

    CareTaskDraft row(SourceType type, Instant planned, Importance importance, Long performer, boolean shared) {
        return new CareTaskDraft(
                77L, null, null, 9, type, 90L, "Nội dung kiểm thử", planned, 15, 15, importance, performer, shared);
    }

    @Test
    void FR_017_validRowFreezesSnapshotAndClockState() {
        var result = validator.validate(row(SourceType.MEAL, at, Importance.NORMAL, 22L, false), shift, now, 1, null);
        assertThat(result.codes()).isEmpty();
        assertThat(result.task().data().content()).isEqualTo("Nội dung kiểm thử");
        assertThat(result.task().status()).isEqualTo(TaskStatus.NOT_YET_DUE);
    }

    @Test
    void FR_024_nurseMayPerformCaregiverWorkButNotTheReverse() {
        assertThat(CareTaskValidator.roleMatches(Role.CAREGIVER, Role.NURSING_STAFF))
                .isTrue();
        assertThat(CareTaskValidator.roleMatches(Role.NURSING_STAFF, Role.CAREGIVER))
                .isFalse();
        when(staff.assignments(77, 5))
                .thenReturn(List.of(new ShiftStaffPort.CareAssignmentView(22, Role.NURSING_STAFF, true)));
        assertThat(validator
                        .validate(row(SourceType.MEAL, at, Importance.NORMAL, 22L, false), shift, now, 1, null)
                        .codes())
                .isEmpty();
    }

    @Test
    void FR_024_sharedTaskIsRejectedWhenMatchingAssignedStaffExists() {
        assertThat(validator
                        .validate(row(SourceType.MEAL, at, Importance.NORMAL, null, true), shift, now, 1, null)
                        .codes())
                .contains("INVALID_PERFORMER");
        when(staff.assignments(77, 5)).thenReturn(List.of());
        assertThat(validator
                        .validate(row(SourceType.MEAL, at, Importance.NORMAL, null, true), shift, now, 1, null)
                        .codes())
                .doesNotContain("INVALID_PERFORMER");
    }

    @Test
    void FR_023_prescribedMeasurementMustBeMandatory() {
        when(source.validate(any(), anyLong(), anyLong(), any()))
                .thenReturn(new SourceValidation(true, true, true, true));
        assertThat(validator
                        .validate(row(SourceType.MEASUREMENT, at, Importance.NORMAL, 22L, false), shift, now, 1, null)
                        .codes())
                .contains("INVALID_BASIS");
        assertThat(validator
                        .validate(
                                row(SourceType.MEASUREMENT, at, Importance.MANDATORY, 22L, false), shift, now, 1, null)
                        .codes())
                .isEmpty();
    }

    @Test
    void FR_021_duplicatesAreReportedWithoutPersisting() {
        when(tasks.duplicate(any())).thenReturn(true);
        assertThat(validator
                        .validate(row(SourceType.MEAL, at, Importance.NORMAL, 22L, false), shift, now, 1, null)
                        .codes())
                .contains("DUPLICATE_BASIS");
    }

    @Test
    void FR_026a_reworkCannotBeValidatedWithoutAnOriginalTaskFromTheQualityCommand() {
        assertThat(validator
                        .validate(row(SourceType.REWORK, at, Importance.NORMAL, 22L, false), shift, now, 1, null)
                        .codes())
                .contains("INVALID_BASIS");
    }
}
