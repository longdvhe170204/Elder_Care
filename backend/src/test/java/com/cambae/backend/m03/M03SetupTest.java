package com.cambae.backend.m03;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.NamedInterface;

/** 003 T001: khung gói tiếng Anh, API công khai và port nội bộ với feature 019. */
class M03SetupTest {

    @Test
    void FR_001_moduleSkeleton_hasThePackagesRequiredByPhaseOne() throws IOException {
        for (String suffix : List.of(
                "api",
                "api.dto",
                "api.spi",
                "internal.structure",
                "internal.bed",
                "internal.bed.domain.port",
                "internal.allocation",
                "internal.isolation",
                "internal.daycare",
                "scheduling")) {
            Path source =
                    Path.of("src/main/java/com/cambae/backend/m03", suffix.replace('.', '/'), "package-info.java");
            assertThat(source).isRegularFile();
            assertThat(Files.readString(source)).contains("package com.cambae.backend.m03." + suffix + ";");
        }
    }

    @Test
    void FR_018_spiPackages_arePublicNamedInterfaces_butAssetPortStaysInternal() throws ClassNotFoundException {
        for (String suffix : List.of("api", "api.dto", "api.spi")) {
            Package apiPackage = Class.forName("com.cambae.backend.m03." + suffix + ".package-info")
                    .getPackage();
            assertThat(apiPackage.getAnnotation(NamedInterface.class)).isNotNull();
        }
        Package port = Class.forName("com.cambae.backend.m03.internal.bed.domain.port.BedAssetPort")
                .getPackage();
        assertThat(port.getAnnotation(NamedInterface.class)).isNull();
    }
}
