package com.cambae.backend.m15;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.domain.Dependency;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Ranh giới giữa các phần của M15 ({@code account}, {@code approval}, {@code auditlog}, {@code correction},
 * {@code parameter}, {@code retention}): Spring Modulith coi cả M15 là một module nên không chặn giữa các phần; luật này
 * cho mỗi phần chỉ dùng gói {@code api} (kể cả gói con) của phần khác (tổ chức lại backend, chủ dự án duyệt 2026-10-21).
 */
@AnalyzeClasses(packages = "com.cambae.backend.m15", importOptions = ImportOption.DoNotIncludeTests.class)
class M15PartBoundaryArchTest {

    private static final Pattern PART = Pattern.compile("com\\.cambae\\.backend\\.m15\\.([a-z]+)(\\..*)?");

    private static final ArchCondition<JavaClass> ONLY_API_OF_OTHER_PARTS =
            new ArchCondition<>("chỉ dùng gói api của phần khác trong M15") {
                @Override
                public void check(JavaClass item, ConditionEvents events) {
                    String own = part(item.getPackageName());
                    for (Dependency d : item.getDirectDependenciesFromSelf()) {
                        String pkg = d.getTargetClass().getPackageName();
                        String target = part(pkg);
                        if (target == null || target.equals(own)) {
                            continue;
                        }
                        String rest = pkg.substring(("com.cambae.backend.m15." + target).length());
                        if (!rest.equals(".api") && !rest.startsWith(".api.")) {
                            events.add(SimpleConditionEvent.violated(item, d.getDescription()));
                        }
                    }
                }
            };

    @ArchTest
    static final ArchRule partsUseOnlyApiOfEachOther =
            classes().that().resideInAPackage("com.cambae.backend.m15.*..").should(ONLY_API_OF_OTHER_PARTS);

    private static String part(String packageName) {
        Matcher m = PART.matcher(packageName);
        return m.matches() ? m.group(1) : null;
    }
}
