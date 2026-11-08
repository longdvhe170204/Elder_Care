package com.eldercare.backend.nutrition.internal.catalog.domain;

/** Một phiên bản nội dung của món. */
public record DishVersion(long id, long dishId, DishVersionStatus status, DishVersionContent content, int version) {}
