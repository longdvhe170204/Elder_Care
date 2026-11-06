package com.eldercare.backend.belongings;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eldercare.backend.incident.api.IncidentCommandApi;
import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.Test;

/**
 * 013 T001 (research K-01, Q-306, Q-307): việc định kỳ của M12 chỉ nhắc, báo, chuyển trạng thái theo giờ; không lập
 * phiếu kiểm kê và không gọi lệnh sự cố của 007. Việc định kỳ gồm gói {@code scheduling} và phần nội bộ của nó
 * ({@code internal.scheduling}, như {@code family}).
 */
class M12ArchTest {

    private static final String JOBS = "com.eldercare.backend.belongings..scheduling..";
    private static final String INVENTORY_CHECK = "com.eldercare.backend.belongings.internal.inventorycheck.";

    private static final JavaClasses MAIN = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.eldercare.backend");

    /** Q-306: không lớp nào của việc định kỳ gọi hàm tạo, chèn của chức năng kiểm kê. */
    static ArchRule jobsDoNotCreateInventoryChecks(String jobPackages, String inventoryCheckOwnerPrefix) {
        return noClasses()
                .that()
                .resideInAPackage(jobPackages)
                .should()
                .callMethodWhere(new DescribedPredicate<JavaMethodCall>("tạo phiếu kiểm kê") {
                    @Override
                    public boolean test(JavaMethodCall call) {
                        var name = call.getTarget().getName();
                        return call.getTarget().getOwner().getName().startsWith(inventoryCheckOwnerPrefix)
                                && (name.startsWith("create") || name.startsWith("insert"));
                    }
                })
                .because("Bộ lập lịch không lập phiếu kiểm kê, chỉ nhắc Hành chính (BR-M12-07, Q-306)")
                .allowEmptyShould(true);
    }

    /** Q-307: việc định kỳ không phụ thuộc lệnh sự cố của 007 ({@code SuCoLenhApi}). */
    static ArchRule jobsDoNotUseIncidentCommands(String jobPackages) {
        return noClasses()
                .that()
                .resideInAPackage(jobPackages)
                .should()
                .dependOnClassesThat()
                .areAssignableTo(IncidentCommandApi.class)
                .because("Sự cố đồ gửi chỉ ghi khi người ghi chọn trong lệnh (FR-021, Q-307)")
                .allowEmptyShould(true);
    }

    @Test
    void Q_306_schedulersDoNotCreateInventoryChecks() {
        jobsDoNotCreateInventoryChecks(JOBS, INVENTORY_CHECK).check(MAIN);
    }

    @Test
    void Q_307_schedulersDoNotCallIncidentCommands() {
        jobsDoNotUseIncidentCommands(JOBS).check(MAIN);
    }

    @Test
    void rulesCatchAViolatingJob() {
        var fixtures = new ClassFileImporter().importClasses(ViolatingJob.class, InventoryCheckFixture.class);
        var fixturePackage = ViolatingJob.class.getPackageName();

        assertThatThrownBy(() -> jobsDoNotCreateInventoryChecks(fixturePackage, InventoryCheckFixture.class.getName())
                        .check(fixtures))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("tạo phiếu kiểm kê");
        assertThatThrownBy(() -> jobsDoNotUseIncidentCommands(fixturePackage).check(fixtures))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining(IncidentCommandApi.class.getSimpleName());
    }

    static class InventoryCheckFixture {
        long create() {
            return 1L;
        }
    }

    static class ViolatingJob {
        void run(InventoryCheckFixture checks, IncidentCommandApi incidents) {
            checks.create();
            incidents.addProgression(1L, "FOUND", "", null);
        }
    }
}
