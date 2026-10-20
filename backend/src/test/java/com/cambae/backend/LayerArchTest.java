package com.cambae.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Chia tầng trong module (research R-02 của 000, mục "Cấu trúc trong module"; chủ dự án duyệt 2026-10-21): áp cho mọi
 * module {@code m01} … {@code m15}. Ranh giới giữa module do {@link ModuleBoundaryTest}, giữa các phần của M15 do
 * {@code M15PartBoundaryArchTest}.
 */
@AnalyzeClasses(packages = "com.cambae.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchTest {

    // m* chỉ khớp m01 … m15, không áp cho common
    private static final String WEB = "com.cambae.backend.m*..web..";
    private static final String SERVICE = "com.cambae.backend.m*..internal..service..";
    private static final String DOMAIN = "com.cambae.backend.m*..internal..domain..";
    private static final String PERSISTENCE = "com.cambae.backend.m*..internal..persistence..";

    /** Lớp trong {@code internal} nằm ở một tầng; module lớn có gói chức năng ở giữa ({@code internal.auth.service}). */
    @ArchTest
    static final ArchRule internalClassesLiveInALayer = classes()
            .that()
            .resideInAPackage("com.cambae.backend.m*..internal..")
            .and()
            .doNotHaveSimpleName("package-info")
            .should()
            .resideInAnyPackage(
                    "com.cambae.backend.m*..internal..service..",
                    "com.cambae.backend.m*..internal..domain..",
                    "com.cambae.backend.m*..internal..persistence..",
                    "com.cambae.backend.m*..internal..config..")
            .allowEmptyShould(true);

    /** Controller chỉ gọi service; không đọc ghi dữ liệu trực tiếp (yêu cầu tổ chức lại, mục 3.2). */
    @ArchTest
    static final ArchRule webDoesNotAccessData = noClasses()
            .that()
            .resideInAPackage(WEB)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(PERSISTENCE, "org.springframework.jdbc..", "org.springframework.data..")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainDependsOnNoOtherLayer = noClasses()
            .that()
            .resideInAPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(SERVICE, PERSISTENCE, WEB)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistenceDoesNotCallServiceOrWeb = noClasses()
            .that()
            .resideInAPackage(PERSISTENCE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(SERVICE, WEB)
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule serviceDoesNotDependOnWeb = noClasses()
            .that()
            .resideInAPackage(SERVICE)
            .should()
            .dependOnClassesThat()
            .resideInAPackage(WEB)
            .allowEmptyShould(true);

    /** Gói {@code api} là hợp đồng: không phụ thuộc phần triển khai của chính module (yêu cầu tổ chức lại, mục 3.1). */
    @ArchTest
    static final ArchRule apiDoesNotDependOnImplementation = noClasses()
            .that()
            .resideInAPackage("com.cambae.backend.m*..api..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.cambae.backend.m*..internal..", WEB)
            .allowEmptyShould(true);
}
