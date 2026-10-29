package com.eldercare.backend.resident;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.resident.internal.status.service.ProfileOpenGuard;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.lang.syntax.ArchRuleDefinition;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 001 T010 (BR-M01-05): mọi lệnh nghiệp vụ ({@code @BusinessCommand}) của M01 gọi {@link ProfileOpenGuard} trong chính
 * lệnh đó, trừ lệnh trong {@link #NOT_ON_A_PROFILE_OR_ALLOWED_WHEN_CLOSED} (lệnh không ghi vào hồ sơ, hoặc được làm cả
 * trên hồ sơ trạng thái cuối). Thêm dòng thì người xem lại kiểm lý do.
 */
@AnalyzeClasses(packages = "com.eldercare.backend.resident", importOptions = ImportOption.DoNotIncludeTests.class)
class ProfileOpenGuardArchTest {

    /** {@code Lớp.phươngThức} → lý do. */
    static final Map<String, String> NOT_ON_A_PROFILE_OR_ALLOWED_WHEN_CLOSED = Map.of(
            "com.eldercare.backend.resident.internal.health.service.CatalogService.deactivate",
            "danh mục dùng chung, không gắn hồ sơ người cao tuổi",
            "com.eldercare.backend.resident.internal.health.service.CatalogService.reactivate",
            "danh mục dùng chung, không gắn hồ sơ người cao tuổi",
            "com.eldercare.backend.resident.internal.consent.service.ConsentService.withdraw",
            "rút lại đồng ý làm được cả trên hồ sơ trạng thái cuối (FR-014a, ngoại lệ của BR-M01-05)");

    static ArchRule everyBusinessCommandCallsGuard() {
        return ArchRuleDefinition.methods()
                .that()
                .areAnnotatedWith(BusinessCommand.class)
                .should(new ArchCondition<JavaMethod>("gọi ProfileOpenGuard hoặc có trong danh sách ngoại lệ") {
                    @Override
                    public void check(JavaMethod method, ConditionEvents events) {
                        boolean exempt = NOT_ON_A_PROFILE_OR_ALLOWED_WHEN_CLOSED.containsKey(
                                method.getOwner().getName() + "." + method.getName());
                        boolean callsGuard = method.getMethodCallsFromSelf().stream()
                                .anyMatch(call -> call.getTargetOwner().isEquivalentTo(ProfileOpenGuard.class));
                        events.add(new SimpleConditionEvent(method, exempt || callsGuard, method.getFullName()));
                    }
                })
                .because("hồ sơ ở trạng thái cuối không ghi mới (BR-M01-05)")
                .allowEmptyShould(true);
    }

    @ArchTest
    static final ArchRule BR_M01_05_everyBusinessCommandCallsProfileOpenGuard = everyBusinessCommandCallsGuard();

    /** Chứng minh luật không rỗng: lớp mẫu có gọi guard thì qua, không gọi thì bị bắt. */
    @Test
    void BR_M01_05_rule_catchesCommandWithoutGuard() {
        JavaClasses fixtures = new ClassFileImporter().importPackages("com.eldercare.backend.archfixture.m01guard");

        assertThat(violates(fixtures, "GuardedService")).isFalse();
        assertThat(violates(fixtures, "UnguardedService")).isTrue();
    }

    private static boolean violates(JavaClasses fixtures, String simpleName) {
        DescribedPredicate<JavaClass> named =
                DescribedPredicate.describe(simpleName, c -> c.getSimpleName().equals(simpleName));
        JavaClasses selected = fixtures.that(named);
        assertThat(selected).hasSize(1);
        return everyBusinessCommandCallsGuard().evaluate(selected).hasViolation();
    }
}
