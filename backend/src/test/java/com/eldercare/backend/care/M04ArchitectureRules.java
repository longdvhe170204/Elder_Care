package com.eldercare.backend.care;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.eldercare.backend.care.internal.scheduling.service.CareDailyWork;
import com.eldercare.backend.care.internal.scheduling.service.CareMinuteWork;
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
            .resideInAPackage("com.eldercare.backend.care..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    "com.eldercare.backend.incident..",
                    "com.eldercare.backend.health..",
                    "com.eldercare.backend.staffing..")
            .because("005 đọc nguồn 007/008 qua SPI do chính 005 khai báo")
            .allowEmptyShould(true);
    static final ArchRule NO_JOB_CREATES_RECORDS = noClasses()
            .that(new DescribedPredicate<JavaClass>("job hoặc bước được job M04 đăng ký") {
                @Override
                public boolean test(JavaClass type) {
                    return type.getPackageName().startsWith("com.eldercare.backend.care.scheduling")
                            || type.isAssignableTo(CareMinuteWork.class)
                            || type.isAssignableTo(CareDailyWork.class);
                }
            })
            .should()
            .callMethodWhere(new DescribedPredicate<JavaMethodCall>("gọi lệnh tạo/lập bản ghi M04") {
                @Override
                public boolean test(JavaMethodCall call) {
                    return call.getTarget()
                                    .getOwner()
                                    .getPackageName()
                                    .startsWith("com.eldercare.backend.care.internal.")
                            && Set.of("create", "createTasks", "planShift", "createAdHoc", "createRework")
                                    .contains(call.getTarget().getName());
                }
            })
            .because("Q-306/Q-307: scheduler chỉ chuyển trạng thái, nhắc, cảnh báo")
            .allowEmptyShould(true);

    /** 014 T001 (research L-01): năm gói con của 014; job không tạo buổi, danh sách, mục kiểm tra, công việc làm lại. */
    static final Set<String> ACTIVITY_PACKAGES = Set.of("activity", "session", "trip", "wellbeing", "qualitycheck");

    static final ArchRule NO_JOB_CREATES_ACTIVITY_RECORDS = noClasses()
            .that(new DescribedPredicate<JavaClass>("job hoặc bước được job M04 đăng ký") {
                @Override
                public boolean test(JavaClass type) {
                    return type.getPackageName().startsWith("com.eldercare.backend.care.scheduling")
                            || type.isAssignableTo(CareMinuteWork.class)
                            || type.isAssignableTo(CareDailyWork.class);
                }
            })
            .should()
            .callMethodWhere(
                    new DescribedPredicate<JavaMethodCall>("gọi lệnh tạo bản ghi của 014 hoặc công việc làm lại") {
                        @Override
                        public boolean test(JavaMethodCall call) {
                            var owner = call.getTarget().getOwner().getPackageName();
                            var name = call.getTarget().getName();
                            boolean activity = ACTIVITY_PACKAGES.stream()
                                    .anyMatch(p -> owner.startsWith("com.eldercare.backend.care.internal." + p));
                            return (activity && (name.startsWith("create") || name.startsWith("add")))
                                    || (owner.startsWith("com.eldercare.backend.care.internal.")
                                            && name.equals("createRework"));
                        }
                    })
            .because("Q-306/Q-307: job 014 chỉ chuyển trạng thái theo giờ, nhắc, cảnh báo")
            .allowEmptyShould(true);
}
