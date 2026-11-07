package com.eldercare.backend.nutrition.web.request;

/** Thân lệnh trên phần bổ sung (api-011 {@code POST /supplementary-portions/{id}/{command}}): {@code content} nội dung sai lệch (FR-048b). */
public record SupplementCommandBody(String content) {}
