package com.eldercare.backend.belongings.internal.consent.service;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.common.file.FilePermissionProvider;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/** Bản ký đồng ý cho tự giữ ({@code QuyenTepProvider} {@code M12.BAN_KY}; contracts mục 1.1): Hành chính, Quản lý viện. */
@Component
class SignedFormPermission implements FilePermissionProvider {

    private static final Set<Role> READERS = Set.of(Role.OFFICE_STAFF, Role.NURSING_HOME_MANAGER);

    private final ObjectProvider<CurrentUser> users;

    SignedFormPermission(ObjectProvider<CurrentUser> users) {
        this.users = users;
    }

    @Override
    public String ownerType() {
        return SelfCustodyConsentService.SIGNED_FORM_OWNER;
    }

    @Override
    public boolean canDownload(long ownerId) {
        return users.getObject().roles().stream().anyMatch(READERS::contains);
    }
}
