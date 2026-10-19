package com.cambae.backend.m15.approval;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.InvalidValueException;
import com.cambae.backend.common.error.UnmetCondition;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.Target;
import com.cambae.backend.m15.approval.api.ApprovalRequestView;
import com.cambae.backend.m15.approval.api.RequestTypeHandler;
import com.cambae.backend.m15.approval.api.ResultRef;
import com.cambae.backend.m15.auditlog.api.AuditLogPermissions;
import com.cambae.backend.m15.parameter.api.ParameterPermissions;
import com.cambae.backend.support.FakePermissions;
import com.cambae.backend.support.SwitchableUser;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;

/**
 * Người dùng, quyền giả và handler mẫu {@code M15.SAMPLE} cho test yêu cầu phê duyệt (000 T082 → T088, T096, T105;
 * quickstart K6 → K13, K21). Quản lý viện và Hành chính có quyền tạo, duyệt loại mẫu; Điều dưỡng không có.
 */
@TestConfiguration
public class ApprovalFixtures {

    public static final String TYPE = "M15.SAMPLE";
    public static final PermissionKey CREATE = new PermissionKey(TYPE, "CREATE");
    public static final PermissionKey APPROVE = new PermissionKey(TYPE, "APPROVE");

    public static final CurrentUser MANAGER = FakePermissions.user(1, Role.NURSING_HOME_MANAGER);
    public static final CurrentUser MANAGER_2 = FakePermissions.user(2, Role.NURSING_HOME_MANAGER);
    public static final CurrentUser NURSE = FakePermissions.user(10, Role.NURSING_STAFF);
    public static final CurrentUser OFFICE_A = FakePermissions.user(20, Role.OFFICE_STAFF);
    public static final CurrentUser OFFICE_B = FakePermissions.user(21, Role.OFFICE_STAFF);
    public static final CurrentUser SCHEDULER = FakePermissions.user(0, Role.SCHEDULER);

    /** Hành vi của handler mẫu, do test bật, tắt. */
    public static class Control {
        volatile boolean conditionUnmet;
        volatile boolean failOnApply;
        volatile boolean allowSelfApproval = true;
        volatile Set<Role> roleRestriction;

        public void reset() {
            conditionUnmet = false;
            failOnApply = false;
            allowSelfApproval = true;
            roleRestriction = null;
        }
    }

    /**
     * Loại yêu cầu mẫu: nội dung {@code {"key": "...", "note": "..."}}; tác động là một mục danh mục mẫu tên
     * "Tác động &lt;key&gt;", để đếm số lần áp dụng (SC-003).
     */
    static class SampleRequestHandler implements RequestTypeHandler {

        private final Control control;
        private final JdbcTemplate jdbc;

        SampleRequestHandler(Control control, JdbcTemplate jdbc) {
            this.control = control;
            this.jdbc = jdbc;
        }

        @Override
        public String typeCode() {
            return TYPE;
        }

        @Override
        public void validateContent(JsonNode content) {
            if (content == null || !content.has("key")) {
                throw new InvalidValueException("Nội dung cần trường key");
            }
        }

        @Override
        public PermissionKey createPermission() {
            return CREATE;
        }

        @Override
        public PermissionKey approvePermission() {
            return APPROVE;
        }

        @Override
        public Target scope(JsonNode content) {
            return new Target(TYPE, content.get("key").asString(), null, null);
        }

        @Override
        public List<UnmetCondition> checkApplyConditions(ApprovalRequestView request) {
            return control.conditionUnmet
                    ? List.of(new UnmetCondition("BR-MAU-01", "Điều kiện mẫu không còn thỏa"))
                    : List.of();
        }

        @Override
        public ResultRef apply(ApprovalRequestView request, LocalDate actualEffectiveDate) {
            jdbc.update("INSERT INTO sample_catalog (name) VALUES (?)", effectName(request));
            if (control.failOnApply) {
                throw new IllegalStateException("Lỗi mẫu khi áp dụng, sau khi đã ghi một phần tác động");
            }
            Long id = jdbc.queryForObject(
                    "SELECT id FROM sample_catalog WHERE name = ?", Long.class, effectName(request));
            return new ResultRef("M15.SAMPLE_CATALOG", String.valueOf(id));
        }

        @Override
        public boolean allowSelfApproval() {
            return control.allowSelfApproval;
        }

        @Override
        public Optional<Set<Role>> approverRoleRestriction(ApprovalRequestView request) {
            return Optional.ofNullable(control.roleRestriction);
        }

        static String effectName(ApprovalRequestView request) {
            return "Tác động " + request.content().get("key").asString();
        }
    }

    @Bean
    SwitchableUser currentUser() {
        return new SwitchableUser();
    }

    @Bean
    Control approvalControl() {
        return new Control();
    }

    @Bean
    RequestTypeHandler sampleRequestHandler(Control control, JdbcTemplate jdbc) {
        return new SampleRequestHandler(control, jdbc);
    }

    @Bean
    FakePermissions permissions() {
        return new FakePermissions()
                .grant(Role.NURSING_HOME_MANAGER, CREATE)
                .grant(Role.NURSING_HOME_MANAGER, APPROVE)
                .grant(Role.NURSING_HOME_MANAGER, AuditLogPermissions.VIEW_FACILITY)
                // Đổi CFG-M15-05 giữa test (FR-017, ParameterChangeNoRecalculationIT)
                .grant(Role.NURSING_HOME_MANAGER, ParameterPermissions.VIEW)
                .grant(Role.NURSING_HOME_MANAGER, ParameterPermissions.CHANGE)
                .grant(Role.OFFICE_STAFF, CREATE)
                .grant(Role.OFFICE_STAFF, APPROVE)
                .account(MANAGER)
                .account(MANAGER_2)
                .account(NURSE)
                .account(OFFICE_A)
                .account(OFFICE_B);
    }
}
