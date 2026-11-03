package com.eldercare.backend.care.internal.catalog.domain;
/** Nội dung riêng của loại lý do; fixed chỉ do migration nạp. */
public record ReasonAttributes(boolean fixed, LegacyCatalogId legacy) {}
