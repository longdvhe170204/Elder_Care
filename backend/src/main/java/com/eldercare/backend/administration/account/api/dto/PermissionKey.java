package com.eldercare.backend.administration.account.api.dto;

/**
 * Khóa quyền: chức năng (một dòng Phụ lục 27) và loại thao tác (002 research A-04; glossary mục 7). 000 khai báo trước
 * (000 T025); 002 đổi hai thành phần sang enum {@link Function}, {@link Operation} (002 T008).
 */
public record PermissionKey(Function function, Operation operation) {}
