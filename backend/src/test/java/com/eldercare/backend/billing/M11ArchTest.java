package com.eldercare.backend.billing;

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
            .importPackages("com.eldercare.backend");

    private static DescribedPredicate<JavaMethodCall> port(String name) {
        return new DescribedPredicate<>("gọi port nội bộ " + name) {
            @Override
            public boolean test(JavaMethodCall call) {
                var owner = call.getTarget().getOwner();
                return owner.getPackageName().startsWith("com.eldercare.backend.billing.")
                        && owner.getSimpleName().equals(name);
            }
        };
    }

    @Test
    void FR_042_onlyBalanceAndProcurementCallTheCostForBalancePort() {
        noClasses()
                .that()
                .resideOutsideOfPackages(
                        "com.eldercare.backend.billing.balance..",
                        "com.eldercare.backend.billing.internal.procurement..")
                .should()
                .callMethodWhere(port("CostForBalancePort"))
                .allowEmptyShould(true)
                .check(MAIN);
    }

    @Test
    void FR_024_onlyPurchaseCallsThePurchaseBalancePort() {
        noClasses()
                .that()
                .resideOutsideOfPackage("com.eldercare.backend.billing.internal.purchase..")
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
                .resideInAPackage("com.eldercare.backend.billing..scheduling..")
                .should()
                .callMethodWhere(new DescribedPredicate<>("lập khoản, bảng, điều chỉnh hoặc đề nghị mua hộ") {
                    @Override
                    public boolean test(JavaMethodCall call) {
                        return call.getTarget().getOwner().getPackageName().startsWith("com.eldercare.backend.billing.")
                                && forbidden.contains(call.getTarget().getName());
                    }
                })
                .allowEmptyShould(true)
                .check(MAIN);
    }

    /**
     * 017 T001 (research E-01, Q-306): job của phần số dư chỉ tính tình trạng, nhắc, đối chiếu; không ghi giao dịch.
     * Giao dịch chỉ ghi qua lệnh của Kế toán, lệnh duyệt hoặc listener chốt bảng ({@code internal.transaction},
     * {@code internal.ledger} hàm {@code post…}).
     */
    @Test
    void Q_306_balanceSchedulersDoNotPostTransactions() {
        noClasses()
                .that()
                .resideInAPackage("com.eldercare.backend.billing.balance.scheduling..")
                .should()
                .dependOnClassesThat()
                .resideInAPackage("com.eldercare.backend.billing.balance.internal.transaction..")
                .orShould()
                .callMethodWhere(new DescribedPredicate<>("ghi giao dịch số dư") {
                    @Override
                    public boolean test(JavaMethodCall call) {
                        return call.getTarget()
                                        .getOwner()
                                        .getPackageName()
                                        .startsWith("com.eldercare.backend.billing.balance.")
                                && call.getTarget().getName().startsWith("post");
                    }
                })
                .allowEmptyShould(true)
                .check(MAIN);
    }

    @Test
    void FR_009_pricingDomainExistsAndDependsOnlyOnPureJavaValues() {
        classes()
                .that()
                .resideInAPackage("com.eldercare.backend.billing.internal.pricing.domain..")
                .should()
                .onlyDependOnClassesThat()
                .resideInAnyPackage("java..", "com.eldercare.backend.billing.internal.pricing.domain..")
                .check(MAIN);
    }
}
