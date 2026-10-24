package com.cambae.backend.m14;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FR-001, SC-008, research O-01, O-09 (016 T001): M14 chỉ đọc. Không có bảng riêng nên không truy cập dữ liệu trực
 * tiếp; không gọi lệnh, port ghi của module khác, không phát sự kiện; service đọc trong transaction {@code readOnly},
 * trừ phần xuất (ghi nhật ký "Xuất báo cáo" và tệp qua api của 000).
 */
@AnalyzeClasses(packages = "com.cambae.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class M14ArchTest {

    private static final String M14 = "com.cambae.backend.m14..";

    @ArchTest
    static final ArchRule FR_001_noDirectDataAccessOrEvents = noClasses()
            .that()
            .resideInAPackage(M14)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.jdbc..", "org.springframework.data..", "jakarta.persistence..")
            .orShould()
            .dependOnClassesThat()
            .belongToAnyOf(org.springframework.context.ApplicationEventPublisher.class)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule FR_001_noCommandApisOfOtherModules = noClasses()
            .that()
            .resideInAPackage(M14)
            .should()
            .dependOnClassesThat(commandApiOutsideM14())
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule O_09_servicesAreReadOnlyExceptExport = classes()
            .that()
            .resideInAPackage("com.cambae.backend.m14..internal..service..")
            .and()
            .resideOutsideOfPackage("com.cambae.backend.m14..internal.export..")
            .and()
            .areMetaAnnotatedWith(Component.class)
            .should(beReadOnlyTransactional())
            .allowEmptyShould(true);

    private static DescribedPredicate<JavaClass> commandApiOutsideM14() {
        return DescribedPredicate.describe(
                "lệnh hoặc port ghi của module khác (*Command*, *Port)",
                c -> c.getPackageName().startsWith("com.cambae.backend.m")
                        && !c.getPackageName().startsWith("com.cambae.backend.m14")
                        && (c.getSimpleName().contains("Command")
                                || c.getSimpleName().endsWith("Port")));
    }

    private static ArchCondition<JavaClass> beReadOnlyTransactional() {
        return new ArchCondition<>("được gắn @Transactional(readOnly = true)") {
            @Override
            public void check(JavaClass c, ConditionEvents events) {
                boolean readOnly = c.tryGetAnnotationOfType(Transactional.class)
                        .map(Transactional::readOnly)
                        .orElse(false);
                events.add(new SimpleConditionEvent(
                        c, readOnly, c.getName() + " không có @Transactional(readOnly = true)"));
            }
        };
    }
}
