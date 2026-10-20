package com.cambae.backend.m15.account.api;

import com.cambae.backend.m15.account.api.dto.InfoGroup;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nhóm thông tin của một trường DTO; 002 bỏ trường người xem không được xem khi tuần tự hóa (FR-022, FR-034; 002
 * research A-08; tên logic {@code @NhomTruong}).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.RECORD_COMPONENT})
public @interface FieldGroup {

    InfoGroup value();
}
