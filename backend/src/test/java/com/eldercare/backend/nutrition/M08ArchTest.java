package com.eldercare.backend.nutrition;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Luật kiến trúc của M08 (011 T001; research M-01, M-09; Q-306). */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class M08ArchTest {

    /** Q-306: job không chốt suất, không lập phiếu; việc đó chỉ do lệnh của Nhân viên bếp. */
    @ArchTest
    static final ArchRule Q_306_schedulingDoesNotCloseOrCreateSlips = noClasses()
            .that()
            .resideInAPackage("com.eldercare.backend.nutrition.scheduling..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.eldercare.backend.nutrition.internal.closing..",
                    "com.eldercare.backend.nutrition.internal.slip..")
            .allowEmptyShould(true);

    /** SC-008, SC-014: phản hồi cho bếp (Kitchen*) không tham chiếu kiểu dị ứng, sức khỏe, thuốc. */
    @ArchTest
    static final ArchRule SC_008_kitchenDtoHasNoHealthTypes = noClasses()
            .that()
            .resideInAPackage("com.eldercare.backend.nutrition..")
            .and()
            .haveSimpleNameStartingWith("Kitchen")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.eldercare.backend.health..", "com.eldercare.backend.medication..")
            .orShould()
            .dependOnClassesThat()
            .haveSimpleNameStartingWith("Allergen")
            .orShould()
            .dependOnClassesThat()
            .haveSimpleNameStartingWith("Allergy")
            .orShould()
            .dependOnClassesThat()
            .haveSimpleNameStartingWith("HealthItem")
            .allowEmptyShould(true);
}
