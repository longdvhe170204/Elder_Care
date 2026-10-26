package com.cambae.backend.m09;

import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m09.duty.internal.service.ForbiddenPortCallerFixture;
import com.cambae.backend.m09.rotation.internal.service.RotationPortCallerFixture;
import com.cambae.backend.m09.scheduling.M09SchedulingFixtures;
import com.cambae.backend.m09.shift.internal.service.port.ShiftCommandPortFixture;
import com.cambae.backend.m09.swap.internal.service.SwapPortCallerFixture;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import org.junit.jupiter.api.Test;

/** Kiểm chứng luật T001 bằng các lớp thử có và không vi phạm, kể cả khi module chưa có dịch vụ thật. */
class M09ArchRuleTest {

    @Test
    void FR_017_commandPortsAllowSwapAndRotationCallers() {
        JavaClasses classes = new ClassFileImporter()
                .importClasses(
                        ShiftCommandPortFixture.class, SwapPortCallerFixture.class, RotationPortCallerFixture.class);

        assertThat(M09ArchTest.INTERNAL_COMMAND_PORTS.evaluate(classes).hasViolation())
                .isFalse();
    }

    @Test
    void FR_017_commandPortsRejectUnrelatedCallers() {
        JavaClasses classes =
                new ClassFileImporter().importClasses(ShiftCommandPortFixture.class, ForbiddenPortCallerFixture.class);

        var result = M09ArchTest.INTERNAL_COMMAND_PORTS.evaluate(classes);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails())
                .anySatisfy(detail -> assertThat(detail).contains("ForbiddenPortCallerFixture", "createShift"));
    }

    @Test
    void Q_306_schedulerRejectsAllRecordCreationCommands() {
        JavaClasses classes = new ClassFileImporter()
                .importClasses(ShiftCommandPortFixture.class, M09SchedulingFixtures.CreatesRecords.class);

        var result = M09ArchTest.SCHEDULERS_DO_NOT_CREATE_RECORDS.evaluate(classes);

        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails()).hasSize(5);
    }

    @Test
    void Q_306_schedulerAllowsStateTransitions() {
        JavaClasses classes = new ClassFileImporter()
                .importClasses(ShiftCommandPortFixture.class, M09SchedulingFixtures.ClosesShift.class);

        assertThat(M09ArchTest.SCHEDULERS_DO_NOT_CREATE_RECORDS
                        .evaluate(classes)
                        .hasViolation())
                .isFalse();
    }
}
