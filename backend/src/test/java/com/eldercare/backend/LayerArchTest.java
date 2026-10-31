package com.eldercare.backend;

import static com.tngtech.archunit.base.DescribedPredicate.not;
import static com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.eldercare.backend.administration.retention.api.RetentionDatabase;
import com.eldercare.backend.administration.retention.api.spi.RetentionProvider;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Chia tầng trong module (research R-02 của 000, mục "Cấu trúc trong module"; chủ dự án duyệt 2026-10-21): áp cho mọi
 * module M01 … M15 (tên gói theo constitution X 3.3.0, research R-02). Ranh giới giữa module do
 * {@link ModuleBoundaryTest}, giữa các phần của M15 do {@code M15PartBoundaryArchTest}.
 */
@AnalyzeClasses(packages = "com.eldercare.backend", importOptions = ImportOption.DoNotIncludeTests.class)
class LayerArchTest {

    /** Gói của mười lăm module M01 … M15; không gồm {@code common}. */
    static final String[] MODULES = {
        "resident",
        "admission",
        "roombedallocation",
        "care",
        "incident",
        "health",
        "medication",
        "nutrition",
        "staffing",
        "family",
        "billing",
        "belongings",
        "notification",
        "reporting",
        "administration"
    };

    static String[] inModules(String... suffixes) {
        return Arrays.stream(suffixes)
                .flatMap(suffix -> Arrays.stream(MODULES).map(module -> "com.eldercare.backend." + module + suffix))
                .toArray(String[]::new);
    }

    private static String[] concat(String[]... parts) {
        return Stream.of(parts).flatMap(Arrays::stream).toArray(String[]::new);
    }

    private static final String[] WEB = inModules("..web..");
    private static final String[] SERVICE = inModules("..internal..service..");
    private static final String[] DOMAIN = inModules("..internal..domain..");
    private static final String[] PERSISTENCE = inModules("..internal..persistence..");

    /*
     * Chỗ lệch có sẵn trước các luật trên (rà 2026-10-30), chủ module sửa khi converge feature của mình (team-plan mục
     * 8). Sửa xong thì xóa dòng; không thêm dòng mới.
     */

    /** E (008): chức năng của staffing chuyển vào {@code staffing.internal.<chức năng>}, controller vào {@code staffing.web}. */
    private static final String[] PENDING_MODULE_ROOT_PACKAGES = Stream.of(
                    "assignment", "credential", "duty", "handover", "ratio", "shift", "staff")
            .map(part -> "com.eldercare.backend.staffing." + part + "..")
            .toArray(String[]::new);

    /** D (006): request, response của medication về {@code medication.web}; E (008): {@code service.port} về {@code service}. */
    private static final String[] PENDING_LAYER_SUB_PACKAGES = {
        "com.eldercare.backend.medication.internal.service.request",
        "com.eldercare.backend.medication.internal.service.response",
        "com.eldercare.backend.staffing..internal.service.port"
    };

    /** Câu SQL chuyển vào lớp {@code *Store} trong {@code persistence}: B (resident), E (staffing). */
    private static final Set<String> PENDING_JDBC_OUTSIDE_PERSISTENCE = Set.of(
            "resident.internal.assessment.service.CareLevelChangeRequestReminderService",
            "resident.internal.assessment.service.ReassessmentNoticeService",
            "resident.internal.consent.service.MissingConsentReminderService",
            "resident.internal.status.service.TemporaryAbsenceOverdueAlert",
            "resident.internal.wish.service.WishReminderService",
            "staffing.assignment.internal.service.AssignmentReminders",
            "staffing.credential.internal.service.CredentialInvalidityEffects",
            "staffing.credential.internal.service.CredentialReminders",
            "staffing.credential.internal.service.CredentialService",
            "staffing.duty.internal.service.PracticeScopeLookup",
            "staffing.handover.internal.service.HandoverCorrection",
            "staffing.internal.config.M09GovernanceConfig",
            "staffing.shift.internal.service.ShiftLifecycle",
            "staffing.staff.internal.service.StaffEmploymentEffects",
            "staffing.staff.internal.service.StaffService");

    /** C (005): care; record, enum ở gốc {@code api} chuyển vào {@code api.dto}. */
    private static final Set<String> PENDING_API_ROOT_TYPES = Set.of("care.api.MedicationChecklistItem");

    /** Record lồng chuyển ra {@code domain}: B (resident), C (roombedallocation). */
    private static final Set<String> PENDING_NESTED_RECORD_OWNERS = Set.of(
            "resident.internal.profile.persistence.ResidentLookup",
            "resident.internal.wish.persistence.EndOfLifeWishStore",
            "resident.internal.wish.persistence.TreatmentWishStore",
            "roombedallocation.internal.allocation.persistence.AllocationReminderStore",
            "roombedallocation.internal.bed.persistence.BedAssetStateStore",
            "roombedallocation.internal.bed.persistence.BedHoldStore",
            "roombedallocation.internal.bed.persistence.BedReadinessStore");

