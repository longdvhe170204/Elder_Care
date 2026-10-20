package com.cambae.backend.m15.account.permission.internal;

import com.cambae.backend.common.error.NotFoundException;
import com.cambae.backend.common.error.PermissionDeniedException;
import com.cambae.backend.m15.account.api.CurrentUser;
import com.cambae.backend.m15.account.api.FailedLayer;
import com.cambae.backend.m15.account.api.Operation;
import com.cambae.backend.m15.account.api.PermissionApi;
import com.cambae.backend.m15.account.api.PermissionKey;
import com.cambae.backend.m15.account.api.PermissionResult;
import com.cambae.backend.m15.account.api.RequiresPermission;
import com.cambae.backend.m15.account.api.Target;
import java.util.Arrays;
import java.util.Map;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Kiểm quyền trước mọi phương thức {@code @RequiresPermission} (002 T014; research A-10; FR-041, FR-043): gọi
 * {@link PermissionApi#hasPermission} với đối tượng là tham số kiểu {@link Target} đầu tiên (không có thì
 * {@link Target#UNSPECIFIED}). Ánh xạ lỗi:
 *
 * <ul>
 *   <li>không xem được chính đối tượng (khóa Xem của chức năng trượt lớp phạm vi): 404 chung, không nêu lớp;
 *   <li>thiếu điều kiện pháp lý: 403 {@code LEGAL_CONDITION_NOT_MET} kèm giấy phép;
 *   <li>còn lại: 403 {@code PERMISSION_DENIED} kèm {@code failedLayer}.
 * </ul>
 */
@Aspect
@Component
// Trước BusinessCommandAspect (Order 0) và @Transactional: người không có quyền không nhận lỗi thiếu lý do
@Order(-1)
public class RequiresPermissionAspect {

    // ObjectProvider: bản thật cài ở T022; trước đó chỉ test khai bản giả
    private final ObjectProvider<PermissionApi> permissions;
    private final ObjectProvider<CurrentUser> currentUser;

    RequiresPermissionAspect(ObjectProvider<PermissionApi> permissions, ObjectProvider<CurrentUser> currentUser) {
        this.permissions = permissions;
        this.currentUser = currentUser;
    }

    @Around("@annotation(required)")
    public Object check(ProceedingJoinPoint command, RequiresPermission required) throws Throwable {
        Target target = Arrays.stream(command.getArgs())
                .filter(Target.class::isInstance)
                .map(Target.class::cast)
                .findFirst()
                .orElse(Target.UNSPECIFIED);
        CurrentUser user = currentUser.getObject();
        PermissionKey key = new PermissionKey(required.function(), required.operation());
        PermissionResult result = permissions.getObject().hasPermission(user, key, target);
        if (result.allowed()) {
            return command.proceed();
        }
        if (!target.equals(Target.UNSPECIFIED) && !canView(user, key, target, result)) {
            throw new NotFoundException();
        }
        FailedLayer layer = result.failedLayer().orElseThrow();
        if (layer == FailedLayer.LEGAL && result.license().isPresent()) {
            throw new PermissionDeniedException(
                    "LEGAL_CONDITION_NOT_MET",
                    "Thiếu điều kiện pháp lý",
                    Map.of(
                            "failedLayer",
                            layer.name(),
                            "license",
                            Map.of(
                                    "name",
                                    result.license().get().name(),
                                    "expiryDate",
                                    result.license().get().expiryDate())));
        }
        throw new PermissionDeniedException("PERMISSION_DENIED", "Không có quyền", Map.of("failedLayer", layer.name()));
    }

    private boolean canView(CurrentUser user, PermissionKey key, Target target, PermissionResult result) {
        PermissionResult view = key.operation() == Operation.VIEW
                ? result
                : permissions
                        .getObject()
                        .hasPermission(user, new PermissionKey(key.function(), Operation.VIEW), target);
        return view.allowed() || view.failedLayer().orElseThrow() != FailedLayer.SCOPE;
    }
}
