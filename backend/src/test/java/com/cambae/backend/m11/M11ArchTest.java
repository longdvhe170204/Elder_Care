package com.cambae.backend.m11;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** T001, P-01/P-12: ranh giới port nội bộ và hàm thuần, phạm vi job theo Q-306. */
class M11ArchTest {
    private static final JavaClasses MAIN = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.cambae.backend");

    private static DescribedPredicate<JavaMethodCall> port(String name) {
        return new DescribedPredicate<>("gọi port nội bộ " + name) {
            @Override
            public boolean test(JavaMethodCall call) {
                var owner = call.getTarget().getOwner();
                return owner.getPackageName().startsWith("com.cambae.backend.m11.")
                        && owner.getSimpleName().equals(name);
            }
        };
    }

    @Test
    void FR_042_onlyBalanceAndProcurementCallTheCostForBalancePort() {
        noClasses()
                .that()
                .resideOutsideOfPackages("com.cambae.backend.m11.balance..", "com.cambae.backend.m11.procurement..")
                .should()
                .callMethodWhere(port("CostForBalancePort"))
                .allowEmptyShould(true)
                .check(MAIN);
    }

    @Test
    void FR_024_onlyPurchaseCallsThePurchaseBalancePort() {
        noClasses()
                .that()
                .resideOutsideOfPackage("com.cambae.backend.m11.purchase..")
                .should()
                .callMethodWhere(port("PurchaseBalancePort"))
                .allowEmptyShould(true)
                .check(MAIN);
    }

    @Test
    void Q_306_schedulersDoNotCreateCostBusinessRecords() {
        var forbidden = Set.of("createItem", "createStatement", "createAdjustment", "createPurchaseAssistanceRequest");
        noClasses()
                .that()
                .resideInAPackage("com.cambae.backend.m11..scheduling..")
                .should()
                .callMethodWhere(new DescribedPredicate<>("lập khoản, bảng, điều chỉnh hoặc đề nghị mua hộ") {
                    @Override
                    public boolean test(JavaMethodCall call) {
                        return call.getTarget().getOwner().getPackageName().startsWith("com.cambae.backend.m11.")
                                && forbidden.contains(call.getTarget().getName());
                    }
                })
                .allowEmptyShould(true)
                .check(MAIN);
    }

    @Test
    void FR_009_pricingDomainExistsAndDependsOnlyOnPureJavaValues() {
        classes()
                .that()
                .resideInAPackage("com.cambae.backend.m11.pricing.internal.domain..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "com.cambae.backend.m11.pricing.internal.domain..")
                .check(MAIN);
    }
}