    /**
     * Lớp trong {@code internal} nằm ở một tầng; module lớn có gói chức năng ở giữa ({@code internal.auth.service}). Tầng
     * không có gói con ({@code service.request}, {@code service.port}).
     */
    @ArchTest
    static final ArchRule internalClassesLiveInALayer = classes()
            .that()
            .resideInAnyPackage(inModules("..internal.."))
            .and()
            .resideOutsideOfPackages(PENDING_LAYER_SUB_PACKAGES)
            .and()
            .doNotHaveSimpleName("package-info")
            .should()
            .resideInAnyPackage(inModules(
                    "..internal..service", "..internal..domain", "..internal..persistence", "..internal..config"))
            .allowEmptyShould(true);

    /** Controller chỉ gọi service; không đọc ghi dữ liệu trực tiếp (yêu cầu tổ chức lại, mục 3.2). */
    @ArchTest
    static final ArchRule webDoesNotAccessData = noClasses()
            .that()
            .resideInAnyPackage(WEB)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                    concat(PERSISTENCE, new String[] {"org.springframework.jdbc..", "org.springframework.data.."}))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule domainDependsOnNoOtherLayer = noClasses()
            .that()
            .resideInAnyPackage(DOMAIN)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(concat(SERVICE, PERSISTENCE, WEB))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule persistenceDoesNotCallServiceOrWeb = noClasses()
            .that()
            .resideInAnyPackage(PERSISTENCE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(concat(SERVICE, WEB))
            .allowEmptyShould(true);

    @ArchTest
    static final ArchRule serviceDoesNotDependOnWeb = noClasses()
            .that()
            .resideInAnyPackage(SERVICE)
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(WEB)
            .allowEmptyShould(true);

    /** Gói {@code api} là hợp đồng: không phụ thuộc phần triển khai của chính module (yêu cầu tổ chức lại, mục 3.1). */
    @ArchTest
    static final ArchRule apiDoesNotDependOnImplementation = noClasses()
            .that()
            .resideInAnyPackage(inModules("..api.."))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(concat(inModules("..internal.."), WEB))
            .allowEmptyShould(true);

    /**
     * Gói cấp một của module chỉ là {@code api}, {@code web}, {@code internal}, {@code scheduling}; M15 thêm một cấp phần
     * ({@code administration.approval.api}). Module lớn chia chức năng trong {@code internal}, không ở gốc module.
     */
    @ArchTest
    static final ArchRule moduleTopLevelPackages = classes()
            .that()
            .resideInAnyPackage(inModules(".."))
            .and()
            .resideOutsideOfPackages(PENDING_MODULE_ROOT_PACKAGES)
            .and()
            .doNotHaveSimpleName("package-info")
            .should()
            .resideInAnyPackage(Stream.of(MODULES)
                    .flatMap(module -> Stream.of("api", "web", "internal", "scheduling")
                            .map(top -> "com.eldercare.backend." + module
                                    + (module.equals("administration") ? ".*." : ".") + top + ".."))
                    .toArray(String[]::new))
            .allowEmptyShould(true);

    /**
     * Chỉ {@code persistence} đọc ghi bảng bằng JDBC; service gọi lớp {@code *Store} (README backend mục 3). Phần
     * {@code administration.retention} và lớp cài {@link RetentionProvider} dùng kết nối {@code eldercare_retention} của
     * {@link RetentionDatabase} (research R-05, R-11 của 000).
     */
    @ArchTest
    static final ArchRule jdbcOnlyInPersistence = noClasses()
            .that()
            .resideInAnyPackage(inModules(".."))
            .and()
            .resideOutsideOfPackages(
                    concat(PERSISTENCE, new String[] {"com.eldercare.backend.administration.retention.."}))
            .and(not(assignableTo(RetentionProvider.class)))
            .and(not(pending(PENDING_JDBC_OUTSIDE_PERSISTENCE)))
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage("org.springframework.jdbc..")
            .allowEmptyShould(true);

    /** Gốc {@code api} chỉ chứa interface và chú thích; record, enum đặt ở {@code api.dto}, sự kiện ở {@code api.events}. */
    @ArchTest
    static final ArchRule apiRootHoldsInterfacesOnly = classes()
            .that()
            .resideInAnyPackage(inModules("..api"))
            .and()
            .areTopLevelClasses()
            .and()
            .doNotHaveSimpleName("package-info")
            .and(not(pending(PENDING_API_ROOT_TYPES)))
            .should()
            .beInterfaces()
            .allowEmptyShould(true);

    /** Lớp persistence trả record của {@code domain}, không khai báo record lồng (README backend mục 2). */
    @ArchTest
    static final ArchRule noNestedRecordsInPersistence = noClasses()
            .that()
            .resideInAnyPackage(PERSISTENCE)
            .and()
            .areNestedClasses()
            .and(not(pending(PENDING_NESTED_RECORD_OWNERS)))
            .should()
            .beRecords()
            .allowEmptyShould(true);

    /** Lớp (kể cả lớp lồng, lambda) có lớp ngoài cùng nằm trong danh sách chờ sửa; tên tính từ gói gốc. */
    private static DescribedPredicate<JavaClass> pending(Set<String> outerClasses) {
        return DescribedPredicate.describe(
                "trong danh sách chờ sửa",
                c -> outerClasses.contains(
                        c.getName().replace("com.eldercare.backend.", "").replaceAll("\\$.*", "")));
    }
}
