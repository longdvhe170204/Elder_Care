package com.eldercare.backend.medication;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** Module thuốc chia theo chức năng trước khi chia tầng (backend/README.md mục 4). */
@AnalyzeClasses(packages = "com.eldercare.backend.medication", importOptions = ImportOption.DoNotIncludeTests.class)
class T069_MedicationStructureArchTest {

    @ArchTest
    static final ArchRule T069_layersBelongToAFunction = noClasses()
            .that()
            .haveSimpleNameNotEndingWith("package-info")
            .should()
            .resideInAnyPackage(
                    "..medication.internal.service..", "..medication.internal.domain..",
                    "..medication.internal.persistence..", "..medication.internal.config..");
}
