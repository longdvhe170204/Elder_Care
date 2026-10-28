package com.cambae.backend.common.file;

/** Siêu dữ liệu một tệp đã gắn vào bản ghi (không kèm nội dung; tải xuống vẫn qua kiểm quyền của chủ sở hữu). */
public record FileView(long id, String originalName, String mimeType, long sizeBytes) {}
