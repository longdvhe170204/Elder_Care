package com.eldercare.backend.roombedallocation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/** T009/G-04: chỉ một dịch vụ làm cổng ghi trạng thái; SQL được giữ trong tầng persistence. */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class SingleBedStateWriterArchTest {
    private static final String SERVICE =
            "com.eldercare.backend.roombedallocation.internal.bed.service.BedStateService";
    private static final String STORE =
            "com.eldercare.backend.roombedallocation.internal.bed.persistence.BedStateStore";

    @ArchTest
    static void FR_011_stateGatewayExists(JavaClasses classes) {
        assertThat(classes.stream().map(c -> c.getName())).contains(SERVICE, STORE);
    }

    @ArchTest
    static final ArchRule FR_011_onlyStateServiceCanWriteState = methods()
            .that()
            .areDeclaredInClassesThat()
            .haveFullyQualifiedName(STORE)
            .and()
            .haveName("writeState")
            .should()
            .onlyBeCalled()
            .byClassesThat()
            .haveFullyQualifiedName(SERVICE);
}
