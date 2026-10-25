package com.cambae.backend.common.scheduling;

import static com.tngtech.archunit.lang.conditions.ArchConditions.be;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Map;
import org.springframework.scheduling.annotation.Scheduled;

/**
 * FR-010a (Q-306, Q-307): Bộ lập lịch chỉ làm việc nguyên tắc cho phép, không tự sinh bản ghi nghiệp vụ. Mọi việc chạy
 * theo lịch nằm trong một {@link ScheduledJob}, và mỗi job phải có trong {@link #ALLOWED} kèm loại việc được phép.
 * Module thêm job thì thêm dòng ở đây; người xem lại kiểm job đó thuộc loại nào của FR-010a.
 */
@AnalyzeClasses(packages = "com.cambae.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class SchedulerScopeArchTest {

    /** Job → loại việc FR-010a cho phép. */
    static final Map<String, String> ALLOWED = Map.ofEntries(
            Map.entry(
                    "com.cambae.backend.m03.scheduling.BedReadinessReminderJob",
                    "nhắc giường Chờ sẵn sàng quá hạn một lần (003 FR-044)"),
            Map.entry(
                    "com.cambae.backend.m03.scheduling.AllocationReminderJob",
                    "nhắc dấu phân bổ còn mở theo CFG-M03-02, không lập yêu cầu (003 FR-025a/030/034)"),
            Map.entry(
                    "com.cambae.backend.m03.scheduling.FutureAllocationPastStartJob",
                    "cảnh báo phân bổ ban đầu quá giờ dự kiến, không tự bắt đầu (003 FR-023)"),
            Map.entry(
                    "com.cambae.backend.m03.scheduling.HoldExpiryJob",
                    "chuyển trạng thái hết hạn giữ hồ sơ chờ và báo hạn giữ người vắng (003 FR-015)"),
            Map.entry(
                    "com.cambae.backend.m15.approval.scheduling.ApplyDueRequestsJob",
                    "áp dụng yêu cầu đã duyệt khi tới ngày hiệu lực (FR-037)"),
            Map.entry("com.cambae.backend.m15.approval.scheduling.RemindPendingRequestsJob", "nhắc, báo (FR-031a)"),
            Map.entry(
                    "com.cambae.backend.m15.retention.scheduling.ExpiredDataPurgeJob",
                    "loại bỏ dữ liệu hết hạn lưu giữ (FR-051)"),
            Map.entry(
                    "com.cambae.backend.m15.account.scheduling.MarkInactiveAccountsJob",
                    "tự chuyển trạng thái theo giờ: tài khoản người thân Không hoạt động (002 FR-010)"),
            Map.entry(
                    "com.cambae.backend.m15.account.scheduling.WarnExpiringLicensesJob",
                    "cảnh báo giấy phép sắp hết hạn; chỉ ghi bảng kỹ thuật chống lặp (002 FR-039)"),
            Map.entry(
                    "com.cambae.backend.m15.account.scheduling.LockAfterResidencyEndJob",
                    "tự chuyển trạng thái theo giờ: khóa tài khoản người thân sau CFG-M01-04 (002 FR-049)"),
            Map.entry(
                    "com.cambae.backend.m13.scheduling.M13FastJob",
                    "nhắc, gửi lại, giải phóng giờ yên tĩnh, chuyển nhắc gọi (009 research B-10)"),
            Map.entry(
                    "com.cambae.backend.m13.scheduling.M13DailyJob",
                    "nhắc liên hệ trực tiếp còn chờ (009 research B-10)"),
            Map.entry(
                    "com.cambae.backend.m01.scheduling.RetryAllergyChecksJob",
                    "phát lại sự kiện kiểm tra dị ứng; không tạo bản ghi nghiệp vụ (001 BR-M01-12, research H-12)"),
            Map.entry(
                    "com.cambae.backend.m01.scheduling.TemporaryAbsenceOverdueJob",
                    "cảnh báo Tạm vắng quá hạn trở lại; không tạo bản ghi nghiệp vụ (001 BR-M01-01, FR-050)"),
            Map.entry(
                    "com.cambae.backend.m01.scheduling.MissingConsentReminderJob",
                    "nhắc Hành chính khi thiếu bản đồng ý; chỉ ghi bảng kỹ thuật nhắc (001 FR-012, T058)"),
            Map.entry(
                    "com.cambae.backend.m01.scheduling.NotifyPeriodicReassessmentJob",
                    "báo bác sĩ cần đánh giá lại định kỳ; chỉ ghi bảng kỹ thuật chống lặp (001 FR-039, T064)"),
            Map.entry(
                    "com.cambae.backend.m01.scheduling.AlertOverdueReassessmentJob",
                    "cảnh báo quá hạn đánh giá lại; chỉ ghi bảng kỹ thuật chống lặp (001 FR-041, T064)"),
            Map.entry(
                    "com.cambae.backend.m01.scheduling.RemindCareLevelChangeRequestJob",
                    "nhắc Hành chính lập yêu cầu đổi mức; chỉ ghi bảng kỹ thuật nhắc (001 FR-036, T064)"),
            Map.entry(
                    "com.cambae.backend.common.file.UnattachedFileCleanupJob",
                    "dọn tệp tải lên chưa gắn sau 24 giờ; dữ liệu tạm, không phải bản ghi nghiệp vụ (001 research H-13, T004)"));

    private static final DescribedPredicate<JavaClass> LISTED = DescribedPredicate.describe(
            "có trong danh sách job được phép của FR-010a", c -> ALLOWED.containsKey(c.getName()));

    @ArchTest
    static final ArchRule FR_010a_scheduledWorkOnlyInScheduledJobs = methods()
            .that()
            .areAnnotatedWith(Scheduled.class)
            .should()
            .beDeclaredInClassesThat()
            .implement(ScheduledJob.class)
            .because("việc chạy theo lịch phải là một job được xem xét theo FR-010a")
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule FR_010a_everyJobIsAnAllowedKind = classes()
            .that()
            .implement(ScheduledJob.class)
            .and()
            .areNotInterfaces()
            .should(be(LISTED))
            .because("Bộ lập lịch không tự sinh bản ghi nghiệp vụ (FR-010a); job mới phải ghi loại việc được phép");
}
