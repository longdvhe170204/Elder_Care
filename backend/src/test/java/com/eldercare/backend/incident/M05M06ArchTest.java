package com.eldercare.backend.incident;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

class M05M06ArchTest {

    @Test
    void T001_healthDoesNotDependOnIncident() {
        // Đọc các lớp mã nguồn chính trong module health.
        var healthClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.eldercare.backend.health");

        // Lấy tên package của mọi lớp mà health phụ thuộc.
        var dependencyPackages = healthClasses.stream()
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .map(dependency -> dependency.getTargetClass().getPackageName())
                .toList();

        // Không phụ thuộc package incident hoặc bất kỳ package con nào.
        assertThat(dependencyPackages)
                .noneMatch(packageName -> packageName.equals("com.eldercare.backend.incident")
                        || packageName.startsWith("com.eldercare.backend.incident."));
    }

    @Test
    void T001_incidentUsesOnlyPublicHealthApi() {
        // Đọc mã nguồn chính của incident, không lấy các lớp kiểm thử.
        var incidentClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.eldercare.backend.incident");

        // Chỉ xét những phụ thuộc từ incident sang module health.
        var healthDependencyPackages = incidentClasses.stream()
                .flatMap(javaClass -> javaClass.getDirectDependenciesFromSelf().stream())
                .map(dependency -> dependency.getTargetClass().getPackageName())
                .filter(packageName -> packageName.equals("com.eldercare.backend.health")
                        || packageName.startsWith("com.eldercare.backend.health."))
                .toList();

        // Chỉ cho phép api và các gói con như api.dto, api.events.
        assertThat(healthDependencyPackages)
                .allMatch(packageName -> packageName.equals("com.eldercare.backend.health.api")
                        || packageName.startsWith("com.eldercare.backend.health.api."));
    }
}
