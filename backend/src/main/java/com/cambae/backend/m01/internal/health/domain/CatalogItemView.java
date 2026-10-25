package com.cambae.backend.m01.internal.health.domain;

import com.cambae.backend.common.data.CatalogStatus;

/**
 * Một mục danh mục để trả ra ngoài: {@code group} chỉ có ở dị nguyên, {@code order} chỉ có ở mức độ dị ứng và mức chăm
 * sóc.
 */
public record CatalogItemView(long id, String name, String group, Integer order, CatalogStatus status, int version) {}
