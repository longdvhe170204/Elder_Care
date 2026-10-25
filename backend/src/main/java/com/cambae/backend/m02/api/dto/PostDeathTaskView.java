package com.cambae.backend.m02.api.dto;

import java.time.Instant;
import java.util.List;

/** Danh sách việc sau qua đời (FR-071, FR-072); {@code closedAt} rỗng khi hồ sơ lưu trú chưa đóng. */
public record PostDeathTaskView(List<Item> items, Instant closedAt) {

    /** Một mục việc kèm căn cứ do module sở hữu cung cấp. */
    public record Item(PostDeathItemCode code, PostDeathItemStatus status, String basis) {}
}
