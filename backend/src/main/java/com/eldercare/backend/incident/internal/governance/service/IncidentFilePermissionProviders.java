package com.eldercare.backend.incident.internal.governance.service;

import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Quyền tải tệp đính kèm của M05 (NFR-08).
 */
@Configuration
public class IncidentFilePermissionProviders {

    public static final String TEP_SU_CO = "M05.TEP_SU_CO";

    @Bean
    public FilePermissionProvider incidentFilePermission() {
        return incidentFile();
    }

    public static FilePermissionProvider incidentFile() {
        return new BaseFilePermission(TEP_SU_CO);
    }

    record BaseFilePermission(String ownerType) implements FilePermissionProvider {
        @Override
        public boolean canDownload(long ownerId) {
            return true;
        }
    }
}
