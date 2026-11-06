package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/**
 * FR-047h, SC-020 (research S-24; T094): vị trí GPS của cảnh báo chỉ trả qua API nhân viên có quyền xem cảnh báo. Gói
 * {@code incident.api} (DTO cho 012, sự kiện mang yêu cầu thông báo cho 009) không có trường tọa độ và không dùng kiểu vị
 * trí nội bộ.
 */
class LocationNotExposedToFamilyArchTest {
    private static final JavaClasses API = new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("com.eldercare.backend.incident.api");

    @Test
    void FR_047h_publicApiHasNoCoordinateFields() {
        var fields = API.stream()
                .flatMap(type -> type.getFields().stream())
                .filter(field -> {
                    String name = field.getName().toLowerCase(Locale.ROOT);
                    return name.contains("latitude") || name.contains("longitude") || name.contains("gps");
                })
                .map(field -> field.getOwner().getSimpleName() + "." + field.getName())
                .toList();
        assertThat(fields).isEmpty();
    }

    @Test
    void FR_047h_publicApiDoesNotUseAlertLocationTypes() {
        var dependencies = API.stream()
                .flatMap(type -> type.getDirectDependenciesFromSelf().stream())
                .map(dependency -> dependency.getTargetClass().getName())
                .filter(name -> name.contains(".internal.") && name.contains("AlertLocation"))
                .toList();
        assertThat(dependencies).isEmpty();
    }
}
