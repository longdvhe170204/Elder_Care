package com.cambae.backend.m15.parameter.api;

import java.time.Instant;
import tools.jackson.databind.JsonNode;

/**
 * Giá trị tham số đã đổi, phát sau khi lệnh đổi commit (contracts/module-interfaces.md mục 1.4). Module chỉ dùng để cập
 * nhật bộ đệm của mình, không tính lại kết quả đã có (FR-017).
 */
public record ParameterChanged(ParameterCode code, JsonNode before, JsonNode after, Instant at) {}
