package com.eldercare.backend.notification;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.common.notification.HasNotificationRequests;
import com.eldercare.backend.notification.api.spi.SmsProvider;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * FR-001, research B-02 (009 T002): một cách gửi thông báo duy nhất. Chỉ M13 nghe {@link HasNotificationRequests}, và
 * chỉ một listener; module khác không gọi kênh gửi (nhà cung cấp tin nhắn, FCM) trực tiếp.
 */
class M13ArchTest {

    private static final JavaClasses MAIN = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.eldercare.backend");

    /**
     * Đúng một listener nhận chính kiểu {@link HasNotificationRequests} và nằm ở M13. Listener khác được nghe một sự kiện
     * nghiệp vụ có mang yêu cầu thông báo (017 nghe {@code CostStatementFinalized} của 010 để trừ số dư, FR-012) nhưng
     * không đọc yêu cầu thông báo của nó (sửa 2026-11-08, A).
     */
    @Test
    void FR_001_exactlyOneListenerForNotificationRequests_inM13() {
        List<JavaMethod> listeners = MAIN.stream()
                .flatMap(c -> c.getMethods().stream())
                .filter(m -> m.isAnnotatedWith(ApplicationModuleListener.class)
                        || m.isAnnotatedWith(EventListener.class)
                        || m.isAnnotatedWith(TransactionalEventListener.class))
                .filter(m -> m.getRawParameterTypes().stream()
                        .anyMatch(p -> p.isAssignableTo(HasNotificationRequests.class)))
                .toList();
        List<JavaMethod> notificationListeners = listeners.stream()
                .filter(m -> m.getRawParameterTypes().stream()
                        .anyMatch(p -> p.isEquivalentTo(HasNotificationRequests.class)))
                .toList();

        assertThat(notificationListeners).hasSize(1);
        assertThat(notificationListeners.getFirst().getOwner().getPackageName())
                .startsWith("com.eldercare.backend.notification.");
        assertThat(listeners)
                .filteredOn(m -> !m.getOwner().getPackageName().startsWith("com.eldercare.backend.notification."))
                .allSatisfy(m -> assertThat(m.getMethodCallsFromSelf())
                        .noneMatch(call -> call.getName().equals("notificationRequests")));
    }

    @Test
    void FR_001_otherModulesDoNotCallDeliveryChannels() {
        noClasses()
                .that()
                .resideOutsideOfPackage("com.eldercare.backend.notification..")
                .should()
                .dependOnClassesThat(JavaClass.Predicates.resideInAPackage("com.google.firebase..")
                        .or(JavaClass.Predicates.belongToAnyOf(SmsProvider.class)))
                .check(MAIN);
    }
}
