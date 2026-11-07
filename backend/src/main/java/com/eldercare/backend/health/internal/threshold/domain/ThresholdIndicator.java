package com.eldercare.backend.health.internal.threshold.domain;

import java.util.Set;
import tools.jackson.databind.JsonNode;

/** Chỉ số dùng để kiểm ngưỡng: mã, các thành phần và khoảng vật lý CFG-M06-03 ({@code null} khi chưa cấu hình). */
public record ThresholdIndicator(long id, String code, Set<String> componentCodes, JsonNode validRange) {}
