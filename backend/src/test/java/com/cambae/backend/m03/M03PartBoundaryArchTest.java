package com.cambae.backend.m03;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.m03.internal.allocation.fixture.AllocationAccessFixture;
import com.cambae.backend.m03.internal.asset.fixture.AssetAccessFixture;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import org.junit.jupiter.api.Test;

/** 003 T001, research G-01: tài sản 019 và phần giường chỉ giao tiếp qua port, không đọc nội bộ của nhau. */
@AnalyzeClasses(packages = "com.cambae.backend.m03", importOptions = ImportOption.DoNotIncludeTests.class)
class M03PartBoundaryArchTest {

    private static final String ASSETS = "com.cambae.backend.m03.internal.asset..";
    private static final String BED = "com.cambae.backend.m03.internal.bed..";
    private static final String ALLOCATION = "com.cambae.backend.m03.internal.allocation..";
    private static final String PORT = "com.cambae.backend.m03.internal.bed.domain.port..";

    @ArchTest
    static final ArchRule FR_013_assetsAccessBedAndAllocationOnlyThroughPorts = noClasses()
            .that()
            .resideInAPackage(ASSETS)
            .should()
            .dependOnClassesThat(
                    resideInAPackage(BED).and(not(resideInAPackage(PORT))).or(resideInAPackage(ALLOCATION)))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule FR_018_bedAndAllocationsDoNotAccessAssetImplementation = noClasses()
            .that()
            .resideInAnyPackage(BED, ALLOCATION)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(ASSETS)
            .allowEmptyShould(true);

    @Test
    void FR_013_boundaryRejectsAssetAccessToAllocation_butAllowsTheBedPort() {
        EvaluationResult result = FR_013_assetsAccessBedAndAllocationOnlyThroughPorts.evaluate(
                new ClassFileImporter().importClasses(AssetAccessFixture.class, AllocationAccessFixture.class));
        assertThat(result.hasViolation()).isTrue();
        assertThat(result.getFailureReport().getDetails())
                .anyMatch(detail -> detail.contains("AllocationAccessFixture"))
                .noneMatch(detail -> detail.contains("BedAssetPort"));
    }
}
