package com.cambae.backend.m09.duty.internal.domain;
/** Danh mục bộ phận cùng phụ trách hiện hành. */
public record Department(long id, String name, Long leadStaffId, boolean active) {}
