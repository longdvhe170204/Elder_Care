package com.eldercare.backend.care.internal.catalog.domain;
/** ID công khai trước R-15; id vật lý khác khi trùng giữa các bảng cũ. */
public record LegacyCatalogId(String table, long id) {}
