package com.eldercare.backend.family.internal.right.config;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Target;
import com.eldercare.backend.administration.account.api.spi.ScopeAssignmentProvider;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.administration.shared.api.SimpleRecordApi;
import com.eldercare.backend.administration.shared.api.dto.SharedKind;
import com.eldercare.backend.common.file.FilePermissionProvider;
import com.eldercare.backend.family.internal.relationship.service.StaffOnlyFilePermission;
import com.eldercare.backend.family.internal.right.domain.RightChangeRequest;
import com.eldercare.backend.family.internal.right.domain.VisitPickupWish;
import java.util.Map;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provider của yêu cầu thay đổi quyền, mong muốn ({@code GanPhamViProvider} {@code M10.YEU_CAU_QUYEN},
 * {@code M10.MONG_MUON}; tệp bằng chứng của yêu cầu; 012 T008, T015, T016a): phạm vi theo người cao tuổi của dòng, số
 * giấy tờ của người ngoài trong mong muốn là dữ liệu cá nhân. Dòng ở {@code simple_record} gắn hồ sơ người cao tuổi nên
 * loại bỏ cùng hồ sơ, không cần {@code LuuTruProvider} riêng.
 */
@Configuration
class RightRecordConfig {

    @Bean
    ScopeAssignmentProvider rightChangeRequestScope(SimpleRecordApi records) {
        return scope(records, RightChangeRequest.KIND);
    }

    @Bean
    ScopeAssignmentProvider visitPickupWishScope(SimpleRecordApi records) {
        return scope(records, VisitPickupWish.KIND);
    }

    @Bean
    FieldClassificationProvider visitPickupWishFields() {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return VisitPickupWish.KIND.code();
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return Map.of("idDocumentNumber", InfoGroup.IDENTITY);
            }
        };
    }

    @Bean
    FilePermissionProvider rightChangeRequestFiles(ObjectProvider<CurrentUser> currentUser) {
        return new StaffOnlyFilePermission(RightChangeRequest.KIND.code(), currentUser);
    }

    private static ScopeAssignmentProvider scope(SimpleRecordApi records, SharedKind<?> kind) {
        return new ScopeAssignmentProvider() {
            @Override
            public String entityType() {
                return kind.code();
            }

            @Override
            public Target assignScope(long id) {
                return new Target(
                        kind.code(),
                        String.valueOf(id),
                        records.require(kind, id).residentId(),
                        null);
            }
        };
    }
}
