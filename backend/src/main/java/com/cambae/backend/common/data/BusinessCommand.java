package com.cambae.backend.common.data;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Đánh dấu phương thức là lệnh nghiệp vụ (mục 2.4). Lệnh nhóm 2 bắt buộc lý do (FR-005, FR-007): phương thức có
 * {@code requiresReason = true} phải nhận tham số {@link com.cambae.backend.common.Reason} (ArchUnit ở 000 T040).
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface BusinessCommand {

    boolean requiresReason() default true;
}
