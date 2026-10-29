package com.eldercare.backend.administration.account;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.eldercare.backend.administration.account.api.RequiresPermission;
import com.eldercare.backend.administration.account.api.ScopeApi;
import com.eldercare.backend.common.data.BusinessCommand;
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
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class RequiresPermissionArchTest {

    /** Lệnh tự kiểm quyền: {@code Lớp.phươngThức} → cách kiểm. Thêm dòng thì người xem lại kiểm lệnh có tự kiểm thật. */
    static final Map<String, String> SELF_CHECKED = Map.ofEntries(
            Map.entry(
                    "com.eldercare.backend.administration.approval.internal.service.ApprovalRequestService.editContent",
                    "khóa Lập theo loại yêu cầu (RequestTypeHandler.createPermission), chỉ người yêu cầu; 000 FR-034"),
            Map.entry(
                    "com.eldercare.backend.administration.approval.internal.service.ApprovalRequestService.reject",
                    "khóa Duyệt theo loại yêu cầu (ApproverDirectory.canDecide); 000 FR-034"),
            Map.entry(
                    "com.eldercare.backend.administration.approval.internal.service.ApprovalRequestService.cancel",
                    "người yêu cầu, hoặc người duyệt khi đã Duyệt (ApproverDirectory.canDecide); 000 FR-034"),
            Map.entry(
                    "com.eldercare.backend.administration.correction.internal.service.CorrectionService.create",
                    "quyền theo loại bản ghi gốc (CorrectableRecordProvider.canRecord, CorrectionMethodSelector); 000 FR-026"),
            Map.entry(
                    "com.eldercare.backend.administration.parameter.internal.service.ParameterService.changeValue",
                    "PermissionApi.hasPermission với khóa tham số, chỉ Quản lý viện; 000 BR-M15-04"),
            Map.entry(
                    "com.eldercare.backend.administration.account.internal.auth.service.AccountService.earlyUnlock",
                    "Quản lý viện, hoặc Trưởng tầng, Người phụ trách ca cùng tầng và ca theo AssignmentApi lúc chạy"
                            + " (AccountService.earlyUnlockAuthority); 002 FR-019c"),
            Map.entry(
                    "com.eldercare.backend.administration.account.internal.offline.service.PendingReviewService.accept",
                    "người quyết định xem lại của tầng lúc chạy (ScopeApi.reviewDecider); 002 FR-041c, CHK056"),
            Map.entry(
                    "com.eldercare.backend.administration.account.internal.offline.service.PendingReviewService.reject",
                    "người quyết định xem lại của tầng lúc chạy (ScopeApi.reviewDecider); 002 FR-041c, CHK056"),
            Map.entry(
                    "com.eldercare.backend.notification.internal.directcontact.service.DirectContactService.record",
                    "vai trò Hành chính theo bảng FR-039 của 009 (Phụ lục 27 không có dòng thông báo)"),
            Map.entry(
                    "com.eldercare.backend.notification.internal.urgent.service.UrgentAcknowledgement.acknowledge",
                    "chỉ người nhận của thông báo (recipient_key của tài khoản), bảng FR-039 của 009"),
            Map.entry(
                    "com.eldercare.backend.notification.internal.urgent.service.CallResultService.record",
                    "người có quyền gọi theo nhiệm vụ lúc ghi: Người phụ trách ca, Trưởng tầng được giao của tầng, Quản lý viện; 009 FR-029"),
            Map.entry(
                    "com.eldercare.backend.notification.internal.channel.service.ChannelService.change",
                    "vai trò Quản lý viện theo bảng FR-039 của 009 (Phụ lục 27 không có dòng thông báo)"),
            Map.entry(
                    "com.eldercare.backend.administration.account.internal.auth.service.ResidentAccountService.createLoginCode",
                    "PermissionApi.hasPermission (RESIDENT_ACCOUNT, CREATE) trên người cao tuổi của tài khoản, đọc sau khi tra"
                            + " tài khoản; Điều dưỡng chỉ trong phạm vi (Phụ lục 27 ⁴⁸); 002 FR-069"));

    /**
     * Lệnh nút SOS của 007 tự kiểm bằng {@code BoundDeviceApi.identify} (token thiết bị, không có phiên) thay cho
     * {@code @RequiresPermission} (002 T093; FR-071, research A-15): mọi lệnh của lớp {@code SosController}.
     */
    static final String SOS_CONTROLLER = "SosController";

    @ArchTest
    static final ArchRule BR_M15_01_everyBusinessCommandRequiresPermission = methods()
            .that()
            .areAnnotatedWith(BusinessCommand.class)
            .should(new ArchCondition<JavaMethod>("có @RequiresPermission hoặc có trong danh sách lệnh tự kiểm quyền") {
                @Override
                public void check(JavaMethod method, ConditionEvents events) {
                    boolean ok = method.isAnnotatedWith(RequiresPermission.class)
                            || SELF_CHECKED.containsKey(method.getOwner().getName() + "." + method.getName())
                            || method.getOwner().getSimpleName().equals(SOS_CONTROLLER);
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
