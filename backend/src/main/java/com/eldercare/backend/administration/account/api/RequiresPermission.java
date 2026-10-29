package com.eldercare.backend.administration.account.api;

import com.eldercare.backend.administration.account.api.dto.Function;
import com.eldercare.backend.administration.account.api.dto.Operation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Lệnh cần quyền: interceptor của 002 gọi {@link PermissionApi#hasPermission} trước khi chạy, lỗi theo FR-043 (403
 * hoặc 404) (002 research A-10; tên logic {@code @CanQuyen}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequiresPermission {

    Function function();

    Operation operation();
}
