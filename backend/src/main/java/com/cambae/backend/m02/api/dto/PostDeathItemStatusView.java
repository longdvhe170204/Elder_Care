package com.cambae.backend.m02.api.dto;

/** Trạng thái một mục việc sau qua đời kèm căn cứ (004 FR-071). Provisional: chữ ký view chưa được chốt. */
public record PostDeathItemStatusView(PostDeathItemStatus status, String basis) {}
