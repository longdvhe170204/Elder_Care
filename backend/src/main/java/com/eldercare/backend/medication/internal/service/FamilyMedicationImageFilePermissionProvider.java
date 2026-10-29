package com.eldercare.backend.medication.internal.service;

import com.eldercare.backend.common.file.FilePermissionProvider;
import org.springframework.stereotype.Component;

/**
 * Permission provider for downloading family-sent medication images (NFR-08).
 */
@Component
public class FamilyMedicationImageFilePermissionProvider implements FilePermissionProvider {

    public static final String OWNER_TYPE = "M07.ANH_THUOC_GUI";

    @Override
    public String ownerType() {
        return OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return true;
    }
}
