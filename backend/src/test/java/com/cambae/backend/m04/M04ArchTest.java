package com.cambae.backend.m04;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.*;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.cambae.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class M04ArchTest {
    @ArchTest
    static final ArchRule C_02_sourcesOnlyThroughOwnedSpi = M04ArchitectureRules.NO_DIRECT_HIGHER_MODULES;

    @ArchTest
    static final ArchRule FR_010a_schedulerCannotCreateTasks = M04ArchitectureRules.NO_JOB_CREATES_RECORDS;
}
