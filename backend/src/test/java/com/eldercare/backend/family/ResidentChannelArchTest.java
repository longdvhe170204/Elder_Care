package com.eldercare.backend.family;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.syntax.elements.GivenClassesConjunction;

/**
 * 012 T062 (US9; FR-084, BR-M10-11; research F-18): kênh người cao tuổi (gói {@code family.internal.residentchannel} và
 * controller của kênh) không gọi API sức khỏe, thuốc, kết quả chăm sóc, sự cố, chi phí, số dư.
 */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ResidentChannelArchTest {

    @ArchTest
    static final ArchRule FR_084_channelServiceReadsNoHealthOrCosts = noForbiddenReads(
            noClasses().that().resideInAPackage("com.eldercare.backend.family.internal.residentchannel.."));

    @ArchTest
    static final ArchRule FR_084_channelControllerReadsNoHealthOrCosts = noForbiddenReads(noClasses()
            .that()
            .resideInAPackage("com.eldercare.backend.family.web..")
            .and()
            .haveSimpleNameStartingWith("ResidentChannel"));

    private static ArchRule noForbiddenReads(GivenClassesConjunction classes) {
        return classes.should()
                .dependOnClassesThat()
                .resideInAnyPackage(
                        "com.eldercare.backend.health..",
                        "com.eldercare.backend.medication..",
                        "com.eldercare.backend.incident..",
                        "com.eldercare.backend.billing..")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.eldercare.backend.resident.api.HealthItemApi")
                .orShould()
                .dependOnClassesThat()
                .haveFullyQualifiedName("com.eldercare.backend.care.api.PublicCareResultApi");
    }
}
