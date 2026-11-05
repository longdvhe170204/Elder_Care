package com.eldercare.backend.family;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 012 T001 (research F-01, Q-306): M10 không lập khoản chi phí; đêm ở lại chỉ đi qua sự kiện của 010 (FR-042 (b)). Phần
 * trả cho người thân (gói {@code web}, {@code api}) không mang dữ liệu nhật ký.
 */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class M10ArchTest {

    private static final String M10 = "com.eldercare.backend.family..";

    @ArchTest
    static final ArchRule Q_306_noCostItemCommands = noClasses()
            .that()
            .resideInAPackage(M10)
            .should()
            .dependOnClassesThat(resideInAPackage("com.eldercare.backend.billing..")
                    .and(not(resideInAPackage("com.eldercare.backend.billing.api.events.."))))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule F_01_familyFacingTypesHaveNoAuditLog = noClasses()
            .that()
            .resideInAnyPackage("com.eldercare.backend.family.web..", "com.eldercare.backend.family.api..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage("com.eldercare.backend.administration.auditlog..")
            .allowEmptyShould(true);
}
