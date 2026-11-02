package com.eldercare.backend.resident.web.response;

import io.swagger.v3.oas.annotations.media.Schema;

/** Kết quả tải tệp lên (contracts/api-001.yaml, {@code POST /files}): mã tệp để gắn vào bản ghi bằng lệnh nghiệp vụ. */
@Schema(name = "UploadedFile")
public record UploadedFileBody(long fileId) {}
