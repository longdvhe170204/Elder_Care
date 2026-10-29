package com.eldercare.backend.common.time;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;

/**
 * DBR-25, NFR-09, constitution XIII (000 T013): thời gian chỉ lấy từ bean {@code Clock}; mốc nghiệp vụ không viết
 * cứng thành hằng {@code Duration} trong tầng nghiệp vụ của module.
 */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class TimeArchTest {

    @ArchTest
    static final ArchRule DBR_25_noNowOutsideClockConfig = noClasses()
            .that()
            .doNotHaveFullyQualifiedName(ClockConfig.class.getName())
            .should()
            .callMethod(Instant.class, "now")
            .orShould()
            .callMethod(LocalDate.class, "now")
            .orShould()
            .callMethod(LocalDateTime.class, "now")
            .orShould()
            .callMethod(LocalTime.class, "now")
            .orShould()
            .callMethod(ZonedDateTime.class, "now")
            .orShould()
            .callMethod(OffsetDateTime.class, "now")
            .because("thời điểm lấy từ bean Clock (research R-04)");

    @ArchTest
    static final ArchRule XIII_noDurationConstantsInModuleLogic = noFields()
            .that()
            .haveRawType(Duration.class)
            .and()
            .areStatic()
            .and()
            .areFinal()
            .should()
            .beDeclaredInClassesThat()
            .resideInAnyPackage("com.eldercare.backend..internal..", "com.eldercare.backend..service..")
            .because("mốc nghiệp vụ đọc từ ParameterApi (constitution XIII)")
            .allowEmptyShould(true);
}
