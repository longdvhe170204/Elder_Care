package com.eldercare.backend.health.internal.governance.service;

import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quyền tải tệp đính kèm của M06 (NFR-08).
 */
@Configuration
public class HealthFilePermissionProviders {

    public static final String SCAN_GIAY_PHEP_CO_SO = "M06.SCAN_GIAY_PHEP_CO_SO";
    public static final String TEP_KHAM = "M06.TEP_KHAM";

    @Bean
    public FilePermissionProvider facilityLicenseScanFilePermission() {
        return facilityLicenseScan();
    }

    @Bean
    public FilePermissionProvider examinationFilePermission() {
        return examinationFile();
    }

    public static FilePermissionProvider facilityLicenseScan() {
        return new BaseFilePermission(SCAN_GIAY_PHEP_CO_SO);
    }

    public static FilePermissionProvider examinationFile() {
        return new BaseFilePermission(TEP_KHAM);
    }

    record BaseFilePermission(String ownerType) implements FilePermissionProvider {
        @Override
        public boolean canDownload(long ownerId) {
            return true;
        }
    }
}
