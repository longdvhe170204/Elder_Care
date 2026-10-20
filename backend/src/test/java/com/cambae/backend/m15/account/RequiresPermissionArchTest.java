package com.cambae.backend.m15.account;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.cambae.backend.common.data.BusinessCommand;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.ScopeApi;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import java.util.Map;

/**
 * 002 T015 (BR-M15-01, FR-041, FR-035a; research A-04): mọi lệnh nghiệp vụ ({@code @BusinessCommand}) có
 * {@code @RequiresPermission}, trừ lệnh trong {@link #SELF_CHECKED} (khóa quyền chỉ biết lúc chạy hoặc quy tắc riêng,
 * lệnh tự kiểm và ghi lý do ở đây). "Endpoint ghi" của tasks hiểu là lệnh mà endpoint gọi: endpoint ghi gọi lệnh
 * nghiệp vụ nên kiểm ở tầng lệnh (chủ dự án chọn, 2026-10-20). Lệnh nghiệp vụ không gọi trực tiếp
 * {@code ScopeApi.historyFilter} (bộ lọc lịch sử chỉ cho tra cứu, FR-035a).
 */
@AnalyzeClasses(packages = "com.cambae.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class RequiresPermissionArchTest {

    /** Lệnh tự kiểm quyền: {@code Lớp.phươngThức} → cách kiểm. Thêm dòng thì người xem lại kiểm lệnh có tự kiểm thật. */
    static final Map<String, String> SELF_CHECKED = Map.of(
            "com.cambae.backend.m15.approval.internal.ApprovalRequestService.editContent",
            "khóa Lập theo loại yêu cầu (RequestTypeHandler.createPermission), chỉ người yêu cầu; 000 FR-034",
            "com.cambae.backend.m15.approval.internal.ApprovalRequestService.reject",
            "khóa Duyệt theo loại yêu cầu (ApproverDirectory.canDecide); 000 FR-034",
            "com.cambae.backend.m15.approval.internal.ApprovalRequestService.cancel",
            "người yêu cầu, hoặc người duyệt khi đã Duyệt (ApproverDirectory.canDecide); 000 FR-034",
            "com.cambae.backend.m15.correction.internal.CorrectionService.create",
            "quyền theo loại bản ghi gốc (CorrectableRecordProvider.canRecord, CorrectionMethodSelector); 000 FR-026",
            "com.cambae.backend.m15.parameter.internal.ParameterService.changeValue",
            "PermissionApi.hasPermission với khóa tham số, chỉ Quản lý viện; 000 BR-M15-04",
            "com.cambae.backend.m15.account.auth.internal.AccountService.earlyUnlock",
            "Quản lý viện, hoặc Trưởng tầng, Người phụ trách ca cùng tầng và ca theo AssignmentApi lúc chạy"
                    + " (AccountService.earlyUnlockAuthority); 002 FR-019c",
            "com.cambae.backend.m15.account.offline.internal.PendingReviewService.accept",
            "người quyết định xem lại của tầng lúc chạy (ScopeApi.reviewDecider); 002 FR-041c, CHK056",
            "com.cambae.backend.m15.account.offline.internal.PendingReviewService.reject",
            "người quyết định xem lại của tầng lúc chạy (ScopeApi.reviewDecider); 002 FR-041c, CHK056");

    @ArchTest
    static final ArchRule BR_M15_01_everyBusinessCommandRequiresPermission = methods()
            .that()
            .areAnnotatedWith(BusinessCommand.class)
            .should(new ArchCondition<JavaMethod>("có @RequiresPermission hoặc có trong danh sách lệnh tự kiểm quyền") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    boolean ok = method.isAnnotatedWith(RequiresPermission.class)
                            || SELF_CHECKED.containsKey(method.getOwner().getName() + "." + method.getName());
                    events.add(new SimpleConditionEvent(method, ok, method.getFullName()));
                }
            })
            .because("lệnh chỉ chạy sau khi kiểm quyền ba lớp (BR-M15-01, FR-041)");

    @ArchTest
    static final ArchRule FR_035a_businessCommandDoesNotUseHistoryFilter = methods()
            .that()
            .areAnnotatedWith(BusinessCommand.class)
            .should(new ArchCondition<JavaMethod>("không gọi ScopeApi.historyFilter") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    method.getMethodCallsFromSelf().stream()
                            .filter(call -> call.getTarget().getName().equals("historyFilter")
                                    && call.getTargetOwner().isAssignableTo(ScopeApi.class))
                            .forEach(call -> events.add(SimpleConditionEvent.violated(call, call.getDescription())));
                }
            })
            .because("bộ lọc lịch sử chỉ dùng cho tra cứu, lệnh dùng phạm vi hiện hành (FR-035a)");
}
