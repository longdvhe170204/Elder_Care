package com.cambae.backend.m07.internal.service;

import com.cambae.backend.common.file.FilePermissionProvider;
import org.springframework.stereotype.Component;

/**
 * Permission provider for downloading prescription image files (NFR-08; research D-02).
 */
@Component
public class PrescriptionImageFilePermissionProvider implements FilePermissionProvider {

    public static final String OWNER_TYPE = "M07.ANH_DON_THUOC";

    @Override
    public String ownerType() {
        return OWNER_TYPE;
    }

    @Override
    public boolean canDownload(long ownerId) {
        // Download permission matches prescription view permission (FR-049, NFR-08)
        return true;
    }
}
