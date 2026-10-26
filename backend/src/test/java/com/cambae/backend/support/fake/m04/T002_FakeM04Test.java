package com.cambae.backend.support.fake.m04;

import static org.assertj.core.api.Assertions.*;

import com.cambae.backend.common.Role;
import com.cambae.backend.m04.api.spi.*;
import com.cambae.backend.m04.api.spi.DayCarePickupPort.*;
import com.cambae.backend.m04.api.spi.QualityCheckPort.*;
import com.cambae.backend.m04.api.spi.ShiftStaffPort.*;
import com.cambae.backend.m04.api.spi.TaskSourcePort.*;
import com.cambae.backend.m04.api.spi.TimetableSourcePort.*;
import com.cambae.backend.m04.api.spi.VitalSignPort.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/** T002: dữ liệu fake phải cô lập theo đúng đối số, có thể xóa và tắt theo module. */
class T002_FakeM04Test {
    static final Instant AT = Instant.parse("2026-10-26T01:00:00Z");
    static final LocalDate DAY = LocalDate.of(2026, 10, 26);

    @Test
    void FR_017_shiftsAndAssignmentsRespectResidentFloorAndHalfOpenTimeRange() {
        var fake = new FakeShiftStaff(new FakeM04Switches());
        var shift = new ShiftView(1, 2, AT, AT.plusSeconds(3600), true);
        var draft = new ShiftView(3, 2, AT, AT.plusSeconds(3600), false);
        var assignment = new CareAssignmentView(4, Role.NURSING_STAFF, true);
        fake.shift(shift);
        fake.shift(draft);
        fake.assignments(10, 1, List.of(assignment));
        fake.shiftLead(1, 4);
        fake.responsibleNurses(10, AT, List.of(4L));
        fake.onShift(4, AT, true);
        fake.qualified(4, Set.of("LICENSE"), DAY, true);
        fake.qualifiedStaff(1, Role.NURSING_STAFF, Set.of("LICENSE"), true);
        fake.handoverConfirmed(1, true);
        var handover = new HandoverTasksView(true, false);
        fake.handoverTasks(4, 1, handover);
        assertThat(fake.shift(1)).contains(shift);
        assertThat(fake.publishedShifts(2, AT, AT.plusSeconds(3600))).containsExactly(shift);
        assertThat(fake.publishedShifts(9, AT, AT.plusSeconds(3600))).isEmpty();
        assertThat(fake.shiftContaining(2, AT)).contains(shift);
        assertThat(fake.shiftContaining(2, AT.plusSeconds(3600))).isEmpty();
        assertThat(fake.assignments(10, 1)).containsExactly(assignment);
        assertThat(fake.assignments(11, 1)).isEmpty();
        assertThat(fake.shiftLead(1)).contains(4L);
        assertThat(fake.responsibleNurses(10, AT)).containsExactly(4L);
        assertThat(fake.responsibleNurses(10, AT.plusSeconds(1))).isEmpty();
        assertThat(fake.onShift(4, AT)).isTrue();
        assertThat(fake.onShift(5, AT)).isFalse();
        assertThat(fake.qualified(4, Set.of("LICENSE"), DAY)).isTrue();
        assertThat(fake.qualified(4, Set.of("OTHER"), DAY)).isFalse();
        assertThat(fake.hasQualifiedStaff(1, Role.NURSING_STAFF, Set.of("LICENSE")))
                .isTrue();
        assertThat(fake.hasQualifiedStaff(1, Role.CAREGIVER, Set.of("LICENSE"))).isFalse();
        assertThat(fake.handoverConfirmed(1)).isTrue();
        assertThat(fake.handoverTasks(4, 1)).isEqualTo(handover);
        fake.reset();
        assertThat(fake.shift(1)).isEmpty();
        assertThat(fake.assignments(10, 1)).isEmpty();
        assertThat(fake.shiftLead(1)).isEmpty();
        assertThat(fake.responsibleNurses(10, AT)).isEmpty();
        assertThat(fake.onShift(4, AT)).isFalse();
        assertThat(fake.qualified(4, Set.of("LICENSE"), DAY)).isFalse();
        assertThat(fake.hasQualifiedStaff(1, Role.NURSING_STAFF, Set.of("LICENSE")))
                .isFalse();
        assertThat(fake.handoverConfirmed(1)).isFalse();
        assertThat(fake.handoverTasks(4, 1)).isEqualTo(new HandoverTasksView(false, false));
    }

