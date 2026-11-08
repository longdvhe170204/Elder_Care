package com.eldercare.backend.roombedallocation;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.roombedallocation.internal.bed.domain.BedAssetCommandPort;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** 019 T001 (research W-01, W-02; Q-306): ba gói của 019, port giường, job không tạo bản ghi, M03 không phụ thuộc M11. */
class AssetArchTest {
    static final String ROOT = "com.eldercare.backend.";
    static final List<String> PACKAGES = List.of(
            "billing.internal.procurement", "roombedallocation.internal.asset", "roombedallocation.internal.vehicle");
    static final List<String> RECORD_PACKAGES =
            PACKAGES.stream().map(p -> ROOT + p).toList();

    /** W-01: chỉ tài sản của 019 (và chính 003, nơi khai, cài cổng) dùng cổng lệnh giường theo tài sản. */
    static final ArchRule ONLY_ASSET_USES_BED_PORT = noClasses()
            .that()
            .resideOutsideOfPackages(
                    ROOT + "roombedallocation.internal.asset..", ROOT + "roombedallocation.internal.bed..")
            .should()
            .dependOnClassesThat()
            .areAssignableTo(BedAssetCommandPort.class)
            .because("019 W-01: chỉ roombedallocation.internal.asset dùng port giường của 003")
            .allowEmptyShould(true);

    /** Q-306: job của M03, M11 không tạo yêu cầu mua sắm, tài sản, lịch xe. */
    static final ArchRule NO_JOB_CREATES_019_RECORDS = noClasses()
            .that()
            .resideInAnyPackage(ROOT + "roombedallocation.scheduling..", ROOT + "billing.scheduling..")
            .should()
            .callMethodWhere(new DescribedPredicate<JavaMethodCall>("gọi lệnh tạo yêu cầu, tài sản, lịch xe của 019") {
                @Override
                public boolean test(JavaMethodCall call) {
                    var owner = call.getTarget().getOwner().getPackageName();
                    var name = call.getTarget().getName();
                    return RECORD_PACKAGES.stream().anyMatch(owner::startsWith)
                            && (name.startsWith("create") || name.startsWith("book") || name.startsWith("record"));
                }
            })
            .because("Q-306/Q-307: job 019 chỉ gắn dấu quá hạn, nhắc, báo")
            .allowEmptyShould(true);

    /** W-02: M11 → M02 → M03 nên M03 không gọi M11; M03 khai SPI để M11 cài. */
    static final ArchRule M03_DOES_NOT_DEPEND_ON_M11 = noClasses()
            .that()
            .resideInAPackage(ROOT + "roombedallocation..")
            .should()
            .dependOnClassesThat()
            .resideInAPackage(ROOT + "billing..")
            .because("019 W-02: tránh vòng M03 ↔ M11")
            .allowEmptyShould(true);

    static JavaClasses classes;

    @TempDir
    Path temporary;

    @BeforeAll
    static void load() {
        classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.eldercare.backend");
    }

    @Test
    void W_01_threePackagesExist() {
        for (var name : PACKAGES)
            assertThat(classes.containPackage(ROOT + name)).as(name).isTrue();
    }

    @Test
    void W_01_onlyAssetPackageUsesBedAssetCommandPort() {
        ONLY_ASSET_USES_BED_PORT.check(classes);
    }

    @Test
    void Q_306_jobsDoNotCreateProcurementAssetOrVehicleRecords() {
        NO_JOB_CREATES_019_RECORDS.check(classes);
    }

    @Test
    void W_02_roomBedAllocationDoesNotDependOnBilling() {
        M03_DOES_NOT_DEPEND_ON_M11.check(classes);
    }

    @Test
    void rulesRejectViolatingFixtures() throws Exception {
        String[][] sources = {
            {
                "AssetCommands.java",
                "package com.eldercare.backend.roombedallocation.internal.asset.service;"
                        + " public class AssetCommands {public void recordIncrease(){}}"
            },
            {
                "VehicleCommands.java",
                "package com.eldercare.backend.roombedallocation.internal.vehicle.service;"
                        + " public class VehicleCommands {public void book(){}}"
            },
            {
                "ProcurementCommands.java",
                "package com.eldercare.backend.billing.internal.procurement.service;"
                        + " public class ProcurementCommands {public void create(){}}"
            },
            {
                "BadJob.java",
                "package com.eldercare.backend.roombedallocation.scheduling; public class BadJob {"
                        + "public void run(){"
                        + "new com.eldercare.backend.roombedallocation.internal.asset.service.AssetCommands().recordIncrease();"
                        + "new com.eldercare.backend.roombedallocation.internal.vehicle.service.VehicleCommands().book();"
                        + "new com.eldercare.backend.billing.internal.procurement.service.ProcurementCommands().create();}}"
            }
        };
        var args = new java.util.ArrayList<String>(List.of("-d", temporary.toString()));
        for (var source : sources) {
            var file = temporary.resolve(source[0]);
            Files.writeString(file, source[1]);
            args.add(file.toString());
        }
        assertThat(ToolProvider.getSystemJavaCompiler().run(null, null, null, args.toArray(String[]::new)))
                .isZero();
        var fixture = new ClassFileImporter().importPath(temporary);

        var jobs = NO_JOB_CREATES_019_RECORDS.evaluate(fixture);
        assertThat(jobs.getFailureReport().getDetails()).hasSize(3);
        var dependency = M03_DOES_NOT_DEPEND_ON_M11.evaluate(fixture);
        assertThat(dependency.hasViolation()).isTrue();
    }
}
