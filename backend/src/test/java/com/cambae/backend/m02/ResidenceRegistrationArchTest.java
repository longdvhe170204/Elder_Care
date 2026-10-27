package com.cambae.backend.m02;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaCodeUnit;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * 018 T001 (Q-306, research Z-01, Z-06): Bộ lập lịch không lập khai báo. Job M02 ủy cho lớp chạy trong {@code internal}
 * (như {@code M02DailyJob} → {@code M02DailyRunner}), nên luật đi theo chuỗi gọi phương thức từ gói {@code m02.scheduling}
 * và không được tới phương thức lập khai báo trong gói {@code m02.internal.registration} (xem
 * {@link #isRegistrationCreation}). Quy ước tên đi kèm (plan 018, glossary 24.5): chỉ phương thức lập khai báo bắt đầu
 * bằng {@code create}; bảng khai báo ghi qua {@code ResidenceRegistrationStore.insert…}.
 */
class ResidenceRegistrationArchTest {

    private static final JavaClasses MAIN = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.cambae.backend");

    private static final String FIXTURES = "com.cambae.backend.archfixture.m02registration";

    @Test
    void Q_306_schedulingDoesNotCreateRegistrations() {
        List<String> reached = creationsReachedFrom(
                MAIN,
                JavaClass.Predicates.resideInAPackage("com.cambae.backend.m02.scheduling.."),
                "com.cambae.backend.m02.internal.registration");

        assertThat(reached).isEmpty();
    }

    /** Chứng minh luật không rỗng: lập khai báo qua lớp chạy trung gian, lambda, tham chiếu phương thức bị bắt. */
    @Test
    void Q_306_rule_catchesCreationThroughRunnerLambdaAndMethodReference() {
        JavaClasses fixtures = new ClassFileImporter().importPackages(FIXTURES);

        assertThat(creationsReachedFrom(fixtures, named("ViolatingJob"), FIXTURES))
                .containsExactly(FIXTURES + ".RegistrationService.create(long)");
        assertThat(creationsReachedFrom(fixtures, named("StoreBypassJob"), FIXTURES))
                .containsExactly(FIXTURES + ".persistence.ResidenceRegistrationStore.insert(long)");
    }

    /**
     * Job 018 thật gọi việc qua interface ({@code RegistrationDailyRunner} → {@code RegistrationDailyTask}, Phase 2): luật
     * đi vào mọi bản cài, bắt bản cài lập khai báo, không bắt bản cài chỉ chuyển Hết hạn.
     */
    @Test
    void Q_306_rule_followsTasksCalledThroughAnInterface() {
        JavaClasses fixtures = new ClassFileImporter().importPackages(FIXTURES);

        assertThat(creationsReachedFrom(fixtures, named("TaskInterfaceJob"), FIXTURES))
                .containsExactly(FIXTURES + ".RegistrationService.create(long)");
    }

    /** Luật chỉ cấm lập khai báo: job chuyển Hết hạn, ghi khóa nhắc, hay gọi lệnh khác của khai báo không bị chặn. */
    @Test
    void Q_306_rule_allowsJobsThatDoNotCreateRegistrations() {
        JavaClasses fixtures = new ClassFileImporter().importPackages(FIXTURES);

        assertThat(creationsReachedFrom(fixtures, named("CompliantJob"), FIXTURES))
                .isEmpty();
        assertThat(creationsReachedFrom(fixtures, named("OtherCommandJob"), FIXTURES))
                .isEmpty();
    }

    /**
     * Phương thức lập khai báo trong {@code registrationPackage} đi tới được từ mọi phương thức của lớp {@code entry}.
     * Đi theo lời gọi và tham chiếu phương thức; tới một lớp thì đi cả các lambda của lớp đó (lời gọi trong lambda nằm
     * ở phương thức tổng hợp riêng); gọi qua interface thì đi cả bản cài.
     */
    static List<String> creationsReachedFrom(
            JavaClasses classes, DescribedPredicate<JavaClass> entry, String registrationPackage) {
        Deque<JavaCodeUnit> todo = classes.stream()
                .filter(entry)
                .flatMap(c -> c.getCodeUnits().stream())
                .collect(Collectors.toCollection(ArrayDeque::new));
        Set<JavaCodeUnit> seen = new HashSet<>(todo);
        List<String> reached = new ArrayList<>();
        while (!todo.isEmpty()) {
            JavaCodeUnit unit = todo.pop();
            if (unit instanceof JavaMethod method && isRegistrationCreation(method, registrationPackage)) {
                reached.add(method.getFullName());
                continue;
            }
            Stream.concat(
                            unit.getMethodCallsFromSelf().stream()
                                    .map(call -> call.getTarget().resolveMember()),
                            unit.getMethodReferencesFromSelf().stream()
                                    .map(ref -> ref.getTarget().resolveMember()))
                    .flatMap(Optional::stream)
                    .flatMap(ResidenceRegistrationArchTest::withLambdasAndImplementations)
                    .filter(seen::add)
                    .forEach(todo::add);
        }
        return reached;
    }

    private static Stream<JavaMethod> withLambdasAndImplementations(JavaMethod method) {
        JavaClass owner = method.getOwner();
        String[] parameters =
                method.getRawParameterTypes().stream().map(JavaClass::getName).toArray(String[]::new);
        Stream<JavaMethod> implementations = owner.getAllSubclasses().stream()
                .map(sub -> sub.tryGetMethod(method.getName(), parameters))
                .flatMap(Optional::stream);
        Stream<JavaMethod> lambdas =
                owner.getMethods().stream().filter(m -> m.getName().startsWith("lambda$"));
        return Stream.of(Stream.of(method), implementations, lambdas).flatMap(s -> s);
    }

    private static DescribedPredicate<JavaClass> named(String simpleName) {
        return DescribedPredicate.describe(simpleName, c -> c.getSimpleName().equals(simpleName));
    }

    /**
     * Lập khai báo (Q-306: khai báo, khai báo gia hạn do Hành chính lập): phương thức {@code create…} trong gói khai báo
     * (có hay không {@code @BusinessCommand}), hoặc {@code insert…} của {@code ResidenceRegistrationStore} (ghi thẳng
     * bảng khai báo). Lệnh khác trên khai báo đã có và việc của job (nhắc, Hết hạn, nghĩa vụ) không thuộc luật này.
     */
    private static boolean isRegistrationCreation(JavaMethod method, String registrationPackage) {
        JavaClass owner = method.getOwner();
        if (!owner.getPackageName().startsWith(registrationPackage)) {
            return false;
        }
        return method.getName().startsWith("create")
                || (owner.getSimpleName().equals("ResidenceRegistrationStore")
                        && method.getName().startsWith("insert"));
    }
}