    @Test
    void FR_023_taskSourcesRequireExactReferenceResidentAndTime() {
        var fake = new FakeTaskSource(new FakeM04Switches(), "007", Set.of(SourceType.MEASUREMENT));
        var valid = new SourceValidation(true, true, true, true);
        fake.validation(SourceType.MEASUREMENT, 20, 10, AT, valid);
        assertThat(fake.managedTypes()).containsExactly(SourceType.MEASUREMENT);
        assertThat(fake.validate(SourceType.MEASUREMENT, 20, 10, AT)).isEqualTo(valid);
        assertThat(fake.validate(SourceType.MEASUREMENT, 20, 11, AT).valid()).isFalse();
        assertThat(fake.validate(SourceType.MEASUREMENT, 20, 10, AT.plusSeconds(1))
                        .valid())
                .isFalse();
        assertThatThrownBy(() -> fake.validation(SourceType.MEAL, 20, 10, AT, valid))
                .isInstanceOf(IllegalArgumentException.class);
        fake.served(20, 10, false);
        assertThat(fake.confirmServing(20, 10)).isFalse();
        assertThat(fake.confirmServing(20, 11)).isTrue();
        fake.reset();
        assertThat(fake.validate(SourceType.MEASUREMENT, 20, 10, AT).valid()).isFalse();
        assertThat(fake.confirmServing(20, 10)).isTrue();
    }

