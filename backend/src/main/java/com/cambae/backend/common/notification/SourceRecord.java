package com.cambae.backend.common.notification;

/** Bản ghi nguồn của thông báo, ví dụ ("M05.INCIDENT", "123"). */
public record SourceRecord(String type, String id) {}
