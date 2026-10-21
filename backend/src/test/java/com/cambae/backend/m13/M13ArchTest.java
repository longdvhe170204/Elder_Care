package com.cambae.backend.m13;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.cambae.backend.common.notification.HasNotificationRequests;
import com.cambae.backend.m13.api.spi.SmsProvider;
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
            .importPackages("com.cambae.backend");

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

        assertThat(listeners).hasSize(1);
        assertThat(listeners.getFirst().getOwner().getPackageName()).startsWith("com.cambae.backend.m13.");
    }

    @Test
    void FR_001_otherModulesDoNotCallDeliveryChannels() {
        noClasses()
                .that()
                .resideOutsideOfPackage("com.cambae.backend.m13..")
                .should()
                .dependOnClassesThat(JavaClass.Predicates.resideInAPackage("com.google.firebase..")
                        .or(JavaClass.Predicates.belongToAnyOf(SmsProvider.class)))
                .check(MAIN);
    }
}
