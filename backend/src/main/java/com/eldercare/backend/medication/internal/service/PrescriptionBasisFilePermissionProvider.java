package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.stereotype.Component;

/**
 * Permission provider for downloading prescription basis document files (BR-M07-16, NFR-08).
 */
@Component
public class PrescriptionBasisFilePermissionProvider implements FilePermissionProvider {

    public static final String OWNER_TYPE = "M07.ANH_CAN_CU";

    @Override
    public String ownerType() {
        return OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return true;
    }
}
