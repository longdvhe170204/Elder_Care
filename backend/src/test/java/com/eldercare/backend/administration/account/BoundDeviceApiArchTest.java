package com.eldercare.backend.administration.account;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.eldercare.backend.administration.account.api.BoundDeviceApi;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * 002 T093 (FR-071; module-interfaces 1.11; research A-15): chỉ endpoint nút SOS của 007 ({@code SosController}) gọi
 * {@code BoundDeviceApi}; token thiết bị không mở phiên, không dùng để xác thực đường nào khác.
 */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class BoundDeviceApiArchTest {

    @ArchTest
    static final ArchRule FR_071_onlySosControllerUsesBoundDeviceApi = noClasses()
            .that()
            .doNotHaveSimpleName(RequiresPermissionArchTest.SOS_CONTROLLER)
            .and()
            .resideOutsideOfPackage("com.eldercare.backend.administration.account..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(BoundDeviceApi.class)
            .because("token thiết bị chỉ dùng cho nút SOS (FR-071, research A-15)");
}
