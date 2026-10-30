package com.eldercare.backend.roombedallocation;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.ApplicationModule;
import org.springframework.modulith.NamedInterface;

/** T001, R-02: khung M03 giữ hợp đồng công khai và các phần triển khai trong cùng một module. */
class FR_001_ModuleSetupTest {

    @Test
    void FR_001_moduleHasOneRootAndEnglishImplementationPackages() {
        assertThat(getClass().getPackage().getAnnotation(ApplicationModule.class))
                .isNotNull();
        for (String suffix : List.of(
                "internal.structure",
                "internal.bed",
                "internal.allocation",
                "internal.isolation",
                "internal.daycare",
                "internal.bed.domain",
                "scheduling")) {
            assertThat(Path.of(
                            "src/main/java/com/eldercare/backend/roombedallocation",
                            suffix.replace('.', '/'),
                            "package-info.java"))
                    .as("Khung gói %s phải có tài liệu", suffix)
                    .exists();
        }
    }

    @Test
    void FR_001_publicApiIsANamedInterface() throws Exception {
        assertThat(getClass()
                        .getClassLoader()
                        .getResource("com/eldercare/backend/roombedallocation/api/package-info.class"))
                .as("Hợp đồng M03 phải có gói api công khai")
                .isNotNull();
        Class<?> packageInfo = Class.forName("com.eldercare.backend.roombedallocation.api.package-info");
        assertThat(packageInfo.getPackage().getAnnotation(NamedInterface.class)).isNotNull();
    }
}
