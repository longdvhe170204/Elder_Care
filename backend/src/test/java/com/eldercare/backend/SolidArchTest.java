package com.eldercare.backend;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.domain.JavaConstructorCall;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import com.tngtech.archunit.library.GeneralCodingRules;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phần tự động kiểm được của luật SOLID (constitution XVI 3.3.0, research R-02 mục "Nguyên tắc SOLID"). Các luật còn
 * lại (một lớp một trách nhiệm, interface nhỏ theo người dùng, mở rộng bằng SPI hoặc strategy) do review kiểm.
 */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class SolidArchTest {

    /** DIP: phụ thuộc được tiêm qua constructor, không tiêm vào trường. */
    @ArchTest
    static final ArchRule noFieldInjection = GeneralCodingRules.NO_CLASSES_SHOULD_USE_FIELD_INJECTION;

    /** DIP: không tiêm qua setter hay phương thức thường; chỉ constructor (một constructor thì không cần chú thích). */
    @ArchTest
    static final ArchRule noMethodInjection = noMethods()
            .should()
            .beAnnotatedWith(Autowired.class)
            .because("phụ thuộc chỉ tiêm qua constructor (constitution XVI)");

    /** DIP: bean không tự tạo bean khác bằng {@code new}; để Spring tiêm (trừ lớp cấu hình). */
    @ArchTest
    static final ArchRule beansAreNotInstantiatedDirectly = noClasses()
            .that()
            .areNotAnnotatedWith(Configuration.class)
            .should(constructBeans())
            .because("bean do Spring tạo và tiêm, không tự khởi tạo (constitution XVI)");

    private static ArchCondition<com.tngtech.archunit.core.domain.JavaClass> constructBeans() {
        return new ArchCondition<>("tạo bean bằng new") {
            @Override
            public void check(com.tngtech.archunit.core.domain.JavaClass item, ConditionEvents events) {
                for (JavaConstructorCall call : item.getConstructorCallsFromSelf()) {
                    var target = call.getTargetOwner();
                    boolean bean = target.isAnnotatedWith(Service.class)
                            || target.isAnnotatedWith(Component.class)
                            || target.isAnnotatedWith(Repository.class)
                            || target.isAnnotatedWith(RestController.class);
                    if (bean && !target.equals(item)) {
                        events.add(SimpleConditionEvent.violated(call, call.getDescription()));
                    }
                }
            }
        };
    }
}