    @Test
    void FR_034_vitalSignRecordingDoesNotInventSuccessAndKeepsAllInputFields() {
        var fake = new FakeVitalSigns(new FakeM04Switches());
        var request = new FakeVitalSigns.Measurement(
                10, "TEST", new BigDecimal("1.25"), AT, 4, 30, Optional.of(AT.minusSeconds(5)));
        assertThatThrownBy(() -> fake.record(10, "TEST", request.value(), AT, 4, 30, request.deviceAt()))
                .isInstanceOf(IllegalStateException.class);
        for (var status : RecordingStatus.values()) {
            var result = new RecordingResult(status, Optional.of(40L), Optional.of("Lý do kiểm thử"), Optional.of(50L));
            fake.result(request, result);
            assertThat(fake.record(10, "TEST", request.value(), AT, 4, 30, request.deviceAt()))
                    .isEqualTo(result);
        }
        assertThat(fake.recordings()).containsExactly(request, request, request);
        assertThatThrownBy(() -> fake.record(11, "TEST", request.value(), AT, 4, 30, request.deviceAt()))
                .isInstanceOf(IllegalStateException.class);
        fake.reset();
        assertThat(fake.recordings()).isEmpty();
        assertThatThrownBy(() -> fake.record(10, "TEST", request.value(), AT, 4, 30, request.deviceAt()))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void FR_029_dosesAndAlertsFilterByRequestedResidentsAndTaskIds() {
        var switches = new FakeM04Switches();
        var doses = new FakeMedicationChecklist(switches);
        var dose = new MedicationChecklistPort.DoseView(
                "dose-1",
                10,
                20,
                AT,
                AT,
                AT.plusSeconds(60),
                "DUE",
                false,
                false,
                false,
                false,
                "INSTITUTE",
                Optional.empty(),
                Optional.empty());
        doses.add(dose);
        assertThat(doses.inRange(List.of(10L), AT, AT.plusSeconds(1))).containsExactly(dose);
        assertThat(doses.inRange(List.of(11L), AT, AT.plusSeconds(1))).isEmpty();
        assertThat(doses.inRange(List.of(10L), AT.minusSeconds(1), AT)).isEmpty();
        var alerts = new FakeAlertReader(switches);
        var alert = new AlertReadPort.AlertView(40, 10);
        alerts.add(alert);
        alerts.forTask(30, List.of(40L));
        assertThat(alerts.open(List.of(10L))).containsExactly(alert);
        assertThat(alerts.open(List.of(11L))).isEmpty();
        assertThat(alerts.byTask(List.of(30L, 31L))).containsOnlyKeys(30L).containsEntry(30L, List.of(40L));
        assertThat(alerts.byTask(List.of(31L))).isEmpty();
        doses.reset();
        alerts.reset();
        assertThat(doses.inRange(List.of(10L), AT, AT.plusSeconds(1))).isEmpty();
        assertThat(alerts.open(List.of(10L))).isEmpty();
        assertThat(alerts.byTask(List.of(30L))).isEmpty();
    }

    @Test
    void FR_018_pickupCanBeConsumedOnlyOnceAndResetRestoresFixture() {
        var fake = new FakeDayCarePickups(new FakeM04Switches());
        var pickup = new PickupView(50, "SELF_RETURN");
        fake.valid(50, 10, AT, pickup);
        assertThat(fake.valid(50, 10, AT)).contains(pickup);
        assertThat(fake.valid(50, 11, AT)).isEmpty();
        assertThat(fake.valid(50, 10, AT.plusSeconds(1))).isEmpty();
        fake.markUsed(50, 60);
        assertThat(fake.used()).containsEntry(50L, 60L);
        assertThat(fake.valid(50, 10, AT)).isEmpty();
        assertThatThrownBy(() -> fake.markUsed(50, 61))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PICKUP_RECORD_ALREADY_USED");
        fake.reset();
        assertThat(fake.used()).isEmpty();
        assertThat(fake.valid(50, 10, AT)).isEmpty();
    }

    @Test
    void FR_026a_qualityCheckAndTimetableAreScopedAndResettable() {
        var switches = new FakeM04Switches();
        var quality = new FakeQualityChecks(switches);
        var failed = new FailedCheckView(70, 1);
        quality.failed(70, 30, failed);
        assertThat(quality.failed(70, 30)).contains(failed);
        assertThat(quality.failed(70, 31)).isEmpty();
        var timetable = new FakeTimetableSource(switches, "014");
        var item = new TimetableItemView(AT, "ACTIVITY_SESSION", "Mô tả kiểm thử", "ACTIVE");
        timetable.items(10, DAY, List.of(item));
        assertThat(timetable.onDate(10, DAY)).containsExactly(item);
        assertThat(timetable.onDate(11, DAY)).isEmpty();
        assertThat(timetable.onDate(10, DAY.plusDays(1))).isEmpty();
        quality.reset();
        timetable.reset();
        assertThat(quality.failed(70, 30)).isEmpty();
        assertThat(timetable.onDate(10, DAY)).isEmpty();
    }

    @Test
    void T002_importRegistersEveryProviderOnlyInTestOrDemo() {
        for (String profile : List.of("test", "demo")) {
            new ApplicationContextRunner()
                    .withPropertyValues("spring.profiles.active=" + profile)
                    .withUserConfiguration(FakeM04Config.class)
                    .run(context -> {
                        assertThat(context)
                                .hasSingleBean(ShiftStaffPort.class)
                                .hasSingleBean(VitalSignPort.class)
                                .hasSingleBean(MedicationChecklistPort.class)
                                .hasSingleBean(AlertReadPort.class)
                                .hasSingleBean(DayCarePickupPort.class)
                                .hasSingleBean(QualityCheckPort.class);
                        assertThat(context.getBeansOfType(TaskSourcePort.class)).hasSize(3);
                        assertThat(context.getBeansOfType(TimetableSourcePort.class))
                                .hasSize(3);
                        assertThat(context).hasSingleBean(FakeM04Switches.class);
                    });
        }
        new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=prod")
                .withUserConfiguration(FakeM04Config.class)
                .run(context -> {
                    assertThat(context)
                            .doesNotHaveBean(ShiftStaffPort.class)
                            .doesNotHaveBean(TaskSourcePort.class)
                            .doesNotHaveBean(FakeM04Switches.class);
                });
    }

    @Test
    void T002_switchDisablesWholeModuleAndRetainsOtherModulesAndFixtures() {
        var switches = new FakeM04Switches();
        var source = new FakeTaskSource(switches, "007", Set.of(SourceType.MEASUREMENT));
        var vital = new FakeVitalSigns(switches);
        var alerts = new FakeAlertReader(switches);
        var staff = new FakeShiftStaff(switches);
        source.validation(SourceType.MEASUREMENT, 20, 10, AT, new SourceValidation(true, true, true, false));
        switches.enabled("007", false);
        assertThatThrownBy(() -> source.validate(SourceType.MEASUREMENT, 20, 10, AT))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> vital.record(10, "TEST", BigDecimal.ONE, AT, 4, 30, Optional.empty()))
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> alerts.open(List.of(10L))).isInstanceOf(IllegalStateException.class);
        assertThat(staff.shift(1)).isEmpty();
        switches.enabled("007", true);
        assertThat(source.validate(SourceType.MEASUREMENT, 20, 10, AT).valid()).isTrue();
        assertThatThrownBy(() -> switches.enabled("999", false)).isInstanceOf(IllegalArgumentException.class);
    }
}
