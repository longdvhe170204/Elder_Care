package com.eldercare.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Arrays;
import java.util.stream.Stream;

/**
 * Chia tầng trong module (research R-02 của 000, mục "Cấu trúc trong module"; chủ dự án duyệt 2026-10-21): áp cho mọi
 * module M01 … M15 (tên gói theo constitution X 3.3.0, research R-02). Ranh giới giữa module do
 * {@link ModuleBoundaryTest}, giữa các phần của M15 do {@code M15PartBoundaryArchTest}.
 */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchTest {

    /** Gói của mười lăm module M01 … M15; không gồm {@code common}. */
    static final String[] MODULES = {
        "resident",
        "admission",
        "roombedallocation",
        "care",
        "incident",
        "health",
        "medication",
        "nutrition",
        "staffing",
        "family",
        "billing",
        "belongings",
        "notification",
        "reporting",
        "administration"
    };

    static String[] inModules(String... suffixes) {
        return Arrays.stream(suffixes)
                .flatMap(suffix -> Arrays.stream(MODULES).map(module -> "com.eldercare.backend." + module + suffix))
                .toArray(String[]::new);
    }

    private static String[] concat(String[]... parts) {
        return Stream.of(parts).flatMap(Arrays::stream).toArray(String[]::new);
    }

    private static final String[] WEB = inModules("..web..");
    private static final String[] SERVICE = inModules("..internal..service..");
    private static final String[] DOMAIN = inModules("..internal..domain..");
    private static final String[] PERSISTENCE = inModules("..internal..persistence..");

    /** Lớp trong {@code internal} nằm ở một tầng; module lớn có gói chức năng ở giữa ({@code internal.auth.service}). */
    @ArchTest
    static final ArchRule internalClassesLiveInALayer = classes()
            .that()
            .resideInAnyPackage(inModules("..internal.."))
            .and()
            .doNotHaveSimpleName("package-info")
            .should()
            .resideInAnyPackage(inModules(
                    "..internal..service..",
                    "..internal..domain..",
                    "..internal..persistence..",
                    "..internal..config.."))
            .allowEmptyShould(true);

    /** Controller chỉ gọi service; không đọc ghi dữ liệu trực tiếp (yêu cầu tổ chức lại, mục 3.2). */
    @ArchTest
    static final ArchRule webDoesNotAccessData = noClasses()
            .that()
            .resideInAnyPackage(WEB)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    concat(PERSISTENCE, new String[] {"org.springframework.jdbc..", "org.springframework.data.."}))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainDependsOnNoOtherLayer = noClasses()
            .that()
            .resideInAnyPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(concat(SERVICE, PERSISTENCE, WEB))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistenceDoesNotCallServiceOrWeb = noClasses()
            .that()
            .resideInAnyPackage(PERSISTENCE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(concat(SERVICE, WEB))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule serviceDoesNotDependOnWeb = noClasses()
            .that()
            .resideInAnyPackage(SERVICE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(WEB)
            .allowEmptyShould(true);

    /** Gói {@code api} là hợp đồng: không phụ thuộc phần triển khai của chính module (yêu cầu tổ chức lại, mục 3.1). */
    @ArchTest
    static final ArchRule apiDoesNotDependOnImplementation = noClasses()
            .that()
            .resideInAnyPackage(inModules("..api.."))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(concat(inModules("..internal.."), WEB))
            .allowEmptyShould(true);
}
