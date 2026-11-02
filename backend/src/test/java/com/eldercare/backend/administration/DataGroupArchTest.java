package com.eldercare.backend.administration;

import static org.assertj.core.api.Assertions.assertThat;

import com.eldercare.backend.archfixture.datagroup.MultipleGroupWithoutLockedAt;
import com.eldercare.backend.common.Reason;
import com.eldercare.backend.common.data.BusinessCommand;
import com.eldercare.backend.common.data.DataGroup;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import jakarta.persistence.Entity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * FR-001, FR-005, FR-007, research R-05 (000 T040): mọi entity có nhóm dữ liệu; entity nhóm 2, 3 không có setter công
 * khai; lệnh nghiệp vụ bắt buộc lý do có tham số {@link Reason}. Quét cả mã test để gồm dữ liệu mẫu.
 */
class DataGroupArchTest {

    private static final JavaClasses CLASSES = new ClassFileImporter().importPackages("com.eldercare.backend");

    @Test
    void FR_001_everyEntity_declaresDataGroup() {
        List<String> entities = CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(Entity.class))
                .map(JavaClass::getName)
                .toList();
        List<String> missing = CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(Entity.class) && !c.isAnnotatedWith(DataGroup.class))
                .map(JavaClass::getName)
                .toList();

        assertThat(entities).isNotEmpty();
        assertThat(missing).as("entity thiếu @DataGroup").isEmpty();
    }

    @Test
    void R_05_businessAndRecordEntities_haveNoPublicSetters() {
        List<String> setters = new ArrayList<>();
        CLASSES.stream()
                .filter(c -> c.isAnnotatedWith(DataGroup.class))
                .filter(c -> c.getAnnotationOfType(DataGroup.class).value() != DataGroup.Group.CATALOG)
                .forEach(c -> c.getAllMethods().stream()
                        .filter(m -> m.getModifiers().contains(JavaModifier.PUBLIC))
                        .filter(m -> m.getName().startsWith("set"))
                        .forEach(m -> setters.add(m.getFullName())));

        assertThat(setters).as("setter công khai trên entity nhóm 2, 3").isEmpty();
    }

    @Test
    void FR_007_businessCommandRequiringReason_takesReason() {
        List<JavaMethod> commands = CLASSES.stream()
                .flatMap(c -> c.getMethods().stream())
                .filter(m -> m.isAnnotatedWith(BusinessCommand.class))
                .toList();
        List<String> withoutReason = commands.stream()
                .filter(m -> m.getAnnotationOfType(BusinessCommand.class).requiresReason())
                .filter(m -> m.getRawParameterTypes().stream().noneMatch(t -> t.isEquivalentTo(Reason.class)))
                .map(JavaMethod::getFullName)
                .toList();

        assertThat(commands).isNotEmpty();
        assertThat(withoutReason)
                .as("lệnh bắt buộc lý do nhưng không nhận Reason")
                .isEmpty();
    }

    /** R-05 (constitution XII 3.2.0): entity của bảng chứa nhiều nhóm có trường {@code lockedAt} (cột {@code locked_at}). */
    @Test
    void R_05_multipleGroupEntities_haveLockedAt() {
        JavaClasses fixtures = new ClassFileImporter().importPackages("com.eldercare.backend.archfixture.datagroup");
        JavaClasses main = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.eldercare.backend");

        assertThat(withoutLockedAt(fixtures)).containsExactly(MultipleGroupWithoutLockedAt.class.getName());
        assertThat(withoutLockedAt(main)).as("entity nhiều nhóm thiếu lockedAt").isEmpty();
    }

    private static List<String> withoutLockedAt(JavaClasses classes) {
        return classes.stream()
                .filter(c -> c.isAnnotatedWith(DataGroup.class))
                .filter(c -> c.getAnnotationOfType(DataGroup.class).value() == DataGroup.Group.MULTIPLE)
                .filter(c ->
                        c.getAllFields().stream().noneMatch(f -> f.getName().equals("lockedAt")))
                .map(JavaClass::getName)
                .toList();
    }
}
