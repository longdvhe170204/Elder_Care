package com.cambae.backend.m09.internal.service;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.file.FilePermissionProvider;
import com.cambae.backend.m09.credential.internal.persistence.CredentialStore;
import com.cambae.backend.m15.account.api.CurrentUser;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** T009, FR-052: tải bản scan chỉ Quản lý viện hoặc chính nhân viên sở hữu chứng chỉ. */
@Component
class M09ScanAccess implements FilePermissionProvider {
    private final CredentialStore credentials;
    private final ObjectProvider<CurrentUser> users;

    M09ScanAccess(CredentialStore credentials, ObjectProvider<CurrentUser> users) {
        this.credentials = credentials;
        this.users = users;
    }

    public String ownerType() {
        return "M09.CREDENTIAL_SCAN";
    }

    public boolean canDownload(long ownerId) {
        return credentials
                .find(ownerId)
                .map(c -> {
                    var user = users.getObject();
                    return user.roles().contains(Role.NURSING_HOME_MANAGER)
                            || user.staffId().filter(id -> id == c.staffId()).isPresent();
                })
                .orElse(false);
    }
}
