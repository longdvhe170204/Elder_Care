package com.cambae.backend.m04;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.cambae.backend.m04.internal.scheduling.service.CareDailyWork;
import com.cambae.backend.m04.internal.scheduling.service.CareMinuteWork;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Set;

/** T009/C-02/C-12: luật dùng cả với application bytecode và fixture cố ý vi phạm. */
final class M04ArchitectureRules {
    private M04ArchitectureRules() {}

    static final ArchRule NO_DIRECT_HIGHER_MODULES = noClasses()
            .that()
            .resideInAPackage("com.cambae.backend.m04..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("com.cambae.backend.m05..", "com.cambae.backend.m06..", "com.cambae.backend.m09..")
            .because("005 đọc nguồn 007/008 qua SPI do chính 005 khai báo")
            .allowEmptyShould(true);
    static final ArchRule NO_JOB_CREATES_RECORDS = noClasses()
            .that(new DescribedPredicate<JavaClass>("job hoặc bước được job M04 đăng ký") {
                @Override
                public boolean test(JavaClass type) {
                    return type.getPackageName().startsWith("com.cambae.backend.m04.scheduling")
                            || type.isAssignableTo(CareMinuteWork.class)
                            || type.isAssignableTo(CareDailyWork.class);
                }
            })
            .should()
            .callMethodWhere(new DescribedPredicate<JavaMethodCall>("gọi lệnh tạo/lập bản ghi M04") {
                @Override
                public boolean test(JavaMethodCall call) {
                    return call.getTarget().getOwner().getPackageName().startsWith("com.cambae.backend.m04.internal.")
                            && Set.of("create", "createTasks", "planShift", "createAdHoc", "createRework")
                                    .contains(call.getTarget().getName());
                }
            })
            .because("Q-306/Q-307: scheduler chỉ chuyển trạng thái, nhắc, cảnh báo")
            .allowEmptyShould(true);
}
