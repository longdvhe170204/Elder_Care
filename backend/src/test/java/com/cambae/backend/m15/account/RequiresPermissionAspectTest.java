package com.cambae.backend.m15.account;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cambae.backend.common.Role;
import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.dto.FailedLayer;
import com.cambae.backend.m15.account.api.dto.Function;
import com.cambae.backend.m15.account.api.dto.LicenseView;
import com.cambae.backend.m15.account.api.dto.Operation;
import com.cambae.backend.m15.account.api.dto.PermissionBasis;
import com.cambae.backend.m15.account.api.dto.PermissionKey;
import com.cambae.backend.m15.account.api.dto.PermissionResult;
import com.cambae.backend.m15.account.api.dto.Target;
import com.cambae.backend.m15.account.internal.permission.service.RequiresPermissionAspect;
import com.cambae.backend.support.FakePermissions;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * 002 T014 (research A-10; FR-041, FR-043): trước mọi phương thức {@code @RequiresPermission} gọi
 * {@code PermissionApi.hasPermission} với đối tượng là tham số kiểu {@code Target} (không có thì
 * {@code Target.UNSPECIFIED}). Không xem được đối tượng (ngoài phạm vi xem) thì 404 chung; xem được mà thiếu quyền thao
 * tác thì 403 {@code PERMISSION_DENIED} kèm {@code failedLayer}; thiếu pháp lý thì 403
 * {@code LEGAL_CONDITION_NOT_MET} kèm giấy phép. Bị từ chối thì lệnh không chạy.
 */
@SpringJUnitConfig(RequiresPermissionAspectTest.Config.class)
class RequiresPermissionAspectTest {

    private static final Target RESIDENT_7 = new Target("RESIDENT", "7", 7L, 3L);
    private static final PermissionKey APPROVE = new PermissionKey(Function.CARE_PLAN, Operation.APPROVE);
    private static final PermissionKey VIEW = new PermissionKey(Function.CARE_PLAN, Operation.VIEW);

    @Autowired
    Commands commands;

    @Autowired
    ScriptedPermissions permissions;

    @BeforeEach
    void reset() {
        permissions.results.clear();
        permissions.calls.clear();
        Commands.runs = 0;
    }

    @Test
    void FR_041_allowed_runsCommand_checkedWithTargetParameter() {
        assertThat(commands.approve(RESIDENT_7, "x")).isEqualTo("ok");

        assertThat(permissions.calls).containsExactly(APPROVE + "@" + RESIDENT_7);
        assertThat(Commands.runs).isEqualTo(1);
    }

    @Test
    void FR_041_noTargetParameter_checksUnspecifiedTarget() {
        commands.facilityWide();

        assertThat(permissions.calls).containsExactly(APPROVE + "@" + Target.UNSPECIFIED);
    }

    @Test
    void FR_043_viewableButMissingOperation_403WithFailedLayer() {
        permissions.results.put(APPROVE, PermissionResult.denied(FailedLayer.ROLE));

        assertThatThrownBy(() -> commands.approve(RESIDENT_7, "x"))
                .isInstanceOfSatisfying(PermissionDeniedException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("PERMISSION_DENIED");
                    assertThat(e.properties()).containsEntry("failedLayer", "ROLE");
                });
        assertThat(Commands.runs).isZero();
    }

    @Test
    void FR_043_targetOutsideViewScope_404WithoutLayer() {
        permissions.results.put(APPROVE, PermissionResult.denied(FailedLayer.SCOPE));
        permissions.results.put(VIEW, PermissionResult.denied(FailedLayer.SCOPE));

        assertThatThrownBy(() -> commands.approve(RESIDENT_7, "x")).isInstanceOf(NotFoundException.class);
        assertThat(Commands.runs).isZero();
    }

    @Test
    void FR_043_outOfScopeWithoutSpecificTarget_403NotHidden() {
        permissions.results.put(APPROVE, PermissionResult.denied(FailedLayer.SCOPE));
        permissions.results.put(VIEW, PermissionResult.denied(FailedLayer.SCOPE));

        assertThatThrownBy(() -> commands.facilityWide())
                .isInstanceOfSatisfying(
                        PermissionDeniedException.class,
                        e -> assertThat(e.properties()).containsEntry("failedLayer", "SCOPE"));
    }

    @Test
    void FR_043_missingLicense_403LegalConditionWithLicense() {
        permissions.results.put(
                APPROVE,
                PermissionResult.deniedByLicense(new LicenseView("Chứng chỉ hành nghề", LocalDate.of(2026, 9, 30))));

        assertThatThrownBy(() -> commands.approve(RESIDENT_7, "x"))
                .isInstanceOfSatisfying(PermissionDeniedException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("LEGAL_CONDITION_NOT_MET");
                    assertThat(e.properties())
                            .containsEntry("failedLayer", "LEGAL")
                            .containsEntry(
                                    "license",
                                    Map.of("name", "Chứng chỉ hành nghề", "expiryDate", LocalDate.of(2026, 9, 30)));
                });
    }

    /** 002 T064: giấy phép cơ sở không bao phạm vi, hoặc chưa cấu hình loại giấy phép: vẫn là lỗi pháp lý, không có giấy phép. */
    @Test
    void FR_043_legalWithoutLicense_403LegalCondition() {
        permissions.results.put(APPROVE, PermissionResult.denied(FailedLayer.LEGAL));

        assertThatThrownBy(() -> commands.approve(RESIDENT_7, "x"))
                .isInstanceOfSatisfying(PermissionDeniedException.class, e -> {
                    assertThat(e.errorCode()).isEqualTo("LEGAL_CONDITION_NOT_MET");
                    assertThat(e.properties())
                            .containsEntry("failedLayer", "LEGAL")
                            .doesNotContainKey("license");
                });
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import(RequiresPermissionAspect.class)
    static class Config {

        @Bean
        ScriptedPermissions permissions() {
            return new ScriptedPermissions();
        }

        @Bean
        CurrentUser currentUser() {
            return FakePermissions.user(1, Role.NURSING_STAFF);
        }

        @Bean
        Commands commands() {
            return new Commands();
        }
    }

    /** Lệnh thử. */
    static class Commands {

        /** Tĩnh: bean là proxy CGLIB, trường của proxy không phải trường của đối tượng thật. */
        static int runs;

        @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.APPROVE)
        public String approve(Target target, String note) {
            runs++;
            return "ok";
        }

        @RequiresPermission(function = Function.CARE_PLAN, operation = Operation.APPROVE)
        public void facilityWide() {
            runs++;
        }
    }

    /** {@code PermissionApi} trả kết quả đặt sẵn theo khóa; mặc định được phép. */
    static class ScriptedPermissions implements PermissionApi {

        final Map<PermissionKey, PermissionResult> results = new HashMap<>();
        final List<String> calls = new ArrayList<>();

        @Override
        public PermissionResult hasPermission(CurrentUser user, PermissionKey key, Target target) {
            calls.add(key + "@" + target);
            return results.getOrDefault(key, PermissionResult.allowed(PermissionBasis.ROLE));
        }

        @Override
        public PermissionResult hasPermissionAt(CurrentUser user, PermissionKey key, Target target, Instant at) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<PermissionKey> permissions(CurrentUser user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<String> allowedCommands(CurrentUser user, Target target, Map<String, PermissionKey> commands) {
            throw new UnsupportedOperationException();
        }
    }
}
