package com.eldercare.backend.staffing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Ranh giới nội bộ M09 theo N-07 và phạm vi job theo Q-306. */
class M09ArchTest {

    static final ArchRule INTERNAL_COMMAND_PORTS = noClasses()
            .that()
            .resideOutsideOfPackages("com.eldercare.backend.staffing.swap..")
            .should()
            .callMethodWhere(new DescribedPredicate<>("gọi port lệnh nội bộ của ca hoặc phân công") {
                @Override
                public boolean test(JavaMethodCall call) {
                    String targetPackage = call.getTarget().getOwner().getPackageName();
                    return (targetPackage.startsWith("com.eldercare.backend.staffing.shift.internal.service.port")
                                    || targetPackage.startsWith(
                                            "com.eldercare.backend.staffing.assignment.internal.service.port"))
                            && call.getTarget().getOwner().getSimpleName().contains("CommandPort");
                }
            })
            .allowEmptyShould(true);

    private static final Set<String> RECORD_CREATION_COMMANDS =
            Set.of("createSchedule", "createShift", "createAssignment", "createHandover", "addHandoverItem");

    static final ArchRule SCHEDULERS_DO_NOT_CREATE_RECORDS = noClasses()
            .that()
            .resideInAPackage("com.eldercare.backend.staffing.scheduling..")
            .should()
            .callMethodWhere(new DescribedPredicate<>("lập lịch, ca, phân công, bàn giao hoặc mục bàn giao") {
                @Override
                public boolean test(JavaMethodCall call) {
                    return call.getTarget().getOwner().getPackageName().startsWith("com.eldercare.backend.staffing.")
                            && RECORD_CREATION_COMMANDS.contains(
                                    call.getTarget().getName());
                }
            })
            .allowEmptyShould(true);

    private static final JavaClasses MAIN_CLASSES = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.eldercare.backend");

    @Test
    void FR_017_onlySwapCallsInternalCommandPorts() {
        INTERNAL_COMMAND_PORTS.check(MAIN_CLASSES);
    }

    @Test
    void Q_306_schedulersDoNotCreateRecords() {
        SCHEDULERS_DO_NOT_CREATE_RECORDS.check(MAIN_CLASSES);
    }
}
