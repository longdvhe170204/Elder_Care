package com.eldercare.backend.administration.auditlog;

import com.eldercare.backend.administration.account.api.CurrentUser;
import com.eldercare.backend.administration.account.api.dto.InfoGroup;
import com.eldercare.backend.administration.account.api.dto.Visibility;
import com.eldercare.backend.administration.auditlog.api.dto.AuditLogPermissions;
import com.eldercare.backend.administration.auditlog.api.spi.FieldClassificationProvider;
import com.eldercare.backend.common.Role;
import com.eldercare.backend.support.FakePermissions;
import com.eldercare.backend.support.SwitchableUser;
import java.util.List;
import java.util.Map;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Người dùng, quyền giả và phân loại trường cho test tra cứu nhật ký (000 T062 → T065). Quyền xem nhật ký theo Phụ lục
 * 27 dòng "Nhật ký thay đổi": Quản lý viện toàn viện, nhân viên trong phạm vi, người thân và người cao tuổi không có.
 * Hồ sơ mẫu {@code M15.SAMPLE_PROFILE}: địa chỉ là thông tin liên hệ, dị ứng là thông tin sức khỏe (US3 #8).
 */
@TestConfiguration
public class AuditLogFixtures {

    public static final String PROFILE = "M15.SAMPLE_PROFILE";
    public static final String UNCLASSIFIED = "M15.SAMPLE_UNCLASSIFIED";

    public static final CurrentUser MANAGER = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);
    public static final CurrentUser NURSE = FakePermissions.user(10, Role.NURSING_STAFF);
    public static final CurrentUser OFFICE = FakePermissions.user(20, Role.OFFICE_STAFF);
    public static final CurrentUser FAMILY = FakePermissions.user(30, Role.FAMILY_MEMBER);
    public static final CurrentUser RESIDENT = FakePermissions.user(31, Role.RESIDENT);
    public static final CurrentUser SCHEDULER = FakePermissions.user(0, Role.SCHEDULER);

    private static final List<Role> STAFF = List.of(
            Role.FLOOR_MANAGER,
            Role.DOCTOR,
            Role.NURSING_STAFF,
            Role.CAREGIVER,
            Role.KITCHEN_STAFF,
            Role.OFFICE_STAFF,
            Role.ACCOUNTANT);

    @Bean
    SwitchableUser currentUser() {
        return new SwitchableUser();
    }

    @Bean
    FakePermissions permissions() {
        FakePermissions fake = new FakePermissions()
                .grant(Role.NURSING_HOME_MANAGER, AuditLogPermissions.VIEW)
                .visibility(Role.OFFICE_STAFF, InfoGroup.CONTACT, Visibility.FULL);
        for (Role role : STAFF) {
            fake.grantAssigned(role, AuditLogPermissions.VIEW)
                    .visibility(role, InfoGroup.INTERNAL_PUBLIC, Visibility.FULL);
        }
        return fake;
    }

    @Bean
    FieldClassificationProvider sampleProfileFields() {
        return new FieldClassificationProvider() {
            @Override
            public String entityType() {
                return PROFILE;
            }

            @Override
            public Map<String, InfoGroup> fieldGroups() {
                return Map.of("address", InfoGroup.CONTACT, "allergies", InfoGroup.HEALTH);
            }
        };
    }
}
