package com.cambae.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.modulith.NamedInterface;

/**
 * Constitution X, research R-02: gói {@code api} của module là phần công khai. Spring Modulith coi gói con của module là
 * nội bộ, nên mỗi gói {@code …api} (kể cả gói con như {@code api.events}) phải có {@code package-info.java} gắn
 * {@link NamedInterface}; thiếu thì module khác dùng sẽ trượt {@link ModuleBoundaryTest}.
 */
@AnalyzeClasses(packages = "com.cambae.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class ApiNamedInterfaceArchTest {

    private static final ArchCondition<JavaClass> PACKAGE_IS_NAMED_INTERFACE =
            new ArchCondition<>("nằm trong gói có package-info gắn @NamedInterface") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    if (!item.getPackage().isAnnotatedWith(NamedInterface.class)) {
                        events.add(SimpleConditionEvent.violated(
                                item, "Gói " + item.getPackageName() + " thiếu package-info.java với @NamedInterface"));
                    }
                }
            };

    @ArchTest
    static final ArchRule X_apiPackagesAreNamedInterfaces = classes()
            .that()
            .haveNameMatching("com\\.cambae\\.backend\\.m\\d{2}\\.(.+\\.)?api(\\..+)?\\.[^.]+")
            .should(PACKAGE_IS_NAMED_INTERFACE)
            .allowEmptyShould(true);
}
