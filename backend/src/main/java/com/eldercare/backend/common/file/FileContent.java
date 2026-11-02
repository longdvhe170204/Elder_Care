package com.eldercare.backend.common.file;

/** Nội dung tệp trả cho người tải xuống (001 research H-13). */
public record FileContent(String originalName, String mimeType, byte[] content) {}
