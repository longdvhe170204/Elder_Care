package com.eldercare.backend.family.internal.relationship.config;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.family.internal.relationship.domain.FamilyRegistrationForm;
import com.eldercare.backend.family.internal.relationship.service.FamilyMemberService;
import com.eldercare.backend.family.internal.relationship.service.StaffOnlyFilePermission;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Quyền tải tệp {@code M10.BANG_CHUNG}, {@code M10.PHIEU_DANG_KY} (T010, T011); {@code M10.PHAN_HOI} cài cùng US6. */
@Configuration
class FamilyFileConfig {

    @Bean
    FilePermissionProvider representativeEvidenceFiles(ObjectProvider<CurrentUser> currentUser) {
        return new StaffOnlyFilePermission(FamilyMemberService.EVIDENCE_OWNER_TYPE, currentUser);
    }

    @Bean
    FilePermissionProvider registrationFormFiles(ObjectProvider<CurrentUser> currentUser) {
        return new StaffOnlyFilePermission(FamilyRegistrationForm.KIND.code(), currentUser);
    }
}
